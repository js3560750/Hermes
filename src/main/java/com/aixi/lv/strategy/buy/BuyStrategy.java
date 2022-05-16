package com.aixi.lv.strategy.buy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.aixi.lv.config.BuyConfig;
import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.Box;
import com.aixi.lv.model.domain.HighFrequency;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.service.BoxService;
import com.aixi.lv.service.HttpService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.NumUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 买入策略
 */
@Component
@Slf4j
public class BuyStrategy {

    @Resource
    HttpService httpService;

    @Resource
    OrderService orderService;

    @Resource
    BoxService boxService;

    @Resource
    MailService mailService;

    @Resource
    PriceService priceService;

    @Resource
    OrderLifeManage orderLifeManage;

    private static final BigDecimal BUY_BOTTOM_FLOAT_RATE = new BigDecimal("0.991");
    private static final BigDecimal BUY_TOP_FLOAT_RATE = new BigDecimal("1.009");

    private static final BigDecimal BUY_HIGH_FREQUENCY_MAX_RATE = new BigDecimal("1.002");

    /**
     * 相似价格判断比例
     */
    private static final BigDecimal SAME_PRICE_RATE = new BigDecimal("0.008");

    /**
     * key : symbol+interval+id
     * value : true 执行，false 不执行
     */
    private static final ConcurrentHashMap<String, HighFrequency> BUY_TASK_MAP = new ConcurrentHashMap<>();

    /**
     * key:symbol+interval+id
     * value : 单任务已下单数
     */
    private static final ConcurrentHashMap<String, Integer> BUY_LIMIT_MAP = new ConcurrentHashMap<>();

    /**
     * 每小时单任务最大下单数
     */
    private static final Integer BUY_LIMIT_TIMES = 1;

    /**
     * 每小时调用清理一次
     */
    public void clearBuyLimitMap() {

        BUY_LIMIT_MAP.clear();
    }

    /**
     * 买入检查
     *
     * @param symbol
     * @param interval
     * @param limit
     * @param id
     */
    public void buy(Symbol symbol, Interval interval, Integer limit, Integer id) {

        String taskKey = buildTaskKey(symbol, interval, id);
        BUY_TASK_MAP.remove(taskKey);

        if (!httpService.testConnected()) {
            log.error(" 买入 | {} | 服务不通", taskKey);
            return;
        }

        // 查K线
        if (limit < 48) {
            limit = 48;
        }
        List<KLine> kLines = priceService.queryKLine(symbol, interval, limit);

        // 找箱体
        Result<Box> boxResult = boxService.findBox(kLines);

        if (!boxResult.getSuccess()) {
            log.info(" 买入 | {} | {}", taskKey, boxResult.getErrorMsg());
            return;
        }

        Box box = boxResult.getData();

        BigDecimal bottomPrice = box.getBottomPrice();
        BigDecimal topPrice = box.getTopPrice();

        BigDecimal newPrice = priceService.queryNewPrice(symbol);

        if (isBottomBreak(bottomPrice, newPrice, symbol, interval)) {

            log.info(" 买入 | {} | 开始高频扫描 | newPrice={} | bottom={} | minBreakPrice={} | top={}",
                taskKey, newPrice, bottomPrice, bottomPrice.multiply(BUY_HIGH_FREQUENCY_MAX_RATE), topPrice);

            // 开始高频扫描
            HighFrequency highFrequency = HighFrequency.builder()
                .scanFlag(Boolean.TRUE)
                .bottomPrice(bottomPrice)
                .topPrice(topPrice)
                .interval(interval)
                .limit(limit)
                .build();

            BUY_TASK_MAP.put(taskKey, highFrequency);

        } else {
            log.info(" 买入 | {} | 当前没有预警点 | newPrice={} | bottom={} | top={}",
                taskKey, newPrice, bottomPrice, topPrice);
        }

    }

    /**
     * 买入 高频扫描
     *
     * @param symbol
     * @param interval
     * @param id
     */
    public void buyHighFrequencyScan(Symbol symbol, Interval interval, Integer id) {

        String taskKey = buildTaskKey(symbol, interval, id);

        HighFrequency highFrequency = BUY_TASK_MAP.get(taskKey);

        if (highFrequency == null || Boolean.FALSE.equals(highFrequency.getScanFlag())) {
            return;
        }

        BigDecimal newPrice = priceService.queryNewPrice(symbol);

        BigDecimal bottomPrice = highFrequency.getBottomPrice();
        BigDecimal topPrice = highFrequency.getTopPrice();

        // 如果市场价大于箱底价格，且没大太多
        if (highFrequency.getScanFlag()
            && newPrice.compareTo(bottomPrice) >= 0
            && newPrice.compareTo(bottomPrice.multiply(BUY_HIGH_FREQUENCY_MAX_RATE)) <= 0) {

            Integer buyTimes = BUY_LIMIT_MAP.get(taskKey);
            if (buyTimes != null && buyTimes >= BUY_LIMIT_TIMES) {
                // 达到购买上限
                log.info(" 买入 | {} | 预警成功但达到购买上限次数 | newPrice={} | bottom={} | top={}",
                    taskKey, newPrice, bottomPrice, topPrice);
                BUY_TASK_MAP.remove(taskKey);
                return;
            }

            // 相似买单校验
            if (isSameBuy(symbol, newPrice, bottomPrice, topPrice)) {
                log.info(" 买入 | {} | 预警成功但已存在相似价格订单 | newPrice={} | bottom={} | top={}",
                    taskKey, newPrice, bottomPrice, topPrice);
                BUY_TASK_MAP.remove(taskKey);
                return;
            }

            // 【买入】
            // 买入数量计算
            BigDecimal buyQuantity = BuyConfig.BUY_AMOUNT.divide(newPrice, 8, RoundingMode.HALF_DOWN);
            // 买入下单
            OrderLife buyServer = orderService.limitBuyOrder(OrderSide.BUY, symbol, buyQuantity, newPrice);
            // Pair 状态更新
            buyServer.setTopPrice(topPrice);
            buyServer.setBottomPrice(bottomPrice);
            buyServer.setTaskKey(taskKey);
            buyServer.setInterval(highFrequency.getInterval());
            buyServer.setLimit(highFrequency.getLimit());
            orderLifeManage.putBuyOrder(buyServer);
            // 移出高频扫描
            BUY_TASK_MAP.remove(taskKey);
            // 购买限制记录
            Integer currentBuyTimes = BUY_LIMIT_MAP.getOrDefault(taskKey, 0);
            BUY_LIMIT_MAP.put(taskKey, currentBuyTimes + 1);
            log.info(" 买入 | {} | 买入成功 | buyPrice={} | newPrice={} | bottom={} | top={}",
                taskKey, buyServer.getBuyPrice(), newPrice, bottomPrice, topPrice);

        }
    }

    /**
     * 是否底部突破，既当前最新价处于箱底附近，并且最近几根K线的最低价都是上升
     *
     * @param bottomPrice 箱底价格
     * @param newPrice    最新市场价
     * @param symbol
     * @return
     */
    public Boolean isBottomBreak(BigDecimal bottomPrice, BigDecimal newPrice, Symbol symbol, Interval interval) {

        if (newPrice.compareTo(bottomPrice.multiply(BUY_BOTTOM_FLOAT_RATE)) >= 0
            && newPrice.compareTo(bottomPrice.multiply(BUY_TOP_FLOAT_RATE)) <= 0) {

            List<KLine> kLines = priceService.queryKLine(symbol, interval, 2);

            List<KLine> sortedKLine = kLines.stream()
                .sorted(Comparator.comparing(KLine::getOpeningTime))
                .collect(Collectors.toList());

            for (int i = 0; i < sortedKLine.size() - 1; i++) {
                if (sortedKLine.get(i).getMinPrice().compareTo(sortedKLine.get(i + 1).getMinPrice()) > 0) {
                    return Boolean.FALSE;
                }
            }

            return Boolean.TRUE;

        }

        return Boolean.FALSE;
    }

    private String buildTaskKey(Symbol symbol, Interval interval, Integer id) {
        return StringUtils.join(symbol.getCode(), "_", interval.getCode(), "_", id);
    }

    /**
     * 判断是否类似购买，如果存在类似的已成交的买单，则拒绝本次交易，防止过于相近的单子不停的重复下
     *
     * @param symbol
     * @param newPrice
     * @param bottomPrice
     * @param topPrice
     * @return
     */
    private Boolean isSameBuy(Symbol symbol, BigDecimal newPrice, BigDecimal bottomPrice, BigDecimal topPrice) {

        List<TradePair> pairList = orderLifeManage.getPairBySymbol(symbol);

        if (CollectionUtils.isEmpty(pairList)) {
            return Boolean.FALSE;
        }

        BigDecimal buyPrice = NumUtil.pricePrecision(symbol, newPrice);

        for (TradePair pair : pairList) {

            OrderLife buyOrder = pair.getBuyOrder();
            BigDecimal orderBuyPrice = buyOrder.getBuyPrice();
            BigDecimal orderBottomPrice = buyOrder.getBottomPrice();
            BigDecimal orderTopPrice = buyOrder.getTopPrice();

            if (isSamePrice(buyPrice, orderBuyPrice)
                && isSamePrice(bottomPrice, orderBottomPrice)
                && isSamePrice(topPrice, orderTopPrice)) {

                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * 相似价格
     *
     * @param numA
     * @param numB
     * @return
     */
    private Boolean isSamePrice(BigDecimal numA, BigDecimal numB) {

        BigDecimal diff = numA.subtract(numB).abs();

        if (diff.divide(numA, 8, RoundingMode.HALF_UP).compareTo(SAME_PRICE_RATE) > 0) {
            return Boolean.FALSE;
        }

        if (diff.divide(numB, 8, RoundingMode.HALF_UP).compareTo(SAME_PRICE_RATE) > 0) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

}

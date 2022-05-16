package com.aixi.lv.strategy.buy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.constant.TradePairStatus;
import com.aixi.lv.model.domain.Box;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.service.BoxService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.NumUtil;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 复购策略
 *
 * 当被止损卖出后，又重回止损价时，再买回来
 */
@Component
@Slf4j
public class ReBuyStrategy {

    @Resource
    OrderService orderService;

    @Resource
    BoxService boxService;

    @Resource
    PriceService priceService;

    @Resource
    OrderLifeManage orderLifeManage;

    /**
     * 最大复购次数
     */
    public static final Integer MAX_RE_BUY_TIMES = 2;

    /**
     * 至少得把手续费赚回来对吧
     */
    private static final BigDecimal LOSS_RE_BUY_RATE = new BigDecimal("0.997");

    private static final BigDecimal BUILD_BOX_TOP_RATE = new BigDecimal("1.01");

    /**
     * 复购检查
     *
     * @param symbol
     */
    public void reBuy(Symbol symbol) {

        List<TradePair> pairList = orderLifeManage.getPairBySymbol(symbol);

        if (CollectionUtils.isEmpty(pairList)) {
            return;
        }

        // 循环处理
        for (TradePair pair : pairList) {

            if (!isNeedCheck(pair)) {
                continue;
            }

            // 市场价
            BigDecimal newPrice = priceService.queryNewPrice(symbol);

            // 上次止损价（含手续费）
            BigDecimal lossPrice = pair.getLossOrder().getSellPrice().multiply(LOSS_RE_BUY_RATE);

            // 达到拐点
            if (tradingVolumeBuy(symbol, lossPrice, newPrice)) {
                // 复购 ,为避免上涨过快，买不到，这里采用市价单
                this.reBuyAction(symbol, pair, newPrice, Boolean.TRUE);
                log.info(" 复购 | V型复购Action | symbol={} | newPrice={} | lossPrice={}",
                    symbol, newPrice, lossPrice);
                continue;
            }

            // 重回上次止损价
            if (priceBuy(newPrice, pair, lossPrice)) {
                // 复购
                this.reBuyAction(symbol, pair, newPrice, Boolean.FALSE);
                log.info(" 复购 | 价格复购Action | symbol={} | newPrice={} | lossPrice={}",
                    symbol, newPrice, lossPrice);
                continue;
            }

            log.info(" 复购 | 未达处理价 | symbol={} | newPrice={} | lossPrice={}",
                symbol, newPrice, lossPrice);

        }

    }

    /**
     * 根据价格判断是否回购
     *
     * @param newPrice
     * @param pair
     * @param lossPrice
     * @return
     */
    private Boolean priceBuy(BigDecimal newPrice, TradePair pair, BigDecimal lossPrice) {

        LocalDateTime lossTime = pair.getLossTime();
        LocalDateTime nowTime = TimeUtil.now();

        // 市场价重回 上次止损价 并且距离上次止损已经过了180分钟了（避免反复止损复购）
        if (newPrice.compareTo(lossPrice) >= 0 && lossTime.plusMinutes(180).isBefore(nowTime)) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * 根据成交量是否激增判断拐点，是的话则回购
     *
     * @return
     */
    public Boolean tradingVolumeBuy(Symbol symbol, BigDecimal lossPrice, BigDecimal newPrice) {

        // 只在市场价持续下跌，低于上次止损价的时候，才有意义
        if (newPrice.compareTo(lossPrice) >= 0) {
            return Boolean.FALSE;
        }

        LocalDateTime endTime = TimeUtil.now();
        LocalDateTime startTime = endTime.minusMinutes(30);

        List<KLine> kLines = priceService.queryKLineByTime(symbol, Interval.MINUTE_1, 50, startTime, endTime);

        Integer size = kLines.size();

        if (kLines.get(size - 2).getMinPrice().compareTo(kLines.get(size - 3).getMinPrice()) > 0
            && kLines.get(size - 3).getMinPrice().compareTo(kLines.get(size - 4).getMinPrice()) < 0) {
            // 符合底部V型

            BigDecimal averageFallTradingVolume = this.getAverageFallTradingVolume(kLines);

            if (kLines.get(size - 3).getTradingVolume().compareTo(
                averageFallTradingVolume.multiply(new BigDecimal("3.00"))) > 0) {
                // V型底部的交易量，大于近期均值的3倍，则认为是交易量暴涨
                return Boolean.TRUE;
            }

        }

        return Boolean.FALSE;

    }

    /**
     * 获得近一段时间下降K线的平均交易量（排除了当前时间附近几分钟）
     *
     * @param kLines
     * @return
     */
    private BigDecimal getAverageFallTradingVolume(List<KLine> kLines) {

        Integer size = kLines.size();

        List<KLine> tempList = Lists.newArrayList();

        for (int i = 0; i < size - 6; i++) {
            tempList.add(kLines.get(i));
        }

        List<KLine> fallList = Lists.newArrayList();

        for (int i = tempList.size() - 1; i > 0; i--) {
            if (tempList.get(i).getMinPrice().compareTo(tempList.get(i - 1).getMinPrice()) < 0) {
                fallList.add(tempList.get(i));
            }
        }

        BigDecimal totalFall = BigDecimal.ZERO;
        for (KLine temp : fallList) {
            totalFall = totalFall.add(temp.getTradingVolume());
        }

        return totalFall.divide(new BigDecimal(fallList.size()), 8, RoundingMode.HALF_DOWN);
    }

    /**
     * 是否需要检查止损
     *
     * @param pair
     * @return
     */
    private Boolean isNeedCheck(TradePair pair) {

        TradePairStatus status = pair.getStatus();
        Integer reBuyTimes = pair.getReBuyTimes() != null ? pair.getReBuyTimes() : 0;
        LocalDateTime lossTime = pair.getLossTime();
        LocalDateTime now = TimeUtil.now();

        /**
         * 1、止损成交
         * 2、小于最大复购次数
         * 3、止损时间未超过7天
         */
        if (TradePairStatus.LOSS_DONE == status
            && reBuyTimes <= MAX_RE_BUY_TIMES
            && lossTime.plusDays(7).isAfter(now)) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private void reBuyAction(Symbol symbol, TradePair pair, BigDecimal newPrice, Boolean isV) {

        Box box = this.buildBox(symbol, newPrice, Interval.MINUTE_15, 96);
        Box box2 = this.buildBox(symbol, newPrice, Interval.MINUTE_5, 144);
        BigDecimal lossAmount = pair.getLossOrder().getCumulativeQuoteQty();
        OrderLife originBuyOrder = pair.getBuyOrder();

        // 【复购买入】

        OrderLife buyServer;
        if (isV) {
            // 市价单
            buyServer = orderService.marketBuyOrder(OrderSide.BUY, symbol, lossAmount);
            buyServer.setTaskKey("V型复购_" + symbol.getCode());
        } else {
            // 限价单
            BigDecimal buyQuantity = lossAmount.divide(newPrice, 8, RoundingMode.HALF_DOWN);
            buyServer = orderService.limitBuyOrder(OrderSide.BUY, symbol, buyQuantity, newPrice);
            buyServer.setTaskKey("价格复购_" + symbol.getCode());
        }

        // 都复购了，选取更宽范围的箱体
        buyServer.setTopPrice(NumUtil.getBiggerPrice(originBuyOrder.getTopPrice(),
            NumUtil.getBiggerPrice(box.getTopPrice(), box2.getTopPrice())));
        buyServer.setBottomPrice(NumUtil.getSmallerPrice(originBuyOrder.getBottomPrice(),
            NumUtil.getSmallerPrice(box.getBottomPrice(), box2.getBottomPrice())));
        // Pair 状态更新
        orderLifeManage.putReBuyOrder(pair.getBuyOrder().getOrderId(), buyServer);
    }

    /**
     * 构建出箱体
     *
     * @param symbol
     * @param newPrice
     * @param interval
     * @param limit
     * @return
     */
    private Box buildBox(Symbol symbol, BigDecimal newPrice, Interval interval, Integer limit) {

        List<KLine> kLines = priceService.queryKLine(symbol, interval, limit);

        // 找箱体
        Result<Box> boxResult = boxService.findBox(kLines);

        if (boxResult.getSuccess()) {

            Box box = boxResult.getData();

            // 箱底价格高于当前市场价，说明市场价过低，设置新的箱底价格稍微低一些，避免反复止损
            if (box.getBottomPrice().compareTo(newPrice) >= 0) {
                box.setBottomPrice(NumUtil.pricePrecision(symbol, newPrice.multiply(new BigDecimal("0.995"))));
            }

            return box;

        }

        //若没找到箱体，以市场价为箱底 ,市场价上涨1%为箱顶价格
        Box newBox = new Box();
        newBox.setBottomPrice(newPrice);
        newBox.setTopPrice(NumUtil.pricePrecision(symbol, newPrice.multiply(BUILD_BOX_TOP_RATE)));
        newBox.setSymbol(symbol);
        return newBox;
    }

}

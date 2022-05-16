package com.aixi.lv.strategy.warn;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.Box;
import com.aixi.lv.model.domain.HighFrequency;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.service.BoxService;
import com.aixi.lv.service.HttpService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 预警策略
 */
@Component
@Slf4j
public class EarlyWarningStrategy {

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

    private static final BigDecimal MIN_PRICE_BOTTOM_FLOAT_RATE = new BigDecimal("0.991");
    private static final BigDecimal MIN_PRICE_TOP_FLOAT_RATE = new BigDecimal("1.009");

    private static final BigDecimal HIGH_FREQUENCY_MAX_RATE = new BigDecimal("1.002");

    /**
     * 每半天邮件发送上限
     */
    private static final Integer MAIL_LIMIT_TIMES = 3;

    /**
     * key : symbol+interval+id
     * value : true 执行，false 不执行
     */
    private static final ConcurrentHashMap<String, HighFrequency> WARN_TASK_MAP = new ConcurrentHashMap<>();

    /**
     * key:symbol+interval+id
     * value : 已发送邮件次数
     */
    private static final ConcurrentHashMap<String, Integer> MAIL_LIMIT_MAP = new ConcurrentHashMap<>();

    public void clearMailLimitMap() {

        MAIL_LIMIT_MAP.clear();
    }

    /**
     * K线预警服务
     *
     * @param symbol
     * @param interval
     * @param id
     */
    public void kLinesEarlyWarning(Symbol symbol, Interval interval, Integer id) {

        String taskKey = buildTaskKey(symbol, interval, id);
        WARN_TASK_MAP.remove(taskKey);

        if (!httpService.testConnected()) {
            log.error(" 预警任务={} | 服务不通", taskKey);
            return;
        }

        // 查K线
        List<KLine> kLines = priceService.queryKLine(symbol, interval, 50);

        // 找箱体
        Result<Box> boxResult = boxService.findBox(kLines);

        if (!boxResult.getSuccess()) {
            log.info(" 预警任务={} | {}", taskKey, boxResult.getErrorMsg());
            return;
        }

        Box box = boxResult.getData();

        BigDecimal bottomPrice = box.getBottomPrice();
        BigDecimal topPrice = box.getTopPrice();

        BigDecimal newPrice = priceService.queryNewPrice(symbol);

        if (isBottomBreak(bottomPrice, newPrice, symbol, taskKey)) {

            // 开始高频扫描
            log.info(" 预警任务={} | 开始高频扫描 | newPrice={} | bottom={} | top={}",
                taskKey, newPrice, bottomPrice, topPrice);

            HighFrequency highFrequency = HighFrequency.builder()
                .scanFlag(Boolean.TRUE)
                .bottomPrice(bottomPrice)
                .topPrice(topPrice)
                .build();

            WARN_TASK_MAP.put(taskKey, highFrequency);

        } else {
            log.info(" 预警任务={} | 当前没有预警点 | newPrice={} | bottom={} | top={}",
                taskKey, newPrice, bottomPrice, topPrice);

        }

    }

    /**
     * 高频扫描找突破
     *
     * @param symbol
     * @param interval
     * @param id
     */
    public void kLinesEarlyWarningHighFrequency(Symbol symbol, Interval interval, Integer id) {

        String taskKey = buildTaskKey(symbol, interval, id);

        HighFrequency highFrequency = WARN_TASK_MAP.get(taskKey);

        if (highFrequency == null || Boolean.FALSE.equals(highFrequency.getScanFlag())) {
            return;
        }

        BigDecimal newPrice = priceService.queryNewPrice(symbol);

        BigDecimal bottomPrice = highFrequency.getBottomPrice();
        BigDecimal topPrice = highFrequency.getTopPrice();

        if (highFrequency.getScanFlag()
            && newPrice.compareTo(bottomPrice) >= 0
            && newPrice.compareTo(bottomPrice.multiply(HIGH_FREQUENCY_MAX_RATE)) <= 0) {

            List<KLine> kLines = priceService.queryKLine(symbol, Interval.MINUTE_3, 3);

            // 突破箱底，且没突破太远
            String title = symbol.getCode() + " 箱底突破";
            StringBuilder content = new StringBuilder();
            content.append("交易对 : " + symbol.getCode());
            content.append("\n");
            content.append("时间 : " + TimeUtil.getCurrentTime());
            content.append("\n");
            content.append("当前价格 : " + newPrice);
            content.append("\n");
            content.append("箱底价格 : " + bottomPrice);
            content.append("\n");
            content.append("箱顶价格 : " + topPrice);
            content.append("\n");
            content.append("预警任务编号 : " + taskKey);
            content.append("\n");
            content.append("\n");
            content.append("最近几条3分钟K线价格 : ");
            content.append("\n");
            for (KLine item : kLines) {
                content.append(TimeUtil.getTime(item.getOpeningTime()));
                content.append(" : ");
                content.append(item.getMinPrice());
                content.append("\n");
            }

            Integer mailTimes = MAIL_LIMIT_MAP.get(taskKey);
            if (mailTimes != null && mailTimes >= MAIL_LIMIT_TIMES) {
                log.info(" 预警任务={} | 预警成功但邮件发送达到上限次数 | 标题={} | 预警内容={} ", taskKey, title, content);
                WARN_TASK_MAP.remove(taskKey);
                return;
            }

            mailService.sendEmail(title, content.toString());
            log.info(" 预警任务={} | 预警成功 | 标题={} | 预警内容={} ", taskKey, title, content);
            Integer currentMailTimes = MAIL_LIMIT_MAP.getOrDefault(taskKey, 0);
            MAIL_LIMIT_MAP.put(taskKey, currentMailTimes + 1);
            WARN_TASK_MAP.remove(taskKey);
        }
    }

    private String buildTaskKey(Symbol symbol, Interval interval, Integer id) {
        return StringUtils.join(symbol.getCode(), interval.getCode(), id);
    }

    /**
     * 是否底部突破，既当前最新价处于箱底附近，并且最近几根K线的最低价都是上升
     *
     * @param bottomPrice
     * @param newPrice
     * @param symbol
     * @return
     */
    public Boolean isBottomBreak(BigDecimal bottomPrice, BigDecimal newPrice, Symbol symbol, String taskKey) {

        if (newPrice.compareTo(bottomPrice.multiply(MIN_PRICE_BOTTOM_FLOAT_RATE)) >= 0
            && newPrice.compareTo(bottomPrice.multiply(MIN_PRICE_TOP_FLOAT_RATE)) <= 0) {

            List<KLine> kLines = priceService.queryKLine(symbol, Interval.MINUTE_3, 3);

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
}

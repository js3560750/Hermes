package com.aixi.lv.strategy.indicator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.indicator.MACD;
import com.aixi.lv.service.BackTestCommonService;
import com.aixi.lv.service.BackTestOrderService;
import com.aixi.lv.service.IndicatorService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.MacdTradeConfig.IGNORE_DAY_MACD_SYMBOL_LIST;

/**
 * @author Js
 */
@Component
@Slf4j
public class MacdWarningStrategy {

    @Resource
    IndicatorService indicatorService;

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BackTestOrderService backTestOrderService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    @Resource
    BackTestCommonService backTestCommonService;

    @Resource
    MacdBuySellStrategy macdStrategy;

    public static final List<Symbol> SIGNAL_CHECK_LIST = Lists.newArrayList();

    static {
        SIGNAL_CHECK_LIST.add(Symbol.ETHUSDT);
    }

    /**
     * 核心逻辑：底部回升信号探测
     */
    public void warningDetect() {

        for (Symbol symbol : SIGNAL_CHECK_LIST) {

            // 4小时级别MACD检查
            //if (macdStrategy.isCur4HourMacdDown(symbol, "warningDetect")) {
            //    continue;
            //}
            //
            //if (isLowSignalByHour(symbol)) {
            //    log.warn(" MACD 底部回升信号 | 币种 = {} | 当前时间 = {}", symbol, TimeUtil.now());
            //}

            if (isLowSignalByFourHour(symbol)) {
                log.warn(" MACD 底部回升信号 | 币种 = {} | 当前时间 = {}", symbol, TimeUtil.now());
            }
        }
    }

    /**
     * MACD 处于底部回升
     *
     * @param symbol
     * @return
     */
    private Boolean isLowSignalByHour(Symbol symbol) {

        Double last1 = macdStrategy.getLast1HourMacd(symbol);
        Double last2 = macdStrategy.getLast2HourMacd(symbol);
        Double last3 = macdStrategy.getLast3HourMacd(symbol);
        Double last4 = macdStrategy.getLast4HourMacd(symbol);
        Double last5 = macdStrategy.getLast5HourMacd(symbol);

        MACD lastMacd = indicatorService.getMACD(symbol, Interval.HOUR_1);
        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);
        lastMacd.update(newPrice.doubleValue());
        double cur = lastMacd.get();

        double changeRate1 = (cur - last1) / Math.abs(last1);
        double changeRate2 = (last1 - last2) / Math.abs(last2);

        // 必须都是负的，否则 return false
        if (last5 >= 0 || cur >= 0) {
            return Boolean.FALSE;
        }

        if (last5 > last4 && last4 > last3 && last3 > last2 && last2 < last1 && last1 < cur) {
            // last2 最小
            if (changeRate1 > 0.12d && changeRate2 > 0.1d) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;

    }

    /**
     * MACD 处于底部回升
     *
     * @param symbol
     * @return
     */
    private Boolean isLowSignalByFourHour(Symbol symbol) {

        Double last1 = this.getLast1FourHourMacd(symbol);
        Double last2 = this.getLast2FourHourMacd(symbol);
        Double last3 = this.getLast3FourHourMacd(symbol);
        Double last4 = this.getLast4FourHourMacd(symbol);

        MACD lastMacd = indicatorService.getMACD(symbol, Interval.HOUR_4);
        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);
        lastMacd.update(newPrice.doubleValue());
        double cur = lastMacd.get();

        double changeRate1 = (cur - last1) / Math.abs(last1);
        double changeRate2 = (last1 - last2) / Math.abs(last2);

        // 必须都是负的，否则 return false
        if (last4 >= 0 || cur >= 0) {
            return Boolean.FALSE;
        }

        if (last4 > last3 && last3 > last2 && last2 < last1 && last1 < cur) {
            // last2 最小
            if (changeRate1 > 0.12d && changeRate2 > 0.1d) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;

    }

    private Boolean isDayMacdDown(Symbol symbol) {

        LocalDateTime nowTime = TimeUtil.now();
        LocalDateTime compareTime
            = LocalDateTime.of(nowTime.getYear(), nowTime.getMonth(), nowTime.getDayOfMonth(), 3, 0);

        if (nowTime.isAfter(compareTime)) {
            // 如果过了凌晨3点，检查今天的MACD
            if (macdStrategy.isTodayMacdDown(symbol, "warningDetect")) {
                return true;
            }
        } else {
            // 如果没过凌晨3点，检查昨天的MACD
            if (macdStrategy.isYesterdayMacdDown(symbol, "warningDetect")) {
                return true;
            }
        }

        return false;
    }

    public Double getLast1FourHourMacd(Symbol symbol) {

        Double macd = indicatorService.getMACD(symbol, Interval.HOUR_4).get();

        return macd;
    }

    /**
     * 获得上2小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast2FourHourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(4);
        LocalDateTime startTime = endTime.minusHours((500 - 1) * 4);

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_4, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上3小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast3FourHourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(8);
        LocalDateTime startTime = endTime.minusHours((500 - 1) * 4);

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_4, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上4小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast4FourHourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(12);
        LocalDateTime startTime = endTime.minusHours((500 - 1) * 4);

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_4, startTime, endTime).get();

        return macd;
    }
}

package com.aixi.lv.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aixi.lv.config.BackTestConfig;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_DATA_MAP;
import static com.aixi.lv.config.BackTestConfig.MINUTE_ONE_K_LINE_OPEN;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 */
@Component
@Slf4j
public class BackTestPriceService {

    public List<KLine> queryKLine(Symbol symbol, Interval interval, Integer limit) {

        try {

            String key = BackTestConfig.key(symbol, interval);
            Map<String, KLine> klineMap = BACK_TEST_DATA_MAP.get(key);
            if (MapUtils.isEmpty(klineMap)) {
                throw new RuntimeException("BackTestPriceService | queryKLine | klineMap isEmpty");
            }

            List<KLine> list = new ArrayList<>(limit);

            LocalDateTime curTime = this.transferCurTime(THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(),
                interval);

            for (int i = 0; i < limit; i++) {

                KLine kLine = klineMap.get(curTime.toString());
                curTime = lastTime(curTime, interval);

                if (kLine != null) {
                    list.add(kLine);
                }
            }

            List<KLine> sortedList = list.stream()
                .sorted(Comparator.comparing(KLine::getOpeningTime))
                .collect(Collectors.toList());

            return sortedList;

        } catch (Exception e) {
            log.error(
                String.format(
                    " BackTestPriceService | queryKLine error | curBackTestComputeTime = %s | symbol = %s | interval ="
                        + " %s",
                    THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(), symbol, interval), e);
            throw e;
        }

    }

    public List<KLine> queryKLineByTime(Symbol symbol, Interval interval, Integer limit, LocalDateTime startTime,
        LocalDateTime endTime) {

        try {

            String key = BackTestConfig.key(symbol, interval);
            Map<String, KLine> klineMap = BACK_TEST_DATA_MAP.get(key);
            if (MapUtils.isEmpty(klineMap)) {
                throw new RuntimeException("BackTestPriceService | queryKLineByTime | klineMap isEmpty");
            }

            List<KLine> list = new ArrayList<>(limit);

            LocalDateTime curTime = this.transferCurTime(startTime, interval);

            for (int i = 0; i < limit; i++) {

                KLine kLine = klineMap.get(curTime.toString());
                curTime = nextTime(curTime, interval);

                if (kLine != null) {
                    list.add(kLine);
                }

                if (curTime.isAfter(endTime)) {
                    break;
                }
            }

            List<KLine> sortedList = list.stream()
                .sorted(Comparator.comparing(KLine::getOpeningTime))
                .collect(Collectors.toList());

            return sortedList;

        } catch (Exception e) {
            log.error(
                String.format(
                    " BackTestPriceService | queryKLineByTime error | curBackTestComputeTime = %s | interval = %s | "
                        + "symbol = %s",
                    THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(), interval, symbol), e);
            throw e;
        }

    }

    public BigDecimal queryNewPrice(Symbol symbol) {

        try {

            if (BackTestConfig.MINUTE_ONE_K_LINE_OPEN) {
                // 开启 1 分钟级 K线
                String key = BackTestConfig.key(symbol, Interval.MINUTE_1);
                Map<String, KLine> klineMap = BACK_TEST_DATA_MAP.get(key);
                if (MapUtils.isEmpty(klineMap)) {
                    throw new RuntimeException("BackTestPriceService | queryNewPrice | klineMap isEmpty");
                }

                // 查最新价，要取上一个1分钟区间的收盘价，既当前时间的开盘价（最新价）
                LocalDateTime curTime = THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime().minusMinutes(1);

                KLine kLine = klineMap.get(curTime.toString());

                return kLine.getClosingPrice();

            } else {

                // 读 5分钟级的 K线
                // 自然时间 10点0分5秒 对应的Map价格需要看 9点55分 的closingPrice

                String key = BackTestConfig.key(symbol, Interval.MINUTE_5);
                Map<String, KLine> klineMap = BACK_TEST_DATA_MAP.get(key);
                if (MapUtils.isEmpty(klineMap)) {
                    throw new RuntimeException("BackTestPriceService | queryNewPrice | klineMap isEmpty");
                }

                // 查最新价，要取上一个5分钟区间的收盘价，既当前时间的开盘价（最新价）
                LocalDateTime curTime = THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime().minusMinutes(5);

                KLine kLine = klineMap.get(curTime.toString());

                return kLine.getClosingPrice();

            }

        } catch (Exception e) {
            log.error(
                String.format(
                    " BackTestPriceService | queryNewPrice error | curBackTestComputeTime = %s | interval = %s | "
                        + "symbol = %s",
                    THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(), Interval.MINUTE_5, symbol), e);
            throw e;
        }

    }

    private LocalDateTime transferCurTime(LocalDateTime cur, Interval interval) {

        if (Interval.MINUTE_1 == interval) {
            return cur;
        }

        if (Interval.MINUTE_5 == interval) {
            return LocalDateTime.of(cur.getYear(), cur.getMonth(), cur.getDayOfMonth(), cur.getHour(),
                (cur.getMinute() / 5) * 5);
        }

        if (Interval.HOUR_1 == interval) {
            return LocalDateTime.of(cur.getYear(), cur.getMonth(), cur.getDayOfMonth(), cur.getHour(), 0);
        }

        if (Interval.HOUR_4 == interval) {
            return LocalDateTime.of(cur.getYear(), cur.getMonth(), cur.getDayOfMonth(), (cur.getHour() / 4) * 4, 0);
        }

        if (Interval.DAY_1 == interval) {
            return LocalDateTime.of(cur.getYear(), cur.getMonth(), cur.getDayOfMonth(), 0, 0);
        }

        throw new RuntimeException("不支持的类型");
    }

    private LocalDateTime lastTime(LocalDateTime curTime, Interval interval) {

        if (Interval.MINUTE_1 == interval) {
            return curTime.minusMinutes(1);
        }

        if (Interval.MINUTE_5 == interval) {
            return curTime.minusMinutes(5);
        }

        if (Interval.HOUR_1 == interval) {
            return curTime.minusHours(1);
        }

        if (Interval.HOUR_4 == interval) {
            return curTime.minusHours(4);
        }

        if (Interval.DAY_1 == interval) {
            return curTime.minusDays(1);
        }

        throw new RuntimeException("不支持的类型");
    }

    private LocalDateTime nextTime(LocalDateTime curTime, Interval interval) {

        if (Interval.MINUTE_1 == interval) {
            return curTime.plusMinutes(1);
        }

        if (Interval.MINUTE_5 == interval) {
            return curTime.plusMinutes(5);
        }

        if (Interval.HOUR_1 == interval) {
            return curTime.plusHours(1);
        }

        if (Interval.HOUR_4 == interval) {
            return curTime.plusHours(4);
        }

        if (Interval.DAY_1 == interval) {
            return curTime.plusDays(1);
        }

        throw new RuntimeException("不支持的类型");
    }

}

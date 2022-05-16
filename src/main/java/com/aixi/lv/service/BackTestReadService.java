package com.aixi.lv.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.BackTestData;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.util.RamUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 读txt加载K线数据
 */
@Component
@Slf4j
public class BackTestReadService {

    public static final String FILE_PREFIX = "/Users/jinsong/project/money/backtest/";

    /**
     * 加载天级K线
     *
     * @param symbol
     */
    public Map<String, KLine> day(Symbol symbol) {

        return this.readFromFile(symbol, Interval.DAY_1);

    }

    public Map<String, KLine> minuteFive(Symbol symbol) {

        return this.readFromFile(symbol, Interval.MINUTE_5);

    }

    public Map<String, KLine> hourOne(Symbol symbol) {

        return this.readFromFile(symbol, Interval.HOUR_1);

    }

    public Map<String, KLine> hourFour(Symbol symbol) {

        return this.readFromFile(symbol, Interval.HOUR_4);

    }

    public Map<String, KLine> readFromFile(Symbol symbol, Interval interval) {

        String currency = StringUtils.substringBefore(symbol.getCode(), "USDT");
        String fileName = FILE_PREFIX + currency + "/" + symbol.getCode() + "_" + interval.getCode() + ".txt";

        BufferedReader reader = null;

        Map<String, KLine> map = new HashMap<>();

        try {

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            String content;

            do {
                content = reader.readLine();
                BackTestData back = JSON.parseObject(content, BackTestData.class);

                if (content == null || back == null) {
                    System.out.println(symbol.getCode() + interval.getCode() + " 内存大小 : " + RamUtil.getRamSize(map));
                    return map;
                }

                KLine kLine = KLine.builder()
                    .symbol(symbol)
                    .openingTime(back.getOpeningTime())
                    .closingPrice(back.getClosingPrice())
                    .tradingVolume(back.getTradingVolume())
                    .build();

                map.put(kLine.getOpeningTime().toString(), kLine);

            } while (content != null);

        } catch (Exception e) {
            log.error("readFromFile", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("readFromFile", e);
            }
        }

        System.out.println(symbol.getCode() + interval.getCode() + " 内存大小 : " + RamUtil.getRamSize(map));
        return map;

    }

    public void checkData(Symbol symbol, Interval interval) {

        String currency = StringUtils.substringBefore(symbol.getCode(), "USDT");
        String fileName = FILE_PREFIX + currency + "/" + symbol.getCode() + "_" + interval.getCode() + ".txt";

        BufferedReader reader = null;

        List<KLine> list = Lists.newArrayList();

        try {

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            String content;

            do {
                content = reader.readLine();
                BackTestData back = JSON.parseObject(content, BackTestData.class);

                if (content == null || back == null) {
                    break;
                }

                KLine kLine = KLine.builder()
                    .symbol(symbol)
                    .openingTime(back.getOpeningTime())
                    .closingPrice(back.getClosingPrice())
                    .tradingVolume(back.getTradingVolume())
                    .build();

                list.add(kLine);

            } while (content != null);

        } catch (Exception e) {
            log.error("readFromFile", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("readFromFile", e);
            }
        }

        List<KLine> sortedList = list.stream()
            .sorted(Comparator.comparing(KLine::getOpeningTime))
            .collect(Collectors.toList());

        LocalDateTime tempTime = sortedList.get(0).getOpeningTime();

        // 最后一个不检查了
        for (int i = 0; i < sortedList.size()-1; i++) {
            if (!tempTime.isEqual(sortedList.get(i).getOpeningTime())) {
                throw new RuntimeException("检查出异常" + symbol + " " + interval + "缺失的时间是" + tempTime);
            }
            tempTime = this.nextTime(tempTime, interval);
        }
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

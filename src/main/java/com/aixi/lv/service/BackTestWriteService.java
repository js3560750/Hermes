package com.aixi.lv.service;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.BackTestData;
import com.aixi.lv.model.domain.KLine;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;

/**
 * @author Js
 *
 * 查K线数据并写入txt
 */
@Component
@Slf4j
public class BackTestWriteService {

    @Resource
    PriceService priceService;

    @Resource
    BackTestReadService backTestReadService;

    private static final String FILE_PREFIX = "/Users/jinsong/project/money/backtest/";

    private static final LocalDateTime DAY_INIT_TIME = LocalDateTime.of(2022, 1, 1, 0, 0);

    private static final LocalDateTime FOUR_HOUR_INIT_TIME = LocalDateTime.of(2022, 1, 1, 0, 0);

    private static final LocalDateTime ONE_HOUR_INIT_TIME = LocalDateTime.of(2022, 1, 1, 0, 0);

    private static final LocalDateTime FIVE_MINUTE_INIT_TIME = LocalDateTime.of(2022, 1, 1, 0, 0);

    private static final LocalDateTime ONE_MINUTE_INIT_TIME = LocalDateTime.of(2022, 3, 1, 0, 0);

    /**
     * 准备所有K线数据
     *
     * 从交易对上市日期到此时
     *
     * @param symbol
     */
    public void initData(Symbol symbol) {
        this.initData(symbol, symbol.getSaleDate().atStartOfDay(), LocalDateTime.now());
    }

    /**
     * 准备所有K线数据
     *
     * @param symbol
     * @param initTime
     * @param finalTime
     */
    public void initData(Symbol symbol, LocalDateTime initTime, LocalDateTime finalTime) {

        this.day(symbol, initTime, finalTime);
        backTestReadService.checkData(symbol, Interval.DAY_1);

        this.hourFour(symbol, initTime, finalTime);
        backTestReadService.checkData(symbol, Interval.HOUR_4);

        this.hourOne(symbol, initTime, finalTime);
        backTestReadService.checkData(symbol, Interval.HOUR_1);

        this.minuteFive(symbol, initTime, finalTime);
        backTestReadService.checkData(symbol, Interval.MINUTE_5);

        this.minuteOne(symbol, initTime, finalTime);
        backTestReadService.checkData(symbol, Interval.MINUTE_1);
    }

    /**
     * 天级K线
     *
     * @param symbol
     * @param initTime
     * @param finalTime
     */
    public void day(Symbol symbol, LocalDateTime initTime, LocalDateTime finalTime) {

        if (initTime.isBefore(DAY_INIT_TIME)) {
            initTime = DAY_INIT_TIME;
        }

        // 开始时间，小时数必须设置成8
        initTime = LocalDateTime.of(initTime.getYear(), initTime.getMonth(), initTime.getDayOfMonth(), 8, 0);

        int diffYears = (int)((finalTime.toLocalDate().toEpochDay() - initTime.toLocalDate().toEpochDay()) / 365 + 1);

        for (int i = 0; i < diffYears; i++) {

            LocalDateTime startTime = initTime;
            LocalDateTime endTime = startTime.plusDays((365 - 1) * 1);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nowDate = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0);
            LocalDateTime endTimeDate = LocalDateTime.of(endTime.getYear(), endTime.getMonth(), endTime.getDayOfMonth(),
                0, 0);
            if (!endTimeDate.isBefore(nowDate)) {
                // 不能超过当天，当天的数据还没跑完，不准
                endTime = now.minusDays(1);
            }

            // 查K线 一年的数据量
            List<KLine> kLines
                = priceService.queryKLineByTime(symbol, Interval.DAY_1, 365, startTime, endTime);

            initTime = endTime.plusDays(1);

            List<KLine> fillKLines = this.fillKLine(symbol, kLines, Interval.DAY_1, startTime, endTime);

            this.writeToFile(symbol, fillKLines, Interval.DAY_1);

        }

    }

    /**
     * 4小时级K线
     *
     * @param symbol
     * @param initTime
     * @param finalTime
     */
    public void hourFour(Symbol symbol, LocalDateTime initTime, LocalDateTime finalTime) {

        if (initTime.isBefore(FOUR_HOUR_INIT_TIME)) {
            initTime = FOUR_HOUR_INIT_TIME;
        }

        int diffDays = (int)(finalTime.toLocalDate().toEpochDay() - initTime.toLocalDate().toEpochDay());

        int circulation = (diffDays / 150) + 1;

        for (int i = 0; i < circulation; i++) {

            LocalDateTime startTime = initTime;
            LocalDateTime endTime = startTime.plusHours((6 * 150 - 1) * 4);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tempHour = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(),
                0).minusHours(4);
            LocalDateTime endTimeDate = LocalDateTime.of(endTime.getYear(), endTime.getMonth(), endTime.getDayOfMonth(),
                endTime.getHour(), 0);
            if (!endTimeDate.isBefore(tempHour)) {
                // 不能超过当期时间，当期时间的数据还没跑完，不准
                endTime = now.minusHours(4);
            }

            // 查K线 一天的数据量
            List<KLine> kLines
                = priceService.queryKLineByTime(symbol, Interval.HOUR_4, 6 * 150, startTime, endTime);

            initTime = endTime.plusHours(4);

            List<KLine> fillKLines = this.fillKLine(symbol, kLines, Interval.HOUR_4, startTime, endTime);

            this.writeToFile(symbol, fillKLines, Interval.HOUR_4);

        }
    }

    /**
     * 1小时级K线
     *
     * @param symbol
     * @param initTime
     * @param finalTime
     */
    public void hourOne(Symbol symbol, LocalDateTime initTime, LocalDateTime finalTime) {

        if (initTime.isBefore(ONE_HOUR_INIT_TIME)) {
            initTime = ONE_HOUR_INIT_TIME;
        }

        int diffDays = (int)(finalTime.toLocalDate().toEpochDay() - initTime.toLocalDate().toEpochDay());

        int circulation = (diffDays / 40) + 1;

        for (int i = 0; i < circulation; i++) {

            LocalDateTime startTime = initTime;
            LocalDateTime endTime = startTime.plusHours((24 * 40 - 1) * 1);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tempHour = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(),
                0).minusHours(1);
            LocalDateTime endTimeDate = LocalDateTime.of(endTime.getYear(), endTime.getMonth(), endTime.getDayOfMonth(),
                endTime.getHour(), 0);
            if (!endTimeDate.isBefore(tempHour)) {
                // 不能超过当期时间，当期时间的数据还没跑完，不准
                endTime = now.minusHours(1);
            }

            // 查K线 一天的数据量
            List<KLine> kLines
                = priceService.queryKLineByTime(symbol, Interval.HOUR_1, 24 * 40, startTime, endTime);

            initTime = endTime.plusHours(1);

            List<KLine> fillKLines = this.fillKLine(symbol, kLines, Interval.HOUR_1, startTime, endTime);

            this.writeToFile(symbol, fillKLines, Interval.HOUR_1);

        }
    }

    /**
     * 5分钟级K线
     *
     * @param symbol
     * @param initTime
     * @param finalTime
     */
    public void minuteFive(Symbol symbol, LocalDateTime initTime, LocalDateTime finalTime) {

        if (initTime.isBefore(FIVE_MINUTE_INIT_TIME)) {
            initTime = FIVE_MINUTE_INIT_TIME;
        }

        int diffDays = (int)(finalTime.toLocalDate().toEpochDay() - initTime.toLocalDate().toEpochDay());

        int circulation = (diffDays / 3) + 1;

        for (int i = 0; i < circulation; i++) {

            LocalDateTime startTime = initTime;
            LocalDateTime endTime = startTime.plusMinutes((288 * 3 - 1) * 5);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime temp = now.minusMinutes(5);
            if (!endTime.isBefore(temp)) {
                // 不能超过当期时间，当期时间的数据还没跑完，不准
                endTime = now.minusMinutes(5);
            }

            // 查K线 一天的数据量
            List<KLine> kLines
                = priceService.queryKLineByTime(symbol, Interval.MINUTE_5, 288 * 3, startTime, endTime);

            initTime = endTime.plusMinutes(5);

            List<KLine> fillKLines = this.fillKLine(symbol, kLines, Interval.MINUTE_5, startTime, endTime);

            this.writeToFile(symbol, fillKLines, Interval.MINUTE_5);

        }
    }

    /**
     * 1分钟级K线
     *
     * @param symbol
     */
    public void minuteOne(Symbol symbol, LocalDateTime initTime, LocalDateTime finalTime) {

        if (initTime.isBefore(ONE_MINUTE_INIT_TIME)) {
            initTime = ONE_MINUTE_INIT_TIME;
        }

        int diffDays = (int)(finalTime.toLocalDate().toEpochDay() - initTime.toLocalDate().toEpochDay());

        for (int i = 0; i < diffDays; i++) {

            // 一天的数据量
            for (int j = 0; j < 2; j++) {

                LocalDateTime startTime = initTime;
                LocalDateTime endTime = startTime.plusMinutes((720 - 1) * 1);

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime temp = now.minusMinutes(5);
                if (!endTime.isBefore(temp)) {
                    // 不能超过当期时间，当期时间的数据还没跑完，不准
                    endTime = now.minusMinutes(5);
                }

                // 查K线
                List<KLine> kLines
                    = priceService.queryKLineByTime(symbol, Interval.MINUTE_1, 720, startTime, endTime);

                initTime = endTime.plusMinutes(1);

                List<KLine> fillKLines = this.fillKLine(symbol, kLines, Interval.MINUTE_1, startTime, endTime);

                this.writeToFile(symbol, fillKLines, Interval.MINUTE_1);

            }

        }
    }

    /**
     * 写到文件里
     *
     * @param symbol
     * @param kLineList
     */
    public void writeToFile(Symbol symbol, List<KLine> kLineList, Interval interval) {

        if (CollectionUtils.isEmpty(kLineList)) {
            return;
        }

        if (OPEN) {
            throw new RuntimeException("回测开关处于开启状态");
        }

        String currency = StringUtils.substringBefore(symbol.getCode(), "USDT");
        String fileName = FILE_PREFIX + currency + "/" + symbol.getCode() + "_" + interval.getCode() + ".txt";

        List<BackTestData> backList = new ArrayList<>(kLineList.size());

        for (KLine kline : kLineList) {

            BackTestData backTestData = BackTestData.builder()
                .symbol(kline.getSymbol())
                .closingPrice(kline.getClosingPrice())
                .tradingVolume(kline.getTradingVolume())
                .build();

            if (Interval.DAY_1 == interval) {
                LocalDateTime openingTime = kline.getOpeningTime();
                backTestData.setOpeningTime(
                    LocalDateTime.of(openingTime.getYear(), openingTime.getMonth(), openingTime.getDayOfMonth(), 0, 0));
            } else {
                backTestData.setOpeningTime(kline.getOpeningTime());
            }

            backList.add(backTestData);
        }

        List<BackTestData> writeList = backList.stream()
            .sorted(Comparator.comparing(BackTestData::getOpeningTime))
            .collect(Collectors.toList());

        System.out.println(symbol + "     " + interval + "     " + writeList.get(0).getOpeningTime());

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
            for (BackTestData temp : writeList) {
                out.write(JSON.toJSONString(temp) + "\r");
            }
        } catch (Exception e) {
            log.error("writeToFile", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                log.error("writeToFile | close ", e);
            }
        }
    }

    /**
     * 如果有缺失的K线，就用上一次的数据补
     *
     * @param symbol
     * @param kLines
     * @param interval
     * @param startTime
     * @return
     */
    public List<KLine> fillKLine(Symbol symbol, List<KLine> kLines, Interval interval,
        LocalDateTime startTime, LocalDateTime endTime) {

        if (CollectionUtils.isEmpty(kLines)) {
            return Lists.newArrayList();
        }

        try {

            if (!kLines.get(0).getOpeningTime().isEqual(startTime)) {
                if (Interval.DAY_1 == interval) {
                    LocalDateTime first = kLines.get(0).getOpeningTime();
                    LocalDateTime dayTime = LocalDateTime.of(first.getYear(), first.getMonth(), first.getDayOfMonth(),
                        0,
                        0);
                    if (!dayTime.isEqual(startTime)) {
                        throw new RuntimeException("第一个就不对");
                    }
                } else {
                    throw new RuntimeException("第一个就不对");
                }

            }

            List<KLine> fillList = new ArrayList<>();

            KLine backup = kLines.get(0);
            LocalDateTime curTime = kLines.get(0).getOpeningTime();
            Integer index = 0;

            try {

                while (!curTime.isAfter(endTime)) {

                    if (kLines.get(index).getOpeningTime().isEqual(curTime)) {
                        backup = kLines.get(index);
                        fillList.add(kLines.get(index));
                        index++;
                    } else {
                        KLine tempKLine = new KLine();
                        tempKLine.setOpeningTime(curTime);
                        tempKLine.setSymbol(symbol);
                        tempKLine.setClosingPrice(backup.getClosingPrice());
                        tempKLine.setTradingVolume(backup.getTradingVolume());
                        fillList.add(tempKLine);
                    }

                    curTime = nextTime(curTime, interval);

                }

            } catch (Exception e) {
                throw e;
            }

            return fillList;

        } catch (Exception e) {
            throw e;
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

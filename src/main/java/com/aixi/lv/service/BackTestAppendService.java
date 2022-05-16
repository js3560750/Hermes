package com.aixi.lv.service;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.BackTestData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.service.BackTestReadService.FILE_PREFIX;

/**
 * @author Js
 *
 * 追加写数据
 */
@Component
@Slf4j
public class BackTestAppendService {

    @Resource
    BackTestReadService backTestReadService;

    @Resource
    BackTestWriteService backTestWriteService;

    /**
     * 追加回测数据
     *
     * @param symbol
     */
    public void appendData(Symbol symbol) {

        if (OPEN) {
            throw new RuntimeException("追加数据失败，回测开关处于开启状态");
        }

        if (!symbol.getBackFlag()) {
            return;
        }

        LocalDateTime finalTime = LocalDateTime.now();

        this.day(symbol, finalTime);
        backTestReadService.checkData(symbol, Interval.DAY_1);

        this.hourFour(symbol, finalTime);
        backTestReadService.checkData(symbol, Interval.HOUR_4);

        this.hourOne(symbol, finalTime);
        backTestReadService.checkData(symbol, Interval.HOUR_1);

        this.minuteFive(symbol, finalTime);
        backTestReadService.checkData(symbol, Interval.MINUTE_5);

    }

    public void appendMinuteOne(Symbol symbol){

        if (OPEN) {
            throw new RuntimeException("追加数据失败，回测开关处于开启状态");
        }

        if (!symbol.getBackFlag()) {
            return;
        }

        LocalDateTime finalTime = LocalDateTime.now();

        this.minuteOne(symbol, finalTime);
        backTestReadService.checkData(symbol, Interval.MINUTE_1);
    }

    /**
     * 天级K线
     *
     * @param symbol
     * @param finalTime
     */
    public void day(Symbol symbol, LocalDateTime finalTime) {

        LocalDateTime lastOpenTime = this.readLastLine(symbol, Interval.DAY_1);

        if (!lastOpenTime.isBefore(finalTime)) {
            return;
        }

        LocalDateTime initTime = lastOpenTime.plusDays(1);

        backTestWriteService.day(symbol, initTime, finalTime);

    }

    /**
     * 4小时级K线
     *
     * @param symbol
     * @param finalTime
     */
    public void hourFour(Symbol symbol, LocalDateTime finalTime) {

        LocalDateTime lastOpenTime = this.readLastLine(symbol, Interval.HOUR_4);

        if (!lastOpenTime.isBefore(finalTime)) {
            return;
        }

        LocalDateTime initTime = lastOpenTime.plusHours(4);

        backTestWriteService.hourFour(symbol, initTime, finalTime);

    }

    /**
     * 1小时级K线
     *
     * @param symbol
     * @param finalTime
     */
    public void hourOne(Symbol symbol, LocalDateTime finalTime) {

        LocalDateTime lastOpenTime = this.readLastLine(symbol, Interval.HOUR_1);

        if (!lastOpenTime.isBefore(finalTime)) {
            return;
        }

        LocalDateTime initTime = lastOpenTime.plusHours(1);

        backTestWriteService.hourOne(symbol, initTime, finalTime);
    }

    /**
     * 5分钟级K线
     *
     * @param symbol
     * @param finalTime
     */
    public void minuteFive(Symbol symbol, LocalDateTime finalTime) {

        LocalDateTime lastOpenTime = this.readLastLine(symbol, Interval.MINUTE_5);

        if (!lastOpenTime.isBefore(finalTime)) {
            return;
        }

        if (lastOpenTime.isAfter(LocalDateTime.now().minusMinutes(30))) {
            return;
        }

        LocalDateTime initTime = lastOpenTime.plusMinutes(5);

        backTestWriteService.minuteFive(symbol, initTime, finalTime);
    }

    /**
     * 1分钟级K线
     *
     * @param symbol
     * @param finalTime
     */
    public void minuteOne(Symbol symbol, LocalDateTime finalTime) {

        LocalDateTime lastOpenTime = this.readLastLine(symbol, Interval.MINUTE_1);

        if (!lastOpenTime.isBefore(finalTime)) {
            return;
        }

        if (lastOpenTime.isAfter(LocalDateTime.now().minusMinutes(30))) {
            return;
        }

        LocalDateTime initTime = lastOpenTime.plusMinutes(1);

        backTestWriteService.minuteOne(symbol, initTime, finalTime);
    }

    /**
     * 读取最后一行数据
     *
     * @param symbol
     * @param interval
     * @return
     */
    public LocalDateTime readLastLine(Symbol symbol, Interval interval) {

        String currency = StringUtils.substringBefore(symbol.getCode(), "USDT");
        String fileName = FILE_PREFIX + currency + "/" + symbol.getCode() + "_" + interval.getCode() + ".txt";

        File file = new File(fileName);

        String lastLine;

        Integer times = 0;
        Integer maxTimes = 1;

        try (ReversedLinesFileReader reversedLinesReader = new ReversedLinesFileReader(file,
            Charset.forName("UTF-8"))) {

            while (true) {

                if (times > maxTimes) {
                    throw new RuntimeException("末尾空行超出限制");
                }

                lastLine = reversedLinesReader.readLine();

                if (lastLine == null || StringUtils.isEmpty(lastLine)) {
                    times++;
                    continue;
                }

                BackTestData back = JSON.parseObject(lastLine, BackTestData.class);

                return back.getOpeningTime();
            }

        } catch (Exception e) {
            log.error("readLastLine error, msg:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }
}

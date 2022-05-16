package com.aixi.lv.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.indicator.BOLL;
import com.aixi.lv.model.indicator.MACD;
import com.aixi.lv.model.indicator.RSI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;

/**
 * @author Js
 */
@Component
@Slf4j
public class IndicatorService {

    @Resource
    PriceFaceService priceFaceService;

    /**
     * 查询当前时间，最近一根K线的 MACD
     *
     * @param symbol
     * @param interval
     * @return
     */
    public MACD getMACD(Symbol symbol, Interval interval) {

        try {
            List<KLine> kLines = priceFaceService.queryKLine(symbol, interval, 500);

            List<Double> closingPrices = kLines.stream()
                .map(kLine -> kLine.getClosingPrice().doubleValue())
                .collect(Collectors.toList());

            KLine kLine = kLines.get(kLines.size() - 2);
            LocalDateTime openingTime = kLine.getOpeningTime();

            MACD macd = new MACD(closingPrices, openingTime, symbol, interval, 12, 26, 9);

            return macd;
        } catch (Exception e) {
            log.error("异常币种 = {}", symbol, e);
            throw e;
        }
    }

    /**
     * 根据时间查MACD
     *
     * @param symbol
     * @param interval
     * @param startTime
     * @param endTime
     * @return
     */
    public MACD getMACDByTime(Symbol symbol, Interval interval, LocalDateTime startTime, LocalDateTime endTime) {

        try {

            List<KLine> kLines = priceFaceService.queryKLineByTime(symbol, interval, 500, startTime, endTime);

            List<Double> closingPrices = kLines.stream()
                .map(kLine -> kLine.getClosingPrice().doubleValue())
                .collect(Collectors.toList());

            KLine kLine = kLines.get(kLines.size() - 2);
            LocalDateTime openingTime = kLine.getOpeningTime();

            MACD macd = new MACD(closingPrices, openingTime, symbol, interval, 12, 26, 9);

            return macd;

        } catch (Exception e) {
            log.error("异常币种 = {}", symbol, e);
            throw e;
        }
    }

    /**
     * 查询当前时间，最近一根K线的 RSI
     *
     * @param symbol
     * @param interval
     * @return
     */
    public RSI getRSI(Symbol symbol, Interval interval) {

        List<KLine> kLines = priceFaceService.queryKLine(symbol, interval, 500);

        List<Double> closingPrices = kLines.stream()
            .map(kLine -> kLine.getClosingPrice().doubleValue())
            .collect(Collectors.toList());

        KLine kLine = kLines.get(kLines.size() - 2);
        LocalDateTime openingTime = kLine.getOpeningTime();

        RSI rsi = new RSI(closingPrices, openingTime, symbol, interval, 12);

        return rsi;

    }

    /**
     * 只适用于5分钟线
     *
     * 查询当前时间，最近一根K线的 BOLL
     *
     * @param symbol
     * @param interval
     * @return
     */
    public BOLL getBOLL(Symbol symbol, Interval interval) {

        if (Interval.MINUTE_5 != interval) {
            throw new RuntimeException(" BOLL 只支持5分钟线");
        }

        List<KLine> kLines = priceFaceService.queryKLine(symbol, interval, 50);

        List<Double> closingPrices = kLines.stream()
            .map(kLine -> kLine.getClosingPrice().doubleValue())
            .collect(Collectors.toList());

        KLine kLine = kLines.get(kLines.size() - 2);
        LocalDateTime openingTime = kLine.getOpeningTime();

        BOLL boll = new BOLL(closingPrices, openingTime, symbol, interval, 20);

        return boll;

    }

    /**
     * 根据时间取 BOLL
     *
     * @param symbol
     * @param interval
     * @param startTime
     * @param endTime
     * @return
     */
    public BOLL getBOLLByTime(Symbol symbol, Interval interval, LocalDateTime startTime, LocalDateTime endTime) {

        if (Interval.MINUTE_5 != interval) {
            throw new RuntimeException(" BOLL 只支持5分钟线");
        }

        List<KLine> kLines = priceFaceService.queryKLineByTime(symbol, interval, 500, startTime, endTime);

        List<Double> closingPrices = kLines.stream()
            .map(kLine -> kLine.getClosingPrice().doubleValue())
            .collect(Collectors.toList());

        KLine kLine = kLines.get(kLines.size() - 2);
        LocalDateTime openingTime = kLine.getOpeningTime();

        BOLL boll = new BOLL(closingPrices, openingTime, symbol, interval, 20);

        return boll;
    }

}

package com.aixi.lv.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.util.ApiUtil;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 行情（价格）服务
 */
@Component
@Slf4j
public class PriceService {

    @Resource
    private HttpService httpService;

    private static final Integer MAX_RETRY_TIMES = 5;

    /**
     * K线查询
     *
     * @param symbol
     * @param interval
     * @param limit
     * @return
     */
    public List<KLine> queryKLine(Symbol symbol, Interval interval, Integer limit) {

        Integer retryTimes = 0;

        while (true) {

            if (retryTimes >= MAX_RETRY_TIMES) {
                log.error("queryKLine 达到最大重试次数 | symbol= {} ", symbol);
                throw new RuntimeException("queryKLine 达到最大重试次数");
            }

            try {

                String url = ApiUtil.url("/api/v3/klines");

                JSONObject params = new JSONObject();
                params.put("symbol", symbol.getCode());
                params.put("interval", interval.getCode());
                params.put("limit", limit);

                JSONArray response = httpService.getArray(url, params);

                List<ArrayList> arrayLists = response.toJavaList(ArrayList.class);

                List<KLine> kLines = Lists.newArrayList();

                for (ArrayList item : arrayLists) {
                    KLine kLine = KLine.parseList(item);
                    kLines.add(kLine);
                }

                return kLines;

            } catch (Exception e) {
                log.error(
                    String.format(" PriceService | queryKLine_fail | symbol=%s | interval=%s | limit=%s", symbol,
                        interval, limit), e);
                retryTimes++;
            }

        }
    }

    /**
     * 根据时间范围查K线
     *
     * @param symbol
     * @param interval
     * @param limit     从开始时间往后数limit
     * @param startTime
     * @param endTime
     * @return
     */
    public List<KLine> queryKLineByTime(Symbol symbol, Interval interval, Integer limit, LocalDateTime startTime,
        LocalDateTime endTime) {

        Integer retryTimes = 0;

        while (true) {

            if (retryTimes >= MAX_RETRY_TIMES) {
                log.error("queryKLine 达到最大重试次数 | symbol= {} ", symbol);
                throw new RuntimeException("queryKLine 达到最大重试次数");
            }

            try {

                String url = ApiUtil.url("/api/v3/klines");

                JSONObject params = new JSONObject();
                params.put("symbol", symbol.getCode());
                params.put("interval", interval.getCode());
                params.put("startTime", TimeUtil.localToLong(startTime));
                params.put("endTime", TimeUtil.localToLong(endTime));
                params.put("limit", limit);

                JSONArray response = httpService.getArray(url, params);

                List<ArrayList> arrayLists = response.toJavaList(ArrayList.class);

                List<KLine> kLines = Lists.newArrayList();

                for (ArrayList item : arrayLists) {
                    KLine kLine = KLine.parseList(item);
                    kLine.setSymbol(symbol);
                    kLines.add(kLine);
                }

                return kLines;

            } catch (Exception e) {
                log.error(
                    String.format(" PriceService | queryKLineByTime_fail | symbol=%s | interval=%s | limit=%s", symbol,
                        interval,
                        limit), e);
                retryTimes++;
            }
        }
    }

    /**
     * 查最新价格
     *
     * @param symbol
     * @return
     */
    public BigDecimal queryNewPrice(Symbol symbol) {

        Integer retryTimes = 0;

        while (true) {

            if (retryTimes >= MAX_RETRY_TIMES) {
                log.error("queryKLine 达到最大重试次数 | symbol= {} ", symbol);
                throw new RuntimeException("queryKLine 达到最大重试次数");
            }

            try {

                String url = ApiUtil.url("/api/v3/ticker/price");

                JSONObject params = new JSONObject();
                params.put("symbol", symbol.getCode());

                JSONObject response = httpService.getObject(url, params);

                return response.getBigDecimal("price");

            } catch (Exception e) {
                log.error(
                    String.format(" PriceService | queryNewPrice_fail | symbol=%s ", symbol), e);
                retryTimes++;
            }
        }
    }
}

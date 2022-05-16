package com.aixi.lv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.config.BackTestConfig;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.NumUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.Test;

/**
 * @author Js
 */
@Slf4j
public class 价格Test extends BaseTest {

    @Resource
    PriceService priceService;

    @Test
    public void 今年1月以来的价格涨幅() {

        List<JSONObject> list = Lists.newArrayList();

        for (Symbol symbol : Symbol.values()) {

            try {

                LocalDateTime startDay = LocalDateTime.of(2022, 1, 1, 0, 0);
                LocalDateTime endDay = startDay.plusDays(1);
                List<KLine> kLines = priceService.queryKLineByTime(symbol, Interval.DAY_1, 2, startDay, endDay);

                BigDecimal startPrice = kLines.get(0).getClosingPrice();

                BigDecimal endPrice = priceService.queryNewPrice(symbol);

                Double rate = endPrice.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_DOWN).doubleValue();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("symbol", symbol.getCode());
                jsonObject.put("rate", rate);
                list.add(jsonObject);

            } catch (Exception e) {
                System.out.println("异常币种是 " + symbol);
                System.out.println(e);
            }

        }

        // 排序
        Collections.sort(list, (o1, o2) -> {
            if (o1.getDouble("rate") <= o2.getDouble("rate")) {
                return 1;
            } else {
                return -1;
            }
        });

        for (JSONObject jo : list) {
            System.out.println(jo.getString("symbol") + " 增长率 = " + NumUtil.percent(jo.getDouble("rate")));
        }
    }

    @Test
    public void 上市以来价格涨幅() {

        for (Symbol symbol : Symbol.values()) {

            try {

                LocalDate saleDate = symbol.getSaleDate();
                LocalDateTime startDay = LocalDateTime.of(saleDate.getYear(), saleDate.getMonth(),
                    saleDate.getDayOfMonth(),
                    0, 0);
                LocalDateTime endDay = startDay.plusDays(1);

                // 时间最早的排 index=0 , 时间最晚的排 index=size-1
                List<KLine> kLines = priceService.queryKLineByTime(symbol, Interval.DAY_1, 2, startDay, endDay);

                BigDecimal startPrice = kLines.get(0).getClosingPrice();

                BigDecimal endPrice = priceService.queryNewPrice(symbol);

                Double rate = endPrice.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_DOWN).doubleValue();

                System.out.println(symbol + " 增长率 = " + NumUtil.percent(rate));

            } catch (Exception e) {
                System.out.println("异常币种是 " + symbol);
                System.out.println(e);
            }

        }
    }
}

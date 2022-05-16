package com.aixi.lv.model.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import com.aixi.lv.model.constant.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KLine {

    /**
     * 参数值示例
     * 1499040000000,      // 开盘时间
     * "0.01634790",       // 开盘价
     * "0.80000000",       // 最高价
     * "0.01575800",       // 最低价
     * "0.01577100",       // 收盘价(当前K线未结束的即为最新价)
     * "148976.11427815",  // 成交量
     * 1499644799999,      // 收盘时间
     * "2434.19055334",    // 成交额
     * 308,                // 成交笔数
     * "1756.87402397",    // 主动买入成交量
     * "28.46694368",      // 主动买入成交额
     * "17928899.62484339" // 请忽略该参数
     **/

    // 开盘时间
    @JSONField(ordinal = 2)
    private LocalDateTime openingTime;

    // 开盘价
    @JSONField(ordinal = 99)
    private BigDecimal openingPrice;

    // 最高价
    @JSONField(ordinal = 5)
    private BigDecimal maxPrice;

    // 最低价
    @JSONField(ordinal = 6)
    private BigDecimal minPrice;

    // 收盘价(当前K线未结束的即为最新价)
    @JSONField(ordinal = 3)
    private BigDecimal closingPrice;

    // 成交量
    @JSONField(ordinal = 4)
    private BigDecimal tradingVolume;

    // 收盘时间
    @JSONField(ordinal = 99)
    private LocalDateTime closingTime;

    // 成交额
    @JSONField(ordinal = 99)
    private BigDecimal tradingAmount;

    // 成交笔数
    @JSONField(ordinal = 99)
    private Integer tradingNumber;

    // 主动买入成交量
    @JSONField(ordinal = 99)
    private BigDecimal buyTradingVolume;

    // 主动买入成交额
    @JSONField(ordinal = 99)
    private BigDecimal buyTradingAmount;

    // 请忽略该参数
    @JSONField(ordinal = 99)
    private BigDecimal ignoreArg;

    // 交易对（币种）
    @JSONField(ordinal = 1)
    private Symbol symbol;

    public static KLine parseList(List list) {

        KLine kLine = new KLine();

        LocalDateTime openingTime =
            Instant.ofEpochMilli((Long)list.get(0)).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
        kLine.setOpeningTime(openingTime);

        BigDecimal openingPrice = new BigDecimal((String)list.get(1));
        kLine.setOpeningPrice(openingPrice);

        BigDecimal maxPrice = new BigDecimal((String)list.get(2));
        kLine.setMaxPrice(maxPrice);

        BigDecimal minPrice = new BigDecimal((String)list.get(3));
        kLine.setMinPrice(minPrice);

        BigDecimal closingPrice = new BigDecimal((String)list.get(4));
        kLine.setClosingPrice(closingPrice);

        BigDecimal tradingVolume = new BigDecimal((String)list.get(5));
        kLine.setTradingVolume(tradingVolume);

        LocalDateTime closingTime =
            Instant.ofEpochMilli((Long)list.get(6)).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
        kLine.setClosingTime(closingTime);

        BigDecimal tradingAmount = new BigDecimal((String)list.get(7));
        kLine.setTradingAmount(tradingAmount);

        Integer tradingNumber = (Integer)list.get(8);
        kLine.setTradingNumber(tradingNumber);

        BigDecimal buyTradingVolume = new BigDecimal((String)list.get(9));
        kLine.setBuyTradingVolume(buyTradingVolume);

        BigDecimal buyTradingAmount = new BigDecimal((String)list.get(10));
        kLine.setBuyTradingAmount(buyTradingAmount);

        BigDecimal ignoreArg = new BigDecimal((String)list.get(11));
        kLine.setIgnoreArg(ignoreArg);

        return kLine;
    }

}

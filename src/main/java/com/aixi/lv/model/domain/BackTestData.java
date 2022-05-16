package com.aixi.lv.model.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alibaba.fastjson.annotation.JSONField;

import com.aixi.lv.model.constant.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 *
 * 回测数据
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackTestData {

    // 交易对（币种）
    @JSONField(ordinal = 1)
    private Symbol symbol;

    // 开盘时间
    @JSONField(ordinal = 2)
    private LocalDateTime openingTime;

    // 收盘价(当前K线未结束的即为最新价)
    @JSONField(ordinal = 3)
    private BigDecimal closingPrice;

    // 成交量
    @JSONField(ordinal = 4)
    private BigDecimal tradingVolume;
}

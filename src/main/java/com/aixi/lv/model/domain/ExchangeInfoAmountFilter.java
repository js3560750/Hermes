package com.aixi.lv.model.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js

 *
 * 服务器交易信息-数量过滤器
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeInfoAmountFilter {

    /**
     * 过滤器类型
     */
    private String filterType;

    /**
     * 一笔订单允许的最小金额
     */
    private BigDecimal minNotional;


}

package com.aixi.lv.model.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 * @date 2022/1/2 11:14 上午
 *
 * 服务器交易信息-数量过滤器
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeInfoQtyFilter {

    /**
     * 过滤器类型
     */
    private String filterType;

    /**
     * 允许的最大数量
     */
    private BigDecimal maxQty;

    /**
     * 允许的最小数量
     */
    private BigDecimal minQty;

    /**
     * 数量步长
     */
    private BigDecimal stepSize;

    /**
     * 数量 BigDecimal的 scale
     */
    private Integer qtyScale;

}

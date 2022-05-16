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
 * 服务器交易信息-价格过滤器
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeInfoPriceFilter {

    /**
     * 过滤器类型
     */
    private String filterType;

    /**
     * 允许的最大价格
     */
    private BigDecimal maxPrice;

    /**
     * 允许的最小价格
     */
    private BigDecimal minPrice;

    /**
     * 价格步长
     */
    private BigDecimal tickSize;

    /**
     * 价格 BigDecimal的 scale
     */
    private Integer priceScale;

}

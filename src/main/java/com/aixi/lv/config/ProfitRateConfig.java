package com.aixi.lv.config;

import java.math.BigDecimal;

/**
 * @author Js
 */
public class ProfitRateConfig {

    /**
     * 一阶段止盈【数量】比例
     */
    public static final BigDecimal FIRST_QTY_RATE = new BigDecimal("0.6");

    /**
     * 二阶段止盈【数量】比例
     */
    public static final BigDecimal SECOND_QTY_RATE = new BigDecimal("0.3");

    /**
     * 三阶段止盈【数量】比例
     */
    public static final BigDecimal THIRD_QTY_RATE = new BigDecimal("0.1");

    /**
     * *
     * *
     * *
     * *
     * *
     * ************************** 分界线 *********************
     * *
     * *
     * *
     * *
     * *
     */


    /**
     * 二阶段止盈【价格】比例
     */
    public static final BigDecimal SECOND_PRICE_RATE = new BigDecimal("1.01");

    /**
     * 三阶段止盈【价格】比例
     */
    public static final BigDecimal THIRD_PRICE_RATE = new BigDecimal("1.03");
}

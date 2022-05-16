package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * 订单方向
 */
public enum OrderSide {

    /**
     * 买入
     */
    BUY("BUY"),

    /**
     * 卖出
     */
    SELL("SELL"),
    ;

    OrderSide(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

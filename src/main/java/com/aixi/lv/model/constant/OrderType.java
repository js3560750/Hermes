package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * 订单类型
 */
public enum OrderType {

    /**
     * 限价单
     */
    LIMIT("LIMIT"),

    /**
     * 市价单
     */
    MARKET("MARKET"),

    /**
     * 止损单
     */
    STOP_LOSS("STOP_LOSS"),

    /**
     * 限价止损单
     */
    STOP_LOSS_LIMIT("STOP_LOSS_LIMIT"),

    /**
     * 止盈单
     */
    TAKE_PROFIT("TAKE_PROFIT"),

    /**
     * 限价止盈单
     */
    TAKE_PROFIT_LIMIT("TAKE_PROFIT_LIMIT"),
    ;

    OrderType(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

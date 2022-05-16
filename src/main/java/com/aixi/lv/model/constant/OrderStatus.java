package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * 现货订单状态
 */
public enum OrderStatus {

    /**
     * 订单被交易引擎接受
     */
    NEW("NEW"),

    /**
     * 部分订单被成交
     */
    PARTIALLY_FILLED("PARTIALLY_FILLED"),

    /**
     * 订单完全成交
     */
    FILLED("FILLED"),

    /**
     * 用户撤销了订单
     */
    CANCELED("CANCELED"),

    /**
     * 订单没有被交易引擎接受，也没被处理
     */
    REJECTED("REJECTED"),

    /**
     * 订单被交易引擎取消, 比如
     * LIMIT FOK 订单没有成交
     * 市价单没有完全成交
     * 强平期间被取消的订单
     * 交易所维护期间被取消的订单
     */
    EXPIRED("EXPIRED"),
    ;

    OrderStatus(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

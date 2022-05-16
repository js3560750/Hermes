package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * 订单有效方式
 *
 * 这里定义了订单多久能够失效
 */
public enum OrderTimeInForce {

    /**
     * 成交为止
     * 订单会一直有效，直到被成交或者取消。
     */
    GTC("GTC"),

    /**
     * 无法立即成交的部分就撤销
     * 订单在失效前会尽量多的成交。
     */
    IOC("IOC"),

    /**
     * 无法全部立即成交就撤销
     * 如果无法全部成交，订单会失效。
     */
    FOK("FOK"),
    ;

    OrderTimeInForce(String code) {
        this.code = code;
    }

    @Getter
    private String code;

}

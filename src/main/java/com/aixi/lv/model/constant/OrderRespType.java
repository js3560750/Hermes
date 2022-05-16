package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * 订单返回类型
 */
public enum OrderRespType {

    /**
     * 返回速度最快，不包含成交信息，信息量最少
     */
    ACK("ACK"),

    /**
     * 返回速度居中，返回吃单成交的少量信息
     */
    RESULT("RESULT"),

    /**
     * 返回速度最慢，返回吃单成交的详细信息
     */
    FULL("FULL"),
    ;

    OrderRespType(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

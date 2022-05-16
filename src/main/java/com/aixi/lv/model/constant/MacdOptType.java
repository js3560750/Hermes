package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * MACD策略的操作类型
 */
public enum MacdOptType {

    /**
     * 所有操作皆可
     */
    ALL("ALL"),

    /**
     * 只买
     */
    ONLY_BUY("ONLY_BUY"),

    /**
     * 只卖
     */
    ONLY_SELL("ONLY_SELL"),


    ;

    MacdOptType(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

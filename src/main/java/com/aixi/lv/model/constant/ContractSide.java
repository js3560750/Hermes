package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * 合约方向
 */
public enum ContractSide {

    /**
     * 做多
     */
    LONG("LONG"),

    /**
     * 做空
     */
    SHORT("SHORT"),
    ;

    ContractSide(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

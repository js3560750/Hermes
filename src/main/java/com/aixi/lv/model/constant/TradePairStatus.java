package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 */
public enum TradePairStatus {

    /**
     * 新下的买单
     */
    NEW("NEW"),

    /**
     * 买单已完成
     */
    ALREADY("ALREADY"),

    /**
     * 买单已取消
     */
    CANCEL("CANCEL"),

    /**
     * 止损单生效中
     */
    LOSS("LOSS"),

    /**
     * 止损单已成交
     */
    LOSS_DONE("LOSS_DONE"),

    /**
     * 一阶段止盈单生效中
     */
    FIRST_PROFIT("FIRST_PROFIT"),

    /**
     * 一阶段止盈单已成交
     */
    FIRST_DONE("FIRST_DONE"),

    /**
     * 二阶段止盈单生效中
     */
    SECOND_PROFIT("SECOND_PROFIT"),

    /**
     * 二阶段止盈单已成交
     */
    SECOND_DONE("SECOND_DONE"),

    /**
     * 三阶段止盈单生效中
     */
    THIRD_PROFIT("THIRD_PROFIT"),

    /**
     * 三阶段止盈单已成交
     */
    THIRD_DONE("THIRD_DONE"),

    /**
     * 强制止盈
     */
    FORCE_PROFIT("FORCE_PROFIT"),


    /**
     * 强制止盈单被取消，这是异常case，得人工处理
     */
    FORCE_PROFIT_CANCEL("FORCE_PROFIT_CANCEL"),

    /**
     * 已全部卖出
     */
    FINISH("FINISH"),

    ;

    TradePairStatus(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

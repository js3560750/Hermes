package com.aixi.lv.model.constant;

import lombok.Getter;

/**
 * @author Js
 *
 * k线间隔
 */
public enum Interval {

    MINUTE_1("1m"),
    MINUTE_3("3m"),
    MINUTE_5("5m"),
    MINUTE_15("15m"),
    MINUTE_30("30m"),
    HOUR_1("1h"),
    HOUR_2("2h"),
    HOUR_4("4h"),
    DAY_1("1d"),
    ;

    Interval(String code) {
        this.code = code;
    }

    @Getter
    private String code;
}

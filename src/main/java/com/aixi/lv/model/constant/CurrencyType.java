package com.aixi.lv.model.constant;

import java.util.Optional;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Js
 *
 * 币种
 */
public enum CurrencyType {

    BTC("BTC"),

    ETH("ETH"),

    DOGE("DOGE"),

    SHIB("SHIB"),

    BNB("BNB"),

    USDT("USDT"),
    ;

    CurrencyType(String code) {
        this.code = code;
    }

    @Getter
    private String code;

    /**
     * 找枚举
     *
     * @param code
     * @return
     */
    public static Optional<CurrencyType> getByCode(String code) {

        for (CurrencyType item : CurrencyType.values()) {
            if (StringUtils.equals(item.getCode(), code)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();

    }
}

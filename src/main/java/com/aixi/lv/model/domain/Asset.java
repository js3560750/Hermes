package com.aixi.lv.model.domain;

import java.math.BigDecimal;

import com.aixi.lv.model.constant.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 *
 * 货币资产
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Asset {

    /**
     * 币种
     */
    private CurrencyType currencyType;

    /**
     * 可自由交易数量
     */
    private BigDecimal freeQty;

    /**
     * 锁定数量
     */
    private BigDecimal lockedQty;
}

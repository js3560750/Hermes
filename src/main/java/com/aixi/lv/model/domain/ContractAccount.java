package com.aixi.lv.model.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aixi.lv.model.constant.ContractSide;
import com.aixi.lv.model.constant.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js

 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractAccount {

    /**
     * 账户名称
     */
    private String name;

    /**
     * 账户涵盖的symbol
     */
    private Symbol symbol;

    /**
     * 当前持有 USDT 金额
     */
    private BigDecimal holdAmount;

    /**
     * 当前持有币数量
     */
    private BigDecimal holdQty;

    /**
     * 是否持有单子
     */
    private Boolean holdFlag;


    /**
     * 单子方向
     */
    private ContractSide contractSide;

    /**
     * 下单价格
     */
    private BigDecimal buyPrice;

    /**
     * 止盈价格
     */
    private BigDecimal profitPrice;

    /**
     * 止损价格
     */
    private BigDecimal lossPrice;

    /**
     * 回测时的当前自然时间
     */
    private LocalDateTime curBackTestComputeTime;

    /**
     * 回测时盈利累计总和
     */
    private BigDecimal backTestTotalProfit;

    /**
     * 回测时亏损累计总和
     */
    private BigDecimal backTestTotalLoss;

    /**
     * 回测时盈利次数
     */
    private int backTestProfitTimes;

    /**
     * 回测时亏损次数
     */
    private int backTestLossTimes;

}

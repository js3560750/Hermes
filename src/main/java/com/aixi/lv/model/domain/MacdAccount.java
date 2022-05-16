package com.aixi.lv.model.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
public class MacdAccount {

    /**
     * 账户名称
     */
    private String name;

    /**
     * 账户涵盖的symbol
     */
    private List<Symbol> symbolList;

    /**
     * 当前持有标记
     */
    private Symbol curHoldSymbol;

    /**
     * 当前持有 USDT 金额
     */
    private BigDecimal curHoldAmount;

    /**
     * 当前持有币数量
     */
    private BigDecimal curHoldQty;

    /**
     * 当前交易对
     */
    private TradePair curPair;

    /**
     * 购买的价格
     */
    private BigDecimal lastBuyPrice;

    /**
     * 归属账户，多币种组合时，方便统计
     */
    private String belongAccount;

    /**
     * 上一次换仓时间
     */
    private LocalDateTime lastSwitchTime;

    /**
     * 上一次卖出时间
     */
    private LocalDateTime lastSellTime;

    /**
     * 上一次卖出的币种
     */
    private Symbol lastSellSymbol;

    /**
     * 准备卖出标记
     */
    private Boolean readySellFlag;

    /**
     * 准备卖出标记的时间
     */
    private LocalDateTime readySellTime;

    /**
     * 回测时的当前自然时间
     */
    private LocalDateTime curBackTestComputeTime;

    /**
     * 回测时的当前增长率
     */
    private Double curBackTestRate;

    /**
     * 回测时，账户涵盖的币种的平均增长率
     */
    private Double symbolNatureRate;

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
    private Integer backTestProfitTimes;

    /**
     * 回测时亏损次数
     */
    private Integer backTestLossTimes;

}

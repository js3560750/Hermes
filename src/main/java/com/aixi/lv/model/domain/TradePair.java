package com.aixi.lv.model.domain;


import java.time.LocalDateTime;

import com.aixi.lv.model.constant.TradePairStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 *
 * 交易对
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePair {

    /**
     * 交易对状态
     */
    private TradePairStatus status;

    /**
     * 买入订单
     */
    private OrderLife buyOrder;

    /**
     * 止损卖单
     */
    private OrderLife lossOrder;

    /**
     * 强制止盈单
     */
    private OrderLife forceProfitOrder;

    /**
     * 一阶段止盈卖单
     */
    private OrderLife firstProfitOrder;

    /**
     * 二阶段止盈卖单
     */
    private OrderLife secondProfitOrder;

    /**
     * 三阶段止盈卖单
     */
    private OrderLife thirdProfitOrder;

    /**
     * 再次购买次数
     */
    private Integer reBuyTimes;

    /**
     * 止损成交的时间
     */
    private LocalDateTime lossTime;
}

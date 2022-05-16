package com.aixi.lv.strategy.profit;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.constant.TradePairStatus;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 强制止盈
 *
 * 当交易对已经属于止盈状态了，此时监测到市场价低于箱顶价格，则无视其他止盈手段，强制止盈
 */
@Component
@Slf4j
public class ForceProfitStrategy {

    @Resource
    OrderLifeManage orderLifeManage;

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    /**
     * 箱顶价格再乘以这个比例
     */
    private static final BigDecimal FORCE_PROFIT_TOP_RATE = new BigDecimal("0.998");

    /**
     * 强制止盈
     *
     * @param symbol
     */
    public void forceProfit(Symbol symbol) {

        List<TradePair> pairList = orderLifeManage.getPairBySymbol(symbol);

        if (CollectionUtils.isEmpty(pairList)) {
            return;
        }

        // 循环处理
        for (TradePair pair : pairList) {

            OrderLife buyOrder = pair.getBuyOrder();

            if (!isNeedForceProfitCheck(pair)) {
                continue;
            }

            // 市场最新价
            BigDecimal newPrice = priceService.queryNewPrice(symbol);
            BigDecimal forcePrice = buyOrder.getTopPrice().multiply(FORCE_PROFIT_TOP_RATE);

            // 市场价低于箱顶时，止盈并完结
            if (newPrice.compareTo(forcePrice) < 0) {

                this.forceProfitAction(symbol, pair, newPrice);

                log.info(" 强制止盈 | 止盈Action | symbol={} | newPrice={} | forcePrice={}",
                    symbol, newPrice, forcePrice);
            } else {
                log.info(" 强制止盈 | 未达处理价 | symbol={} | newPrice={} | forcePrice={}",
                    symbol, newPrice, forcePrice);
            }

        }
    }

    /**
     * 是否需要检查止盈
     *
     * @param pair
     * @return
     */
    private Boolean isNeedForceProfitCheck(TradePair pair) {

        TradePairStatus status = pair.getStatus();

        if (TradePairStatus.FIRST_PROFIT == status
            || TradePairStatus.FIRST_DONE == status
            || TradePairStatus.SECOND_PROFIT == status
            || TradePairStatus.SECOND_DONE == status
            || TradePairStatus.THIRD_PROFIT == status) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private void forceProfitAction(Symbol symbol, TradePair pair, BigDecimal newPrice) {

        OrderLife buyOrder = pair.getBuyOrder();

        // 一阶段止盈单已完成
        if (TradePairStatus.FIRST_DONE == pair.getStatus()) {

            // 下强制止盈单
            BigDecimal firstQty = pair.getFirstProfitOrder().getExecutedQty();
            BigDecimal quantity = buyOrder.getExecutedQty().subtract(firstQty);
            this.postForceProfitOrder(symbol, pair, newPrice, quantity);
            return;
        }

        // 二阶段止盈单已完成
        if (TradePairStatus.SECOND_DONE == pair.getStatus()) {

            // 下强制止盈单
            BigDecimal firstQty = pair.getFirstProfitOrder().getExecutedQty();
            BigDecimal secondQty = pair.getSecondProfitOrder().getExecutedQty();
            BigDecimal quantity = buyOrder.getExecutedQty().subtract(firstQty).subtract(secondQty);
            this.postForceProfitOrder(symbol, pair, newPrice, quantity);
            return;
        }

        // 一阶段止盈单生效中
        if (TradePairStatus.FIRST_PROFIT == pair.getStatus()) {

            OrderLife firstOrder = pair.getFirstProfitOrder();
            // 取消原单
            orderService.cancelByOrderId(symbol, firstOrder.getOrderId());

            // 下强制止盈单
            BigDecimal quantity = buyOrder.getExecutedQty();
            this.postForceProfitOrder(symbol, pair, newPrice, quantity);
            return;
        }

        // 二阶段止盈单生效中
        if (TradePairStatus.SECOND_PROFIT == pair.getStatus()) {

            OrderLife secondOrder = pair.getSecondProfitOrder();
            // 取消原单
            orderService.cancelByOrderId(symbol, secondOrder.getOrderId());

            // 下强制止盈单
            BigDecimal firstQty = pair.getFirstProfitOrder().getExecutedQty();
            BigDecimal quantity = buyOrder.getExecutedQty().subtract(firstQty);
            this.postForceProfitOrder(symbol, pair, newPrice, quantity);
            return;
        }

        // 三阶段止盈单生效中
        if (TradePairStatus.THIRD_PROFIT == pair.getStatus()) {

            OrderLife thirdOrder = pair.getThirdProfitOrder();
            // 取消原单
            orderService.cancelByOrderId(symbol, thirdOrder.getOrderId());

            // 下强制止盈单
            BigDecimal firstQty = pair.getFirstProfitOrder().getExecutedQty();
            BigDecimal secondQty = pair.getSecondProfitOrder().getExecutedQty();
            BigDecimal quantity = buyOrder.getExecutedQty().subtract(firstQty).subtract(secondQty);
            this.postForceProfitOrder(symbol, pair, newPrice, quantity);
            return;
        }
    }

    /**
     * 强制止盈
     *
     * @param symbol
     * @param pair
     * @param newPrice
     */
    private void postForceProfitOrder(Symbol symbol, TradePair pair, BigDecimal newPrice, BigDecimal quantity) {

        OrderLife buyOrder = pair.getBuyOrder();

        // 强制止盈
        OrderLife sellOrder = orderService.limitSellOrder(OrderSide.SELL, symbol, quantity, newPrice);

        sellOrder.setBuyOrderId(buyOrder.getOrderId());

        // Pair 状态更新
        orderLifeManage.putForceProfit(buyOrder.getOrderId(), sellOrder);

        String title = symbol.getCode() + " 强制止盈下单";
        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + symbol.getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");
        content.append("当前价格 : " + newPrice);
        content.append("\n");
        content.append("止盈价格 : " + sellOrder.getSellPrice());
        content.append("\n");
        content.append("买入价格 : " + buyOrder.getBuyPrice());
        content.append("\n");
        content.append("卖出数量 : " + quantity);
        content.append("\n");
        content.append("卖出金额 : " + sellOrder.getSellPrice().multiply(quantity));
        content.append("\n");

        log.info(" 强制止盈 | {} | {}", title, content);
    }
}

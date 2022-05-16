package com.aixi.lv.strategy.defense;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.OrderStatus;
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
 * 防守策略
 *
 * 当探测到市场价高于买入价时，随时调整止损单价格
 */
@Component
@Slf4j
public class DefenseStrategy {

    @Resource
    OrderLifeManage orderLifeManage;

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    /**
     * 防守线，大于 买卖合计手续费 0.15%
     */
    private static final BigDecimal DEFENSE_PRICE_RATE = new BigDecimal("1.002");

    /**
     * 防守检查
     *
     * @param symbol
     */
    public void defense(Symbol symbol) {

        List<TradePair> pairList = orderLifeManage.getPairBySymbol(symbol);

        if (CollectionUtils.isEmpty(pairList)) {
            return;
        }

        // 循环处理
        for (TradePair pair : pairList) {

            OrderLife buyOrder = pair.getBuyOrder();

            if (!isNeedCheck(pair)) {
                continue;
            }

            BigDecimal newPrice = priceService.queryNewPrice(symbol);

            // TODO 防守线的逻辑还需要再理一理
            // 达到防守线
            if (newPrice.compareTo(buyOrder.getBuyPrice().multiply(DEFENSE_PRICE_RATE)) >= 0) {
                // 下止损单
                this.defenseAction(symbol, pair, newPrice);
            }

        }

    }

    private Boolean isNeedCheck(TradePair pair) {

        TradePairStatus status = pair.getStatus();

        if (TradePairStatus.ALREADY == status
            || TradePairStatus.LOSS == status) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private void defenseAction(Symbol symbol, TradePair pair, BigDecimal newPrice) {

        OrderLife buyOrder = pair.getBuyOrder();

        // 买单已完成
        if (pair.getStatus() == TradePairStatus.ALREADY) {

            this.postDefenseOrder(symbol, buyOrder, newPrice);
            return;
        }

        // 止损单生效中
        if (pair.getStatus() == TradePairStatus.LOSS) {

            OrderLife lossOrder = pair.getLossOrder();

            // 先查服务器状态
            OrderLife lossServer = orderService.queryByOrderId(symbol, lossOrder.getOrderId());

            // 已止损
            if (OrderStatus.FILLED == lossServer.getStatus()) {
                // Pair 状态更新 【终态】
                TradePair pairFromManage = orderLifeManage.getPairById(buyOrder.getOrderId());
                mailService.tradeDoneEmail("止损成交", pairFromManage);
                orderLifeManage.removeTradePair(buyOrder.getOrderId());
                return;
            }

            // 部分成交，等下一次检查
            if (OrderStatus.PARTIALLY_FILLED == lossServer.getStatus()) {
                return;
            }

            // 服务端已取消止损单，下新的 止损单
            if (OrderStatus.CANCELED == lossServer.getStatus()
                || OrderStatus.EXPIRED == lossServer.getStatus()
                || OrderStatus.REJECTED == lossServer.getStatus()) {

                // Pair 状态更新
                orderLifeManage.removeLossOrder(buyOrder.getOrderId(), lossServer);
                this.postDefenseOrder(symbol, buyOrder, newPrice);
            }

            // 未成交，取消未完成的止损卖单，移除Map,后续重新止损
            if (OrderStatus.NEW == lossServer.getStatus()) {
                try {
                    // 如果最新价大于原卖价
                    if (newPrice.compareTo(lossServer.getSellPrice()) > 0) {
                        // 取消原单
                        orderService.cancelByOrderId(symbol, lossServer.getOrderId());
                        // Pair 状态更新
                        orderLifeManage.removeLossOrder(buyOrder.getOrderId(), lossServer);
                        // 下止损单
                        this.postDefenseOrder(symbol, buyOrder, newPrice);
                    }
                } catch (Exception e) {
                    log.error(
                        String.format(" defenseAction | 撤销订单失败 | lossOrderServer=%s",
                            JSON.toJSONString(lossServer)), e);
                }
            }

            return;
        }

    }

    /**
     * 下止损单
     *
     * @param symbol
     * @param buyOrder
     * @param newPrice
     */
    private void postDefenseOrder(Symbol symbol, OrderLife buyOrder, BigDecimal newPrice) {

        BigDecimal quantity = buyOrder.getExecutedQty();
        BigDecimal stopPrice = newPrice.multiply(new BigDecimal("0.9996"));
        BigDecimal sellPrice = newPrice.multiply(new BigDecimal("0.9996"));

        // 止损限价卖单
        // TODO JS 这里的 stopPrice 可能会因为过于接近市场价而抛异常
        OrderLife sellOrder = orderService.stopLossOrderV2(OrderSide.SELL, symbol, quantity, stopPrice, sellPrice);

        sellOrder.setBuyOrderId(buyOrder.getOrderId());

        // Pair 状态更新
        orderLifeManage.putLossOrder(buyOrder.getOrderId(), sellOrder);

        String title = symbol.getCode() + " 止损下单";
        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + symbol.getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");
        content.append("当前价格 : " + newPrice);
        content.append("\n");
        content.append("止损价格 : " + sellPrice);
        content.append("\n");
        content.append("买入价格 : " + buyOrder.getBuyPrice());
        content.append("\n");
        content.append("数量 : " + quantity);
        content.append("\n");
        content.append("卖出金额 : " + sellPrice.multiply(quantity));
        content.append("\n");

        log.info(" 止损策略 | {} | {}", title, content);
    }

}

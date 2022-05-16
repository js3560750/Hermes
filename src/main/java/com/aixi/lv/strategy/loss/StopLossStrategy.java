package com.aixi.lv.strategy.loss;

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
 * 止损策略
 */
@Component
@Slf4j
public class StopLossStrategy {

    @Resource
    OrderLifeManage orderLifeManage;

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    /**
     * 止损线，亏 1% 就止损
     */
    private static final BigDecimal STOP_LOSS_MIN_RATE = new BigDecimal("0.99");

    /**
     * 为确保卖出，卖出设置的价格再乘以一个比例
     */
    private static final BigDecimal STOP_LOSS_SELL_ACTION_RATE = new BigDecimal("0.999");

    /**
     * 止损监测
     *
     * @param symbol
     */
    public void stopLoss(Symbol symbol) {

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
            BigDecimal stopLossPrice = buyOrder.getBottomPrice().multiply(STOP_LOSS_MIN_RATE);

            // 市场价低于止损线时
            if (newPrice.compareTo(stopLossPrice) <= 0) {
                // 止损
                this.stopLossAction(symbol, pair, newPrice);
                log.info(" 止损 | 止损Action | symbol={} | newPrice={} | stopLossPrice={}",
                    symbol, newPrice, stopLossPrice);
            } else {
                log.info(" 止损 | 未达处理价 | symbol={} | newPrice={} | stopLossPrice={}",
                    symbol, newPrice, stopLossPrice);
            }

        }

    }

    /**
     * 是否需要检查止损
     *
     * @param pair
     * @return
     */
    private Boolean isNeedCheck(TradePair pair) {

        TradePairStatus status = pair.getStatus();

        if (TradePairStatus.ALREADY == status
            || TradePairStatus.LOSS == status) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * 止损动作 , 只处理 ALREADY 和 LOSS 状态
     *
     * @param pair
     */
    private void stopLossAction(Symbol symbol, TradePair pair, BigDecimal newPrice) {

        OrderLife buyOrder = pair.getBuyOrder();

        // 买单已完成
        if (pair.getStatus() == TradePairStatus.ALREADY) {

            this.postStopLossOrder(symbol, buyOrder, newPrice);
            return;
        }

        // 止损单生效中
        if (pair.getStatus() == TradePairStatus.LOSS) {

            OrderLife lossOrder = pair.getLossOrder();

            // 先查服务器状态
            OrderLife lossServer = orderService.queryByOrderId(symbol, lossOrder.getOrderId());

            // 已止损
            if (OrderStatus.FILLED == lossServer.getStatus()) {
                TradePair pairFromManage = orderLifeManage.getPairById(buyOrder.getOrderId());
                mailService.tradeDoneEmail("止损成交", pairFromManage);
                // Pair 状态更新
                orderLifeManage.updateLossOrderToDone(buyOrder.getOrderId(), lossServer);
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
                this.postStopLossOrder(symbol, buyOrder, newPrice);
                return;
            }

            // 未成交，取消未完成的止损卖单，移除Map,后续重新止损
            if (OrderStatus.NEW == lossServer.getStatus()) {
                try {
                    // 取消原单
                    orderService.cancelByOrderId(symbol, lossServer.getOrderId());
                    // Pair 状态更新
                    orderLifeManage.removeLossOrder(buyOrder.getOrderId(), lossServer);
                    // 下止损单
                    this.postStopLossOrder(symbol, buyOrder, newPrice);
                } catch (Exception e) {
                    log.error(
                        String.format(" stopLossAction | 撤销订单失败 | lossOrderServer=%s",
                            JSON.toJSONString(lossServer)), e);
                }
                return;
            }

        }

    }

    /**
     * 下止损单
     *
     * @param symbol
     * @param buyOrder
     * @param newPrice
     */
    private void postStopLossOrder(Symbol symbol, OrderLife buyOrder, BigDecimal newPrice) {

        BigDecimal quantity = buyOrder.getExecutedQty();

        BigDecimal sellPrice = newPrice.multiply(STOP_LOSS_SELL_ACTION_RATE);

        // 下止损卖单
        OrderLife sellOrder = orderService.limitSellOrder(OrderSide.SELL, symbol, quantity, sellPrice);

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

        log.info(" 止损 | {} | {}", title, content);
    }

}

package com.aixi.lv.strategy.profit;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.OrderStatus;
import com.aixi.lv.config.ProfitRateConfig;
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
 * 止盈策略
 *
 * 当探测到市场价格到达止盈点时，设置止盈卖单，分批卖出获利
 */
@Component
@Slf4j
public class FirstProfitStrategy {

    @Resource
    OrderLifeManage orderLifeManage;

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    /**
     * 快到箱顶了就能卖
     */
    private static final BigDecimal TOP_SELL_RATE = new BigDecimal("0.998");

    /**
     * 一阶段止盈监测
     *
     * @param symbol
     */
    public void firstProfit(Symbol symbol) {

        List<TradePair> pairList = orderLifeManage.getPairBySymbol(symbol);

        if (CollectionUtils.isEmpty(pairList)) {
            return;
        }

        // 循环处理
        for (TradePair pair : pairList) {

            OrderLife buyOrder = pair.getBuyOrder();

            if (!isNeedFirstProfitCheck(pair)) {
                continue;
            }

            BigDecimal newPrice = priceService.queryNewPrice(symbol);
            BigDecimal topSellPrice = buyOrder.getTopPrice().multiply(TOP_SELL_RATE);

            // 市场价高于箱顶价格
            if (newPrice.compareTo(topSellPrice) >= 0) {

                // 设置一阶段止盈
                this.firstProfitAction(symbol, pair, newPrice);

                log.info(" 一阶止盈 | 止盈Action | symbol={} | newPrice={} | topSellPrice={}",
                    symbol, newPrice, topSellPrice);
            } else {
                log.info(" 一阶止盈 | 未达处理价 | symbol={} | newPrice={} | topSellPrice={}",
                    symbol, newPrice, topSellPrice);
            }

        }
    }

    /**
     * 是否需要检查止盈
     *
     * @param pair
     * @return
     */
    private Boolean isNeedFirstProfitCheck(TradePair pair) {

        TradePairStatus status = pair.getStatus();

        if (TradePairStatus.ALREADY == status
            || TradePairStatus.FIRST_PROFIT == status) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    public void firstProfitAction(Symbol symbol, TradePair pair, BigDecimal newPrice) {

        OrderLife buyOrder = pair.getBuyOrder();

        // 买单已完成
        if (TradePairStatus.ALREADY == pair.getStatus()) {

            // 下一阶段止盈单
            this.postFirstProfitOrder(symbol, buyOrder, newPrice);
            return;
        }

        // 一阶段止盈单生效中
        if (TradePairStatus.FIRST_PROFIT == pair.getStatus()) {

            OrderLife firstOrder = pair.getFirstProfitOrder();
            // 先查服务器状态
            OrderLife firstServer = orderService.queryByOrderId(symbol, firstOrder.getOrderId());

            // 服务端已取消，下新的一阶段止盈单
            if (OrderStatus.CANCELED == firstServer.getStatus()
                || OrderStatus.EXPIRED == firstServer.getStatus()
                || OrderStatus.REJECTED == firstServer.getStatus()) {

                // Pair 状态更新
                orderLifeManage.removeFirstProfit(buyOrder.getOrderId(), firstServer);
                this.postFirstProfitOrder(symbol, buyOrder, newPrice);
                return;
            }

            // 其余状态 交给 OrderLifeTask 探测处理
            return;

        }
    }

    private void postFirstProfitOrder(Symbol symbol, OrderLife buyOrder, BigDecimal newPrice) {

        BigDecimal quantity = buyOrder.getExecutedQty().multiply(ProfitRateConfig.FIRST_QTY_RATE);

        // 一阶段止盈卖单
        OrderLife sellOrder = orderService.limitSellOrder(OrderSide.SELL, symbol, quantity, newPrice);

        sellOrder.setBuyOrderId(buyOrder.getOrderId());

        // Pair 状态更新
        orderLifeManage.putFirstProfit(buyOrder.getOrderId(), sellOrder);

        String title = symbol.getCode() + " 一阶段止盈下单";
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

        log.info(" 一阶段止盈策略 | {} | {}", title, content);
    }

}

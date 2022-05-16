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
public class ThirdProfitStrategy {

    @Resource
    OrderLifeManage orderLifeManage;

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    /**
     * 三阶段止盈监测
     *
     * @param symbol
     */
    public void thirdProfit(Symbol symbol) {

        List<TradePair> pairList = orderLifeManage.getPairBySymbol(symbol);

        if (CollectionUtils.isEmpty(pairList)) {
            return;
        }

        // 循环处理
        for (TradePair pair : pairList) {

            OrderLife buyOrder = pair.getBuyOrder();

            if (!isNeedThirdProfitCheck(pair)) {
                continue;
            }

            // 市场最新价
            BigDecimal newPrice = priceService.queryNewPrice(symbol);

            // 三阶段止盈价
            BigDecimal thirdPrice = buyOrder.getTopPrice().multiply(ProfitRateConfig.THIRD_PRICE_RATE);

            // 市场价高于三阶段止盈价
            if (newPrice.compareTo(thirdPrice) >= 0) {

                // 设置三阶段止盈
                this.thirdProfitAction(symbol, pair, newPrice);

                log.info(" 三阶止盈 | 止盈Action | symbol={} | newPrice={} | thirdPrice={}",
                    symbol, newPrice, thirdPrice);
            } else {
                log.info(" 三阶止盈 | 未达处理价 | symbol={} | newPrice={} | thirdPrice={}",
                    symbol, newPrice, thirdPrice);
            }

        }
    }

    /**
     * 是否需要检查止盈
     *
     * @param pair
     * @return
     */
    private Boolean isNeedThirdProfitCheck(TradePair pair) {

        TradePairStatus status = pair.getStatus();

        if (TradePairStatus.SECOND_DONE == status
            || TradePairStatus.THIRD_PROFIT == status) {

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private void thirdProfitAction(Symbol symbol, TradePair pair, BigDecimal newPrice) {

        OrderLife buyOrder = pair.getBuyOrder();

        // 二阶段止盈单已完成
        if (TradePairStatus.SECOND_DONE == pair.getStatus()) {

            // 下三阶段止盈单
            this.postThirdProfitOrder(symbol, pair, newPrice);
            return;
        }

        // 三阶段止盈单生效中
        if (TradePairStatus.THIRD_PROFIT == pair.getStatus()) {

            OrderLife thirdOrder = pair.getThirdProfitOrder();
            // 先查服务器状态
            OrderLife thirdServer = orderService.queryByOrderId(symbol, thirdOrder.getOrderId());

            // 服务端已取消，下新的一阶段止盈单
            if (OrderStatus.CANCELED == thirdServer.getStatus()
                || OrderStatus.EXPIRED == thirdServer.getStatus()
                || OrderStatus.REJECTED == thirdServer.getStatus()) {

                // Pair 状态更新
                orderLifeManage.removeThirdProfit(buyOrder.getOrderId(), thirdServer);
                this.postThirdProfitOrder(symbol, pair, newPrice);
                return;
            }

            // 其余状态 交给 OrderLifeTask 探测处理
            return;
        }
    }

    private void postThirdProfitOrder(Symbol symbol, TradePair pair, BigDecimal newPrice) {

        OrderLife buyOrder = pair.getBuyOrder();
        BigDecimal firstQty = pair.getFirstProfitOrder().getExecutedQty();
        BigDecimal secondQty = pair.getSecondProfitOrder().getExecutedQty();

        BigDecimal quantity = buyOrder.getExecutedQty().subtract(firstQty).subtract(secondQty);

        // 三阶段止盈卖单
        OrderLife sellOrder = orderService.limitSellOrder(OrderSide.SELL, symbol, quantity, newPrice);

        sellOrder.setBuyOrderId(buyOrder.getOrderId());

        // 更新 Pair 状态
        orderLifeManage.putThirdProfit(buyOrder.getOrderId(), sellOrder);

        String title = symbol.getCode() + " 三阶段止盈下单";
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

        log.info(" 三阶段止盈策略 | {} | {}", title, content);
    }

}

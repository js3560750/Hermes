package com.aixi.lv.model.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.OrderTimeInForce;
import com.aixi.lv.model.constant.OrderStatus;
import com.aixi.lv.model.constant.OrderType;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.util.NumUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Js
 *
 * 订单生命周期
 */
@Data
public class OrderLife {

    /**
     * 交易对
     */
    private Symbol symbol;

    /**
     * 订单号
     */
    private Long orderId;

    /**
     * 交易对里对应的买单orderId ，只有卖单具有此属性
     */
    private Long buyOrderId;

    /**
     * OCO订单的ID，不然就是-1
     */
    private Long orderListId;

    /**
     * 客户自己设置的ID
     */
    private String clientOrderId;

    /**
     * 下单时箱底
     */
    private BigDecimal bottomPrice;

    /**
     * 下单时箱顶
     */
    private BigDecimal topPrice;

    /**
     * 买入下单价格
     */
    private BigDecimal buyPrice;

    /**
     * 卖出下单价格
     */
    private BigDecimal sellPrice;

    /**
     * 用户设置的要买入or卖出的币种的数量
     */
    private BigDecimal originQty;

    /**
     * 交易的订单数量 (币数量)
     */
    private BigDecimal executedQty;

    /**
     * 累计交易的金额
     */
    private BigDecimal cumulativeQuoteQty;

    /**
     * 现货订单状态
     */
    private OrderStatus status;

    /**
     * 订单有效方式
     */
    private OrderTimeInForce timeInForce;

    /**
     * 订单类型
     */
    private OrderType type;

    /**
     * 订单方向
     */
    private OrderSide side;

    /**
     * 止损价格
     */
    private BigDecimal stopPrice;

    /**
     * 冰山数量
     */
    private BigDecimal icebergQty;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 订单是否出现在 orderBook 中， 既此订单是否有效
     */
    private Boolean isWorking;

    /**
     * 原始的交易金额
     */
    private BigDecimal originQuoteOrderQty;

    /**
     * 任务id
     */
    private String taskKey;

    /**
     * k线间隔
     */
    private Interval interval;

    /**
     * k线数量
     */
    private Integer limit;

    public static OrderLife parseObject(JSONObject jo) {

        OrderLife orderLife = new OrderLife();

        Symbol symbol = Symbol.valueOf(jo.getString("symbol"));
        orderLife.setSymbol(symbol);

        Long orderId = jo.getLong("orderId");
        orderLife.setOrderId(orderId);

        Long orderListId = jo.getLong("orderListId");
        orderLife.setOrderListId(orderListId);

        String clientOrderId = jo.getString("clientOrderId");
        orderLife.setClientOrderId(clientOrderId);

        if (StringUtils.isNotEmpty(jo.getString("side"))) {
            OrderSide side = OrderSide.valueOf(jo.getString("side"));
            orderLife.setSide(side);
        }

        if (StringUtils.isNotEmpty(jo.getString("type"))) {
            OrderType type = OrderType.valueOf(jo.getString("type"));
            orderLife.setType(type);
        }

        BigDecimal executedQty = jo.getBigDecimal("executedQty");
        orderLife.setExecutedQty(executedQty);

        BigDecimal cumulativeQuoteQty = jo.getBigDecimal("cummulativeQuoteQty");
        orderLife.setCumulativeQuoteQty(cumulativeQuoteQty);

        if (StringUtils.isNotEmpty(jo.getString("status"))) {
            OrderStatus status = OrderStatus.valueOf(jo.getString("status"));
            orderLife.setStatus(status);
        }

        BigDecimal price = jo.getBigDecimal("price");
        if (OrderSide.BUY.equals(orderLife.side)) {

            if (OrderType.MARKET == orderLife.getType() && OrderStatus.FILLED == orderLife.getStatus()) {
                // 市价单的实际买入价得自己算
                BigDecimal marketBuyPrice = NumUtil.pricePrecision(orderLife.getSymbol(),
                    orderLife.getCumulativeQuoteQty().divide(orderLife.getExecutedQty(), 8, RoundingMode.HALF_DOWN));
                orderLife.setBuyPrice(marketBuyPrice);
            } else {
                orderLife.setBuyPrice(price);

            }
        }
        if (OrderSide.SELL.equals(orderLife.side)) {

            if (OrderType.MARKET == orderLife.getType() && OrderStatus.FILLED == orderLife.getStatus()) {
                // 市价单的实际卖出价得自己算
                BigDecimal marketSellPrice = NumUtil.pricePrecision(orderLife.getSymbol(),
                    orderLife.getCumulativeQuoteQty().divide(orderLife.getExecutedQty(), 8, RoundingMode.HALF_DOWN));
                orderLife.setSellPrice(marketSellPrice);
            } else {
                orderLife.setSellPrice(price);

            }
        }

        BigDecimal origQty = jo.getBigDecimal("origQty");
        orderLife.setOriginQty(origQty);

        if (StringUtils.isNotEmpty(jo.getString("timeInForce"))) {
            OrderTimeInForce timeInForce = OrderTimeInForce.valueOf(jo.getString("timeInForce"));
            orderLife.setTimeInForce(timeInForce);
        }

        BigDecimal stopPrice = jo.getBigDecimal("stopPrice");
        orderLife.setStopPrice(stopPrice);

        BigDecimal icebergQty = jo.getBigDecimal("icebergQty");
        orderLife.setIcebergQty(icebergQty);

        if (jo.getLong("time") != null) {
            LocalDateTime createTime =
                Instant.ofEpochMilli(jo.getLong("time")).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
            orderLife.setCreateTime(createTime);
        }

        if (jo.getLong("updateTime") != null) {
            LocalDateTime updateTime =
                Instant.ofEpochMilli(jo.getLong("updateTime")).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
            orderLife.setUpdateTime(updateTime);
        }

        Boolean isWorking = jo.getBoolean("isWorking");
        orderLife.setIsWorking(isWorking);

        BigDecimal originQuoteOrderQty = jo.getBigDecimal("origQuoteOrderQty");
        orderLife.setOriginQuoteOrderQty(originQuoteOrderQty);

        return orderLife;
    }
}

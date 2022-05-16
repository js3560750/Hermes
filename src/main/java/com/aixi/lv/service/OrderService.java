package com.aixi.lv.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.OrderRespType;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.OrderStatus;
import com.aixi.lv.model.constant.OrderTimeInForce;
import com.aixi.lv.model.constant.OrderType;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.util.ApiUtil;
import com.aixi.lv.util.NumUtil;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 订单服务
 */
@Component
@Slf4j
public class OrderService {

    @Resource
    EncryptHttpService encryptHttpService;

    @Resource
    OrderLifeManage orderLifeManage;

    private static final Integer TRADE_PAIR_MAX_LIMIT = 30;

    /**
     * 下单：限价买入单
     *
     * @param symbol
     * @param quantity 要买入的币种的数量
     * @param price    买入价格
     * @return
     */
    public OrderLife limitBuyOrder(OrderSide orderSide, Symbol symbol, BigDecimal quantity, BigDecimal price) {

        BigDecimal precisionBuyPrice = NumUtil.pricePrecision(symbol, price);

        BigDecimal precisionQty = NumUtil.qtyPrecision(symbol, quantity);

        try {

            if (!OrderSide.BUY.equals(orderSide)) {
                throw new RuntimeException(" 买单交易方向设置错误 ");
            }

            List<TradePair> tradePairList = orderLifeManage.getAllPair();
            if (tradePairList.size() > TRADE_PAIR_MAX_LIMIT) {
                throw new RuntimeException(" 现存交易对数量达到上限 " + TRADE_PAIR_MAX_LIMIT);
            }

            if (NumUtil.isErrorAmount(symbol, precisionBuyPrice, precisionQty)) {
                throw new RuntimeException(" 单笔买单价值低于最小金额限制 ");
            }

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("side", OrderSide.BUY.getCode());
            body.put("type", OrderType.LIMIT.getCode());
            body.put("timeInForce", OrderTimeInForce.GTC.getCode());
            body.put("timestamp", timeStamp);
            body.put("quantity", precisionQty);
            body.put("price", precisionBuyPrice);

            JSONObject response = encryptHttpService.postObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(response);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(" OrderService | limitBuyOrder_fail | symbol=%s | quantity=%s | buyPrice=%s",
                symbol, precisionQty, precisionBuyPrice), e);
            throw e;
        }
    }

    /**
     * 市价买入单
     *
     * @param orderSide
     * @param symbol
     * @param quoteOrderQty 买入花费的USDT数量
     * @return
     */
    public OrderLife marketBuyOrder(OrderSide orderSide, Symbol symbol, BigDecimal quoteOrderQty) {

        // 买入花费的USDT数量
        BigDecimal precisionQuoteOrderQty = quoteOrderQty.setScale(2, RoundingMode.DOWN);

        try {

            if (!OrderSide.BUY.equals(orderSide)) {
                throw new RuntimeException(" 买单交易方向设置错误 ");
            }

            List<TradePair> tradePairList = orderLifeManage.getAllPair();
            if (tradePairList.size() > TRADE_PAIR_MAX_LIMIT) {
                throw new RuntimeException(" 现存交易对数量达到上限 " + TRADE_PAIR_MAX_LIMIT);
            }

            if (NumUtil.isErrorAmount(symbol, BigDecimal.ONE, precisionQuoteOrderQty)) {
                throw new RuntimeException(" 单笔买单价值低于最小金额限制 ");
            }

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("side", OrderSide.BUY.getCode());
            body.put("type", OrderType.MARKET.getCode());
            body.put("timestamp", timeStamp);
            body.put("quoteOrderQty", precisionQuoteOrderQty);

            JSONObject response = encryptHttpService.postObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(response);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(" OrderService | limitBuyOrder_fail | symbol=%s | quoteOrderQty=%s",
                symbol, quoteOrderQty), e);
            throw e;
        }

    }

    /**
     * 市价卖出单
     *
     * @param orderSide
     * @param symbol
     * @param quantity 卖出币种的数量
     * @return
     */
    public OrderLife marketSellOrder(OrderSide orderSide, Symbol symbol, BigDecimal quantity) {

        // 卖出币种的数量
        BigDecimal precisionQty = NumUtil.qtyPrecision(symbol, quantity);

        try {

            if (!OrderSide.SELL.equals(orderSide)) {
                throw new RuntimeException(" 卖单交易方向设置错误 ");
            }

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("side", OrderSide.SELL.getCode());
            body.put("type", OrderType.MARKET.getCode());
            body.put("timestamp", timeStamp);
            body.put("quantity", precisionQty);

            JSONObject response = encryptHttpService.postObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(response);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(" OrderService | marketSellOrder_fail | symbol=%s | quantity=%s",
                symbol, quantity), e);
            throw e;
        }

    }

    /**
     * 下单：限价卖出单
     *
     * @param symbol
     * @param quantity 要卖出的币种的数量
     * @param price    卖出价格
     * @return
     */
    public OrderLife limitSellOrder(OrderSide orderSide, Symbol symbol, BigDecimal quantity, BigDecimal price) {

        BigDecimal precisionSellPrice = NumUtil.pricePrecision(symbol, price);

        BigDecimal precisionQty = NumUtil.qtyPrecision(symbol, quantity);

        try {

            if (!OrderSide.SELL.equals(orderSide)) {
                throw new RuntimeException(" 卖单交易方向设置错误 ");
            }

            if (NumUtil.isErrorAmount(symbol, precisionSellPrice, precisionQty)) {
                throw new RuntimeException(" 单笔卖单价值低于最小金额限制 ");
            }

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("side", OrderSide.SELL.getCode());
            body.put("type", OrderType.LIMIT.getCode());
            body.put("timeInForce", OrderTimeInForce.GTC.getCode());
            body.put("timestamp", timeStamp);
            body.put("quantity", precisionQty);
            body.put("price", precisionSellPrice);

            JSONObject response = encryptHttpService.postObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(response);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(" OrderService | limitSellOrder_fail | symbol=%s | quantity=%s | sellPrice=%s",
                symbol, precisionQty, precisionSellPrice), e);
            throw e;
        }
    }

    /**
     * 下单： 止损限价卖单 （stopPrice和sellPrice不同价）
     *
     * 只有当价格低于stopPrice时，以sellPrice的价格卖出
     *
     * @param orderSide
     * @param symbol
     * @param quantity
     * @param stopPrice
     * @return respOrderLife 只有 symbol 、 orderId 和 clientOrderId ， 其他属性为空
     */
    @Deprecated // 每个交易对限制只能下5单，有风险
    public OrderLife stopLossOrder(OrderSide orderSide, Symbol symbol, BigDecimal quantity, BigDecimal stopPrice) {

        BigDecimal sellPrice = stopPrice.multiply(new BigDecimal("0.999"));

        return this.stopLossOrderV2(orderSide, symbol, quantity, stopPrice, sellPrice);
    }

    /**
     * 下单： 止损限价卖单 （stopPrice和sellPrice同价）
     *
     * 只有当价格低于stopPrice时，以sellPrice的价格卖出
     *
     * @param orderSide
     * @param symbol
     * @param quantity
     * @param stopPrice
     * @param sellPrice
     * @return
     */
    @Deprecated
    public OrderLife stopLossOrderV2(OrderSide orderSide, Symbol symbol, BigDecimal quantity, BigDecimal stopPrice,
        BigDecimal sellPrice) {

        BigDecimal precisionStopPrice = NumUtil.pricePrecision(symbol, stopPrice);
        BigDecimal precisionSellPrice = NumUtil.pricePrecision(symbol, sellPrice);

        BigDecimal precisionQty = NumUtil.qtyPrecision(symbol, quantity);

        try {

            if (!OrderSide.SELL.equals(orderSide)) {
                throw new RuntimeException(" 卖单交易方向设置错误 ");
            }

            if (NumUtil.isErrorAmount(symbol, precisionStopPrice, precisionQty)) {
                throw new RuntimeException(" 单笔卖单价值低于最小金额限制 ");
            }

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("side", OrderSide.SELL.getCode());
            body.put("type", OrderType.STOP_LOSS_LIMIT.getCode());
            body.put("timeInForce", OrderTimeInForce.GTC.getCode());
            body.put("timestamp", timeStamp);
            body.put("quantity", precisionQty);
            body.put("stopPrice", precisionStopPrice);
            body.put("price", precisionSellPrice);
            body.put("newOrderRespType", OrderRespType.RESULT.getCode());

            JSONObject response = encryptHttpService.postObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(response);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(
                " OrderService | stopLossOrderV2_fail | symbol=%s | quantity=%s | stopPrice=%s | sellPrice=%s",
                symbol, precisionQty, precisionStopPrice, precisionSellPrice), e);
            throw e;
        }
    }

    /**
     * 下单： 止盈限价卖单
     *
     * 只有当价格高于stopPrice时，以sellPrice的价格卖出
     *
     * @param orderSide
     * @param symbol
     * @param quantity
     * @param stopPrice
     * @return respOrderLife 只有 symbol 、 orderId 和 clientOrderId ， 其他属性为空
     */
    @Deprecated
    public OrderLife takeProfitOrder(OrderSide orderSide, Symbol symbol, BigDecimal quantity, BigDecimal stopPrice) {

        BigDecimal sellPrice = stopPrice.multiply(new BigDecimal("1"));
        BigDecimal precisionStopPrice = NumUtil.pricePrecision(symbol, stopPrice);
        BigDecimal precisionSellPrice = NumUtil.pricePrecision(symbol, sellPrice);

        BigDecimal precisionQty = NumUtil.qtyPrecision(symbol, quantity);

        try {

            if (!OrderSide.SELL.equals(orderSide)) {
                throw new RuntimeException(" 卖单交易方向设置错误 ");
            }

            if (NumUtil.isErrorAmount(symbol, precisionStopPrice, precisionQty)) {
                throw new RuntimeException(" 单笔卖单价值低于最小金额限制 ");
            }

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("side", OrderSide.SELL.getCode());
            body.put("type", OrderType.TAKE_PROFIT_LIMIT.getCode());
            body.put("timeInForce", OrderTimeInForce.GTC.getCode());
            body.put("timestamp", timeStamp);
            body.put("quantity", precisionQty);
            body.put("stopPrice", precisionStopPrice);
            body.put("price", precisionSellPrice);
            body.put("newOrderRespType", OrderRespType.RESULT.getCode());

            JSONObject response = encryptHttpService.postObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(response);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(
                " OrderService | takeProfitOrder_fail | symbol=%s | quantity=%s | stopPrice=%s | sellPrice=%s",
                symbol, precisionQty, precisionStopPrice, precisionSellPrice), e);
            throw e;
        }
    }

    /**
     * 根据订单id 撤销挂单
     *
     * @param symbol
     * @param orderId
     * @return
     */
    public Boolean cancelByOrderId(Symbol symbol, Long orderId) {

        try {

            String url = ApiUtil.url("/api/v3/order");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("orderId", orderId);
            body.put("timestamp", timeStamp);

            JSONObject object = encryptHttpService.deleteObject(url, body);

            OrderLife orderLife = OrderLife.parseObject(object);

            if (!OrderStatus.CANCELED.equals(orderLife.getStatus())) {
                throw new RuntimeException(
                    String.format(" OrderService | 撤销挂单失败，返回状态并非CANCELED | response=%s", JSON.toJSONString(orderLife)));
            }

            return Boolean.TRUE;

        } catch (Exception e) {

            if (e.getMessage().contains("-2011")) {
                // 已经撤销或已经完成的订单无法再次被撤销，既重复撤销等同于成功
                return Boolean.TRUE;
            }

            log.error(String.format(" OrderService | cancelByOrderId_fail | symbol=%s | orderId=%s",
                symbol, orderId), e);

            throw e;
        }
    }

    /**
     * 撤销指定交易对的当前所有挂单
     *
     * @param symbol
     * @return
     */
    public Boolean cancelOpenOrder(Symbol symbol) {

        try {

            String url = ApiUtil.url("/api/v3/openOrders");

            JSONObject body = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            body.put("symbol", symbol.getCode());
            body.put("timestamp", timeStamp);

            JSONArray response = encryptHttpService.deleteArray(url, body);

            List<JSONObject> arrayLists = response.toJavaList(JSONObject.class);

            List<OrderLife> orderLifeList = Lists.newArrayList();

            for (JSONObject item : arrayLists) {
                OrderLife orderLife = OrderLife.parseObject(item);
                orderLifeList.add(orderLife);
            }

            for (OrderLife cancelOrder : orderLifeList) {
                if (!OrderStatus.CANCELED.equals(cancelOrder.getStatus())) {
                    throw new RuntimeException(
                        String.format(" OrderService | 撤销挂单失败，返回状态并非CANCELED | response=%s",
                            JSON.toJSONString(cancelOrder)));
                }
            }

            return Boolean.TRUE;

        } catch (Exception e) {
            log.error(String.format(" OrderService | cancelOpenOrder_fail | symbol=%s",
                symbol), e);
            throw e;
        }
    }

    /**
     * 根据订单id 查询订单信息
     *
     * 已取消、已完成、新的 订单都可以查到
     *
     * @param symbol
     * @param orderId
     * @return
     */
    public OrderLife queryByOrderId(Symbol symbol, Long orderId) {

        try {

            String url = ApiUtil.url("/api/v3/order");

            JSONObject params = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            params.put("symbol", symbol.getCode());
            params.put("orderId", orderId);
            params.put("timestamp", timeStamp);

            JSONObject object = encryptHttpService.getObject(url, params);

            OrderLife orderLife = OrderLife.parseObject(object);

            return orderLife;

        } catch (Exception e) {
            log.error(String.format(" OrderService | queryByOrderId_fail | symbol=%s | orderId=%s",
                symbol, orderId), e);
            throw e;
        }
    }

    /**
     * 查询指定交易对的当前所有挂单
     *
     * @param symbol
     * @return
     */
    public List<OrderLife> queryOpenOrder(Symbol symbol) {

        try {

            String url = ApiUtil.url("/api/v3/openOrders");

            JSONObject params = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            params.put("symbol", symbol.getCode());
            params.put("timestamp", timeStamp);

            JSONArray response = encryptHttpService.getArray(url, params);

            List<JSONObject> arrayLists = response.toJavaList(JSONObject.class);

            List<OrderLife> orderLifeList = Lists.newArrayList();

            for (JSONObject item : arrayLists) {
                OrderLife orderLife = OrderLife.parseObject(item);
                orderLifeList.add(orderLife);
            }

            return orderLifeList;

        } catch (Exception e) {
            log.error(String.format(" OrderService | queryOpenOrder_fail | symbol=%s",
                symbol), e);
            throw e;
        }
    }

    /**
     * 根据时间获取所有的订单； 有效，已取消或已完成
     *
     * @param symbol
     * @param startTime
     * @param endTime
     * @return
     */
    public List<OrderLife> queryAllOrder(Symbol symbol, LocalDateTime startTime, LocalDateTime endTime) {

        try {

            String url = ApiUtil.url("/api/v3/allOrders");

            JSONObject params = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            params.put("symbol", symbol.getCode());
            params.put("startTime", TimeUtil.localToLong(startTime));
            params.put("endTime", TimeUtil.localToLong(endTime));
            params.put("recvWindow", 30000);
            params.put("timestamp", timeStamp);

            JSONArray response = encryptHttpService.getArray(url, params);

            List<JSONObject> arrayLists = response.toJavaList(JSONObject.class);

            List<OrderLife> orderLifeList = Lists.newArrayList();

            for (JSONObject item : arrayLists) {
                OrderLife orderLife = OrderLife.parseObject(item);
                orderLifeList.add(orderLife);
            }

            return orderLifeList;

        } catch (Exception e) {
            log.error(String.format(" OrderService | queryAllOrder_fail | symbol=%s | startTime=%s | endTime=%s",
                symbol, startTime, endTime), e);
            throw e;
        }
    }
}

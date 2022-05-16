package com.aixi.lv.manage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.constant.TradePairStatus;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 */
@Component
@Slf4j
public class OrderLifeManage {

    /**
     * 订单存储介质
     *
     * key : 买入订单号
     * value : 交易对
     */
    private static final ConcurrentHashMap<Long, TradePair> TRADE_PAIR_MAP = new ConcurrentHashMap<>(128);

    /**
     * 获得所有的交易对
     *
     * @return
     */
    public List<TradePair> getAllPair() {
        return Lists.newArrayList(TRADE_PAIR_MAP.values());
    }

    /**
     * 获取指定Symbol的交易对
     *
     * @param symbol
     * @return
     */
    public List<TradePair> getPairBySymbol(Symbol symbol) {

        ArrayList<TradePair> pairList = Lists.newArrayList(TRADE_PAIR_MAP.values());

        if (CollectionUtils.isEmpty(pairList)) {
            return Lists.newArrayList();
        }

        // 过滤检查币种
        List<TradePair> needProcessList = pairList.stream()
            .filter(i -> i.getBuyOrder().getSymbol() == symbol)
            .collect(Collectors.toList());

        return needProcessList;
    }

    /**
     * 根据key找value
     *
     * @param buyOrderId
     * @return
     */
    public TradePair getPairById(Long buyOrderId) {
        return TRADE_PAIR_MAP.get(buyOrderId);
    }

    /**
     * 移出交易对
     *
     * @param buyOrderId
     */
    public void removeTradePair(Long buyOrderId) {
        TRADE_PAIR_MAP.remove(buyOrderId);
    }

    /**
     * 加入买单
     *
     * @param buyOrder
     */
    public void putBuyOrder(OrderLife buyOrder) {

        if (TRADE_PAIR_MAP.containsKey(buyOrder.getOrderId())) {
            throw new RuntimeException(String.format("不允许重复加入买单 | buyOrderId=%s", buyOrder.getOrderId()));
        }

        TradePair pair = new TradePair();
        pair.setBuyOrder(buyOrder);
        pair.setStatus(TradePairStatus.NEW);

        TRADE_PAIR_MAP.put(buyOrder.getOrderId(), pair);
    }

    /**
     * 适用于人工加入买单
     * @param buyOrder
     */
    public void putAlreadyBuyOrder(OrderLife buyOrder) {

        if (TRADE_PAIR_MAP.containsKey(buyOrder.getOrderId())) {
            throw new RuntimeException(String.format("不允许重复加入买单 | buyOrderId=%s", buyOrder.getOrderId()));
        }

        TradePair pair = new TradePair();
        pair.setBuyOrder(buyOrder);
        pair.setStatus(TradePairStatus.ALREADY);

        TRADE_PAIR_MAP.put(buyOrder.getOrderId(), pair);
    }

    /**
     * 完成买单
     *
     * @param buyOrder
     */
    public void doneBuyOrder(OrderLife buyOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrder.getOrderId())) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrder.getOrderId()));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrder.getOrderId());
        OrderLife originBuy = pair.getBuyOrder();
        log.info("完成买单 | originPair = {} | buyOrder = {}", JSON.toJSONString(pair), JSON.toJSONString(buyOrder));
        buyOrder.setTopPrice(originBuy.getTopPrice());
        buyOrder.setBottomPrice(originBuy.getBottomPrice());
        buyOrder.setTaskKey(originBuy.getTaskKey());
        buyOrder.setInterval(originBuy.getInterval());
        buyOrder.setLimit(originBuy.getLimit());

        pair.setBuyOrder(buyOrder);
        pair.setStatus(TradePairStatus.ALREADY);
    }

    /**
     * 加入止损卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void putLossOrder(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife lossOrder = pair.getLossOrder();

        if (lossOrder != null) {
            throw new RuntimeException(String.format("不允许重复加入卖单 | lossOrder=%s", lossOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setLossOrder(sellOrder);
        pair.setStatus(TradePairStatus.LOSS);

    }

    /**
     * 更新止损卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void updateLossOrderToDone(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife lossOrder = pair.getLossOrder();
        if (lossOrder == null) {
            throw new RuntimeException(String.format(" 不存在止损卖单，无法更新 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!lossOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的止损卖单，更新对象错误 | sellOrderId=%s | lossOrderId=%s", sellOrder.getOrderId(),
                    lossOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setLossOrder(sellOrder);
        pair.setLossTime(TimeUtil.now());
        pair.setStatus(TradePairStatus.LOSS_DONE);

    }

    /**
     * 移除止损卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void removeLossOrder(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife lossOrder = pair.getLossOrder();
        if (lossOrder == null) {
            throw new RuntimeException(String.format(" 不存在止损卖单，无法移除 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!lossOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的止损卖单，移除对象错误 | sellOrderId=%s | lossOrderId=%s", sellOrder.getOrderId(),
                    lossOrder.getOrderId()));
        }

        pair.setLossOrder(null);
        pair.setStatus(TradePairStatus.ALREADY);
    }

    /**
     * 加入一阶段止盈卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void putFirstProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife firstProfitOrder = pair.getFirstProfitOrder();

        if (firstProfitOrder != null) {
            throw new RuntimeException(
                String.format("不允许重复加入一阶段止盈卖单 | firstProfitOrder=%s", firstProfitOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setFirstProfitOrder(sellOrder);
        pair.setStatus(TradePairStatus.FIRST_PROFIT);

    }

    /**
     * 完成一阶段止盈卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void doneFirstProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife firstProfitOrder = pair.getFirstProfitOrder();

        if (firstProfitOrder == null) {
            throw new RuntimeException(String.format(" 不存在一阶段止盈卖单，无法更新 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!firstProfitOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的一阶段止盈卖单，更新对象错误 | sellOrderId=%s | firstProfitOrder=%s", sellOrder.getOrderId(),
                    firstProfitOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setFirstProfitOrder(sellOrder);
        pair.setStatus(TradePairStatus.FIRST_DONE);

    }

    /**
     * 移除一阶段止盈卖单
     *
     * @param buyOrderId
     */
    public void removeFirstProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife firstProfitOrder = pair.getFirstProfitOrder();
        if (firstProfitOrder == null) {
            throw new RuntimeException(String.format(" 不存在一阶段止盈卖单，无法移除 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!firstProfitOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的一阶段止盈卖单，移除对象错误 | sellOrderId=%s | firstProfitOrder=%s", sellOrder.getOrderId(),
                    firstProfitOrder.getOrderId()));
        }

        pair.setFirstProfitOrder(null);
        pair.setStatus(TradePairStatus.ALREADY);
    }

    /**
     * 加入二阶段止盈卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void putSecondProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife secondProfitOrder = pair.getSecondProfitOrder();

        if (secondProfitOrder != null) {
            throw new RuntimeException(
                String.format("不允许重复加入二阶段止盈卖单 | secondProfitOrder=%s", secondProfitOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setSecondProfitOrder(sellOrder);
        pair.setStatus(TradePairStatus.SECOND_PROFIT);

    }

    /**
     * 完成二阶段止盈卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void doneSecondProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife secondProfitOrder = pair.getSecondProfitOrder();

        if (secondProfitOrder == null) {
            throw new RuntimeException(String.format(" 不存在二阶段止盈卖单，无法更新 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!secondProfitOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的二阶段止盈卖单，更新对象错误 | sellOrderId=%s | secondProfitOrder=%s", sellOrder.getOrderId(),
                    secondProfitOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setSecondProfitOrder(sellOrder);
        pair.setStatus(TradePairStatus.SECOND_DONE);

    }

    /**
     * 移除二阶段止盈卖单
     *
     * @param buyOrderId
     */
    public void removeSecondProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife secondProfitOrder = pair.getSecondProfitOrder();
        if (secondProfitOrder == null) {
            throw new RuntimeException(String.format(" 不存在二阶段止盈卖单，无法移除 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!secondProfitOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的二阶段止盈卖单，移除对象错误 | sellOrderId=%s | secondProfitOrder=%s", sellOrder.getOrderId(),
                    secondProfitOrder.getOrderId()));
        }

        pair.setSecondProfitOrder(null);
        pair.setStatus(TradePairStatus.FIRST_DONE);
    }

    /**
     * 加入三阶段止盈卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void putThirdProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife thirdProfitOrder = pair.getThirdProfitOrder();

        if (thirdProfitOrder != null) {
            throw new RuntimeException(
                String.format("不允许重复加入三阶段止盈卖单 | thirdProfitOrder=%s", thirdProfitOrder.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setThirdProfitOrder(sellOrder);
        pair.setStatus(TradePairStatus.THIRD_PROFIT);

    }

    /**
     * 完成三阶段止盈卖单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void doneThirdProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife thirdProfitOrder = pair.getThirdProfitOrder();

        if (thirdProfitOrder == null) {
            throw new RuntimeException(String.format(" 不存在三阶段止盈卖单，无法更新 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!thirdProfitOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的三阶段止盈卖单，更新对象错误 | sellOrderId=%s | thirdProfitOrder=%s", sellOrder.getOrderId(),
                    thirdProfitOrder.getOrderId()));
        }

        // 三阶段完成既终态
        TRADE_PAIR_MAP.remove(buyOrderId);
    }

    /**
     * 移除三阶段止盈卖单
     *
     * @param buyOrderId
     */
    public void removeThirdProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife thirdProfitOrder = pair.getThirdProfitOrder();
        if (thirdProfitOrder == null) {
            throw new RuntimeException(String.format(" 不存在三阶段止盈卖单，无法移除 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!thirdProfitOrder.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的三阶段止盈卖单，移除对象错误 | sellOrderId=%s | thirdProfitOrder=%s", sellOrder.getOrderId(),
                    thirdProfitOrder.getOrderId()));
        }

        pair.setThirdProfitOrder(null);
        pair.setStatus(TradePairStatus.SECOND_DONE);
    }

    /**
     * 加入强制止盈单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void putForceProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife force = pair.getForceProfitOrder();

        if (force != null) {
            throw new RuntimeException(
                String.format("不允许重复加入止盈防守单 | force=%s", force.getOrderId()));
        }

        sellOrder.setBuyOrderId(buyOrderId);
        pair.setForceProfitOrder(sellOrder);
        pair.setStatus(TradePairStatus.FORCE_PROFIT);

    }

    /**
     * 完成强制止盈单
     *
     * @param buyOrderId
     * @param sellOrder
     */
    public void doneForceProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife force = pair.getForceProfitOrder();

        if (force == null) {
            throw new RuntimeException(String.format(" 不存在强制止盈单，无法更新 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!force.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的强制止盈单，更新对象错误 | sellOrderId=%s | force=%s", sellOrder.getOrderId(),
                    force.getOrderId()));
        }

        // 强制止盈完成既终态
        TRADE_PAIR_MAP.remove(buyOrderId);
    }

    /**
     * 移除强制止盈单
     *
     * @param buyOrderId
     */
    public void removeForceProfit(Long buyOrderId, OrderLife sellOrder) {

        if (!TRADE_PAIR_MAP.containsKey(buyOrderId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | buyOrderId=%s", buyOrderId));
        }

        TradePair pair = TRADE_PAIR_MAP.get(buyOrderId);

        OrderLife force = pair.getForceProfitOrder();
        if (force == null) {
            throw new RuntimeException(String.format(" 不存在强制止盈单，无法移除 | sellOrderId=%s", sellOrder.getOrderId()));
        }

        if (!force.getOrderId().equals(sellOrder.getOrderId())) {
            throw new RuntimeException(
                String.format("不存在对应的强制止盈单，移除对象错误 | sellOrderId=%s | force=%s", sellOrder.getOrderId(),
                    force.getOrderId()));
        }

        pair.setForceProfitOrder(null);
        pair.setStatus(TradePairStatus.FORCE_PROFIT_CANCEL);
    }

    /**
     * 加入复购买单
     *
     * @param oldBuyId
     * @param reBuyOrder
     */
    public void putReBuyOrder(Long oldBuyId, OrderLife reBuyOrder) {

        if (!TRADE_PAIR_MAP.containsKey(oldBuyId)) {
            throw new RuntimeException(String.format("不存在对应的买单 | oldBuyId=%s", oldBuyId));
        }
        if (TRADE_PAIR_MAP.containsKey(reBuyOrder.getOrderId())) {
            throw new RuntimeException(String.format("不允许重复加入买单 | reBuyOrder=%s", reBuyOrder.getOrderId()));
        }

        // 移除原Map
        TradePair oldPair = TRADE_PAIR_MAP.get(oldBuyId);
        Integer oldReBuyTimes = oldPair.getReBuyTimes() != null ? oldPair.getReBuyTimes() : 0;
        TRADE_PAIR_MAP.remove(oldBuyId);

        // 新增复购
        TradePair pair = new TradePair();
        pair.setBuyOrder(reBuyOrder);
        pair.setStatus(TradePairStatus.NEW);
        pair.setReBuyTimes(oldReBuyTimes + 1);

        TRADE_PAIR_MAP.put(reBuyOrder.getOrderId(), pair);
    }

}

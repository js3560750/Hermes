//package com.aixi.lv.schedule;
//
//import java.util.List;
//
//import javax.annotation.Resource;
//
//import com.alibaba.fastjson.JSON;
//
//import com.aixi.lv.manage.OrderLifeManage;
//import com.aixi.lv.model.constant.OrderStatus;
//import com.aixi.lv.model.constant.TradePairStatus;
//import com.aixi.lv.model.domain.OrderLife;
//import com.aixi.lv.model.domain.TradePair;
//import com.aixi.lv.service.MailService;
//import com.aixi.lv.service.OrderService;
//import com.aixi.lv.strategy.buy.ReBuyStrategy;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
///**
// * @author Js
// */
////@Component
////@EnableScheduling   // 1.开启定时任务
////@EnableAsync        // 2.开启多线程
//@Slf4j
//public class OrderLifeTask {
//
//    @Resource
//    OrderLifeManage orderLifeManage;
//
//    @Resource
//    OrderService orderService;
//
//    @Resource
//    MailService mailService;
//
//    /**
//     * *
//     * *
//     * *
//     * *
//     * *
//     * ************************** 附加配置任务 *********************
//     * *
//     * *
//     * *
//     * *
//     * *
//     */
//
//    /**
//     * 打印交易对信息
//     */
//    @Async
//    @Scheduled(cron = "0 0/5 * * * ? ") // 每5分钟
//    public void printOrderLife() {
//
//        List<TradePair> allTradePair = orderLifeManage.getAllPair();
//
//        log.info(" 现存交易对 | pairSize= {}", allTradePair.size());
//
//        log.info(" 现存交易对 | pairList= {}", JSON.toJSONString(allTradePair));
//
//    }
//
//    /**
//     * *
//     * *
//     * *
//     * *
//     * *
//     * ************************** 买单 *********************
//     * *
//     * *
//     * *
//     * *
//     * *
//     */
//
//    /**
//     * 轮询检查买单状态
//     */
//    @Async
//    @Scheduled(cron = "6 0/1 * * * ? ") // 每分钟第6秒
//    public void checkBuyOrder() {
//
//        try {
//
//            List<TradePair> pairList = orderLifeManage.getAllPair();
//
//            for (TradePair pair : pairList) {
//
//                OrderLife buyOrder = pair.getBuyOrder();
//
//                // 已完成
//                if (TradePairStatus.ALREADY == pair.getStatus()) {
//                    continue;
//                }
//
//                // 已取消
//                if (TradePairStatus.CANCEL == pair.getStatus()) {
//                    orderLifeManage.removeTradePair(buyOrder.getOrderId());
//                    continue;
//                }
//
//                // 未成交的订单就去查一下
//                if (TradePairStatus.NEW == pair.getStatus()) {
//                    this.checkBuyOrderServer(buyOrder, pair);
//                    continue;
//                }
//
//            }
//
//        } catch (Exception e) {
//            log.error("OrderLifeTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("OrderLifeTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//    }
//
//    /**
//     * *
//     * *
//     * *
//     * *
//     * *
//     * ************************** 卖单 *********************
//     * *
//     * *
//     * *
//     * *
//     * *
//     */
//
//    /**
//     * 轮询检查止损卖单
//     */
//    @Async
//    @Scheduled(cron = "7 0/1 * * * ? ") // 每分钟第7秒
//    public void checkLossOrder() {
//
//        try {
//
//            List<TradePair> pairList = orderLifeManage.getAllPair();
//
//            for (TradePair pair : pairList) {
//
//                if (TradePairStatus.LOSS == pair.getStatus()) {
//                    this.checkLossOrderServer(pair.getBuyOrder(), pair.getLossOrder());
//                    continue;
//                }
//
//            }
//
//        } catch (Exception e) {
//            log.error("OrderLifeTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("OrderLifeTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//    }
//
//    /**
//     * 轮询检查止盈单
//     */
//    @Async
//    @Scheduled(cron = "8 0/1 * * * ? ") // 每分钟第8秒
//    public void checkProfitOrder() {
//
//        try {
//
//            List<TradePair> pairList = orderLifeManage.getAllPair();
//
//            for (TradePair pair : pairList) {
//
//                OrderLife buyOrder = pair.getBuyOrder();
//
//                if (TradePairStatus.FIRST_PROFIT == pair.getStatus()) {
//                    this.checkFirstProfitServer(buyOrder, pair.getFirstProfitOrder());
//                    continue;
//                }
//
//                if (TradePairStatus.SECOND_PROFIT == pair.getStatus()) {
//                    this.checkSecondProfitServer(buyOrder, pair.getSecondProfitOrder());
//                    continue;
//                }
//
//                if (TradePairStatus.THIRD_PROFIT == pair.getStatus()) {
//                    this.checkThirdProfitServer(buyOrder, pair.getThirdProfitOrder());
//                    continue;
//                }
//
//                if (TradePairStatus.FORCE_PROFIT == pair.getStatus()) {
//                    this.checkForceProfitServer(buyOrder, pair.getForceProfitOrder());
//                    continue;
//                }
//
//            }
//
//        } catch (Exception e) {
//            log.error("OrderLifeTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("OrderLifeTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//    }
//
//    /**
//     * *
//     * *
//     * *
//     * *                        上面是DTS任务区，下面都是private方法
//     * *
//     * *
//     * *
//     * *
//     * *
//     * *
//     * * ∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧
//     */
//
//    /**
//     * 检查买单服务端状态
//     *
//     * @param buyOrder
//     */
//    private void checkBuyOrderServer(OrderLife buyOrder, TradePair pair) {
//
//        if (buyOrder == null) {
//            return;
//        }
//
//        OrderLife buyServer = orderService.queryByOrderId(buyOrder.getSymbol(), buyOrder.getOrderId());
//
//        // 查到买单已成交，则更新Map
//        if (OrderStatus.FILLED == buyServer.getStatus()) {
//
//            Integer reBuyTimes = pair.getReBuyTimes();
//            log.info("订单成交 | pair = {} | buyServer = {}", JSON.toJSONString(pair), JSON.toJSONString(buyServer));
//
//            // pair 状态更新
//            orderLifeManage.doneBuyOrder(buyServer);
//
//            // 发邮件
//            if (reBuyTimes != null && reBuyTimes > 0) {
//                mailService.buyDealMail("复购成交", buyServer);
//            } else {
//                mailService.buyDealMail("买入成交", buyServer);
//            }
//            return;
//        }
//
//        // 查到买单已取消，则移出Map
//        if (OrderStatus.CANCELED == buyServer.getStatus()
//            || OrderStatus.EXPIRED == buyServer.getStatus()
//            || OrderStatus.REJECTED == buyServer.getStatus()) {
//            orderLifeManage.removeTradePair(buyServer.getBuyOrderId());
//            return;
//        }
//
//        // 查到买单未成交 or 部分成交，则等下一次扫描
//        if (OrderStatus.PARTIALLY_FILLED == buyServer.getStatus()
//            || OrderStatus.NEW == buyServer.getStatus()) {
//            return;
//        }
//    }
//
//    /**
//     * 检查服务端状态
//     *
//     * @param buyOrder
//     * @param lossOrder
//     */
//    private void checkLossOrderServer(OrderLife buyOrder, OrderLife lossOrder) {
//
//        if (lossOrder == null) {
//            return;
//        }
//
//        OrderLife lossServer = orderService.queryByOrderId(lossOrder.getSymbol(), lossOrder.getOrderId());
//
//        // 卖单已成交
//        if (OrderStatus.FILLED == lossServer.getStatus()) {
//            TradePair pair = orderLifeManage.getPairById(buyOrder.getOrderId());
//            if (pair.getReBuyTimes() != null && pair.getReBuyTimes() > ReBuyStrategy.MAX_RE_BUY_TIMES) {
//                mailService.tradeDoneEmail("止损成交（已到最大复购次数）", pair);
//                orderLifeManage.removeTradePair(pair.getBuyOrder().getOrderId());
//            } else {
//                mailService.tradeDoneEmail("止损成交", pair);
//                orderLifeManage.updateLossOrderToDone(buyOrder.getOrderId(), lossServer);
//            }
//            return;
//        }
//
//        // 卖单已取消
//        if (OrderStatus.CANCELED == lossServer.getStatus()
//            || OrderStatus.EXPIRED == lossServer.getStatus()
//            || OrderStatus.REJECTED == lossServer.getStatus()) {
//            orderLifeManage.removeLossOrder(buyOrder.getOrderId(), lossServer);
//            return;
//        }
//
//        // 卖单未成交 or 部分成交，则等下一次扫描
//        if (OrderStatus.PARTIALLY_FILLED == lossServer.getStatus()
//            || OrderStatus.NEW == lossServer.getStatus()) {
//            return;
//        }
//    }
//
//    private void checkFirstProfitServer(OrderLife buyOrder, OrderLife firstOrder) {
//
//        if (firstOrder == null) {
//            return;
//        }
//
//        OrderLife firstServer = orderService.queryByOrderId(firstOrder.getSymbol(), firstOrder.getOrderId());
//
//        // 卖单已成交
//        if (OrderStatus.FILLED == firstServer.getStatus()) {
//            mailService.sellDealEmail("一阶止盈成交", buyOrder, firstServer);
//            orderLifeManage.doneFirstProfit(buyOrder.getOrderId(), firstServer);
//            return;
//        }
//
//        // 卖单已取消
//        if (OrderStatus.CANCELED == firstServer.getStatus()
//            || OrderStatus.EXPIRED == firstServer.getStatus()
//            || OrderStatus.REJECTED == firstServer.getStatus()) {
//            orderLifeManage.removeFirstProfit(buyOrder.getOrderId(), firstServer);
//            return;
//        }
//
//        // 卖单未成交 or 部分成交，则等下一次扫描
//        if (OrderStatus.PARTIALLY_FILLED == firstServer.getStatus()
//            || OrderStatus.NEW == firstServer.getStatus()) {
//            return;
//        }
//    }
//
//    private void checkSecondProfitServer(OrderLife buyOrder, OrderLife secondOrder) {
//
//        if (secondOrder == null) {
//            return;
//        }
//
//        OrderLife secondServer = orderService.queryByOrderId(secondOrder.getSymbol(), secondOrder.getOrderId());
//
//        // 卖单已成交
//        if (OrderStatus.FILLED == secondServer.getStatus()) {
//            mailService.sellDealEmail("二阶止盈成交", buyOrder, secondServer);
//            orderLifeManage.doneSecondProfit(buyOrder.getOrderId(), secondServer);
//            return;
//        }
//
//        // 卖单已取消
//        if (OrderStatus.CANCELED == secondServer.getStatus()
//            || OrderStatus.EXPIRED == secondServer.getStatus()
//            || OrderStatus.REJECTED == secondServer.getStatus()) {
//            orderLifeManage.removeSecondProfit(buyOrder.getOrderId(), secondServer);
//            return;
//        }
//
//        // 卖单未成交 or 部分成交，则等下一次扫描
//        if (OrderStatus.PARTIALLY_FILLED == secondServer.getStatus()
//            || OrderStatus.NEW == secondServer.getStatus()) {
//            return;
//        }
//    }
//
//    private void checkThirdProfitServer(OrderLife buyOrder, OrderLife thirdOrder) {
//
//        if (thirdOrder == null) {
//            return;
//        }
//
//        OrderLife thirdServer = orderService.queryByOrderId(thirdOrder.getSymbol(), thirdOrder.getOrderId());
//
//        // 卖单已成交
//        if (OrderStatus.FILLED == thirdServer.getStatus()) {
//            TradePair pair = orderLifeManage.getPairById(buyOrder.getOrderId());
//            mailService.tradeDoneEmail("三阶止盈成交", pair);
//            orderLifeManage.doneThirdProfit(buyOrder.getOrderId(), thirdServer);
//            return;
//        }
//
//        // 卖单已取消
//        if (OrderStatus.CANCELED == thirdServer.getStatus()
//            || OrderStatus.EXPIRED == thirdServer.getStatus()
//            || OrderStatus.REJECTED == thirdServer.getStatus()) {
//            orderLifeManage.removeThirdProfit(buyOrder.getOrderId(), thirdServer);
//            return;
//        }
//
//        // 卖单未成交 or 部分成交，则等下一次扫描
//        if (OrderStatus.PARTIALLY_FILLED == thirdServer.getStatus()
//            || OrderStatus.NEW == thirdServer.getStatus()) {
//            return;
//        }
//    }
//
//    private void checkForceProfitServer(OrderLife buyOrder, OrderLife forceOrder) {
//
//        if (forceOrder == null) {
//            return;
//        }
//
//        OrderLife forceServer = orderService.queryByOrderId(forceOrder.getSymbol(), forceOrder.getOrderId());
//
//        // 卖单已成交
//        if (OrderStatus.FILLED == forceServer.getStatus()) {
//            TradePair pair = orderLifeManage.getPairById(buyOrder.getOrderId());
//            mailService.tradeDoneEmail("强制止盈成交", pair);
//            orderLifeManage.doneForceProfit(buyOrder.getOrderId(), forceServer);
//            return;
//        }
//
//        // 卖单已取消
//        if (OrderStatus.CANCELED == forceServer.getStatus()
//            || OrderStatus.EXPIRED == forceServer.getStatus()
//            || OrderStatus.REJECTED == forceServer.getStatus()) {
//            orderLifeManage.removeForceProfit(buyOrder.getOrderId(), forceServer);
//            return;
//        }
//
//        // 卖单未成交 or 部分成交，则等下一次扫描
//        if (OrderStatus.PARTIALLY_FILLED == forceServer.getStatus()
//            || OrderStatus.NEW == forceServer.getStatus()) {
//            return;
//        }
//    }
//
//}

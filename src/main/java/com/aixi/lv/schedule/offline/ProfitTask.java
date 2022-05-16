//package com.aixi.lv.schedule;
//
//import javax.annotation.Resource;
//
//import com.aixi.lv.model.constant.Symbol;
//import com.aixi.lv.service.MailService;
//import com.aixi.lv.strategy.profit.FirstProfitStrategy;
//import com.aixi.lv.strategy.profit.ForceProfitStrategy;
//import com.aixi.lv.strategy.profit.SecondProfitStrategy;
//import com.aixi.lv.strategy.profit.ThirdProfitStrategy;
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
//public class ProfitTask {
//
//    @Resource
//    FirstProfitStrategy firstProfitStrategy;
//
//    @Resource
//    SecondProfitStrategy secondProfitStrategy;
//
//    @Resource
//    ThirdProfitStrategy thirdProfitStrategy;
//
//    @Resource
//    ForceProfitStrategy forceProfitStrategy;
//
//    @Resource
//    MailService mailService;
//
//    /**
//     * 轮询止盈策略
//     */
//    @Async
//    @Scheduled(cron = "40 0/1 * * * ? ") // 每分钟第40秒
//    public void firstProfit() {
//
//        try {
//
//            firstProfitStrategy.firstProfit(Symbol.ETHUSDT);
//            firstProfitStrategy.firstProfit(Symbol.DOGEUSDT);
//            firstProfitStrategy.firstProfit(Symbol.BTCUSDT);
//
//        } catch (Exception e) {
//            log.error("ProfitTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("ProfitTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//
//    }
//
//    /**
//     * 轮询止盈策略
//     */
//    @Async
//    @Scheduled(cron = "43 0/1 * * * ? ") // 每分钟第43秒
//    public void secondProfit() {
//
//        try {
//
//            secondProfitStrategy.secondProfit(Symbol.ETHUSDT);
//            secondProfitStrategy.secondProfit(Symbol.DOGEUSDT);
//            secondProfitStrategy.secondProfit(Symbol.BTCUSDT);
//
//        } catch (Exception e) {
//            log.error("ProfitTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("ProfitTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//
//    }
//
//    /**
//     * 轮询止盈策略
//     */
//    @Async
//    @Scheduled(cron = "46 0/1 * * * ? ") // 每分钟第46秒
//    public void thirdProfit() {
//
//        try {
//
//            thirdProfitStrategy.thirdProfit(Symbol.ETHUSDT);
//            thirdProfitStrategy.thirdProfit(Symbol.DOGEUSDT);
//            thirdProfitStrategy.thirdProfit(Symbol.BTCUSDT);
//
//        } catch (Exception e) {
//            log.error("ProfitTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("ProfitTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//
//    }
//
//    /**
//     * 轮询止盈策略
//     */
//    @Async
//    @Scheduled(cron = "49 0/1 * * * ? ") // 每分钟第49秒
//    public void forceProfit() {
//
//        try {
//
//            forceProfitStrategy.forceProfit(Symbol.ETHUSDT);
//            forceProfitStrategy.forceProfit(Symbol.DOGEUSDT);
//            forceProfitStrategy.forceProfit(Symbol.BTCUSDT);
//
//        } catch (Exception e) {
//            log.error("ProfitTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("ProfitTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//
//    }
//
//}

//package com.aixi.lv.schedule;
//
//import javax.annotation.Resource;
//
//import com.aixi.lv.model.constant.Interval;
//import com.aixi.lv.model.constant.Symbol;
//import com.aixi.lv.service.MailService;
//import com.aixi.lv.strategy.buy.BuyStrategy;
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
//public class BuyTask {
//
//    @Resource
//    BuyStrategy buyStrategy;
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
//    @Async
//    @Scheduled(cron = "0 0 0/6 * * ? ") // 每6小时
//    public void clearBuyTimes() {
//
//        // 清理购买上限次数
//        buyStrategy.clearBuyLimitMap();
//    }
//
//    /**
//     * *
//     * *
//     * *
//     * *
//     * *
//     * ************************** 5分钟的 *********************
//     * *
//     * *
//     * *
//     * *
//     * *
//     */
//
//    @Async
//    @Scheduled(cron = "30 0/1 * * * ? ") // 每分钟第30秒
//    public void MINUTE_5_ID_2000() {
//
//        try {
//
//            buyStrategy.buy(Symbol.ETHUSDT, Interval.MINUTE_5, 144, 2000);
//            buyStrategy.buy(Symbol.BTCUSDT, Interval.MINUTE_5, 144, 2000);
//
//        } catch (Exception e) {
//            log.error("BuyTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("BuyTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//    }
//
//    @Async
//    @Scheduled(cron = "0/5 * * * * ? ") // 每5秒执行一次
//    public void MINUTE_5_ID_2000_High() {
//
//        try {
//
//            buyStrategy.buyHighFrequencyScan(Symbol.ETHUSDT, Interval.MINUTE_5, 2000);
//            buyStrategy.buyHighFrequencyScan(Symbol.BTCUSDT, Interval.MINUTE_5, 2000);
//
//        } catch (Exception e) {
//            log.error("BuyTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("BuyTask异常", e.getMessage());
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
//     * ************************** 10分钟的 *********************
//     * *
//     * *
//     * *
//     * *
//     * *
//     */
//
//    @Async
//    @Scheduled(cron = "0 0/1 * * * ? ") // 每分钟第0秒
//    public void MINUTE_15_ID_1000() {
//
//        try {
//
//            buyStrategy.buy(Symbol.ETHUSDT, Interval.MINUTE_15, 96, 1000);
//            buyStrategy.buy(Symbol.BTCUSDT, Interval.MINUTE_15, 96, 1000);
//
//        } catch (Exception e) {
//            log.error("BuyTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("BuyTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//    }
//
//    @Async
//    @Scheduled(cron = "0/5 * * * * ? ") // 每5秒执行一次
//    public void MINUTE_15_ID_1000_High() {
//
//        try {
//
//            buyStrategy.buyHighFrequencyScan(Symbol.ETHUSDT, Interval.MINUTE_15, 1000);
//            buyStrategy.buyHighFrequencyScan(Symbol.BTCUSDT, Interval.MINUTE_15, 1000);
//
//        } catch (Exception e) {
//            log.error("BuyTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("BuyTask异常", e.getMessage());
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
//}

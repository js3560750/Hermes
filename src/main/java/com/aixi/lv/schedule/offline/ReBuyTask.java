//package com.aixi.lv.schedule;
//
//import javax.annotation.Resource;
//
//import com.aixi.lv.model.constant.Symbol;
//import com.aixi.lv.service.MailService;
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
// *
// * 复购任务
// */
////@Component
////@EnableScheduling   // 1.开启定时任务
////@EnableAsync        // 2.开启多线程
//@Slf4j
//public class ReBuyTask {
//
//    @Resource
//    ReBuyStrategy reBuyStrategy;
//
//    @Resource
//    MailService mailService;
//
//    /**
//     * 轮询止盈策略
//     */
//    @Async
//    @Scheduled(cron = "52 0/1 * * * ? ") // 每分钟第52秒
//    public void firstProfit() {
//
//        try {
//
//            reBuyStrategy.reBuy(Symbol.ETHUSDT);
//            reBuyStrategy.reBuy(Symbol.DOGEUSDT);
//            reBuyStrategy.reBuy(Symbol.BTCUSDT);
//
//        } catch (Exception e) {
//            log.error("ReBuyTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("ReBuyTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//
//    }
//}

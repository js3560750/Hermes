//package com.aixi.lv.schedule;
//
//import javax.annotation.Resource;
//
//import com.aixi.lv.model.constant.Symbol;
//import com.aixi.lv.service.MailService;
//import com.aixi.lv.strategy.loss.StopLossStrategy;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
///**
// * @author Js
// * @date 2022/1/3 12:49 下午
// */
////@Component
////@EnableScheduling   // 1.开启定时任务
////@EnableAsync        // 2.开启多线程
//@Slf4j
//public class StopLossTask {
//
//    @Resource
//    StopLossStrategy stopLossStrategy;
//
//    @Resource
//    MailService mailService;
//
//    /**
//     * 轮询止损策略
//     */
//    @Async
//    @Scheduled(cron = "55 0/1 * * * ? ") // 每分钟第55秒
//    public void stopLoss() {
//
//        try {
//
//            stopLossStrategy.stopLoss(Symbol.ETHUSDT);
//            stopLossStrategy.stopLoss(Symbol.DOGEUSDT);
//            stopLossStrategy.stopLoss(Symbol.BTCUSDT);
//
//        } catch (Exception e) {
//            log.error("StopLossTask异常 " + e.getMessage(), e);
//            mailService.sendEmail("StopLossTask异常", e.getMessage());
//            // 终止程序运行
//            System.exit(0);
//        }
//
//    }
//}

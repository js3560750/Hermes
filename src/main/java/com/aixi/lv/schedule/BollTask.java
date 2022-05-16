package com.aixi.lv.schedule;


import javax.annotation.Resource;


import com.aixi.lv.service.MailService;
import com.aixi.lv.strategy.indicator.BollSellStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @author Js
 */
//@Component
//@EnableScheduling   // 1.开启定时任务
//@EnableAsync        // 2.开启多线程
//@Profile({"lv","hermes"})
@Slf4j
public class BollTask {

    @Resource
    BollSellStrategy bollSellStrategy;

    @Resource
    MailService mailService;

    //@Async
    //@Scheduled(cron = "10 5/5 0/1 * * ? ") // 每小时 第5分钟10秒开始执行 ，每隔5分钟执行一次
    public void bollSellTask() {

        try {

            bollSellStrategy.detectSell();

        } catch (Exception e) {
            log.error("MacdTask异常 " + e.getMessage(), e);
            mailService.sendEmail("MacdTask异常", e.getMessage());
            // 终止程序运行
            System.exit(0);
        }

    }
}

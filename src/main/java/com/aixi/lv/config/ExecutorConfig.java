package com.aixi.lv.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Js
 */
@Configuration
public class ExecutorConfig {

    @Bean("listeningExecutorService")
    public ListeningExecutorService executorService() {

        return MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor(4,
                8,
                30,
                TimeUnit.SECONDS,
                // 有界阻塞队列
                new LinkedBlockingQueue<>(20000)));
    }
}

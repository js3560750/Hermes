package com.aixi.lv;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.aixi.lv.service.BackTestAppendService;
import com.aixi.lv.service.BackTestReadService;
import com.aixi.lv.service.BackTestWriteService;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.service.PriceService;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author Js
 */
@Slf4j
public class 准备数据Test extends BaseTest {

    @Resource
    ListeningExecutorService listeningExecutorService;

    @Resource
    PriceService priceService;

    @Resource
    BackTestWriteService backTestWriteService;

    @Resource
    BackTestReadService backTestReadService;

    @Resource
    BackTestAppendService backTestAppendService;

    @Test
    public void 追加K线数据到此时() {

        //for (Symbol symbol : Symbol.values()) {
        //    backTestAppendService.appendData(symbol);
        //    System.out.println(symbol.getCode() + " 追加K线数据完成");
        //}

        // 需要1分钟级数据的
        List<Symbol> needMinuteOne = Lists.newArrayList();
        needMinuteOne.add(Symbol.APEUSDT);

        for (Symbol symbol : needMinuteOne) {
            backTestAppendService.appendMinuteOne(symbol);
        }

    }

    /**
     * 初始化K线数据到此时
     */
    @Test
    public void 初始化K线数据() {

       backTestWriteService.initData(Symbol.APEUSDT);
       //backTestWriteService.initData(Symbol.SHIBUSDT);
       //backTestWriteService.initData(Symbol.LUNAUSDT);
       //backTestWriteService.initData(Symbol.NEARUSDT);
       //backTestWriteService.initData(Symbol.PEOPLEUSDT);
       //backTestWriteService.initData(Symbol.SOLUSDT);
       //backTestWriteService.initData(Symbol.GMTUSDT);

    }

    private void 初始化单个Symbol数据(Symbol symbol) {
        backTestWriteService.minuteOne(symbol, symbol.getSaleDate().atStartOfDay(), LocalDateTime.now());
    }

    /**
     * 准备K线数据到此时
     */
    @Test
    public void 准备K线数据_多线程() {

        List<Symbol> list = Lists.newArrayList();
        list.add(Symbol.SOLUSDT);

        final CountDownLatch cd = new CountDownLatch(list.size());
        final List<Throwable> throwableList = Lists.newArrayList();

        for (Symbol symbol : list) {

            ListenableFuture future = listeningExecutorService.submit(
                () -> backTestWriteService.initData(symbol));

            Futures.addCallback(future, new FutureCallback() {

                @Override
                public void onSuccess(@Nullable Object o) {
                    try {

                    } finally {
                        cd.countDown();
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    try {
                        throwableList.add(throwable);
                    } finally {
                        cd.countDown();
                    }
                }
            }, listeningExecutorService);
        }

        try {
            // cd 计数器减到0 或者超时时间到了，继续执行主线程
            boolean await = cd.await(30, TimeUnit.MINUTES);
            if (!await) {
                throw new RuntimeException(" 准备K线数据 | 主线程等待超过了设定的30秒超时时间");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(" 准备K线数据 | 计数器异常", e);
        }

        // 异常判断必须在 cd.await之后
        if (throwableList.size() > 0) {
            throw new RuntimeException(throwableList.get(0));
        }
    }

}

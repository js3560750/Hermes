package com.aixi.lv.analysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.util.CombineUtil;
import com.aixi.lv.util.NumUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_ACCOUNT_NAME;
import static com.aixi.lv.config.BackTestConfig.INIT_BACK_TEST_AMOUNT;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 *
 * 币种数量分析
 */
@Component
@Slf4j
public class SymbolNumAnalysis {

    @Resource
    BackTestAnalysis backTestAnalysis;

    @Resource
    ListeningExecutorService listeningExecutorService;

    public static final List<JSONObject> ANALYSIS_RESULT_LIST = Lists.newArrayList();

    public void 近期币种数量分析_多线程(LocalDateTime endTime, Integer durationDay, Integer initSymbolSize) {

        log.warn("开始分析 近期币种数量分析_多线程");

        // 初始币种
        List<Symbol> symbolList = Lists.newArrayList(Symbol.values());
        //List<Symbol> symbolList = Lists.newArrayList();
        //symbolList.add(Symbol.BTCUSDT);
        //symbolList.add(Symbol.SANDUSDT);
        //symbolList.add(Symbol.MANAUSDT);

        // 结束时间

        // 循环最大次数
        Integer maxCount = 2000;

        // 起始币种数量
        Integer size;
        if (initSymbolSize == null) {
            size = 8;
        } else {
            size = initSymbolSize;
        }

        List<MutablePair<Integer, List<List<Symbol>>>> listPair = Lists.newArrayList();

        while (size >= 1) {

            List<List<Symbol>> lists = CombineUtil.assignSymbolCombine(symbolList, size);
            Collections.shuffle(lists);

            listPair.add(MutablePair.of(size, lists));

            if (size > 10) {
                size = size - 3;
            } else {
                size = size - 1;
            }
        }

        final CountDownLatch cd = new CountDownLatch(listPair.size());
        final List<Throwable> throwableList = Lists.newArrayList();

        for (MutablePair<Integer, List<List<Symbol>>> pair : listPair) {

            ListenableFuture future = listeningExecutorService.submit(
                () -> this.analysisAction(pair, endTime, maxCount, durationDay));

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
            boolean await = cd.await(3, TimeUnit.DAYS);
            if (!await) {
                throw new RuntimeException(" 多线程分析 | 主线程等待超过了设定的3天超时时间");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(" 多线程分析 | 计数器异常", e);
        }

        // 异常判断必须在 cd.await之后
        if (throwableList.size() > 0) {
            throw new RuntimeException(throwableList.get(0));
        }

    }

    /**
     * 具体的分析逻辑
     *
     * @param pair
     */
    private void analysisAction(MutablePair<Integer, List<List<Symbol>>> pair, LocalDateTime endTime,
        Integer maxCount, Integer durationDay) {

        Integer size = pair.getLeft();
        List<List<Symbol>> lists = pair.getRight();

        MacdAccount account = new MacdAccount();
        account.setName(BACK_TEST_ACCOUNT_NAME + size);

        THREAD_LOCAL_ACCOUNT.set(account);
        log.warn(" 当前分析的账户名 = {}", THREAD_LOCAL_ACCOUNT.get().getName());

        Double accountRate = 0d;
        Double natureRate = 0d;
        Integer count = 0;

        for (List<Symbol> curSymbolList : lists) {

            // 最多做多少次
            if (count >= maxCount) {
                break;
            }

            LocalDateTime tempStart = endTime.minusDays(durationDay);

            // 初始化账户
            initBackTestAccount(curSymbolList);

            // 具体回测逻辑
            backTestAnalysis.doAnalysis(tempStart, endTime);

            accountRate += THREAD_LOCAL_ACCOUNT.get().getCurBackTestRate();
            natureRate += THREAD_LOCAL_ACCOUNT.get().getSymbolNatureRate();

            count++;

            if (count % 100 == 0) {
                log.warn(" 币种数量 = {} | 当前次数 = {}", size, count);
            }
        }

        String averageAccountRate = NumUtil.percent(accountRate / count);
        String averageNatureRate = NumUtil.percent(natureRate / count);
        log.warn(" 币种数量 = {} | 账户多次平均增长率 = {} | 币种多次平均自然增长率 = {}",
            size,
            StringUtils.rightPad(averageAccountRate, 8),
            StringUtils.rightPad(averageNatureRate, 8));

        // 存分析结果
        JSONObject js = new JSONObject();
        js.put("accountName", THREAD_LOCAL_ACCOUNT.get().getName());
        js.put("size", size);
        js.put("endTime", endTime);
        js.put("accountRate", accountRate / count);
        js.put("natureRate", natureRate / count);
        ANALYSIS_RESULT_LIST.add(js);

    }

    /**
     * 初始化回测账户
     */
    private void initBackTestAccount(List<Symbol> curSymbolList) {

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();
        account.setSymbolList(curSymbolList);
        account.setCurBackTestRate(NumberUtils.DOUBLE_ZERO);
        account.setSymbolNatureRate(NumberUtils.DOUBLE_ZERO);
        account.setCurHoldQty(BigDecimal.ZERO);
        account.setCurHoldSymbol(null);
        account.setCurHoldAmount(new BigDecimal(INIT_BACK_TEST_AMOUNT));
        account.setLastBuyPrice(null);
        account.setBackTestTotalLoss(BigDecimal.ZERO);
        account.setBackTestTotalProfit(BigDecimal.ZERO);
        account.setBackTestProfitTimes(0);
        account.setBackTestLossTimes(0);

    }
}

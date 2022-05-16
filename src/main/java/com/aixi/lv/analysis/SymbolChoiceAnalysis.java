package com.aixi.lv.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
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
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_ACCOUNT_NAME;
import static com.aixi.lv.config.BackTestConfig.INIT_BACK_TEST_AMOUNT;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 *
 * 币种组合分析
 */
@Component
@Slf4j
public class SymbolChoiceAnalysis {

    public static final Map<String, JSONObject> ACCOUNT_RATE_MAP = new HashMap<>();
    public static final Map<String, Double> CHOICE_RATE_MAP = new LinkedHashMap<>();

    @Resource
    ListeningExecutorService listeningExecutorService;

    @Resource
    BackTestAnalysis backTestAnalysis;

    public void 币种组合分析_分析结果打印(LocalDateTime endTime, Integer durationDay,
        List<Symbol> choiceSymbolList, Integer combineSize) {

        List<String> symbolNameList = this.币种组合分析(endTime, durationDay, null, choiceSymbolList, combineSize, null,
            null);

        List<List<Symbol>> symbolCombineList = Lists.newArrayList();

        Set<Symbol> choiceSymbolSet = new HashSet<>();

        // 解析增长率前100的币种组合
        for (String str : symbolNameList) {

            String substring = str.substring(BACK_TEST_ACCOUNT_NAME.length());
            String[] splits = StringUtils.split(substring, " ");

            List<Symbol> tempList = Lists.newArrayList();
            for (String temp : splits) {
                Symbol tempSymbol = Symbol.getByCode(temp);
                tempList.add(tempSymbol);

                // 选择的币种不要超过9个
                if (choiceSymbolSet.size() >= 9) {
                    continue;
                } else {
                    choiceSymbolSet.add(tempSymbol);
                }

            }

            symbolCombineList.add(tempList);
        }

        // 信息打印
        for (List<Symbol> combine : symbolCombineList) {
            System.out.println("增长率前100的币种组合 = " + JSON.toJSONString(combine));
        }

        System.out.println("最终选择的币种");
        choiceSymbolSet.stream().forEach(i -> System.out.println(i));
    }

    /**
     * @param endTime          回测截止日期
     * @param durationDay      回测天数
     * @param maxCircle        最大循环次数， 设置1的话，就是回测多少天，就跑多少天
     * @param choiceSymbolList 初始可供选择的币种
     * @param combineSize      币种组合数量
     * @param sb               用来打印信息的
     * @return 增长率前100的币种组合
     */
    public List<String> 币种组合分析(LocalDateTime endTime, Integer durationDay, Integer maxCircle,
        List<Symbol> choiceSymbolList, Integer combineSize, StringBuilder sb, Integer stepDay) {

        // 6种 74613
        // 5种 26334
        // 4种 7315
        // 3种 1540
        // 2种 231
        // 1种 22

        // 获取组合后的币种list
        List<List<Symbol>> lists = CombineUtil.assignSymbolCombine(choiceSymbolList, combineSize);

        LocalDateTime finalEndTime = LocalDateTime.of(2021, 10, 1, 0, 0);

        Integer forCount = 0;

        // 最大循环次数
        if (maxCircle == null) {
            maxCircle = 1;
        }

        if (sb == null) {
            sb = new StringBuilder();
        }

        if (stepDay == null) {
            stepDay = 5;
        }

        // 增长率记录先初始化
        ACCOUNT_RATE_MAP.clear();

        log.warn(" 开始币种组合分析 | 币种组合数量 = {}", lists.size());

        for (int i = 0; i < maxCircle; i++) {

            if (endTime.isBefore(finalEndTime)) {
                break;
            }

            // 回测的持续天数
            LocalDateTime startTime = endTime.minusDays(durationDay);

            this.多线程分析(lists, startTime, endTime);

            forCount++;
            endTime = endTime.minusDays(stepDay);

            log.warn(" 当前周期 = {} | 大循环已完成次数 = {}", durationDay, forCount);

        }

        List<JSONObject> accountRateList = Lists.newArrayList(ACCOUNT_RATE_MAP.values());

        // 排序 默认 从小到大
        Collections.sort(accountRateList, Comparator.comparing(o -> o.getDouble("accountRate")));

        // 逆序，保证 从大到小
        Collections.reverse(accountRateList);

        Double totalRate = 0d;
        Double totalNature = 0d;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        Double profitTimes = 0d;
        Double lossTimes = 0d;

        Integer count = 0;

        List<String> symbolNameList = Lists.newArrayList();

        for (int i = 0; i < accountRateList.size(); i++) {

            JSONObject jo = accountRateList.get(i);

            // 只取前100
            if (i < 100) {
                symbolNameList.add(jo.getString("name"));
            }

            log.info("回测币种增长率 = {} | 自然增长率 = {} | 单一组合 = {}",
                NumUtil.percent(jo.getDouble("accountRate") / forCount),
                NumUtil.percent(jo.getDouble("natureRate") / forCount),
                jo.getString("name"));

            totalRate += (jo.getDouble("accountRate") / forCount);
            totalNature += (jo.getDouble("natureRate") / forCount);

            totalProfit = totalProfit.add(jo.getBigDecimal("totalProfit").divide(new BigDecimal(forCount), 4,
                RoundingMode.HALF_DOWN));
            totalLoss = totalLoss.add(jo.getBigDecimal("totalLoss").divide(new BigDecimal(forCount), 4,
                RoundingMode.HALF_DOWN));

            profitTimes += (jo.getDouble("profitTimes") / forCount);
            lossTimes += (jo.getDouble("lossTimes") / forCount);

            count++;
        }

        BigDecimal profitLossRate = BigDecimal.ZERO;
        if (totalLoss.compareTo(BigDecimal.ZERO) != 0) {
            profitLossRate = totalProfit.divide(totalLoss, 4, RoundingMode.HALF_DOWN);
        }

        // 信息记录
        sb.append(String.format(
                " 天数 = %s | 账户增长率 = %s | 币种增长率 = %s | 盈亏比 = %s | 盈利 = %s | 亏损 = %s | 盈利次数 = %s | 亏损次数 = %s",
                StringUtils.rightPad(durationDay.toString(), 3),
                StringUtils.rightPad(NumUtil.percent(totalRate / count), 8),
                StringUtils.rightPad(NumUtil.percent(totalNature / count), 8),
                StringUtils.rightPad(NumUtil.showDouble(profitLossRate.doubleValue()), 6),
                StringUtils.rightPad(NumUtil.showDouble(totalProfit.doubleValue() / count), 10),
                StringUtils.rightPad(NumUtil.showDouble(totalLoss.doubleValue() / count), 10),
                StringUtils.rightPad(NumUtil.showDouble(profitTimes / count), 6),
                StringUtils.rightPad(NumUtil.showDouble(lossTimes / count), 6)
            )
        );
        sb.append("\n");

        // 记录一下多次调用时的合计增长率
        String choiceKey = JSON.toJSONString(choiceSymbolList);
        if (CHOICE_RATE_MAP.containsKey(choiceKey)) {
            Double temp = CHOICE_RATE_MAP.get(choiceKey);
            CHOICE_RATE_MAP.put(choiceKey, (totalRate / count) + temp);
        } else {
            CHOICE_RATE_MAP.put(choiceKey, totalRate / count);
        }

        return symbolNameList;

    }

    private void 多线程分析(List<List<Symbol>> lists, LocalDateTime startTime, LocalDateTime endTime) {

        final CountDownLatch cd = new CountDownLatch(lists.size());
        final List<Throwable> throwableList = Lists.newArrayList();

        for (List<Symbol> symbolList : lists) {

            ListenableFuture future = listeningExecutorService.submit(
                () -> this.analysisAction(symbolList, startTime, endTime));

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

    private void analysisAction(List<Symbol> symbolList, LocalDateTime startTime, LocalDateTime endTime) {

        // 初始化账户
        initBackTestAccount(symbolList);

        // 具体回测逻辑
        backTestAnalysis.doAnalysis(startTime, endTime);

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();
        String name = account.getName();

        // 账户增长率
        if (ACCOUNT_RATE_MAP.containsKey(name)) {
            JSONObject temp = ACCOUNT_RATE_MAP.get(name);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            // 累加的，不求平均
            jsonObject.put("accountRate", account.getCurBackTestRate() + temp.getDouble("accountRate"));
            jsonObject.put("natureRate", account.getSymbolNatureRate() + temp.getDouble("natureRate"));
            jsonObject.put("totalProfit", account.getBackTestTotalProfit().add(temp.getBigDecimal("totalProfit")));
            jsonObject.put("totalLoss", account.getBackTestTotalLoss().add(temp.getBigDecimal("totalLoss")));
            jsonObject.put("profitTimes", account.getBackTestProfitTimes() + temp.getInteger("profitTimes"));
            jsonObject.put("lossTimes", account.getBackTestLossTimes() + temp.getInteger("lossTimes"));
            ACCOUNT_RATE_MAP.put(name, jsonObject);
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("accountRate", account.getCurBackTestRate());
            jsonObject.put("natureRate", account.getSymbolNatureRate());
            jsonObject.put("totalProfit", account.getBackTestTotalProfit());
            jsonObject.put("totalLoss", account.getBackTestTotalLoss());
            jsonObject.put("profitTimes", account.getBackTestProfitTimes());
            jsonObject.put("lossTimes", account.getBackTestLossTimes());
            ACCOUNT_RATE_MAP.put(name, jsonObject);
        }

    }

    private String getSymbolChoiceName(List<Symbol> symbolList) {

        List<Symbol> sortedList = symbolList.stream()
            .sorted(Comparator.comparing(Symbol::getCode))
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : sortedList) {
            sb.append(symbol.getCode());
            sb.append(" ");
        }

        return sb.toString();

    }

    /**
     * 初始化回测账户
     */
    private void initBackTestAccount(List<Symbol> symbolList) {

        MacdAccount account = new MacdAccount();

        account.setName(BACK_TEST_ACCOUNT_NAME + getSymbolChoiceName(symbolList));
        THREAD_LOCAL_ACCOUNT.set(account);

        account.setSymbolList(symbolList);
        account.setCurBackTestRate(NumberUtils.DOUBLE_ZERO);
        account.setSymbolNatureRate(NumberUtils.DOUBLE_ZERO);
        account.setCurHoldQty(BigDecimal.ZERO);
        account.setCurHoldSymbol(null);
        account.setCurHoldAmount(new BigDecimal(INIT_BACK_TEST_AMOUNT));
        account.setCurBackTestComputeTime(null);
        account.setBackTestTotalLoss(BigDecimal.ZERO);
        account.setBackTestTotalProfit(BigDecimal.ZERO);
        account.setBackTestProfitTimes(0);
        account.setBackTestLossTimes(0);
        account.setReadySellFlag(Boolean.FALSE);
        account.setReadySellTime(null);
        account.setLastSellTime(null);
        account.setLastSellSymbol(null);
        account.setLastBuyPrice(null);
        account.setCurPair(null);

    }

}

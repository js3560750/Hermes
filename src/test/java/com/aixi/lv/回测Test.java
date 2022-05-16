package com.aixi.lv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.analysis.BackTestAnalysis;
import com.aixi.lv.analysis.SymbolChoiceAnalysis;
import com.aixi.lv.analysis.SymbolNumAnalysis;
import com.aixi.lv.config.BackTestConfig;
import com.aixi.lv.controller.BackTestController;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_DATA_MAP;
import static com.aixi.lv.config.BackTestConfig.INIT_BACK_TEST_AMOUNT;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 */
@Slf4j
public class 回测Test extends BaseTest {

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BackTestAnalysis backTestAnalysis;

    @Resource
    SymbolNumAnalysis symbolNumAnalysis;

    @Resource
    SymbolChoiceAnalysis symbolChoiceAnalysis;

    @Resource
    BackTestController backTestController;

    @Test
    public void 币种组合分析_单次回测() {

        long start = System.currentTimeMillis();

        List<Symbol> choiceSymbolList = Lists.newArrayList();

            //choiceSymbolList.add(Symbol.GALAUSDT);
            //choiceSymbolList.add(Symbol.MATICUSDT);
            //choiceSymbolList.add(Symbol.BNBUSDT);
            //choiceSymbolList.add(Symbol.SHIBUSDT);
            //choiceSymbolList.add(Symbol.SOLUSDT);
            //choiceSymbolList.add(Symbol.ETHUSDT);
            //choiceSymbolList.add(Symbol.LUNAUSDT);

        choiceSymbolList.add(Symbol.XRPUSDT);
        choiceSymbolList.add(Symbol.BTCUSDT);
        choiceSymbolList.add(Symbol.ADAUSDT);
        choiceSymbolList.add(Symbol.AVAXUSDT);
        choiceSymbolList.add(Symbol.PEOPLEUSDT);
        choiceSymbolList.add(Symbol.TRXUSDT);
        choiceSymbolList.add(Symbol.BETAUSDT);


        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 16, 23, 0);

        StringBuilder sb = new StringBuilder();

        // 分析逻辑
        symbolChoiceAnalysis.币种组合分析(endTime, 15, null, choiceSymbolList, combineSize, sb,1);

        System.out.println(sb);

        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);

    }

    @Test
    public void 多线程分析币种数量() {

        long start = System.currentTimeMillis();

        LocalDateTime endTime = LocalDateTime.of(2022, 1, 17, 0, 0);

        // 分析逻辑
        symbolNumAnalysis.近期币种数量分析_多线程(endTime, 60, 8);

        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);
    }

    @Test
    public void 多次币种数量分析() {

        String js = "Js83862973~";
        Integer durationDay = 30;
        backTestController.多次币种数量分析(js, durationDay, 2022, 2, 1, 2, 2);

    }

    @Test
    public void 增长率() {

        List<Symbol> symbolList = Lists.newArrayList(Symbol.values());
        //symbolList.add(Symbol.BTCUSDT);
        //symbolList.add(Symbol.ETHUSDT);

        LocalDateTime end = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime start = end.minusDays(30);

        Double totalRate = 0d;

        for (Symbol symbol : symbolList) {

            LocalDateTime startDay = LocalDateTime.of(start.getYear(), start.getMonth(), start.getDayOfMonth(), 0, 0);
            KLine startKLine = BACK_TEST_DATA_MAP.get(BackTestConfig.key(symbol, Interval.DAY_1)).get(
                startDay.toString());

            BigDecimal startPrice = startKLine.getClosingPrice();

            LocalDateTime endDay = LocalDateTime.of(end.getYear(), end.getMonth(), end.getDayOfMonth(), 0, 0);
            KLine endKLine = BACK_TEST_DATA_MAP.get(BackTestConfig.key(symbol, Interval.DAY_1)).get(
                endDay.toString());

            BigDecimal endPrice = endKLine.getClosingPrice();

            Double rate = endPrice.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_DOWN).doubleValue();

            System.out.println(symbol + " 增长率 = " + rate);

            totalRate += rate;

        }

        System.out.println("平均增长率 = " + totalRate / symbolList.size());

    }

    @Test
    public void 单次回测() {

        // 开启 1分钟级 K线
        BackTestConfig.MINUTE_ONE_K_LINE_OPEN = Boolean.TRUE;

        LocalDateTime startTime = LocalDateTime.of(2022, 3, 21, 1, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 27, 12, 0);

        List<Symbol> symbolList = Lists.newArrayList();
        symbolList.add(Symbol.BTCUSDT);
        symbolList.add(Symbol.ETHUSDT);
        symbolList.add(Symbol.PEOPLEUSDT);
        symbolList.add(Symbol.LUNAUSDT);
        symbolList.add(Symbol.GALAUSDT);

        MacdAccount account = initSimpleAccount(symbolList);
        THREAD_LOCAL_ACCOUNT.set(account);
        backTestAnalysis.doAnalysis(startTime, endTime);

        String averageAccountRate = NumUtil.percent(THREAD_LOCAL_ACCOUNT.get().getCurBackTestRate());
        String averageNatureRate = NumUtil.percent(THREAD_LOCAL_ACCOUNT.get().getSymbolNatureRate());

        System.out.println(averageAccountRate);
        System.out.println(averageNatureRate);

    }

    /**
     * 初始化回测账户（只适用于单线程处理）
     */
    private MacdAccount initSimpleAccount(List<Symbol> symbolList) {

        MacdAccount account = new MacdAccount();
        account.setName("主线程");
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

        return account;
    }
}

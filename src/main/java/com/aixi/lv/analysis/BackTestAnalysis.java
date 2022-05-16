package com.aixi.lv.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.config.BackTestConfig;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.MacdOptType;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.service.BackTestOrderService;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.strategy.indicator.BollSellStrategy;
import com.aixi.lv.strategy.indicator.MacdBuySellStrategy;
import com.aixi.lv.strategy.indicator.MacdRateSellStrategy;
import com.aixi.lv.strategy.indicator.MacdWarningStrategy;
import com.aixi.lv.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_DATA_MAP;
import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.config.BackTestConfig.INIT_BACK_TEST_AMOUNT;
import static com.aixi.lv.config.BackTestConfig.SWITCH_ACCOUNT_OPEN;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.MACD_ACCOUNT_MAP;

/**
 * @author Js

 *
 * 回测分析
 */
@Component
@DependsOn("macdTradeConfig")
@Slf4j
public class BackTestAnalysis {

    @Resource
    MacdBuySellStrategy macdBuySellStrategy;

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BollSellStrategy bollSellStrategy;

    @Resource
    MacdWarningStrategy macdWarningStrategy;

    @Resource
    BackTestOrderService backTestOrderService;

    @Resource
    MacdRateSellStrategy macdRateSellStrategy;

    /**
     * 底部回升探测分析
     *
     * @param start
     * @param end
     */
    public void doWarning(LocalDateTime start, LocalDateTime end) {

        if (!OPEN) {
            throw new RuntimeException("回测状态未打开");
        }

        if (start.getMinute() != 0 || end.getMinute() != 0) {
            throw new RuntimeException("回测起止时间必须是0分");
        }

        Duration between = Duration.between(start, end);

        long steps = between.toMinutes() / 5;

        LocalDateTime natureTime = start;

        for (int i = 0; i <= steps; i++) {

            // 设置当前回测账户的自然时间
            THREAD_LOCAL_ACCOUNT.get().setCurBackTestComputeTime(natureTime);

            int curMinute = natureTime.getMinute();

            if (curMinute == 15) {

                // 探测逻辑
                macdWarningStrategy.warningDetect();

            }

            natureTime = natureTime.plusMinutes(5);
        }

    }

    /**
     * 执行分析
     *
     * @param start
     * @param end
     */
    public void doAnalysis(LocalDateTime start, LocalDateTime end) {

        if (!OPEN) {
            throw new RuntimeException("回测状态未打开");
        }

        if (start.getMinute() != 0 || end.getMinute() != 0) {
            throw new RuntimeException("回测起止时间必须是0分");
        }

        Duration between = Duration.between(start, end);

        long steps = between.toMinutes() / 1;

        LocalDateTime natureTime = start;

        for (int i = 0; i <= steps; i++) {

            // 设置当前回测账户的自然时间
            THREAD_LOCAL_ACCOUNT.get().setCurBackTestComputeTime(natureTime);

            int curMinute = natureTime.getMinute();

            this.buySellDetect(curMinute);
            this.priceSell(curMinute);
            //this.macdRateSell(curMinute);

            natureTime = natureTime.plusMinutes(1);
        }

        // 信息打印
        this.accountInfo();
        //this.natureRate(start, end);

        // 多币种自然平均增长率
        this.symbolNatureRate(start, end);
    }

    /**
     * 打印账户信息
     */
    private void accountInfo() {

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();

        BigDecimal initAmount = new BigDecimal(INIT_BACK_TEST_AMOUNT);

        if (account.getCurHoldSymbol() != null) {

            BigDecimal newPrice = priceFaceService.queryNewPrice(account.getCurHoldSymbol());

            BigDecimal curHoldQty = account.getCurHoldQty();

            BigDecimal symbolAmount = curHoldQty.multiply(newPrice);

            BigDecimal totalAmount = account.getCurHoldAmount().add(symbolAmount);

            Double curRate = totalAmount.subtract(initAmount)
                .divide(initAmount, 4, RoundingMode.HALF_DOWN)
                .doubleValue();
            account.setCurBackTestRate(curRate);
            String percent = NumUtil.percent(curRate);

            //System.out.println(
            //    String.format(" 回测结束 | %s | 增长率 %s | 当前账户总额 %s | 初始金额 %s",
            //        StringUtils.rightPad(account.getAccountName(), 10), StringUtils.rightPad(percent, 8),
            //        totalAmount, INIT_BACK_TEST_AMOUNT));

        } else {
            Double curRate = account.getCurHoldAmount().subtract(initAmount)
                .divide(initAmount, 4, RoundingMode.HALF_DOWN)
                .doubleValue();
            account.setCurBackTestRate(curRate);
            String percent = NumUtil.percent(curRate);
            //System.out.println(
            //    String.format(" 回测结束 | %s | 增长率 %s | 当前账户总额 %s | 初始金额 %s",
            //        StringUtils.rightPad(account.getAccountName(), 10), StringUtils.rightPad(percent, 8),
            //        account.getCurHoldAmount(), INIT_BACK_TEST_AMOUNT));
        }

    }

    /**
     * 自然增长率
     *
     * @param start
     * @param end
     */
    private void symbolNatureRate(LocalDateTime start, LocalDateTime end) {

        Double totalRate = 0d;

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();

        for (Symbol symbol : account.getSymbolList()) {

            LocalDateTime startDay = LocalDateTime.of(start.getYear(), start.getMonth(), start.getDayOfMonth(), 0, 0);
            KLine startKLine = BACK_TEST_DATA_MAP.get(BackTestConfig.key(symbol, Interval.DAY_1)).get(
                startDay.toString());

            BigDecimal startPrice = startKLine.getClosingPrice();

            LocalDateTime endDay = LocalDateTime.of(end.getYear(), end.getMonth(), end.getDayOfMonth(), 0, 0);
            KLine endKLine = BACK_TEST_DATA_MAP.get(BackTestConfig.key(symbol, Interval.DAY_1)).get(
                endDay.toString());

            BigDecimal endPrice = endKLine.getClosingPrice();

            Double rate = endPrice.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_DOWN).doubleValue();

            totalRate += rate;

        }

        account.setSymbolNatureRate(totalRate / account.getSymbolList().size());

    }

    /**
     * 自然增长率
     *
     * @param start
     * @param end
     */
    private void natureRate(LocalDateTime start, LocalDateTime end) {

        Set<Symbol> symbolSet = new HashSet<>();

        for (MacdAccount account : MACD_ACCOUNT_MAP.values()) {
            account.getSymbolList().forEach(i -> symbolSet.add(i));
        }

        for (Symbol symbol : symbolSet) {

            LocalDateTime startDay = LocalDateTime.of(start.getYear(), start.getMonth(), start.getDayOfMonth(), 0, 0);
            KLine startKLine = BACK_TEST_DATA_MAP.get(BackTestConfig.key(symbol, Interval.DAY_1)).get(
                startDay.toString());

            BigDecimal startPrice = startKLine.getClosingPrice();

            LocalDateTime endDay = LocalDateTime.of(end.getYear(), end.getMonth(), end.getDayOfMonth(), 0, 0);
            KLine endKLine = BACK_TEST_DATA_MAP.get(BackTestConfig.key(symbol, Interval.DAY_1)).get(
                endDay.toString());

            BigDecimal endPrice = endKLine.getClosingPrice();

            String percent = NumUtil.percent(endPrice.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_DOWN)
                .doubleValue());

            //System.out.println(
            //    String.format(" 回测结束 | %s | 增长率 %s | 开始价格 %s | 结束价格 %s", StringUtils.rightPad(symbol.getCode(), 10),
            //        StringUtils.rightPad(percent, 8), startPrice, endPrice));
        }

    }

    /**
     * 购买卖出准入
     * 每小时 0分 执行
     */
    private void buySellDetect(int curMinute) {

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();

        if (curMinute == 5) {

            macdBuySellStrategy.detect(account);

        }

    }

    private void macdRateSell(int curMinute) {

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();

        if (curMinute == 5) {

            macdRateSellStrategy.sellDetect(account);

        }

    }

    /**
     * 成本价卖出
     *
     * 每分钟 执行
     */
    private void priceSell(Integer curMinute) {

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();

        if (BackTestConfig.MINUTE_ONE_K_LINE_OPEN) {

            macdBuySellStrategy.sellDetect(account);

        } else {

            if (curMinute == 0
                || curMinute == 5
                || curMinute == 10
                || curMinute == 15
                || curMinute == 20
                || curMinute == 25
                || curMinute == 30
                || curMinute == 35
                || curMinute == 40
                || curMinute == 45
                || curMinute == 50
                || curMinute == 55) {

                macdBuySellStrategy.sellDetect(account);
            }
        }

    }

    /**
     * boll卖出
     *
     * 每小时 5,10,15,20,25,30,35,40,45,50,55 分 执行
     */
    private void bollSell(int curMinute) {

        if (curMinute == 5
            || curMinute == 10
            || curMinute == 15
            || curMinute == 20
            || curMinute == 25
            || curMinute == 30
            || curMinute == 35
            || curMinute == 40
            || curMinute == 45
            || curMinute == 50
            || curMinute == 55) {

            bollSellStrategy.detectSell();
        }

    }
}

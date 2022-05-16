package com.aixi.lv.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.ContractAccount;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.strategy.contract.PriceContractStrategy;
import com.aixi.lv.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js

 */
@Component
@Slf4j
public class ContractBackTestAnalysis {

    public static final ThreadLocal<List<ContractAccount>> THREAD_LOCAL_CONTRACT_ACCOUNT = new ThreadLocal<>();

    public static final BigDecimal INIT_CONTRACT_AMOUNT = new BigDecimal(10000);

    @Resource
    PriceContractStrategy priceContractStrategy;

    @Resource
    PriceFaceService priceFaceService;

    /**
     * 执行分析
     *
     * @param start
     * @param end
     */
    public void doAnalysis(LocalDateTime start, LocalDateTime end) {

        // 初始化账户
        this.initContractAccount();

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

            List<ContractAccount> accounts = THREAD_LOCAL_CONTRACT_ACCOUNT.get();
            THREAD_LOCAL_ACCOUNT.get().setCurBackTestComputeTime(natureTime);

            for (ContractAccount account : accounts) {

                // 设置当前回测账户的自然时间
                account.setCurBackTestComputeTime(natureTime);

            }

            priceContractStrategy.buyDetect(accounts);

            priceContractStrategy.sellDetect(accounts);

            natureTime = natureTime.plusMinutes(1);

        }

        // 信息打印
        this.accountInfo();

    }

    private void initContractAccount() {

        List<Symbol> list = Lists.newArrayList();
        list.add(Symbol.DOGEUSDT);
        list.add(Symbol.SHIBUSDT);
        list.add(Symbol.LUNAUSDT);
        list.add(Symbol.NEARUSDT);
        list.add(Symbol.PEOPLEUSDT);
        list.add(Symbol.SOLUSDT);
        list.add(Symbol.GMTUSDT);
        list.add(Symbol.BTCUSDT);
        list.add(Symbol.APEUSDT);

        List<ContractAccount> accounts = Lists.newArrayList();

        for (Symbol symbol : list) {
            accounts.add(this.buildAccount(symbol));
        }

        THREAD_LOCAL_CONTRACT_ACCOUNT.set(accounts);

        MacdAccount macdAccount = new MacdAccount();
        THREAD_LOCAL_ACCOUNT.set(macdAccount);

    }

    private ContractAccount buildAccount(Symbol symbol) {

        ContractAccount account = new ContractAccount();
        account.setName(symbol.name());
        account.setSymbol(symbol);
        account.setHoldAmount(INIT_CONTRACT_AMOUNT);
        account.setHoldQty(BigDecimal.ZERO);
        account.setHoldFlag(false);
        account.setContractSide(null);
        account.setBuyPrice(null);
        account.setProfitPrice(null);
        account.setLossPrice(null);
        account.setCurBackTestComputeTime(null);
        account.setBackTestTotalProfit(null);
        account.setBackTestTotalLoss(null);
        account.setBackTestProfitTimes(0);
        account.setBackTestLossTimes(0);

        return account;

    }

    /**
     * 打印账户信息
     */
    private void accountInfo() {

        List<ContractAccount> accounts = THREAD_LOCAL_CONTRACT_ACCOUNT.get();

        for (ContractAccount account : accounts) {

            Double curRate = account.getHoldAmount().subtract(INIT_CONTRACT_AMOUNT)
                .divide(INIT_CONTRACT_AMOUNT, 4, RoundingMode.HALF_DOWN)
                .doubleValue();
            String percent = NumUtil.percent(curRate);

            System.out.println(
                String.format(" 回测结束 | %s | 增长率 %s | 当前账户总额 %s | 初始金额 %s",
                    StringUtils.rightPad(account.getSymbol().name(), 10),
                    StringUtils.rightPad(percent, 8),
                    account.getHoldAmount(), INIT_CONTRACT_AMOUNT));

        }
    }
}

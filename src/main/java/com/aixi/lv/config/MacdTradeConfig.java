package com.aixi.lv.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.util.CombineUtil;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.MACD_ACCOUNT_MAP;

/**
 * @author Js
 */
@Component
@DependsOn("backTestConfig")
@Slf4j
public class MacdTradeConfig implements ApplicationListener<ApplicationReadyEvent> {

    public static final String JS_ACCOUNT_NAME = "精选币账户";

    public static final ThreadLocal<MacdAccount> THREAD_LOCAL_ACCOUNT = new ThreadLocal<>();

    /**
     * 有些新上的币，就不做天级别MACD检查了
     */
    public static final List<Symbol> IGNORE_DAY_MACD_SYMBOL_LIST = Lists.newArrayList();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        log.warn(" START | 加载 MACD 账户");

        if (!OPEN) {

            // 初始化账户
            List<Symbol> list = Lists.newArrayList();
            list.add(Symbol.ETHUSDT);
            list.add(Symbol.XRPUSDT);
            list.add(Symbol.LUNAUSDT);
            list.add(Symbol.BNBUSDT);
            list.add(Symbol.SOLUSDT);
            list.add(Symbol.SHIBUSDT);
            list.add(Symbol.NEARUSDT);
            this.loadBelongAccount(list, JS_ACCOUNT_NAME);

            // 有些新上的币，就不做天级别MACD检查了
            this.loadIgnoreDaySymbol();
        }

        log.warn(" FINISH | 加载 MACD 账户");
    }

    /**
     * 生产账户-归属账户
     */
    private void loadBelongAccount(List<Symbol> list, String name) {

        // 5个币为一个组合
        Integer symbolCombineNum = 5;

        List<List<Symbol>> combineList = CombineUtil.assignSymbolCombine(list, symbolCombineNum);

        for (int i = 0; i < combineList.size(); i++) {
            List<Symbol> symbolList = combineList.get(i);
            String accountName = name + "_" + String.format("%02d", i + 1); // 账户从01开始
            String belongAccount = name + "_归属账户";
            MacdAccount account = initAccount(accountName, symbolList, 30, belongAccount);
            MACD_ACCOUNT_MAP.put(account.getName(), account);
        }

    }

    private MacdAccount initAccount(String accountName, List<Symbol> symbolList, Integer amount, String belongAccount) {

        List<Symbol> collect = symbolList.stream().distinct().collect(Collectors.toList());

        MacdAccount account = new MacdAccount();
        account.setName(accountName);
        account.setSymbolList(collect);
        account.setCurHoldQty(BigDecimal.ZERO);
        account.setCurHoldAmount(new BigDecimal(amount));
        account.setReadySellFlag(Boolean.FALSE);

        // 归属账户，多币种组合时，方便统计
        if (StringUtils.isNotEmpty(belongAccount)) {
            account.setBelongAccount(belongAccount);
        }

        return account;
    }

    /**
     * 有些新上的币，就不做天级别MACD检查了
     */
    private void loadIgnoreDaySymbol() {

        for (Symbol symbol : Symbol.values()) {
            if (isIgnoreDayMacd(symbol)) {
                IGNORE_DAY_MACD_SYMBOL_LIST.add(symbol);
            }
        }
    }

    /**
     * 是否忽略天级MACD
     *
     * @param symbol
     * @return
     */
    private Boolean isIgnoreDayMacd(Symbol symbol) {

        LocalDate saleDate = symbol.getSaleDate();
        LocalDateTime now = TimeUtil.now();
        LocalDate nowDate = LocalDate.of(now.getYear(), now.getMonth(), now.getDayOfMonth());
        long diffDay = nowDate.toEpochDay() - saleDate.toEpochDay();

        // 理论上 计算天级MACD至少需要60天的数据，这里写62保险一点
        if (diffDay <= 62) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

}

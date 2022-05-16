package com.aixi.lv.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.service.BackTestReadService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 回测配置
 */
@Component
@Slf4j
public class BackTestConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    BackTestReadService backTestReadService;

    public static final Integer INIT_BACK_TEST_AMOUNT = 10000;

    public static final String BACK_TEST_ACCOUNT_NAME = "BACK_TEST_ACCOUNT:";

    /**
     * 回测数据
     */
    public static final Map<String/**symbol+interval**/, Map<String/**LocalDateTime.toString()**/, KLine>>
        BACK_TEST_DATA_MAP = new HashMap<>();

    /**
     * 是否开启回测
     */
    public static final Boolean OPEN = Boolean.TRUE;

    /**
     * 是否开启灵活币种账户回测
     */
    public static final Boolean SWITCH_ACCOUNT_OPEN = Boolean.FALSE;

    /**
     * 是否开启 1分钟级 K线 （默认 关闭）
     */
    public static Boolean MINUTE_ONE_K_LINE_OPEN = Boolean.TRUE;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        try {

            if (OPEN) {

                log.warn(" START | 加载回测数据");

                this.loadSome();
                //this.loadAll();

                log.warn(" FINISH | 加载回测数据");

            }

        } catch (Exception e) {
            log.error("BackTestConfig 异常", e);
            throw e;
        }
    }

    /**
     * 回测数据 Map key
     *
     * @param symbol
     * @param interval
     * @return
     */
    public static String key(Symbol symbol, Interval interval) {
        return symbol.getCode() + interval.getCode();
    }

    /**
     * 回测账户-所有账户
     */
    private void loadAll() {

        for (Symbol symbol : Symbol.values()) {
            this.loadData(symbol);
        }

    }

    /**
     * 回测账户-指定币种
     */
    private void loadSome() {

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

        for (Symbol symbol : list) {
            this.loadData(symbol);
        }

        //this.loadMinuteOne();

    }

    /**
     * 初始化数据
     *
     * @param symbol
     */
    private void loadData(Symbol symbol) {

        if (!symbol.getBackFlag()) {
            return;
        }

        //BACK_TEST_DATA_MAP.put(key(symbol, Interval.DAY_1),
        //    backTestReadService.readFromFile(symbol, Interval.DAY_1));
        //
        //BACK_TEST_DATA_MAP.put(key(symbol, Interval.HOUR_4),
        //    backTestReadService.readFromFile(symbol, Interval.HOUR_4));
        //
        //BACK_TEST_DATA_MAP.put(key(symbol, Interval.HOUR_1),
        //    backTestReadService.readFromFile(symbol, Interval.HOUR_1));
        //
        //BACK_TEST_DATA_MAP.put(key(symbol, Interval.MINUTE_5),
        //    backTestReadService.readFromFile(symbol, Interval.MINUTE_5));

        BACK_TEST_DATA_MAP.put(key(symbol, Interval.MINUTE_1),
            backTestReadService.readFromFile(symbol, Interval.MINUTE_1));
    }

    /**
     * 1分钟级K线
     */
    private void loadMinuteOne() {

        List<Symbol> needMinuteOne = Lists.newArrayList();
        needMinuteOne.add(Symbol.BTCUSDT);
        needMinuteOne.add(Symbol.ETHUSDT);
        needMinuteOne.add(Symbol.XRPUSDT);
        needMinuteOne.add(Symbol.LUNAUSDT);
        needMinuteOne.add(Symbol.ADAUSDT);
        needMinuteOne.add(Symbol.SOLUSDT);
        needMinuteOne.add(Symbol.DOTUSDT);
        needMinuteOne.add(Symbol.PEOPLEUSDT);
        needMinuteOne.add(Symbol.GALAUSDT);
        needMinuteOne.add(Symbol.MATICUSDT);
        needMinuteOne.add(Symbol.BNBUSDT);
        needMinuteOne.add(Symbol.SHIBUSDT);

        for (Symbol symbol : needMinuteOne) {
            BACK_TEST_DATA_MAP.put(key(symbol, Interval.MINUTE_1),
                backTestReadService.readFromFile(symbol, Interval.MINUTE_1));
        }
    }
}

package com.aixi.lv.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.ExchangeInfoAmountFilter;
import com.aixi.lv.model.domain.ExchangeInfoPriceFilter;
import com.aixi.lv.model.domain.ExchangeInfoQtyFilter;
import com.aixi.lv.service.HttpService;
import com.aixi.lv.util.ApiUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;

/**
 * @author Js
 * @date 2022/1/2 11:35 上午
 */
@Component
@DependsOn("backTestConfig")
@Slf4j
public class ExchangeInfoConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    HttpService httpService;

    public static final Map<Symbol, ExchangeInfoPriceFilter> PRICE_FILTER_MAP = new HashMap<>();

    public static final Map<Symbol, ExchangeInfoQtyFilter> QTY_FILTER_MAP = new HashMap<>();

    public static final Map<Symbol, ExchangeInfoAmountFilter> AMOUNT_FILTER_MAP = new HashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        log.warn(" START | 加载交易信息配置");

        loadSome();

        log.warn(" FINISH | 加载交易信息配置");

    }

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

            log.warn(" {} | 开始加载交易信息配置", symbol);
            this.setExchangeInfoFilter(symbol);
        }
    }

    private void loadAll() {

        for (Symbol symbol : Symbol.values()) {

            log.warn(" {} | 开始加载交易信息配置", symbol);
            this.setExchangeInfoFilter(symbol);
        }
    }

    private void setExchangeInfoFilter(Symbol symbol) {

        if (OPEN && !symbol.getBackFlag()) {
            return;
        }

        String url = ApiUtil.url("/api/v3/exchangeInfo");

        JSONObject body = new JSONObject();
        body.put("symbol", symbol.getCode());

        JSONObject response = httpService.getObject(url, body);

        List<JSONObject> jsonObjects = response.getJSONArray("symbols")
            .getJSONObject(0)
            .getJSONArray("filters")
            .toJavaList(JSONObject.class);

        for (JSONObject jo : jsonObjects) {

            // 价格配置
            this.setPriceFilter(symbol, jo);

            // 数量配置
            this.setQtyFilter(symbol, jo);

            // 金额配置
            this.setAmountFilter(symbol, jo);

        }

    }

    private void setPriceFilter(Symbol symbol, JSONObject jo) {

        if (jo.getString("filterType").equals("PRICE_FILTER")) {

            ExchangeInfoPriceFilter priceFilter = jo.toJavaObject(ExchangeInfoPriceFilter.class);

            if (priceFilter == null) {
                log.error("加载交易信息priceFilter配置错误 | symbol={}", symbol);
                throw new RuntimeException(" 加载交易信息priceFilter配置错误 ");
            }

            BigDecimal tickSize = priceFilter.getTickSize();
            priceFilter.setPriceScale(tickSize.stripTrailingZeros().scale());
            PRICE_FILTER_MAP.put(symbol, priceFilter);
        }
    }

    private void setQtyFilter(Symbol symbol, JSONObject jo) {

        if (jo.getString("filterType").equals("LOT_SIZE")) {

            ExchangeInfoQtyFilter qtyFilter = jo.toJavaObject(ExchangeInfoQtyFilter.class);

            if (qtyFilter == null) {
                log.error("加载交易信息qtyFilter配置错误 | symbol={}", symbol);
                throw new RuntimeException(" 加载交易qtyFilter信息配置错误 ");
            }

            BigDecimal stepSize = qtyFilter.getStepSize();
            qtyFilter.setQtyScale(stepSize.stripTrailingZeros().scale());
            QTY_FILTER_MAP.put(symbol, qtyFilter);
        }
    }

    private void setAmountFilter(Symbol symbol, JSONObject jo) {

        if (jo.getString("filterType").equals("MIN_NOTIONAL")) {

            ExchangeInfoAmountFilter amountFilter = jo.toJavaObject(ExchangeInfoAmountFilter.class);

            if (amountFilter == null) {
                log.error("加载交易信息amountFilter配置错误 | symbol={}", symbol);
                throw new RuntimeException(" 加载交易amountFilter信息配置错误 ");
            }

            AMOUNT_FILTER_MAP.put(symbol, amountFilter);
        }
    }
}

package com.aixi.lv;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.util.CombineUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.junit.Test;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_ACCOUNT_NAME;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.MACD_ACCOUNT_MAP;

/**
 * @author Js
 */
public class EasyTest {

    @Test
    public void 测试1() {

        String str = BACK_TEST_ACCOUNT_NAME + Symbol.BTCUSDT.getCode() + " " + Symbol.ETHUSDT + " ";

        System.out.println(str);

        String substring = str.substring(BACK_TEST_ACCOUNT_NAME.length());

        System.out.println(substring);

        String[] s = StringUtils.split(substring, " ");

        for (String temp : s) {
            System.out.println(Symbol.getByCode(temp));
        }
    }

    @Test
    public void 测试2() {

        // 9选4 ，有126种组合

        // 9选3 ，有84种组合
        // 8选3 ，有56种组合
        // 7选3 ，有35种组合
        // 6选3 ，有20种组合

        // 7选4 ，有35种组合
        // 7选5 ，有21种组合

        // 8选4 ，有70种组合
        // 8选5 ，有56种组合

        // 13选7 ，有1716种组合
        // 12选7 ，有792种组合
        // 11选7 ，有330种组合
        List<Symbol> symbolList = Lists.newArrayList();
        symbolList.add(Symbol.BTCUSDT);
        symbolList.add(Symbol.ETHUSDT);
        symbolList.add(Symbol.DOGEUSDT);
        symbolList.add(Symbol.SHIBUSDT);
        symbolList.add(Symbol.FTMUSDT);
        symbolList.add(Symbol.ROSEUSDT);
        symbolList.add(Symbol.LUNAUSDT);
        symbolList.add(Symbol.SLPUSDT);
        symbolList.add(Symbol.MATICUSDT);
        symbolList.add(Symbol.BNBUSDT);
        symbolList.add(Symbol.AVAXUSDT);
        //symbolList.add(Symbol.ADAUSDT);
        //symbolList.add(Symbol.ACHUSDT);
        List<List<Symbol>> lists = CombineUtil.assignSymbolCombine(symbolList, 7);

        System.out.println(lists.size());
    }

    @Test
    public void 测试3() {

        List<MutablePair<Symbol, BigDecimal>> tempList = Lists.newArrayList();

        tempList.add(MutablePair.of(Symbol.ETHUSDT,new BigDecimal(0.05)));
        tempList.add(MutablePair.of(Symbol.BNBUSDT,new BigDecimal(0.31)));

        Collections.sort(tempList, (o1, o2) -> {
            if (o1.getRight().compareTo(o2.getRight()) < 0) {
                return 1;
            } else {
                return -1;
            }
        });

        List<Symbol> resultList = Lists.newArrayList();

        for (int i = 0; i < tempList.size(); i++) {

            // 只取前3
            if (i >= 3) {
                break;
            }

            MutablePair<Symbol, BigDecimal> pair = tempList.get(i);

            resultList.add(pair.getLeft());
        }

        System.out.println(JSON.toJSONString(resultList));
    }

}

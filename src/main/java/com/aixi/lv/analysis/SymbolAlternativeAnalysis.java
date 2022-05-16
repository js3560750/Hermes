package com.aixi.lv.analysis;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.util.CombineUtil;
import com.aixi.lv.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.aixi.lv.analysis.SymbolChoiceAnalysis.CHOICE_RATE_MAP;

/**
 * @author Js

 *
 * 可供选择的币种分析
 */
@Component
@Slf4j
public class SymbolAlternativeAnalysis {

    @Resource
    SymbolChoiceAnalysis symbolChoiceAnalysis;

    /**
     * 到底选哪7个币种
     */
    public void alternativeAnalysis() {

        // 13选7 ，有1716种组合
        // 12选7 ，有792种组合
        // 11选7 ，有330种组合
        List<Symbol> list = Lists.newArrayList();
        list.add(Symbol.BTCUSDT);
        list.add(Symbol.ETHUSDT);
        list.add(Symbol.BNBUSDT);
        list.add(Symbol.XRPUSDT);
        list.add(Symbol.LUNAUSDT);
        list.add(Symbol.ADAUSDT);
        list.add(Symbol.SOLUSDT);
        list.add(Symbol.AVAXUSDT);
        list.add(Symbol.DOTUSDT);
        list.add(Symbol.SHIBUSDT);
        list.add(Symbol.MATICUSDT);
        list.add(Symbol.NEARUSDT);

        List<List<Symbol>> lists = CombineUtil.assignSymbolCombine(list, 7);

        for (int i = 0; i < lists.size(); i++) {
            List<Symbol> choiceSymbolList = lists.get(i);
            this.标准_精选账户回测_短期初测Action(choiceSymbolList);
            log.warn(" 可供选择的币种分析_当前已完成 = " + i);
        }

        Map<String, Double> sortedMap = new LinkedHashMap<>();

        // Map 按Value 排序
        CHOICE_RATE_MAP.entrySet()
            .stream()
            .sorted(Comparator.comparing(Entry::getValue)) // 默认升序排列，小的放前面
            .collect(Collectors.toList())
            .forEach(element -> sortedMap.put(element.getKey(), element.getValue()));

        // 信息打印
        for (Entry<String, Double> entry : sortedMap.entrySet()) {
            log.warn(" 分析币种增长率 = {} | 币种组合 = {} ", NumUtil.percent(entry.getValue() / 2), entry.getKey());
        }

    }

    private void 标准_精选账户回测_短期初测Action(List<Symbol> choiceSymbolList) {

        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 16, 23, 0);

        // 分析逻辑
        symbolChoiceAnalysis.币种组合分析(endTime, 30, null, choiceSymbolList, combineSize, null, null);

        symbolChoiceAnalysis.币种组合分析(endTime, 15, null, choiceSymbolList, combineSize, null, null);

    }
}

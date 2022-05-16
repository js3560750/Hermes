package com.aixi.lv;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.analysis.SymbolChoiceAnalysis;
import com.aixi.lv.config.BackTestConfig;
import com.aixi.lv.model.constant.Symbol;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

/**
 * @author Js
 */
@Slf4j
public class 近期回测Test extends BaseTest {

    @Resource
    SymbolChoiceAnalysis symbolChoiceAnalysis;

    @Test
    public void 近期_单次初测() {

        this.近期_单次初测Action(null, null);

    }

    /**
     *
     近期_单次初测Action ，结束日期 2022-03-27T23:00
     天数 = 30  | 账户增长率 = 31.69%   | 币种增长率 = 21.43%   | 盈亏比 = 3.263  | 盈利 = 4841.531   | 亏损 = 1483.942   | 盈利次数 = 16.371 | 亏损次数 = 4.171
     天数 = 14  | 账户增长率 = 12.14%   | 币种增长率 = 30.19%   | 盈亏比 = 3.375  | 盈利 = 1843.041   | 亏损 = 546.017    | 盈利次数 = 8.471  | 亏损次数 = 1.843
     天数 = 5   | 账户增长率 = 4.84%    | 币种增长率 = 12.30%   | 盈亏比 = 4.826  | 盈利 = 641.522    | 亏损 = 132.921    | 盈利次数 = 2.843  | 亏损次数 = 0.286
     天数 = 3   | 账户增长率 = 3.02%    | 币种增长率 = 6.41%    | 盈亏比 = 0      | 盈利 = 311.525    | 亏损 = 0          | 盈利次数 = 1.286  | 亏损次数 = 0

     */
    @Test
    public void 近期_对比() {

        List<Symbol> list = Lists.newArrayList();
        list.add(Symbol.BTCUSDT);
        list.add(Symbol.ETHUSDT);
        list.add(Symbol.XRPUSDT);
        list.add(Symbol.LUNAUSDT);
        list.add(Symbol.ADAUSDT);
        list.add(Symbol.SOLUSDT);
        list.add(Symbol.DOTUSDT);
        list.add(Symbol.PEOPLEUSDT);

        this.近期_单次初测Action(list, 4);

    }

    /**
     *
     近期_单次初测Action ，结束日期 2022-03-27T23:00
     天数 = 30  | 账户增长率 = 8.98%    | 币种增长率 = 13.12%   | 盈亏比 = 1.5    | 盈利 = 3176.702   | 亏损 = 2117.902   | 盈利次数 = 15.571 | 亏损次数 = 4.667
     天数 = 14  | 账户增长率 = 5.93%    | 币种增长率 = 22.68%   | 盈亏比 = 1.777  | 盈利 = 1537.382   | 亏损 = 865.208    | 盈利次数 = 8.095  | 亏损次数 = 2
     天数 = 5   | 账户增长率 = 0.00%    | 币种增长率 = 9.67%    | 盈亏比 = 1.05   | 盈利 = 496.265    | 亏损 = 472.763    | 盈利次数 = 1.952  | 亏损次数 = 1
     天数 = 3   | 账户增长率 = -5.48%   | 币种增长率 = 3.80%    | 盈亏比 = 0      | 盈利 = 0          | 亏损 = 535.162    | 盈利次数 = 0      | 亏损次数 = 1.714

     */
    private void 近期_单次初测Action(List<Symbol> list, Integer combineSize) {

        // 开启 1分钟级 K线
        BackTestConfig.MINUTE_ONE_K_LINE_OPEN = Boolean.TRUE;

        // 初始可供选择的币种   7选5 = 21 个账户
        if (CollectionUtils.isEmpty(list)) {
            list = Lists.newArrayList();
            list.add(Symbol.GALAUSDT);
            list.add(Symbol.MATICUSDT);
            list.add(Symbol.BNBUSDT);
            list.add(Symbol.SHIBUSDT);
            list.add(Symbol.SOLUSDT);
            list.add(Symbol.ETHUSDT);
            list.add(Symbol.LUNAUSDT);
        }

        // 组合账户币种数量
        if (combineSize == null) {
            combineSize = 5;
        }

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 27, 23, 0);

        StringBuilder sb = new StringBuilder();

        // 分析逻辑
        symbolChoiceAnalysis.币种组合分析(endTime, 30, null, list, combineSize, sb, null);

        symbolChoiceAnalysis.币种组合分析(endTime, 14, null, list, combineSize, sb, null);

        symbolChoiceAnalysis.币种组合分析(endTime, 5, null, list, combineSize, sb, null);

        symbolChoiceAnalysis.币种组合分析(endTime, 3, null, list, combineSize, sb, null);

        System.out.println("\n");
        System.out.println(" 近期_单次初测Action ，结束日期 " + endTime);
        System.out.println(sb);

    }


}

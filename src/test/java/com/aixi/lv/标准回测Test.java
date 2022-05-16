package com.aixi.lv;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.analysis.SymbolAlternativeAnalysis;
import com.aixi.lv.analysis.SymbolChoiceAnalysis;
import com.aixi.lv.model.constant.Symbol;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;


/**
 * @author Js
 */
@Slf4j
public class 标准回测Test extends BaseTest {

    @Resource
    SymbolChoiceAnalysis symbolChoiceAnalysis;

    @Resource
    SymbolAlternativeAnalysis symbolAlternativeAnalysis;

    @Test
    public void 标准_全流程(){

        this.标准_精选账户回测_初测Action(null);
        this.标准_精选账户回测_短期初测Action(null);
        this.标准_精选账户回测_短期滚动Action(null);
        this.标准_精选账户回测_短期复测Action(null);
        this.标准_精选账户回测_中期复测Action(null);

    }


    @Test
    public void 标准_精选账户回测_初测() {

        this.标准_精选账户回测_初测Action(null);

    }

    @Test
    public void 标准_精选账户回测_中期复测() {

        this.标准_精选账户回测_中期复测Action(null);

    }

    @Test
    public void 标准_精选账户回测_短期初测() {

        this.标准_精选账户回测_短期初测Action(null);

    }


    @Test
    public void 标准_精选账户回测_短期复测() {

        this.标准_精选账户回测_短期复测Action(null);

    }

    @Test
    public void 标准_精选账户回测_短期滚动() {

        this.标准_精选账户回测_短期滚动Action(null);

    }

    @Test
    public void 选币_到底选哪7个币种() {

        // 13选7 ，有1716种组合
        // 12选7 ，有792种组合
        symbolAlternativeAnalysis.alternativeAnalysis();

    }

    /**
     *

     天数 = 180 | 账户增长率 = 192.92%  | 币种增长率 = 72.29%   | 盈亏比 = 1.481  | 盈利 = 64243.189  | 亏损 = 43376.474  | 盈利次数 = 92.857 | 亏损次数 = 49.667
     天数 = 90  | 账户增长率 = 18.55%   | 币种增长率 = -20.12%  | 盈亏比 = 1.242  | 盈利 = 10893.037  | 亏损 = 8768.705   | 盈利次数 = 46     | 亏损次数 = 25.524
     天数 = 30  | 账户增长率 = 17.91%   | 币种增长率 = 4.33%    | 盈亏比 = 1.615  | 盈利 = 4420.326   | 亏损 = 2736.707   | 盈利次数 = 17.905 | 亏损次数 = 8.905
     天数 = 15  | 账户增长率 = -2.05%   | 币种增长率 = 2.75%    | 盈亏比 = 0.723  | 盈利 = 958.109    | 亏损 = 1325.209   | 盈利次数 = 10     | 亏损次数 = 5.857
     天数 = 7   | 账户增长率 = 4.16%    | 币种增长率 = 10.34%   | 盈亏比 = 1.692  | 盈利 = 467.552    | 亏损 = 276.34     | 盈利次数 = 6.667  | 亏损次数 = 3.238

     */
    private void 标准_精选账户回测_初测Action(List<Symbol> choiceSymbolList) {

        long start = System.currentTimeMillis();

        // 初始可供选择的币种   7选5 = 21 个账户
        if (CollectionUtils.isEmpty(choiceSymbolList)) {
            choiceSymbolList = Lists.newArrayList();
            choiceSymbolList.add(Symbol.ETHUSDT);
            choiceSymbolList.add(Symbol.MATICUSDT);
            choiceSymbolList.add(Symbol.BNBUSDT);
            choiceSymbolList.add(Symbol.FTMUSDT);
            choiceSymbolList.add(Symbol.SOLUSDT);
            choiceSymbolList.add(Symbol.SHIBUSDT);
            choiceSymbolList.add(Symbol.LUNAUSDT);
        }

        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 19, 23, 0);

        // 信息打印
        StringBuilder sb = new StringBuilder();

        // 分析逻辑
        symbolChoiceAnalysis.币种组合分析(endTime, 180, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 90, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 30, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 15, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 7, null, choiceSymbolList, combineSize, sb,null);

        // 信息打印
        System.out.println(" 标准_精选账户回测_初测Action ，结束日期 " + endTime);
        System.out.println(sb);
        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);

    }

    /**
     *
     天数 = 180 | 账户增长率 = 309.52%  | 币种增长率 = 64.66%   | 盈亏比 = 1.526  | 盈利 = 100770.708 | 亏损 = 66016.173  | 盈利次数 = 91.327 | 亏损次数 = 43.68
     天数 = 90  | 账户增长率 = 118.23%  | 币种增长率 = 40.72%   | 盈亏比 = 1.817  | 盈利 = 28902.901  | 亏损 = 15908.266  | 盈利次数 = 44.526 | 亏损次数 = 22.503
     天数 = 60  | 账户增长率 = 81.36%   | 币种增长率 = 40.36%   | 盈亏比 = 2.019  | 盈利 = 16934.625  | 亏损 = 8386.234   | 盈利次数 = 30.121 | 亏损次数 = 14.607
     天数 = 30  | 账户增长率 = 22.31%   | 币种增长率 = -2.14%   | 盈亏比 = 1.933  | 盈利 = 5132.178   | 亏损 = 2654.557   | 盈利次数 = 13.105 | 亏损次数 = 5.981
     天数 = 15  | 账户增长率 = 10.42%   | 币种增长率 = 0.24%    | 盈亏比 = 2.039  | 盈利 = 2409.984   | 亏损 = 1182.197   | 盈利次数 = 6.768  | 亏损次数 = 3.065
     天数 = 5   | 账户增长率 = 2.64%    | 币种增长率 = 0.56%    | 盈亏比 = 2.134  | 盈利 = 689.55     | 亏损 = 323.102    | 盈利次数 = 2.319  | 亏损次数 = 1.024
     */
    private void 标准_精选账户回测_中期复测Action(List<Symbol> choiceSymbolList) {

        long start = System.currentTimeMillis();

        // 初始可供选择的币种   7选5 = 21 个账户
        if (CollectionUtils.isEmpty(choiceSymbolList)) {
            choiceSymbolList = Lists.newArrayList();
            choiceSymbolList.add(Symbol.ETHUSDT);
            choiceSymbolList.add(Symbol.MATICUSDT);
            choiceSymbolList.add(Symbol.BNBUSDT);
            choiceSymbolList.add(Symbol.FTMUSDT);
            choiceSymbolList.add(Symbol.SOLUSDT);
            choiceSymbolList.add(Symbol.SHIBUSDT);
            choiceSymbolList.add(Symbol.LUNAUSDT);
        }

        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 16, 23, 0);

        StringBuilder sb = new StringBuilder();

        // 分析逻辑

        // 最早到 2021-07-24
        symbolChoiceAnalysis.币种组合分析(endTime, 180, 7, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-07-24
        symbolChoiceAnalysis.币种组合分析(endTime, 90, 25, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-07-24
        symbolChoiceAnalysis.币种组合分析(endTime, 60, 31, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-12-31
        symbolChoiceAnalysis.币种组合分析(endTime, 30, 5, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-12-31
        symbolChoiceAnalysis.币种组合分析(endTime, 15, 8, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-12-31
        symbolChoiceAnalysis.币种组合分析(endTime, 5, 10, choiceSymbolList, combineSize, sb,null);

        System.out.println(" 标准_精选账户回测_中期复测Action ，结束日期 " + endTime);
        System.out.println(sb);
        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);

    }

    /**
     *
     标准_精选账户回测_短期初测Action ，结束日期 2022-03-16T23:00
     天数 = 60  | 账户增长率 = 17.95%   | 币种增长率 = -24.21%  | 盈亏比 = 1.287  | 盈利 = 9432.853   | 亏损 = 7327.745   | 盈利次数 = 30.762 | 亏损次数 = 11.143
     天数 = 30  | 账户增长率 = 12.46%   | 币种增长率 = -0.93%   | 盈亏比 = 1.431  | 盈利 = 4664.191   | 亏损 = 3258.34    | 盈利次数 = 14     | 亏损次数 = 6.048
     天数 = 15  | 账户增长率 = -15.56%  | 币种增长率 = -8.21%   | 盈亏比 = 0.274  | 盈利 = 566.499    | 亏损 = 2064.383   | 盈利次数 = 6.238  | 亏损次数 = 4.81
     天数 = 5   | 账户增长率 = -0.70%   | 币种增长率 = 6.04%    | 盈亏比 = 0.912  | 盈利 = 171.847    | 亏损 = 188.486    | 盈利次数 = 4.238  | 亏损次数 = 1.19

     */
    private void 标准_精选账户回测_短期初测Action(List<Symbol> choiceSymbolList) {

        long start = System.currentTimeMillis();

        // 初始可供选择的币种   7选5 = 21 个账户
        if (CollectionUtils.isEmpty(choiceSymbolList)) {
            choiceSymbolList = Lists.newArrayList();
            choiceSymbolList.add(Symbol.GALAUSDT);
            choiceSymbolList.add(Symbol.MATICUSDT);
            choiceSymbolList.add(Symbol.BNBUSDT);
            choiceSymbolList.add(Symbol.SHIBUSDT);
            choiceSymbolList.add(Symbol.SOLUSDT);
            choiceSymbolList.add(Symbol.ETHUSDT);
            choiceSymbolList.add(Symbol.LUNAUSDT);
        }

        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 16, 23, 0);

        StringBuilder sb = new StringBuilder();

        // 分析逻辑

        symbolChoiceAnalysis.币种组合分析(endTime, 60, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 30, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 15, null, choiceSymbolList, combineSize, sb,null);

        symbolChoiceAnalysis.币种组合分析(endTime, 5, null, choiceSymbolList, combineSize, sb,null);

        System.out.println(" 标准_精选账户回测_短期初测Action ，结束日期 " + endTime);
        System.out.println(sb);
        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);

    }

    /**
     *
     天数 = 30  | 账户增长率 = 41.80%   | 币种增长率 = -5.04%   | 盈亏比 = 3.091  | 盈利 = 6439.241   | 亏损 = 2083.424   | 盈利次数 = 13.862 | 亏损次数 = 4.979
     天数 = 15  | 账户增长率 = 17.29%   | 币种增长率 = -3.18%   | 盈亏比 = 2.829  | 盈利 = 2854.184   | 亏损 = 1008.784   | 盈利次数 = 7.039  | 亏损次数 = 2.563
     天数 = 5   | 账户增长率 = 4.03%    | 币种增长率 = -1.20%   | 盈亏比 = 2.362  | 盈利 = 804.334    | 亏损 = 340.473    | 盈利次数 = 2.325  | 亏损次数 = 0.921
     */
    private void 标准_精选账户回测_短期复测Action(List<Symbol> choiceSymbolList) {

        long start = System.currentTimeMillis();

        // 初始可供选择的币种   7选5 = 21 个账户
        if (CollectionUtils.isEmpty(choiceSymbolList)) {
            choiceSymbolList = Lists.newArrayList();
            choiceSymbolList.add(Symbol.GALAUSDT);
            choiceSymbolList.add(Symbol.MATICUSDT);
            choiceSymbolList.add(Symbol.BNBUSDT);
            choiceSymbolList.add(Symbol.SHIBUSDT);
            choiceSymbolList.add(Symbol.SOLUSDT);
            choiceSymbolList.add(Symbol.ETHUSDT);
            choiceSymbolList.add(Symbol.LUNAUSDT);
        }

        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 16, 23, 0);

        StringBuilder sb = new StringBuilder();

        // 分析逻辑

        // 最早到 2021-12-11
        symbolChoiceAnalysis.币种组合分析(endTime, 30, 9, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-12-16
        symbolChoiceAnalysis.币种组合分析(endTime, 15, 11, choiceSymbolList, combineSize, sb,null);

        // 最早到 2021-12-21
        symbolChoiceAnalysis.币种组合分析(endTime, 5, 12, choiceSymbolList, combineSize, sb,null);

        System.out.println(" 标准_精选账户回测_短期复测Action ，结束日期 " + endTime);
        System.out.println(sb);
        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);

    }

    /**
     *
     天数 = 15  | 账户增长率 = 23.22%   | 币种增长率 = 5.28%    | 盈亏比 = 3.556  | 盈利 = 3311.241   | 亏损 = 931.189    | 盈利次数 = 7.668  | 亏损次数 = 2.15

     */
    private void 标准_精选账户回测_短期滚动Action(List<Symbol> choiceSymbolList) {

        long start = System.currentTimeMillis();

        // 初始可供选择的币种   7选5 = 21 个账户
        if (CollectionUtils.isEmpty(choiceSymbolList)) {
            choiceSymbolList = Lists.newArrayList();
            choiceSymbolList.add(Symbol.GALAUSDT);
            choiceSymbolList.add(Symbol.MATICUSDT);
            choiceSymbolList.add(Symbol.BNBUSDT);
            choiceSymbolList.add(Symbol.SHIBUSDT);
            choiceSymbolList.add(Symbol.SOLUSDT);
            choiceSymbolList.add(Symbol.ETHUSDT);
            choiceSymbolList.add(Symbol.LUNAUSDT);
        }

        // 组合账户币种数量
        Integer combineSize = 5;

        // 回测截止日期
        LocalDateTime endTime = LocalDateTime.of(2022, 3, 16, 23, 0);

        StringBuilder sb = new StringBuilder();

        // 分析逻辑
        // 最早到 2021-12-31
        symbolChoiceAnalysis.币种组合分析(endTime, 15, 40, choiceSymbolList, combineSize, sb,1);

        System.out.println(" 标准_精选账户回测_短期滚动Action ，结束日期 " + endTime);
        System.out.println(sb);
        long spend = System.currentTimeMillis() - start;
        log.warn(" 回测结束 | 耗时 = {} 分", spend / 1000 / 60);

    }
}

package com.aixi.lv.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.analysis.SymbolAlternativeAnalysis;
import com.aixi.lv.analysis.SymbolNumAnalysis;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.service.MailService;
import com.aixi.lv.util.NumUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.aixi.lv.analysis.SymbolNumAnalysis.ANALYSIS_RESULT_LIST;
import static com.aixi.lv.config.ApiKeyConfig.WEB_PASSWORD;
import static com.aixi.lv.config.BackTestConfig.OPEN;

/**
 * @author Js
 */
@RestController
@RequestMapping("/backtest")
@Api(tags = "回测服务")
@Slf4j
public class BackTestController {

    @Resource
    SymbolNumAnalysis symbolNumAnalysis;

    @Resource
    SymbolAlternativeAnalysis symbolAlternativeAnalysis;

    @Resource
    MailService mailService;

    private Boolean curRun = Boolean.FALSE;

    @GetMapping("/symbol_alternative")
    @ApiOperation("到底选哪7个币种")
    public Result 到底选哪7个币种(String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        if (!OPEN) {
            return Result.fail("回测未开启");
        }

        if (curRun == Boolean.TRUE) {
            return Result.fail("当前正在跑回测任务");
        }

        try {

            curRun = Boolean.TRUE;

            symbolAlternativeAnalysis.alternativeAnalysis();

            mailService.sendEmail("回测结束", "到底选哪7个币种回测结束");

        } catch (Exception e) {

            log.error("到底选哪7个币种 异常", e);
            curRun = Boolean.FALSE;

            mailService.sendEmail("到底选哪7个币种", e.getMessage());
            // 终止程序运行
            System.exit(0);
        }

        return Result.success("回测结束");
    }

    /**
     * @param js
     * @param durationDay    回测天数
     * @param year           回测截止日期-年
     * @param month          回测截止日期-月
     * @param day            回测截止日期-日
     * @param initSymbolSize 回测起始的币种组合数量
     * @return
     */
    @GetMapping("/symbol_num")
    @ApiOperation("近期币种数量分析")
    public Result 近期币种数量分析(String js, Integer durationDay, Integer year, Integer month, Integer day,
        Integer initSymbolSize) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        if (!OPEN) {
            return Result.fail("回测未开启");
        }

        if (curRun == Boolean.TRUE) {
            return Result.fail("当前正在跑回测任务");
        }

        try {

            long start = System.currentTimeMillis();
            curRun = Boolean.TRUE;

            LocalDateTime endTime = LocalDateTime.of(year, month, day, 0, 0);

            symbolNumAnalysis.近期币种数量分析_多线程(endTime, durationDay, initSymbolSize);

            curRun = Boolean.FALSE;

            long spend = System.currentTimeMillis() - start;
            log.warn(" 回测结束 | 截止时间 = {} | 天数 = {}", endTime, durationDay);
            log.warn(" 回测结束 | 耗时 = {} 分钟", spend / 1000 / 60);
            mailService.sendEmail("回测结束", "近期币种数量分析回测结束");

        } catch (Exception e) {

            log.error("近期币种数量分析 异常", e);
            curRun = Boolean.FALSE;

            mailService.sendEmail("近期币种数量分析异常", e.getMessage());
            // 终止程序运行
            System.exit(0);
        }

        return Result.success("回测结束");
    }

    /**
     * @param js
     * @param durationDay
     * @param year
     * @param month
     * @param day
     * @param initSymbolSize
     * @param forSize        循环次数
     * @return
     */
    @GetMapping("/symbol_for")
    @ApiOperation("多次币种数量分析")
    public Result 多次币种数量分析(String js, Integer durationDay, Integer year, Integer month, Integer day,
        Integer initSymbolSize, Integer forSize) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        if (!OPEN) {
            return Result.fail("回测未开启");
        }

        if (curRun == Boolean.TRUE) {
            return Result.fail("当前正在跑回测任务");
        }

        try {

            long start = System.currentTimeMillis();
            curRun = Boolean.TRUE;

            LocalDateTime finalEndTime = LocalDateTime.of(2021, 8, 1, 0, 0);

            LocalDateTime endTime = LocalDateTime.of(year, month, day, 0, 0);

            HashMap<String, MutablePair<Double, Double>> map = new HashMap<>();

            Integer forCount = 0;

            for (int i = 0; i < forSize; i++) {

                if (endTime.isBefore(finalEndTime)) {
                    break;
                }

                symbolNumAnalysis.近期币种数量分析_多线程(endTime, durationDay, initSymbolSize);

                for (JSONObject jsonObject : ANALYSIS_RESULT_LIST) {
                    String accountName = jsonObject.getString("accountName");
                    Double accountRate = jsonObject.getDouble("accountRate");
                    Double natureRate = jsonObject.getDouble("natureRate");

                    log.warn(" 单次循环回测结果 | endTime = {} | 账户 = {} | 账户增长率 = {} | 自然增长率 = {}", endTime, accountName,
                        NumUtil.percent(accountRate), NumUtil.percent(natureRate));

                    if (map.containsKey(accountName)) {
                        MutablePair<Double, Double> pair = map.get(accountName);
                        pair.setLeft(pair.getLeft() + accountRate);
                        pair.setRight(pair.getRight() + natureRate);
                    } else {
                        map.put(accountName, MutablePair.of(accountRate, natureRate));
                    }
                }

                forCount++;
                endTime = endTime.minusDays(15);
                ANALYSIS_RESULT_LIST.clear();

                log.warn(" 当前大循环已完成次数 = " + forCount);

            }

            for (Entry<String, MutablePair<Double, Double>> entry : map.entrySet()) {
                log.warn(" 整体循环回测结果 | 账户 = {} | 账户增长率 = {} | 自然增长率 = {}", entry.getKey(),
                    NumUtil.percent(entry.getValue().getLeft() / forCount),
                    NumUtil.percent(entry.getValue().getRight()));
            }

            curRun = Boolean.FALSE;

            long spend = System.currentTimeMillis() - start;
            log.warn(" 回测结束 | 耗时 = {} 分钟", spend / 1000 / 60);
            mailService.sendEmail("回测结束", "多次币种数量分析");

        } catch (Exception e) {

            log.error("多次币种数量分析 异常", e);
            curRun = Boolean.FALSE;

            mailService.sendEmail("多次币种数量分析", e.getMessage());
            // 终止程序运行
            System.exit(0);
        }

        return Result.success("回测结束");
    }
}

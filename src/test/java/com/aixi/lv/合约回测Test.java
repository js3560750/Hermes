package com.aixi.lv;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.analysis.ContractBackTestAnalysis;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.ContractAccount;
import com.aixi.lv.model.domain.MacdAccount;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static com.aixi.lv.analysis.ContractBackTestAnalysis.THREAD_LOCAL_CONTRACT_ACCOUNT;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 */
@Slf4j
public class 合约回测Test extends BaseTest {

    @Resource
    ContractBackTestAnalysis contractBackTestAnalysis;

    @Test
    public void 简单回测() {

        LocalDateTime startTime = LocalDateTime.of(2022, 3, 19, 0, 0);
        LocalDateTime endTime = startTime.plusDays(45);


        contractBackTestAnalysis.doAnalysis(startTime, endTime);
    }

}

package com.aixi.lv;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.analysis.BackTestAnalysis;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;

import static com.aixi.lv.config.BackTestConfig.INIT_BACK_TEST_AMOUNT;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 */
@Slf4j
public class 预警分析Test extends BaseTest {

    @Resource
    BackTestAnalysis backTestAnalysis;

    @Test
    public void 底部回升预警() {

        MacdAccount account = new MacdAccount();
        THREAD_LOCAL_ACCOUNT.set(account);

        LocalDateTime endTime = LocalDateTime.of(2022, 3, 19, 23, 0);

        LocalDateTime startTime = endTime.minusDays(19);

        System.out.println(startTime);

        backTestAnalysis.doWarning(startTime, endTime);

    }



}

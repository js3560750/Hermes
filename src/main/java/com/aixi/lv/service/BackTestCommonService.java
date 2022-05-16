package com.aixi.lv.service;

import com.aixi.lv.model.domain.MacdAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 *
 * 回测通用服务
 */
@Component
@Slf4j
public class BackTestCommonService {

    /**
     * 返回当前回测的自然时间
     *
     *
     * @return
     */
    public String backTestNatureTime() {


        if (!OPEN) {
            return "非回测";
        }

        MacdAccount account = THREAD_LOCAL_ACCOUNT.get();

        return account.getCurBackTestComputeTime().toString();
    }
}

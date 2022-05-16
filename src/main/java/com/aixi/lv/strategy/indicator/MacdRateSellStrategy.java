package com.aixi.lv.strategy.indicator;

import java.math.BigDecimal;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.indicator.MACD;
import com.aixi.lv.service.BackTestCommonService;
import com.aixi.lv.service.IndicatorService;
import com.aixi.lv.util.NumUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Js
 */
@Component
@Slf4j
public class MacdRateSellStrategy {

    @Resource
    MacdBuySellStrategy macdStrategy;

    @Resource
    IndicatorService indicatorService;

    @Resource
    BackTestCommonService backTestCommonService;

    /**
     * 卖出探测
     *
     * @param account
     */
    public void sellDetect(MacdAccount account) {

        if (macdStrategy.isEmptyAccount(account)) {
            return;
        }

        if (account.getReadySellFlag()) {
            return;
        }

        Symbol curHoldSymbol = account.getCurHoldSymbol();
        String name = account.getName();

        if (curHoldSymbol == null) {
            return;
        }

        // 当前MACD是否处于高位下降趋势
        if (isCurLowerThanLast(curHoldSymbol, name)) {
            macdStrategy.sellAction(curHoldSymbol, account, "MACD高位下降卖出", new BigDecimal("0.1"));
        }

    }

    /**
     * 当前MACD是否处于高位下降趋势
     *
     * @param symbol
     * @param name
     * @return
     */
    private Boolean isCurLowerThanLast(Symbol symbol, String name) {

        Double cur = macdStrategy.getCurHourMacd(symbol);
        Double last1 = macdStrategy.getLast1HourMacd(symbol);
        Double last2 = macdStrategy.getLast2HourMacd(symbol);
        Double last3 = macdStrategy.getLast3HourMacd(symbol);
        Double last4 = macdStrategy.getLast4HourMacd(symbol);

        double changeRate = (last1 - last2) / Math.abs(last2);

        if (last4 > 0 && last4 < last3 && last3 < last2 && last2 < last1) {

            log.info(" MACD 计算 | {} | {} | 当前MACD处于高位下降趋势 | last1 = {} | last2 = {} | last3 = {} | 回测自然时间 = {}",
                StringUtils.rightPad(name, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.showDouble(Math.abs(last1)),
                NumUtil.showDouble(Math.abs(last2)),
                NumUtil.showDouble(Math.abs(last3)),
                backTestCommonService.backTestNatureTime()
            );

            return true;
        } else {
            return false;
        }
    }

}

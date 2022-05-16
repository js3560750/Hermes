package com.aixi.lv.strategy.indicator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.indicator.BOLL;
import com.aixi.lv.service.BackTestOrderService;
import com.aixi.lv.service.IndicatorService;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.MACD_ACCOUNT_MAP;

/**
 * @author Js
 *
 * 布林带卖出策略， 和 MacdBuySellStrategy 一起结合使用
 */
@Component
@Slf4j
public class BollSellStrategy {

    @Resource
    MacdBuySellStrategy macdBuySellStrategy;

    @Resource
    IndicatorService indicatorService;

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BackTestOrderService backTestOrderService;

    /**
     * 布林线卖出探测
     */
    public void detectSell() {

        for (MacdAccount account : MACD_ACCOUNT_MAP.values()) {

            Symbol symbol = account.getCurHoldSymbol();

            if (symbol == null) {
                continue;
            }

            if (isUnderLowerBand(symbol, account)) {
                // 卖出
                if (OPEN) {
                    backTestOrderService.sellAction(symbol, account, "跌破布林线卖出", BigDecimal.ONE);
                } else {
                    macdBuySellStrategy.sellAction(symbol, account, "跌破布林线卖出 " + account.getName(), BigDecimal.ONE);
                }
            }

        }

    }

    /**
     * 看最近2根K线是否持续低于 BOLL 带
     *
     * @param symbol
     * @return
     */
    private Boolean isUnderLowerBand(Symbol symbol, MacdAccount account) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime;
        LocalDateTime startTime = endTime.minusMinutes((500 - 1) * 5);
        BOLL boll = indicatorService.getBOLLByTime(symbol, Interval.MINUTE_5, startTime, endTime);
        BigDecimal lowerBandPrice = new BigDecimal(boll.getLowerBand());

        List<KLine> kLines = priceFaceService.queryKLineByTime(symbol, Interval.MINUTE_5, 10, endTime.minusMinutes(10),
            endTime);
        KLine kLine = kLines.get(kLines.size() - 2);

        if (kLine.getClosingPrice().compareTo(lowerBandPrice) >= 0) {
            return Boolean.FALSE;
        }

        LocalDateTime endTime2 = endTime.minusMinutes(5);
        LocalDateTime startTime2 = endTime2.minusMinutes((500 - 1) * 5);
        BOLL boll2 = indicatorService.getBOLLByTime(symbol, Interval.MINUTE_5, startTime2, endTime2);
        BigDecimal lowerBandPrice2 = new BigDecimal(boll2.getLowerBand());

        List<KLine> kLines2 = priceFaceService.queryKLineByTime(symbol, Interval.MINUTE_5, 10,
            endTime2.minusMinutes(10),
            endTime2);
        KLine kLine2 = kLines2.get(kLines2.size() - 2);

        if (kLine2.getClosingPrice().compareTo(lowerBandPrice2) >= 0) {
            return Boolean.FALSE;
        }

        double bollDiffRate = (boll.getUpperBand() - boll.getLowerBand()) / boll.getMiddleBand();
        if (bollDiffRate < 0.011) {
            log.info(
                " BOLL 策略 | {} | 布林区间过小 {} | upperBand = {} | lowerBand = {} | bollDiffRate = {} | bollTime = {} | "
                    + "kLineClosingPrice = {}",
                account.getName(), symbol, boll.getUpperBand(), boll.getLowerBand(), bollDiffRate,
                boll.getKLineOpenTime(), kLine.getClosingPrice());
            return Boolean.FALSE;
        }

        // 持续低于布林带 下带 ，卖出
        log.info(" BOLL 策略 | {} | 布林线卖出 {} | 最近一根K线 | openTime = {} | closingPrice = {} | lowerBandPrice= {}",
            account.getName(), symbol, kLine.getOpeningTime(), kLine.getClosingPrice(), lowerBandPrice);
        log.info(" BOLL 策略 | {} | 布林线卖出 {} | 次近一根K线 | openTime = {} | closingPrice = {} | lowerBandPrice= {}",
            account.getName(), symbol, kLine2.getOpeningTime(), kLine2.getClosingPrice(), lowerBandPrice2);
        return Boolean.TRUE;
    }
}

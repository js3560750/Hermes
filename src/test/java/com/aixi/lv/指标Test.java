package com.aixi.lv;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.indicator.BOLL;
import com.aixi.lv.model.indicator.RSI;
import com.aixi.lv.service.BackTestPriceService;
import com.aixi.lv.service.IndicatorService;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.strategy.indicator.MacdBuySellStrategy;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static com.aixi.lv.config.MacdTradeConfig.IGNORE_DAY_MACD_SYMBOL_LIST;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js
 */
@Slf4j
public class 指标Test extends BaseTest {

    @Resource
    PriceService priceService;

    @Resource
    IndicatorService indicatorService;

    @Resource
    MacdBuySellStrategy macdBuySellStrategy;

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BackTestPriceService backTestPriceService;

    @Test
    public void testMACD() {

        Symbol symbol = Symbol.BTCUSDT;
        Double last1HourMacd = macdBuySellStrategy.getLast1HourMacd(symbol);
        Double last2HourMacd = macdBuySellStrategy.getLast2HourMacd(symbol);
        Double last3HourMacd = macdBuySellStrategy.getLast3HourMacd(symbol);
        Double last4HourMacd = macdBuySellStrategy.getLast4HourMacd(symbol);

        System.out.println(last1HourMacd);
        System.out.println(last2HourMacd);
        System.out.println(last3HourMacd);
        System.out.println(last4HourMacd);

    }

    @Test
    public void 回测状态下MACD() {

        MacdAccount account = new MacdAccount();
        account.setName("主线程");
        account.setCurBackTestComputeTime(LocalDateTime.of(2022, 2, 24, 20, 0));
        THREAD_LOCAL_ACCOUNT.set(account);

        Symbol symbol = Symbol.BTCUSDT;
        System.out.println(macdBuySellStrategy.getLast1HourMacd(symbol));
        System.out.println(macdBuySellStrategy.getLast2HourMacd(symbol));
        System.out.println(macdBuySellStrategy.getLast3HourMacd(symbol));
        System.out.println(macdBuySellStrategy.getLast4HourMacd(symbol));

    }

    @Test
    public void testMACD2() {

        LocalDateTime endTime = LocalDateTime.of(2021, 2, 1, 0, 0);
        LocalDateTime startTime = endTime.minusDays((500 - 1) * 1);
        for (Symbol symbol : Symbol.values()) {
            try {
                indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();
            } catch (Exception e) {
                System.out.println("没数据的币种是" + symbol);
                System.out.println(e);
            }
        }
    }

    @Test
    public void 价格数据对比() {

        Symbol symbol = Symbol.FTMUSDT;
        LocalDateTime startTime = LocalDateTime.of(2020, 10, 4, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 2, 15, 10, 0);

        List<KLine> kLines = backTestPriceService.queryKLineByTime(symbol, Interval.DAY_1, 500, startTime, endTime);
        List<KLine> kLines2 = priceService.queryKLineByTime(symbol, Interval.DAY_1, 500, startTime, endTime);

        for (int i = 0; i < kLines.size(); i++) {

            KLine backTestKLine = kLines.get(i);
            KLine onlineKLine = kLines2.get(i);

            if (backTestKLine.getClosingPrice().compareTo(onlineKLine.getClosingPrice()) != 0) {
                System.out.println("时间 = " + backTestKLine.getOpeningTime());
                System.out.println("回测价格 = " + backTestKLine.getClosingPrice());
                System.out.println("市场价格 = " + onlineKLine.getClosingPrice());
            }
        }

    }

    @Test
    public void testRSI() {

        LocalDateTime initTime = LocalDateTime.of(2022, 1, 11, 12, 0);
        for (int i = 0; i < 30; i++) {

            LocalDateTime endTime = initTime.plusMinutes(5);
            initTime = endTime;
            LocalDateTime startTime = endTime.minusMinutes((500 - 1) * 5);

            // 查K线
            List<KLine> kLines = priceService.queryKLineByTime(Symbol.ETHUSDT, Interval.MINUTE_5, 500, startTime,
                endTime);

            List<Double> closingPrices = kLines.stream().map(kLine -> kLine.getClosingPrice().doubleValue()).collect(
                Collectors.toList());

            RSI rsi = new RSI(closingPrices, 12);

            System.out.println(TimeUtil.getTime(endTime.minusMinutes(5)) + " RSI = " + rsi.get());
        }

    }

    @Test
    public void testRSI2() {

        RSI rsi = indicatorService.getRSI(Symbol.DOGEUSDT, Interval.HOUR_1);

        System.out.println(rsi.getKLineOpenTime() + "    " + rsi.get());
        System.out.println(rsi.getSymbol());
        System.out.println(rsi.getInterval());
    }

    @Test
    public void testBOLL2() {

        LocalDateTime initEndTime = LocalDateTime.now();

        LocalDateTime endTime = initEndTime;
        LocalDateTime startTime = endTime.minusMinutes((500 - 1) * 5);
        BOLL boll = indicatorService.getBOLLByTime(Symbol.ETHUSDT, Interval.MINUTE_5, startTime, endTime);
        BigDecimal lowerBandPrice = new BigDecimal(boll.getLowerBand());

        List<KLine> kLines = priceService.queryKLineByTime(Symbol.ETHUSDT, Interval.MINUTE_5, 10,
            endTime.minusMinutes(10),
            endTime);
        KLine kLine = kLines.get(kLines.size() - 2);

        LocalDateTime endTime2 = endTime.minusMinutes(5);
        LocalDateTime startTime2 = endTime2.minusMinutes((500 - 1) * 5);
        BOLL boll2 = indicatorService.getBOLLByTime(Symbol.ETHUSDT, Interval.MINUTE_5, startTime2, endTime2);
        BigDecimal lowerBandPrice2 = new BigDecimal(boll2.getLowerBand());

        List<KLine> kLines2 = priceService.queryKLineByTime(Symbol.ETHUSDT, Interval.MINUTE_5, 10,
            endTime2.minusMinutes(10),
            endTime2);
        KLine kLine2 = kLines2.get(kLines2.size() - 2);

        // 持续低于布林带 下带 ，卖出
        log.info(" BOLL 策略 | {} | 布林线卖出 {} | 最近一根K线 | openTime = {} | closingPrice = {} | lowerBandPrice= {}",
            "测试", Symbol.ETHUSDT, kLine.getOpeningTime(), kLine.getClosingPrice(), lowerBandPrice);
        log.info(" BOLL 策略 | {} | 布林线卖出 {} | 次近一根K线 | openTime = {} | closingPrice = {} | lowerBandPrice= {}",
            "测试", Symbol.ETHUSDT, kLine2.getOpeningTime(), kLine2.getClosingPrice(), lowerBandPrice2);
    }

    @Test
    public void tesBOLL() {

        LocalDateTime initTime = LocalDateTime.of(2022, 1, 14, 13, 0);
        for (int i = 0; i < 5; i++) {

            LocalDateTime endTime = initTime.plusMinutes(5);
            initTime = endTime;
            LocalDateTime startTime = endTime.minusMinutes((50 - 1) * 5);

            // 查K线
            List<KLine> kLines = priceService.queryKLineByTime(Symbol.ETHUSDT, Interval.MINUTE_5, 50, startTime,
                endTime);

            List<Double> closingPrices = kLines.stream().map(kLine -> kLine.getClosingPrice().doubleValue()).collect(
                Collectors.toList());

            BOLL BOLL = new BOLL(closingPrices, 20);

            System.out.println(TimeUtil.getTime(endTime.minusMinutes(5)));
            System.out.println(BOLL.getUpperBand());

            System.out.println(BOLL.getMiddleBand());

            System.out.println(BOLL.getLowerBand());
        }

    }

    @Test
    public void 上2小时K线() {
        LocalDateTime endTime = LocalDateTime.of(2022, 2, 14, 15, 1);
        LocalDateTime startTime = endTime.minusHours(5);
        List<KLine> kLines = priceFaceService.queryKLineByTime(Symbol.BTCUSDT, Interval.HOUR_1, 10, startTime, endTime);
        System.out.println(kLines);

        List<KLine> sortedList = kLines.stream()
            .sorted(Comparator.comparing(KLine::getOpeningTime).reversed())
            .collect(Collectors.toList());

        KLine last1Hour = sortedList.get(1);

        KLine beforeLast1Hour = sortedList.get(2);

        System.out.println(last1Hour);
        System.out.println(beforeLast1Hour);
    }

    @Test
    public void testDayMacd() {

        for (Symbol symbol : Symbol.values()) {

            try {

                // 4小时级别
                macdBuySellStrategy.isCur4HourMacdDown(symbol, "测试");

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            try {

                // 天级别
                if (IGNORE_DAY_MACD_SYMBOL_LIST.contains(symbol)) {
                    continue;
                }
                macdBuySellStrategy.isYesterdayMacdDown(symbol, "测试");

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }

    }

    @Test
    public void testDayMacd2() {

            List<Symbol> symbolList = Lists.newArrayList();
            symbolList.add(Symbol.GMTUSDT);


            for (Symbol symbol : symbolList) {

                try {

                    // 4小时级别
                    macdBuySellStrategy.isCur4HourMacdDown(symbol, "测试");

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                try {

                    // 天级别
                    macdBuySellStrategy.isYesterdayMacdDown(symbol, "测试");

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

    }

    @Test
    public void 币种数据_可用日期检测2() {

        LocalDateTime endTime = LocalDateTime.of(2021, 12, 20, 0, 0);

        LocalDateTime startTime = endTime.minusDays((500 - 1) * 1);

        Symbol symbol = Symbol.BTCUSDT;
        try {
            indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    @Test
    public void 币种数据_可用日期检测() {

        LocalDateTime endTime = LocalDateTime.of(2022, 1, 10, 0, 0);
        LocalDateTime startTime = endTime.minusDays((500 - 1) * 1);
        for (Symbol symbol : Symbol.values()) {
            try {
                indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();
            } catch (Exception e) {
                System.out.println("没数据的币种是" + symbol);
                System.out.println(e);
            }
        }
    }
}

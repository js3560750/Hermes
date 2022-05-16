package com.aixi.lv.strategy.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.aixi.lv.config.BackTestConfig;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.OrderStatus;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.model.indicator.BOLL;
import com.aixi.lv.model.indicator.MACD;
import com.aixi.lv.model.indicator.RSI;
import com.aixi.lv.service.BackTestCommonService;
import com.aixi.lv.service.BackTestOrderService;
import com.aixi.lv.service.IndicatorService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceFaceService;
import com.aixi.lv.util.NumUtil;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.config.MacdTradeConfig.IGNORE_DAY_MACD_SYMBOL_LIST;

/**
 * @author Js
 */
@Component
@Slf4j
public class MacdBuySellStrategy {

    @Resource
    IndicatorService indicatorService;

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BackTestOrderService backTestOrderService;

    @Resource
    OrderService orderService;

    @Resource
    MailService mailService;

    @Resource
    BackTestCommonService backTestCommonService;

    /**
     * key : accountName
     * value : account object
     */
    public static final Map<String, MacdAccount> MACD_ACCOUNT_MAP = new ConcurrentHashMap<>();

    /**
     * 卖出预警
     */
    public static final Set<Symbol> SELL_WARNING_SET = new HashSet<>();

    /**
     * 邮件买卖提醒
     */
    public static final Set<Symbol> EMAIL_SELL_SET = new HashSet<>();
    public static final Set<Symbol> EMAIL_BUY_SET = new HashSet<>();

    public static Double DAY_MACD_RATE_VALUE = 0.102;

    public static Double CHANGE_RATE_VALUE = 0.049;

    public static Double DEFAULT_MAX_RATE = 0.049;

    public static Double SWITCH_CHANGE_RATE_VALUE = 2.02d;

    /**
     * 买入卖出监测
     *
     * @param account
     */
    public void detect(MacdAccount account) {

        if (isEmptyAccount(account)) {
            return;
        }

        if (account.getReadySellFlag()) {
            return;
        }

        List<Symbol> symbolList = account.getSymbolList();

        Symbol curHoldSymbol = account.getCurHoldSymbol();

        // 买入标记
        Symbol buySymbol = null;
        Double maxRate = DEFAULT_MAX_RATE; // macd changeRate至少要大于10%

        for (Symbol symbol : symbolList) {

            // 购买前置条件检查
            if (!isAllowBuy(symbol, account)) {
                continue;
            }

            // 1小时级别MACD增长率比较
            Double last1HourMacd = this.getLast1HourMacd(symbol);
            Double beforeLast1HourMacd = this.getLast2HourMacd(symbol);

            double changeRate = (last1HourMacd - beforeLast1HourMacd) / Math.abs(beforeLast1HourMacd);

            if (last1HourMacd > beforeLast1HourMacd && changeRate > maxRate) {
                buySymbol = symbol;
                maxRate = changeRate;
            }
        }

        // 如果准备购买
        if (buySymbol != null && curHoldSymbol == null) {

            // 但上次卖出间隔不足4小时，则不买
            if (isLastSellTimeNear(buySymbol, account)) {
                buySymbol = null;
            }

        }

        // !!!!!! 这个是强兜底，要放最后执行，执行完之后，就直接 handleAction()
        // 如果当前已经持有了
        if (curHoldSymbol != null) {

            // 如果MACD增长比率太小，则继续持有当前的
            if (maxRate < SWITCH_CHANGE_RATE_VALUE) {
                buySymbol = null;
            }

            // 如果当前没有购买的币种，但当前持有的币种是正的macd，则继续持有
            if (buySymbol == null && isHoldCurSymbol(curHoldSymbol, account)) {
                buySymbol = curHoldSymbol;
            }

        }

        // 处理动作
        this.handleAction(buySymbol, curHoldSymbol, maxRate, account);

        // 上述策略执行完毕后， 当前持有币种可能会变，所以需重新赋值
        curHoldSymbol = account.getCurHoldSymbol();

        // 执行结果打印
        if (curHoldSymbol != null) {
            BigDecimal newPrice = priceFaceService.queryNewPrice(curHoldSymbol);
            // 账户余额 + 持有货币价值
            BigDecimal curAmount = account.getCurHoldAmount()
                .add(account.getCurHoldQty().multiply(newPrice))
                .setScale(2, RoundingMode.HALF_DOWN);
            log.info(" MACD 策略 | {} | 执行完毕 | 当前持有 {} | 账户总额 {} | 回测自然时间 = {}",
                StringUtils.rightPad(account.getName(), 10),
                curHoldSymbol,
                curAmount,
                backTestCommonService.backTestNatureTime());
        } else {
            log.info(" MACD 策略 | {} | 执行完毕 | 当前空仓 | 账户总额 {} | 回测自然时间 = {}",
                StringUtils.rightPad(account.getName(), 10),
                account.getCurHoldAmount(),
                backTestCommonService.backTestNatureTime());
        }

    }

    /**
     * 是否允许购买
     *
     * @param symbol
     * @param account
     * @return
     */
    public Boolean isAllowBuy(Symbol symbol, MacdAccount account) {

        LocalDateTime nowTime = TimeUtil.now();
        LocalDateTime compareTime
            = LocalDateTime.of(nowTime.getYear(), nowTime.getMonth(), nowTime.getDayOfMonth(), 3, 0);

        // 有些太新的币，就不做天级别检查了，因为算不出来天级别MACD
        if (!IGNORE_DAY_MACD_SYMBOL_LIST.contains(symbol)) {
            if (nowTime.isAfter(compareTime)) {
                // 如果过了凌晨3点，检查今天的MACD
                if (isTodayMacdDown(symbol, account.getName())) {
                    return false;
                }
            } else {
                // 如果没过凌晨3点，检查昨天的MACD
                if (isYesterdayMacdDown(symbol, account.getName())) {
                    return false;
                }
            }
        }

        // MACD绝对值是否过小
        //if (isDayMacdTooSmaller(symbol, account.getAccountName())
        //    || isHourMacdTooSmaller(symbol, account.getAccountName())) {
        //    return false;
        //}

        // 4小时级别MACD检查
        if (isLast4HourMacdDown(symbol, account.getName())) {
            return false;
        }

        // 4小时级别MACD检查
        if (isCur4HourMacdDown(symbol, account.getName())) {
            return false;
        }

        return true;
    }

    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/
    /**************************   ∧∧∧∧∧∧∧∧∧∧∧∧∧∧  核心策略  ∧∧∧∧∧∧∧∧∧∧∧∧∧∧   ****************************/

    /**
     * 当前是否等于空账户
     *
     * @param account
     * @return
     */
    public Boolean isEmptyAccount(MacdAccount account) {

        if (account.getCurHoldSymbol() != null) {
            return Boolean.FALSE;
        }

        if (account.getCurHoldAmount() == null
            && account.getCurHoldQty() == null) {
            return Boolean.TRUE;
        }

        if (account.getCurHoldQty().compareTo(BigDecimal.ZERO) <= 0
            && account.getCurHoldAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * 判断 MACD 增长比例是否过小
     *
     * @param changeRate
     * @param curMacd
     * @param lastMacd
     * @param accountName
     * @param symbol
     * @return
     */
    private Boolean isChangeRateSmaller(double changeRate, Double curMacd, Double lastMacd, String accountName,
        Symbol symbol) {

        if (changeRate < 0.19) {
            // macd 增长比例太小也不购买
            log.info(" MACD 计算 | {} | {} | 1小时 MACD 未明显增长 | changeRate = {} | cur = {} | last = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.percent(changeRate),
                NumUtil.showDouble(curMacd),
                NumUtil.showDouble(lastMacd),
                backTestCommonService.backTestNatureTime());
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 当前时间上根K线的收盘价是否大于 BOLL下带
     *
     * @param symbol
     * @return
     */
    private Boolean isBiggerThanBollLowerBand(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime;
        LocalDateTime startTime = endTime.minusMinutes((500 - 1) * 5);
        BOLL boll = indicatorService.getBOLLByTime(symbol, Interval.MINUTE_5, startTime, endTime);
        BigDecimal lowerBandPrice = new BigDecimal(boll.getLowerBand());

        List<KLine> kLines = priceFaceService.queryKLineByTime(symbol, Interval.MINUTE_5, 10, endTime.minusMinutes(10),
            endTime);
        KLine kLine = kLines.get(kLines.size() - 2);

        if (kLine.getClosingPrice().compareTo(lowerBandPrice) > 0) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 获得上1天的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast1DayMacd(Symbol symbol) {

        Double macd = indicatorService.getMACD(symbol, Interval.DAY_1).get();

        return macd;
    }

    /**
     * 获得上2小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast2DayMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusDays(1);
        LocalDateTime startTime = endTime.minusDays((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上3小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast3DayMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusDays(2);
        LocalDateTime startTime = endTime.minusDays((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上4小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast4DayMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusDays(3);
        LocalDateTime startTime = endTime.minusDays((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得当前小时级的macd
     *
     * @param symbol
     * @return
     */
    public Double getCurHourMacd(Symbol symbol) {

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        return cur;
    }

    /**
     * 获得上1小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast1HourMacd(Symbol symbol) {

        Double macd = indicatorService.getMACD(symbol, Interval.HOUR_1).get();

        return macd;
    }

    /**
     * 获得上2小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast2HourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(1);
        LocalDateTime startTime = endTime.minusHours((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_1, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上3小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast3HourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(2);
        LocalDateTime startTime = endTime.minusHours((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_1, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上4小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast4HourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(3);
        LocalDateTime startTime = endTime.minusHours((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_1, startTime, endTime).get();

        return macd;
    }

    /**
     * 获得上5小时的macd
     *
     * @param symbol
     * @return
     */
    public Double getLast5HourMacd(Symbol symbol) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime.minusHours(4);
        LocalDateTime startTime = endTime.minusHours((500 - 1));

        Double macd = indicatorService.getMACDByTime(symbol, Interval.HOUR_1, startTime, endTime).get();

        return macd;
    }

    public Boolean isDayMacdTooSmaller(Symbol symbol, String accountName) {

        Double last1 = this.getLast1DayMacd(symbol);
        Double last2 = this.getLast2DayMacd(symbol);
        Double last3 = this.getLast3DayMacd(symbol);
        Double last4 = this.getLast4DayMacd(symbol);

        double average = (Math.abs(last2) + Math.abs(last3) + Math.abs(last4)) / 3 * 0.2d;

        MACD macd = indicatorService.getMACD(symbol, Interval.DAY_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        if (Math.abs(cur) < average || Math.abs(last1) < average) {

            log.info(" MACD 计算 | {} | {} | 天级MACD绝对值过小 | cur = {} | last1 = {} | average = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.showDouble(Math.abs(cur)),
                NumUtil.showDouble(Math.abs(last1)),
                NumUtil.showDouble(Math.abs(average)),
                backTestCommonService.backTestNatureTime()
            );

            return true;
        } else {
            return false;
        }

    }

    public Boolean isPossibleSell(Symbol symbol, String accountName) {

        Double last1 = this.getLast1HourMacd(symbol);
        Double last2 = this.getLast2HourMacd(symbol);
        Double last3 = this.getLast3HourMacd(symbol);
        Double last4 = this.getLast4HourMacd(symbol);

        double average = (Math.abs(last2) + Math.abs(last3) + Math.abs(last4)) / 3 * 0.3d;

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        if (Math.abs(cur) < average || Math.abs(last1) < average) {
            log.info(" MACD 计算 | {} | {} | 小时级MACD绝对值过小_卖出警告 | cur = {} | last1 = {} | average = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.showDouble(Math.abs(cur)),
                NumUtil.showDouble(Math.abs(last1)),
                NumUtil.showDouble(Math.abs(average))
            );
            return true;
        } else {
            return false;
        }

    }

    public Boolean isPossibleSell2(Symbol symbol, String accountName) {

        Double last1 = this.getLast1HourMacd(symbol);
        Double last2 = this.getLast2HourMacd(symbol);

        double changeValue = last1 - last2;

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        double nextMacd = cur + changeValue;

        if (nextMacd < 0) {
            log.info(" MACD 计算 | {} | {} | 下一小时MACD很可能为负_卖出警告 | curMacd = {} | nextMacd = {} | changeValue = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.showDouble(Math.abs(cur)),
                NumUtil.showDouble(Math.abs(nextMacd)),
                NumUtil.showDouble(Math.abs(changeValue))
            );
            return true;
        } else {
            return false;
        }

    }

    public Boolean isHourMacdTooSmaller(Symbol symbol, String accountName) {

        Double last1 = this.getLast1HourMacd(symbol);
        Double last2 = this.getLast2HourMacd(symbol);
        Double last3 = this.getLast3HourMacd(symbol);
        Double last4 = this.getLast4HourMacd(symbol);

        double average = (Math.abs(last2) + Math.abs(last3) + Math.abs(last4)) / 3 * 0.2d;

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        if (Math.abs(cur) < average || Math.abs(last1) < average) {

            log.info(" MACD 计算 | {} | {} | 小时级MACD绝对值过小 | cur = {} | last1 = {} | average = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.showDouble(Math.abs(cur)),
                NumUtil.showDouble(Math.abs(last1)),
                NumUtil.showDouble(Math.abs(average)),
                backTestCommonService.backTestNatureTime()
            );

            return true;
        } else {
            return false;
        }

    }

    /**
     * 是否连着4个小时级 MACD 都在下跌
     *
     * @param symbol
     * @return
     */
    public Boolean isContinuousDown(Symbol symbol, MacdAccount account) {

        Double last1 = this.getLast1HourMacd(symbol);
        Double last2 = this.getLast2HourMacd(symbol);
        Double last3 = this.getLast3HourMacd(symbol);
        Double last4 = this.getLast4HourMacd(symbol);

        double changeRate1 = (last1 - last2) / Math.abs(last2);
        double changeRate2 = (last2 - last3) / Math.abs(last3);
        double changeRate3 = (last3 - last4) / Math.abs(last4);

        if (changeRate3 < changeRate2 && changeRate2 < changeRate1) {
            if (changeRate1 < -0.2d && changeRate2 < -0.2d && changeRate3 < -0.2d) {
                log.info(" MACD 计算 | {} | {} | 连着4个小时级MACD都在下跌 | 回测自然时间 = {}",
                    StringUtils.rightPad(account.getName(), 10),
                    StringUtils.rightPad(symbol.getCode(), 10),
                    backTestCommonService.backTestNatureTime());
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;

    }

    /**
     * 距离上一次换仓时间太接近
     *
     * @param buySymbol
     * @param curHoldSymbol
     * @param account
     * @return
     */
    private Boolean isLastSwitchNear(Symbol buySymbol, Symbol curHoldSymbol, MacdAccount account) {

        if (buySymbol != null && curHoldSymbol != null && buySymbol != curHoldSymbol) {

            LocalDateTime lastSwitchTime = account.getLastSwitchTime();

            if (lastSwitchTime != null && TimeUtil.now().isBefore(lastSwitchTime.plusMinutes(90))) {

                log.info(" MACD 策略 | {} | 距离上次换仓时间太近 | 上次换仓时间 = {} | 回测自然时间 = {}",
                    StringUtils.rightPad(account.getName(), 10),
                    lastSwitchTime,
                    backTestCommonService.backTestNatureTime());

                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * 上4小时和 上上4小时 MACD是否下跌
     *
     * @param symbol
     * @param accountName
     * @return
     */
    public Boolean isLast4HourMacdDown(Symbol symbol, String accountName) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime;
        LocalDateTime startTime = endTime.minusHours((500 - 1) * 4);
        Double last = indicatorService.getMACDByTime(symbol, Interval.HOUR_4, startTime, endTime).get();

        LocalDateTime endTime2 = endTime.minusHours(4);
        LocalDateTime startTime2 = endTime2.minusHours((500 - 1) * 4);
        Double beforeLast = indicatorService.getMACDByTime(symbol, Interval.HOUR_4, startTime2, endTime2).get();

        double changeRate = (last - beforeLast) / Math.abs(beforeLast);

        if (changeRate < CHANGE_RATE_VALUE) {

            // 非增长趋势
            log.info(" MACD 计算 | {} | {} | 4小时MACD 未明显增长 | changeRate = {} | last = {} | beforeLast = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10), StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.percent(changeRate),
                NumUtil.showDouble(last),
                NumUtil.showDouble(beforeLast),
                backTestCommonService.backTestNatureTime()
            );

            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }

    }

    /**
     * 此时和上4小时的MACD对比，是否下降
     *
     * @param symbol
     * @param accountName
     * @return
     */
    public Boolean isCur4HourMacdDown(Symbol symbol, String accountName) {

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_4);

        Double last4Hour = macd.get();

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        double changeRate = (cur - last4Hour) / Math.abs(last4Hour);

        if (changeRate < CHANGE_RATE_VALUE) {
            // 非增长趋势
            log.info(
                " MACD 计算 | {} | {} | 此时和上4小时MACD 未明显增长 | changeRate = {} | cur = {} | last4Hour = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10), StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.percent(changeRate),
                NumUtil.showDouble(cur),
                NumUtil.showDouble(last4Hour),
                backTestCommonService.backTestNatureTime()
            );
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 此时和上1小时的MACD对比，是否下降
     *
     * @param symbol
     * @param accountName
     * @return
     */
    public Boolean isCur1HourMacdDown(Symbol symbol, String accountName) {

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        Double last1Hour = macd.get();

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        double changeRate = (cur - last1Hour) / Math.abs(last1Hour);

        if (changeRate < CHANGE_RATE_VALUE) {
            // 非增长趋势
            log.info(
                " MACD 计算 | {} | {} | 此时和上1小时MACD 未明显增长 | changeRate = {} | cur = {} | last1Hour = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10), StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.percent(changeRate),
                NumUtil.showDouble(cur),
                NumUtil.showDouble(last1Hour),
                backTestCommonService.backTestNatureTime()
            );
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 昨天和前天的MACD对比，是否下降
     *
     * @param symbol
     * @param accountName
     * @return
     */
    public Boolean isYesterdayMacdDown(Symbol symbol, String accountName) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime;
        LocalDateTime startTime = endTime.minusDays((500 - 1) * 1);
        Double yesterday = indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime, endTime).get();

        LocalDateTime endTime2 = endTime.minusDays(1);
        LocalDateTime startTime2 = endTime2.minusDays((500 - 1) * 1);
        Double beforeYesterday = indicatorService.getMACDByTime(symbol, Interval.DAY_1, startTime2, endTime2).get();

        double changeRate = (yesterday - beforeYesterday) / Math.abs(beforeYesterday);

        if (changeRate < DAY_MACD_RATE_VALUE) {
            // 非增长趋势
            log.info(
                " MACD 计算 | {} | {} | 昨天MACD 未明显增长 | changeRate = {} | yesterday = {} | beforeYesterday = {} | 回测自然时间"
                    + " = {}",
                StringUtils.rightPad(accountName, 10), StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.percent(changeRate),
                NumUtil.showDouble(yesterday),
                NumUtil.showDouble(beforeYesterday),
                backTestCommonService.backTestNatureTime()
            );
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }

    }

    /**
     * 今天和昨天的MACD对比，是否下降
     *
     * @param symbol
     * @param accountName
     * @return
     */
    public Boolean isTodayMacdDown(Symbol symbol, String accountName) {

        MACD macd = indicatorService.getMACD(symbol, Interval.DAY_1);

        Double yesterday = macd.get();

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double today = macd.get();

        double changeRate = (today - yesterday) / Math.abs(yesterday);

        if (changeRate < DAY_MACD_RATE_VALUE) {
            // 非增长趋势
            log.info(" MACD 计算 | {} | {} | 今天MACD 未明显增长 | changeRate = {} | today = {} | yesterday = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10), StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.percent(changeRate),
                NumUtil.showDouble(today),
                NumUtil.showDouble(yesterday),
                backTestCommonService.backTestNatureTime()
            );
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 是否达到 布林带上带
     *
     * @param symbol
     * @return
     */
    private Boolean isBollUpBand(Symbol symbol, BigDecimal newPrice, BOLL boll, String accountName) {

        BigDecimal upperBand = new BigDecimal(boll.getUpperBand());

        if (newPrice.compareTo(upperBand) >= 0) {
            log.info(" BOLL 计算 | {} | {} | 达到布林带上带 | newPrice = {} | upPrice = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                newPrice,
                upperBand,
                backTestCommonService.backTestNatureTime()
            );
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 5分钟级 MACD检查，检查最近3根 5分钟级MACD 是否递减
     *
     * @param symbol
     * @return
     */
    private Boolean isMacdDownByMinute(Symbol symbol, String accountName) {

        LocalDateTime initEndTime = TimeUtil.now();

        LocalDateTime endTime = initEndTime;
        LocalDateTime startTime = endTime.minusMinutes((500 - 1) * 5);
        MACD cur = indicatorService.getMACDByTime(symbol, Interval.MINUTE_5, startTime, endTime);

        LocalDateTime endTime2 = endTime.minusMinutes(5);
        LocalDateTime startTime2 = endTime2.minusMinutes((500 - 1) * 5);
        MACD last = indicatorService.getMACDByTime(symbol, Interval.MINUTE_5, startTime2, endTime2);

        LocalDateTime endTime3 = endTime2.minusMinutes(5);
        LocalDateTime startTime3 = endTime3.minusMinutes((500 - 1) * 5);
        MACD beforeLast = indicatorService.getMACDByTime(symbol, Interval.MINUTE_5, startTime3, endTime3);

        if (beforeLast.get() >= last.get() && last.get() >= cur.get()) {
            // 下降趋势
            log.info(" MACD 近3根5分钟线计算 | {} | {} | MACD 持续下降 | beforeLast = {} | last = {} | cur = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10), StringUtils.rightPad(symbol.getCode(), 10),
                NumUtil.showDouble(beforeLast.get()),
                NumUtil.showDouble(last.get()),
                NumUtil.showDouble(cur.get()),
                backTestCommonService.backTestNatureTime()
            );
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 具体处理动作
     *
     * @param buySymbol
     * @param curHoldSymbol
     * @param maxRate
     * @param account
     */
    private void handleAction(Symbol buySymbol, Symbol curHoldSymbol, Double maxRate, MacdAccount account) {

        if (buySymbol != null) {
            // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 有建议购买的币种 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

            if (curHoldSymbol != null) {
                // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 当前有持仓 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

                if (curHoldSymbol.equals(buySymbol)) {
                    // 持有
                    log.info(" MACD 策略 | {} | 继续持有 {} | 回测自然时间 = {}",
                        StringUtils.rightPad(account.getName(), 10),
                        curHoldSymbol,
                        backTestCommonService.backTestNatureTime());

                } else {
                    // 换仓

                    //log.info(" MACD 策略 | {} | 换仓 当前 = {} | 准备换成 = {} | 回测自然时间 = {}",
                    //    StringUtils.rightPad(account.getName(), 10),
                    //    curHoldSymbol,
                    //    buySymbol,
                    //    backTestCommonService.backTestNatureTime());
                    //
                    //account.setLastSwitchTime(TimeUtil.now());
                    //
                    //this.sellAction(curHoldSymbol, account, "换仓卖出 " + account.getName(), BigDecimal.ONE);
                    //
                    //// 购买动作
                    //this.buyAction(buySymbol, account, "换仓买入 " + account.getName());

                }

            } else {
                // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 当前无持仓 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

                // 买入
                log.info(" MACD 策略 | {} | 准备购买 {} | 回测自然时间 = {}",
                    StringUtils.rightPad(account.getName(), 10),
                    buySymbol,
                    backTestCommonService.backTestNatureTime());

                // 购买动作
                this.buyAction(buySymbol, account, "买入成交 " + account.getName());
            }

        } else {
            // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 没有建议购买的币种 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

            if (curHoldSymbol != null) {
                // 卖出
                log.info(" MACD 策略 | {} | 卖出 {} | maxRate={} | 回测自然时间 = {}",
                    StringUtils.rightPad(account.getName(), 10),
                    curHoldSymbol,
                    maxRate,
                    backTestCommonService.backTestNatureTime());

                LocalDateTime now = TimeUtil.now();
                account.setLastSellTime(now);
                account.setLastSellSymbol(curHoldSymbol);

                this.readySell(account);
                //this.sellAction(curHoldSymbol, account, "卖出成交 " + account.getName(), BigDecimal.ONE);

            } else {
                // 保持空仓
                log.info(" MACD 策略 | {} | 无购买币种 保持空仓 | maxRate={} | 回测自然时间 = {}",
                    StringUtils.rightPad(account.getName(), 10),
                    maxRate,
                    backTestCommonService.backTestNatureTime());
            }
        }

        return;
    }

    /**
     * 距离上一次卖出时间，太近
     *
     * @param account
     * @return
     */
    private Boolean isLastSellTimeNear(Symbol buySymbol, MacdAccount account) {

        LocalDateTime lastSellTime = account.getLastSellTime();
        Symbol lastSellSymbol = account.getLastSellSymbol();
        if (lastSellTime == null || lastSellSymbol == null) {
            return Boolean.FALSE;
        }

        if (buySymbol != lastSellSymbol) {
            return Boolean.FALSE;
        }

        LocalDateTime now = TimeUtil.now();

        LocalDateTime compareTime = lastSellTime.plusHours(3).plusMinutes(20);

        // 至少要间隔 X小时X分钟
        if (now.isBefore(compareTime)) {

            log.info(
                " MACD 计算 | {} | {} | 距离上一次卖出时间 | nowTime = {} | compareTime = {} | lastSellTime = {} | 回测自然时间 = {}",
                StringUtils.rightPad(account.getName(), 10),
                StringUtils.rightPad(buySymbol.getCode(), 10),
                now,
                compareTime,
                lastSellTime,
                backTestCommonService.backTestNatureTime()
            );

            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 是否继续持有当前币种 （强兜底）
     *
     * @param symbol
     * @return
     */
    private Boolean isHoldCurSymbol(Symbol symbol, MacdAccount account) {

        if (isLastHourMacdPositive(symbol)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 当前4小时MACD是否大于0
     *
     * @param symbol
     * @return
     */
    private Boolean isLast4HourMacdPositive(Symbol symbol) {

        // 上4小时MACD
        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_4);

        return macd.get() > 0;
    }

    /**
     * 当前4小时MACD是否大于0
     *
     * @param symbol
     * @return
     */
    private Boolean isCur4HourMacdPositive(Symbol symbol) {

        // 上4小时MACD
        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_4);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        // 更新后，MACD变成当前实时4小时MACD
        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        return cur > 0;
    }

    /**
     * 上1小时MACD是否大于0
     *
     * @param symbol
     * @return
     */
    private Boolean isLastHourMacdPositive(Symbol symbol) {

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        Double last1Hour = macd.get();

        return last1Hour > 0;
    }

    private Boolean isLastHourMacdDifSmall(Symbol symbol) {

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        double dif = macd.getCurrentDIF();
        double dem = macd.getCurrentDEM();

        if ((dif - dem) / dif < 0.05d) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }

    }

    /**
     * 今天此时MACD是否大于0
     *
     * @param symbol
     * @return
     */
    private Boolean isTodayMacdPositive(Symbol symbol) {

        MACD macd = indicatorService.getMACD(symbol, Interval.DAY_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        macd.update(newPrice.doubleValue());

        Double today = macd.get();

        return today > 0;
    }

    /**
     * 当前MACD是否大于0
     *
     * @param symbol
     * @return
     */
    private Boolean isCurHourMacdPositive(Symbol symbol) {

        MACD macd = indicatorService.getMACD(symbol, Interval.HOUR_1);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        // 更新后，MACD变成当前实时1小时MACD
        macd.update(newPrice.doubleValue());

        Double cur = macd.get();

        return cur > 0;
    }

    /**
     * 检查是否严重超买
     *
     * @param symbol
     * @param rsi
     * @return
     */
    private Boolean isOverBuyExtremely(Symbol symbol, RSI rsi, String accountName) {

        if (rsi.get() >= RSI.OVER_BUY_MAX) {
            log.info(" RSI 计算 | {} | {} | 严重超买 | rsi = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                rsi.get(),
                backTestCommonService.backTestNatureTime());
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * 检查是否严重超卖
     *
     * @param symbol
     * @param interval
     * @return
     */
    private Boolean isOverSellExtremely(Symbol symbol, Interval interval, String accountName) {

        RSI rsi = indicatorService.getRSI(symbol, interval);

        if (rsi.get() <= RSI.OVER_SELL_MIN) {
            log.info(" RSI 计算 | {} | {} | 严重超卖 | rsi = {} | 回测自然时间 = {}",
                StringUtils.rightPad(accountName, 10),
                StringUtils.rightPad(symbol.getCode(), 10),
                rsi.get(),
                backTestCommonService.backTestNatureTime());
            return Boolean.TRUE;
        }

        return Boolean.FALSE;

    }

    /**
     * 购买
     *
     * @param symbol
     * @param account
     * @param title
     */
    public void buyAction(Symbol symbol, MacdAccount account, String title) {

        if (OPEN) {
            backTestOrderService.buyAction(symbol, account);
            return;
        }

        OrderLife buyServer = orderService.marketBuyOrder(OrderSide.BUY, symbol, account.getCurHoldAmount());

        while (true) {

            OrderLife temp = orderService.queryByOrderId(symbol, buyServer.getOrderId());

            // 已完成
            if (OrderStatus.FILLED == temp.getStatus()) {

                TradePair curPair = new TradePair();
                curPair.setBuyOrder(temp);

                account.setLastBuyPrice(temp.getBuyPrice());
                account.setCurHoldSymbol(symbol);
                account.setCurHoldQty(temp.getExecutedQty());
                account.setCurHoldAmount(account.getCurHoldAmount().subtract(temp.getCumulativeQuoteQty()));

                // curPair 发邮件用
                account.setCurPair(curPair);

                //// 重复Symbol就不发重复邮件了
                //if (!EMAIL_BUY_SET.contains(symbol)) {
                //    EMAIL_BUY_SET.add(symbol);
                //    mailService.macdBuyMail(title, temp, account);
                //}

                return;
            }

            // 已取消
            if (OrderStatus.CANCELED == temp.getStatus()
                || OrderStatus.EXPIRED == temp.getStatus()
                || OrderStatus.REJECTED == temp.getStatus()) {
                return;
            }

            // 查到买单未成交 or 部分成交，等待几秒，再查一下
            if (OrderStatus.PARTIALLY_FILLED == temp.getStatus()
                || OrderStatus.NEW == temp.getStatus()) {

                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }

    }

    /**
     * 准备卖出
     *
     * @param account
     */
    public void readySell(MacdAccount account) {

        Symbol curHoldSymbol = account.getCurHoldSymbol();
        BigDecimal lastBuyPrice = account.getLastBuyPrice();

        BigDecimal newPrice = priceFaceService.queryNewPrice(curHoldSymbol);

        if (newPrice.compareTo(lastBuyPrice) > 0) {
            this.sellAction(curHoldSymbol, account, "MACD正常卖出成交", BigDecimal.ONE);
            return;
        }

        if (isHighPriceBuy(curHoldSymbol, lastBuyPrice)) {
            this.sellAction(curHoldSymbol, account, "高位风险卖出成交", BigDecimal.ONE);
            return;
        }

        // 卖出探测
        account.setReadySellFlag(Boolean.TRUE);
        account.setReadySellTime(TimeUtil.now());

        this.sellDetect(account);
    }

    /**
     * 卖出监测
     *
     * @param account
     */
    public void sellDetect(MacdAccount account) {

        if (!account.getReadySellFlag()) {
            return;
        }

        BigDecimal lastBuyPrice = account.getLastBuyPrice();
        BigDecimal comparePrice = lastBuyPrice.multiply(new BigDecimal("1.0015")); // 算上手续费的成本价
        Symbol curHoldSymbol = account.getCurHoldSymbol();
        LocalDateTime readySellTime = account.getReadySellTime();

        // 卖出探测
        BigDecimal newPrice = priceFaceService.queryNewPrice(curHoldSymbol);

        if (newPrice.compareTo(comparePrice) >= 0) {
            this.sellAction(curHoldSymbol, account, "成本卖出成交", BigDecimal.ONE);
            return;
        }

        LocalDateTime now = TimeUtil.now();

        LocalDateTime urgentTime = readySellTime.plusHours(35);
        LocalDateTime outTime = readySellTime.plusHours(37);

        // 快超时了，寻找合理价格卖出
        if (now.isAfter(urgentTime) && now.isBefore(outTime)) {
            BigDecimal medianPrice = this.findHighPrice(curHoldSymbol);
            if (newPrice.compareTo(medianPrice) >= 0) {
                this.sellAction(curHoldSymbol, account, "临近超时卖出成交", BigDecimal.ONE);
            }
            return;
        }

        // 超时卖出
        if (now.isAfter(outTime)) {
            this.sellAction(curHoldSymbol, account, "超时卖出成交", BigDecimal.ONE);
            return;
        }

    }

    /**
     * 寻找最近2小时价格的  (最大值+最小值)/2
     *
     * @param symbol
     * @return
     */
    private BigDecimal findMedianPrice(Symbol symbol) {

        List<KLine> kLines;
        if(OPEN){
            if (BackTestConfig.MINUTE_ONE_K_LINE_OPEN) {
                kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_1, 60 * 2);
            } else {
                kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_5, 12 * 2);
            }
        }else{
            kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_1, 60 * 2);
        }


        BigDecimal maxPrice = kLines.get(0).getClosingPrice();
        BigDecimal minPrice = kLines.get(0).getClosingPrice();
        BigDecimal medianPrice;

        for (KLine kline : kLines) {
            if (kline.getClosingPrice().compareTo(maxPrice) > 0) {
                maxPrice = kline.getClosingPrice();
            }
            if (kline.getClosingPrice().compareTo(minPrice) < 0) {
                minPrice = kline.getClosingPrice();
            }
        }

        BigDecimal sumPrice = maxPrice.add(minPrice);

        medianPrice = sumPrice.divide(new BigDecimal(2), 8, RoundingMode.HALF_DOWN);

        return NumUtil.qtyPrecision(symbol, medianPrice);

    }

    /**
     * 取价格靠前50%的均值
     *
     * @param symbol
     * @return
     */
    private BigDecimal findHighPrice(Symbol symbol) {

        List<KLine> kLines;
        if (OPEN) {
            if (BackTestConfig.MINUTE_ONE_K_LINE_OPEN) {
                kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_1, 60 * 2);
            } else {
                kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_5, 12 * 2);
            }
        } else {
            kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_1, 60 * 2);
        }


        List<KLine> sortedList = kLines.stream()
            .sorted(Comparator.comparing(KLine::getClosingPrice).reversed())
            .collect(Collectors.toList());

        BigDecimal sumPrice = BigDecimal.ZERO;
        int size = sortedList.size() / 2;
        for (int i = 0; i < size; i++) {
            sumPrice = sumPrice.add(sortedList.get(i).getClosingPrice());
        }

        BigDecimal medianPrice = sumPrice.divide(new BigDecimal(size), 8, RoundingMode.HALF_DOWN);

        return NumUtil.qtyPrecision(symbol, medianPrice);

    }

    private Boolean isHighPriceBuy(Symbol symbol, BigDecimal lastBuyPrice) {

        List<KLine> kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_5, 1000);

        BigDecimal maxPrice = BigDecimal.ZERO;

        for (KLine kline : kLines) {
            if (kline.getClosingPrice().compareTo(maxPrice) > 0) {
                maxPrice = kline.getClosingPrice();
            }
        }

        BigDecimal comparePrice = maxPrice.multiply(new BigDecimal("0.997"));

        if (lastBuyPrice.compareTo(comparePrice) > 0) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }

    }

    /**
     * 卖出
     *
     * @param symbol
     * @param account
     * @param title
     */
    public void sellAction(Symbol symbol, MacdAccount account, String title, BigDecimal sellRate) {

        if (OPEN) {
            backTestOrderService.sellAction(symbol, account, title, sellRate);
            return;
        }

        // 卖单
        OrderLife sellOrder = orderService.marketSellOrder(OrderSide.SELL, symbol, account.getCurHoldQty());

        while (true) {

            OrderLife temp = orderService.queryByOrderId(symbol, sellOrder.getOrderId());

            if (OrderStatus.FILLED == temp.getStatus()) {

                account.setLastBuyPrice(null);
                account.setReadySellFlag(Boolean.FALSE);
                account.setReadySellTime(null);
                account.setCurHoldSymbol(null);
                account.setCurHoldQty(BigDecimal.ZERO);
                account.setCurHoldAmount(account.getCurHoldAmount().add(temp.getCumulativeQuoteQty()));
                TradePair curPair = account.getCurPair();
                curPair.setLossOrder(temp);

                // 重复Symbol就不发重复邮件了
                //if (!EMAIL_SELL_SET.contains(symbol)) {
                //    EMAIL_SELL_SET.add(symbol);
                //    mailService.macdSellMail(title, curPair, account);
                //}

                account.setCurPair(null);

                return;
            }

            // 已取消
            if (OrderStatus.CANCELED == temp.getStatus()
                || OrderStatus.EXPIRED == temp.getStatus()
                || OrderStatus.REJECTED == temp.getStatus()) {
                return;
            }

            // 查到买单未成交 or 部分成交，等待几秒，再查一下
            if (OrderStatus.PARTIALLY_FILLED == temp.getStatus()
                || OrderStatus.NEW == temp.getStatus()) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 获取最近1小时价格和成交量涨幅最高的3个币种
     *
     * @return
     */
    public List<Symbol> last1HourNatureRateMax() {

        List<MutablePair<Symbol, BigDecimal>> tempList = Lists.newArrayList();

        for (Symbol symbol : Symbol.values()) {

            if (OPEN && !symbol.getBackFlag()) {
                continue;
            }

            LocalDateTime endTime = TimeUtil.now();
            LocalDateTime startTime = endTime.minusHours(5);
            List<KLine> kLines = priceFaceService.queryKLineByTime(symbol, Interval.HOUR_1, 10, startTime, endTime);

            List<KLine> sortedList = kLines.stream()
                .sorted(Comparator.comparing(KLine::getOpeningTime).reversed())
                .collect(Collectors.toList());

            // 前1小时
            KLine last1Hour = sortedList.get(1);

            // 前前1小时
            KLine beforeLast1Hour = sortedList.get(2);

            // 价格涨幅
            BigDecimal priceRate = last1Hour.getClosingPrice()
                .subtract(beforeLast1Hour.getClosingPrice())
                .divide(beforeLast1Hour.getClosingPrice(), 4, RoundingMode.HALF_DOWN);

            if (priceRate.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 成交量涨幅
            BigDecimal volumeRate = last1Hour.getTradingVolume()
                .subtract(beforeLast1Hour.getTradingVolume())
                .divide(beforeLast1Hour.getTradingVolume(), 4, RoundingMode.HALF_DOWN);

            if (volumeRate.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 总涨幅
            BigDecimal totalRate = priceRate.add(volumeRate);

            // 总上涨幅度至少120%
            if (totalRate.compareTo(new BigDecimal("1.2")) > 0) {
                tempList.add(MutablePair.of(symbol, totalRate));
            }
        }

        // 数值大的排前面
        Collections.sort(tempList, (o1, o2) -> {
            if (o1.getRight().compareTo(o2.getRight()) < 0) {
                return 1;
            } else {
                return -1;
            }
        });

        List<Symbol> resultList = Lists.newArrayList();

        for (int i = 0; i < tempList.size(); i++) {

            // 只取前3
            if (i >= 3) {
                break;
            }

            MutablePair<Symbol, BigDecimal> pair = tempList.get(i);

            resultList.add(pair.getLeft());
        }

        return resultList;

    }

    public Boolean isLast1HourPriceDownMuch(Symbol symbol) {

        LocalDateTime endTime = TimeUtil.now();
        LocalDateTime startTime = endTime.minusHours(5);
        List<KLine> kLines = priceFaceService.queryKLineByTime(symbol, Interval.HOUR_1, 10, startTime, endTime);

        List<KLine> sortedList = kLines.stream()
            .sorted(Comparator.comparing(KLine::getOpeningTime).reversed())
            .collect(Collectors.toList());

        // 前1小时
        KLine last1Hour = sortedList.get(1);

        // 前前1小时
        KLine beforeLast1Hour = sortedList.get(2);

        // 价格涨幅
        BigDecimal priceRate = last1Hour.getClosingPrice()
            .subtract(beforeLast1Hour.getClosingPrice())
            .divide(beforeLast1Hour.getClosingPrice(), 4, RoundingMode.HALF_DOWN);

        if (priceRate.compareTo(new BigDecimal("0.081").negate()) <= 0) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }
}

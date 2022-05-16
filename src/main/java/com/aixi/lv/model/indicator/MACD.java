package com.aixi.lv.model.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import lombok.Getter;

/**
 * @author Js
 *
 * MACD (移动平均收敛散度)
 *
 * Default setting in crypto are period of 9, short 12 and long 26.
 * MACD = 12 EMA - 26 EMA and compare to 9 period of MACD value.
 */
public class MACD implements Indicator {

    @Getter
    private double currentDIF; // DIF

    @Getter
    private double currentDEM; // DEM

    private final EMA shortEMA; //Will be the EMA object for shortEMA-
    private final EMA longEMA; //Will be the EMA object for longEMA.
    private final int period; //Only value that has to be calculated in setInitial.
    private final double multiplier;
    private final int periodDifference;
    private String explanation;
    public static double SIGNAL_CHANGE = 0.25;

    private double lastTick;

    /**
     * 指标对应的 K线 开盘时间 （这根K线已经走完了）
     */
    @Getter
    private LocalDateTime kLineOpenTime;

    @Getter
    private Symbol symbol;

    @Getter
    private Interval interval;

    public MACD(List<Double> closingPrices, int shortPeriod, int longPeriod, int signalPeriod) {
        this.shortEMA = new EMA(closingPrices, shortPeriod,
            true); //true, because history is needed in MACD calculations.
        this.longEMA = new EMA(closingPrices, longPeriod, true); //true for the same reasons.
        this.period = signalPeriod;
        this.multiplier = 2.0 / (double)(signalPeriod + 1);
        this.periodDifference = longPeriod - shortPeriod;
        explanation = "";
        init(closingPrices); //initializing the calculations to get current MACD and signal line.
    }

    public MACD(List<Double> closingPrices, LocalDateTime kLineOpenTime, Symbol symbol, Interval interval,
        int shortPeriod, int longPeriod, int signalPeriod) {

        this.shortEMA = new EMA(closingPrices, shortPeriod,
            true); //true, because history is needed in MACD calculations.
        this.longEMA = new EMA(closingPrices, longPeriod, true); //true for the same reasons.
        this.period = signalPeriod;
        this.multiplier = 2.0 / (double)(signalPeriod + 1);
        this.periodDifference = longPeriod - shortPeriod;
        explanation = "";
        this.kLineOpenTime = kLineOpenTime;
        this.symbol = symbol;
        this.interval = interval;
        init(closingPrices); //initializing the calculations to get current MACD and signal line.
    }

    /**
     * 这才是MACD ,计算的是排除当前K线，上一根K线时期的MACD
     *
     * @return
     */
    @Override
    public double get() {
        return currentDIF - currentDEM;
    }

    public double getCurrentMACD() {
        return this.get();
    }

    @Override
    public double getTemp(double newPrice) {
        //temporary values
        double longTemp = longEMA.getTemp(newPrice);
        double shortTemp = shortEMA.getTemp(newPrice);

        double tempDIF = shortTemp - longTemp;
        double tempDEM = tempDIF * multiplier + currentDEM * (1 - multiplier);
        return tempDIF - tempDEM; //Getting the difference between the two signals.
    }

    @Override
    public void init(List<Double> closingPrices) {
        //Initial signal line
        //i = longEMA.getPeriod(); because the sizes of shortEMA and longEMA are different.
        for (int i = longEMA.getPeriod(); i < longEMA.getPeriod() + period; i++) {
            //i value with shortEMA gets changed to compensate the list size difference
            currentDIF = shortEMA.getEMAhistory().get(i + periodDifference) - longEMA.getEMAhistory().get(i);
            currentDEM += currentDIF;
        }
        currentDEM = currentDEM / (double)period;

        //Everything after the first calculation of signal line.
        for (int i = longEMA.getPeriod() + period; i < longEMA.getEMAhistory().size(); i++) {
            currentDIF = shortEMA.getEMAhistory().get(i + periodDifference) - longEMA.getEMAhistory().get(i);
            currentDEM = currentDIF * multiplier + currentDEM * (1 - multiplier);
        }

        lastTick = get();
    }

    @Override
    public void update(double newPrice) {
        //Updating the EMA values before updating MACD and Signal line.
        lastTick = get();
        shortEMA.update(newPrice);
        longEMA.update(newPrice);
        currentDIF = shortEMA.get() - longEMA.get();
        currentDEM = currentDIF * multiplier + currentDEM * (1 - multiplier);
    }

    /**
     * MACD 变化的斜率超过 0.25 并且 当前MACD<0 ，则买入
     *
     * @param newPrice
     * @return
     */
    @Override
    public int check(double newPrice) {
        // change 指当前价格输入后， MACD 指标变化的斜率
        double change = (getTemp(newPrice) - lastTick) / Math.abs(lastTick);
        if (change > MACD.SIGNAL_CHANGE && get() < 0) {
            NumberFormat PERCENT_FORMAT = new DecimalFormat("0.000%");
            explanation = "MACD histogram grew by " + PERCENT_FORMAT.format(change);
            return 1;
        }
        /*if (change < -MACD.change) {
            explanation = "MACD histogram fell by " + Formatter.formatPercent(change);
            return -1;
        }*/
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}

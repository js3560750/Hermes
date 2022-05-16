package com.aixi.lv.model.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import lombok.Getter;

/**
 * @author Js
 *
 * 相对强弱指数
 */
public class RSI implements Indicator {

    private double avgUp;
    private double avgDwn;
    private double prevClose;
    private final int period;
    private String explanation;

    /**
     * 严重超卖
     */
    public static final Integer OVER_SELL_MIN = 15;

    /**
     * 超卖
     */
    public static final Integer OVER_SELL_MAX = 30;

    /**
     * 超买
     */
    public static final Integer OVER_BUY_MIN = 70;

    /**
     * 严重超买
     */
    public static final Integer OVER_BUY_MAX = 78;

    /**
     * 指标对应的 K线 开盘时间 （这根K线已经走完了）
     */
    @Getter
    private LocalDateTime kLineOpenTime;

    @Getter
    private Symbol symbol;

    @Getter
    private Interval interval;

    public RSI(List<Double> closingPrice, int period) {
        avgUp = 0;
        avgDwn = 0;
        this.period = period;
        explanation = "";
        init(closingPrice);
    }

    public RSI(List<Double> closingPrice, LocalDateTime kLineOpenTime, Symbol symbol, Interval interval, int period) {
        avgUp = 0;
        avgDwn = 0;
        this.period = period;
        explanation = "";
        this.kLineOpenTime = kLineOpenTime;
        this.symbol = symbol;
        this.interval = interval;
        init(closingPrice);
    }

    @Override
    public void init(List<Double> closingPrices) {
        prevClose = closingPrices.get(0);
        for (int i = 1; i < period + 1; i++) {
            double change = closingPrices.get(i) - prevClose;
            if (change > 0) {
                avgUp += change;
            } else {
                avgDwn += Math.abs(change);
            }
        }

        //Initial SMA values
        avgUp = avgUp / (double)period;
        avgDwn = avgDwn / (double)period;

        //Dont use latest unclosed value
        for (int i = period + 1; i < closingPrices.size() - 1; i++) {
            update(closingPrices.get(i));
        }
    }

    /**
     * get 返回 RSI 具体值
     *
     * @return
     */
    @Override
    public double get() {
        double rsi = 100 - 100.0 / (1 + avgUp / avgDwn);
        return new BigDecimal(rsi).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    @Override
    public double getTemp(double newPrice) {
        double change = newPrice - prevClose;
        double tempUp;
        double tempDwn;
        if (change > 0) {
            tempUp = (avgUp * (period - 1) + change) / (double)period;
            tempDwn = (avgDwn * (period - 1)) / (double)period;
        } else {
            tempDwn = (avgDwn * (period - 1) + Math.abs(change)) / (double)period;
            tempUp = (avgUp * (period - 1)) / (double)period;
        }
        return 100 - 100.0 / (1 + tempUp / tempDwn);
    }

    @Override
    public void update(double newPrice) {
        double change = newPrice - prevClose;
        if (change > 0) {
            avgUp = (avgUp * (period - 1) + change) / (double)period;
            avgDwn = (avgDwn * (period - 1)) / (double)period;
        } else {
            avgUp = (avgUp * (period - 1)) / (double)period;
            avgDwn = (avgDwn * (period - 1) + Math.abs(change)) / (double)period;
        }
        prevClose = newPrice;
    }

    /**
     * 各指标数相加，>=2 就应该买入！！！   <= -2 就应该卖出 ！！！
     * @param newPrice
     * @return
     */
    @Override
    public int check(double newPrice) {
        double temp = getTemp(newPrice);
        if (temp < OVER_SELL_MIN) {
            return 2;
        }
        if (temp < OVER_SELL_MAX) {
            return 1;
        }
        if (temp > OVER_BUY_MAX) {
            return -2;
        }
        if (temp > OVER_BUY_MIN) {
            return -1;
        }
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}

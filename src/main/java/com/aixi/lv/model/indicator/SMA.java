package com.aixi.lv.model.indicator;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import lombok.Getter;

/**
 * @author Js
 *
 * 简单移动加权平均
 */
public class SMA implements Indicator {

    private double currentSum;
    private final int period;
    private final LinkedList<Double> prices;

    /**
     * 指标对应的 K线 开盘时间 （这根K线已经走完了）
     */
    @Getter
    private LocalDateTime kLineOpenTime;

    @Getter
    private Symbol symbol;

    @Getter
    private Interval interval;

    public SMA(List<Double> closingPrices, int period) {
        this.period = period;
        prices = new LinkedList<>();
        init(closingPrices);
    }

    public SMA(List<Double> closingPrices, LocalDateTime kLineOpenTime, Symbol symbol, Interval interval, int period) {
        this.period = period;
        this.kLineOpenTime = kLineOpenTime;
        this.symbol = symbol;
        this.interval = interval;
        prices = new LinkedList<>();
        init(closingPrices);
    }

    @Override
    public double get() {
        return currentSum / (double)period;
    }

    @Override
    public double getTemp(double newPrice) {
        return ((currentSum - prices.get(0) + newPrice) / (double)period);
    }

    @Override
    public void init(List<Double> closingPrices) {
        if (period > closingPrices.size()) {return;}

        //Initial sum
        for (int i = closingPrices.size() - period - 1; i < closingPrices.size() - 1; i++) {
            prices.add(closingPrices.get(i));
            currentSum += (closingPrices.get(i));
        }
    }

    @Override
    public void update(double newPrice) {
        currentSum -= prices.get(0);
        prices.removeFirst();
        prices.add(newPrice);
        currentSum += newPrice;
    }

    @Override
    public int check(double newPrice) {
        return 0;
    }

    @Override
    public String getExplanation() {
        return null;
    }

    public double standardDeviation() {
        double mean = currentSum / (double)period;
        double stdev = 0.0;
        for (double price : prices) {
            stdev += Math.pow(price - mean, 2);
        }
        return Math.sqrt(stdev / (double)period);
    }

    public double tempStandardDeviation(double newPrice) {

        double tempMean = (currentSum - prices.get(0) + newPrice) / (double)period;
        double tempStdev = 0.0;

        for (int i = 1; i < prices.size(); i++) {
            tempStdev += Math.pow(prices.get(i) - tempMean, 2);
        }

        tempStdev += Math.pow(newPrice - tempMean, 2);
        return Math.sqrt(tempStdev / (double)period);

    }
}

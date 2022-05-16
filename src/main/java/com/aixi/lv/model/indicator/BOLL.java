package com.aixi.lv.model.indicator;

import java.time.LocalDateTime;
import java.util.List;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import lombok.Getter;

/**
 * @author Js
 *
 * 双布林带
 */
public class BOLL implements Indicator {

    private double closingPrice;
    private double standardDeviation;
    private final int period;

    /**
     * 外层高线 （上带）
     */
    @Getter
    private double upperBand;

    /**
     * 内层高线 （上中带）
     */
    @Getter
    private double upperMidBand;

    /**
     * 中线（中带）
     */
    @Getter
    private double middleBand;

    /**
     * 内层低线（下中带）
     */
    @Getter
    private double lowerMidBand;

    /**
     * 外层低线（下带）
     */
    @Getter
    private double lowerBand;

    private String explanation;

    private SMA sma;

    /**
     * 指标对应的 K线 开盘时间 （这根K线已经走完了）
     */
    @Getter
    private LocalDateTime kLineOpenTime;

    @Getter
    private Symbol symbol;

    @Getter
    private Interval interval;

    public BOLL(List<Double> closingPrices, int period) {
        this.period = period;
        this.sma = new SMA(closingPrices, period);
        init(closingPrices);
    }

    public BOLL(List<Double> closingPrices, LocalDateTime kLineOpenTime, Symbol symbol, Interval interval, int period) {
        this.period = period;
        this.kLineOpenTime = kLineOpenTime;
        this.symbol = symbol;
        this.interval = interval;
        this.sma = new SMA(closingPrices, period);
        init(closingPrices);
    }

    @Override
    public double get() {
        if ((upperBand - lowerBand) / middleBand < 0.005) {
            return 0;
        }
        if (upperMidBand < closingPrice && closingPrice <= upperBand) {
            return 1;
        }
        if (lowerBand < closingPrice && closingPrice <= lowerMidBand) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public double getTemp(double newPrice) {
        double tempMidBand = sma.getTemp(newPrice);
        double tempStdev = sma.tempStandardDeviation(newPrice);
        double tempUpperBand = tempMidBand + tempStdev * 2;
        double tempUpperMidBand = tempMidBand + tempStdev;
        double tempLowerMidBand = tempMidBand - tempStdev;
        double tempLowerBand = tempMidBand - tempStdev * 2;
        if ((tempUpperBand - tempLowerBand) / tempMidBand < 0.005) //Low volatility case
        {return 0;}
        if (tempUpperMidBand < newPrice && newPrice <= tempUpperBand) {return 1;}
        if (tempLowerBand < newPrice && newPrice <= tempLowerMidBand) {return -1;} else {return 0;}
    }

    @Override
    public void init(List<Double> closingPrices) {
        if (period > closingPrices.size()) {
            return;
        }

        closingPrice = closingPrices.get(closingPrices.size() - 2);
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation * 2;
        upperMidBand = middleBand + standardDeviation;
        lowerMidBand = middleBand - standardDeviation;
        lowerBand = middleBand - standardDeviation * 2;

    }

    @Override
    public void update(double newPrice) {
        closingPrice = newPrice;
        sma.update(newPrice);
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation * 2;
        upperMidBand = middleBand + standardDeviation;
        lowerMidBand = middleBand - standardDeviation;
        lowerBand = middleBand - standardDeviation * 2;
    }

    @Override
    public int check(double newPrice) {
        if (getTemp(newPrice) == 1) {
            explanation = "Price in DBB buy zone";
            return 1;
        }
        if (getTemp(newPrice) == -1) {
            explanation = "Price in DBB sell zone";
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

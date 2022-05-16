package com.aixi.lv.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.aixi.lv.config.ExchangeInfoConfig;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.ExchangeInfoAmountFilter;
import com.aixi.lv.model.domain.ExchangeInfoPriceFilter;
import com.aixi.lv.model.domain.ExchangeInfoQtyFilter;

/**
 * @author Js

 */
public class NumUtil {

    /**
     * 价格精度过滤
     *
     * @param symbol
     * @param price
     * @return
     */
    public static BigDecimal pricePrecision(Symbol symbol, BigDecimal price) {

        ExchangeInfoPriceFilter priceFilter = ExchangeInfoConfig.PRICE_FILTER_MAP.get(symbol);

        if (priceFilter == null) {
            throw new RuntimeException(" 价格过滤器未找到 | symbol=" + symbol);
        }

        return price.setScale(priceFilter.getPriceScale(), RoundingMode.HALF_UP);
    }

    /**
     * 数量精度过滤
     *
     * @param symbol
     * @param quantity
     * @return
     */
    public static BigDecimal qtyPrecision(Symbol symbol, BigDecimal quantity) {

        ExchangeInfoQtyFilter qtyFilter = ExchangeInfoConfig.QTY_FILTER_MAP.get(symbol);

        if (qtyFilter == null) {
            throw new RuntimeException(" 数量过滤器未找到 | symbol=" + symbol);
        }

        return quantity.setScale(qtyFilter.getQtyScale(), RoundingMode.DOWN);
    }

    /**
     * 去掉多余的零
     *
     * @param num
     * @return
     */
    public static String cutZero(BigDecimal num) {
        return num.stripTrailingZeros().toPlainString();
    }

    /**
     * 最小订单金额检查
     *
     * @param symbol
     * @param quantity
     * @param price
     * @return
     */
    public static Boolean isErrorAmount(Symbol symbol, BigDecimal quantity, BigDecimal price) {

        ExchangeInfoAmountFilter amountFilter = ExchangeInfoConfig.AMOUNT_FILTER_MAP.get(symbol);

        if (amountFilter == null) {
            throw new RuntimeException(" 金额过滤器未找到 | symbol=" + symbol);
        }

        BigDecimal orderAmount = quantity.multiply(price);

        BigDecimal minAmount = amountFilter.getMinNotional();

        if (orderAmount.compareTo(minAmount) > 0) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;

        }
    }

    /**
     * 获取两者之间更小的
     *
     * @param numA
     * @param numB
     * @return
     */
    public static BigDecimal getSmallerPrice(BigDecimal numA, BigDecimal numB) {

        if (numA.compareTo(numB) <= 0) {
            return numA;
        } else {
            return numB;
        }
    }

    /**
     * 获取两者之间更大的
     *
     * @param numA
     * @param numB
     * @return
     */
    public static BigDecimal getBiggerPrice(BigDecimal numA, BigDecimal numB) {

        if (numA.compareTo(numB) >= 0) {
            return numA;
        } else {
            return numB;
        }
    }

    /**
     * 百分比显示
     *
     * @param num
     * @return
     */
    public static String percent(Double num) {

        DecimalFormat df = new DecimalFormat("0.00%");
        String str = df.format(num);

        return str;
    }

    /**
     * 最多只展示3位小数
     *
     * @param num
     * @return
     */
    public static String showDouble(Double num) {

        BigDecimal bigDecimal = new BigDecimal(num);
        return bigDecimal.setScale(3, RoundingMode.HALF_DOWN).stripTrailingZeros().toPlainString();
    }

}

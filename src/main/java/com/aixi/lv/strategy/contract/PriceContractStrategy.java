package com.aixi.lv.strategy.contract;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.ContractSide;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.ContractAccount;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.service.BackTestOrderService;
import com.aixi.lv.service.PriceFaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 合约价格策略
 */
@Component
@Slf4j
public class PriceContractStrategy {

    @Resource
    PriceFaceService priceFaceService;

    @Resource
    BackTestOrderService backTestOrderService;

    public static final Double LONG_PROFIT = 1.03;

    public static final Double LONG_LOSS = 0.9;

    public static final Double SHORT_PROFIT = 0.98;

    public static final Double SHORT_LOSS = 1.1;

    /**
     * 开仓探测
     *
     * @param accounts
     */
    public void buyDetect(List<ContractAccount> accounts) {

        for (ContractAccount account : accounts) {

            if (account.getHoldFlag()) {
                continue;
            }

            Symbol symbol = account.getSymbol();

            if (isPriceRise(symbol)) {
                // 下多单
                this.longOrder(account);
                continue;
            }

            if (isPriceFall(symbol)) {
                // 下空单
                this.shortOrder(account);
                continue;
            }

        }

    }

    /**
     * 平仓探测
     *
     * @param accounts
     */
    public void sellDetect(List<ContractAccount> accounts) {

        for (ContractAccount account : accounts) {

            if (!account.getHoldFlag()) {
                continue;
            }

            Symbol symbol = account.getSymbol();
            ContractSide contractSide = account.getContractSide();
            BigDecimal profitPrice = account.getProfitPrice();
            BigDecimal lossPrice = account.getLossPrice();
            BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

            if (ContractSide.LONG == contractSide) {
                if (newPrice.compareTo(profitPrice) > 0) {
                    backTestOrderService.sellContract(account);
                }
                if (newPrice.compareTo(lossPrice) < 0) {
                    backTestOrderService.sellContract(account);
                }
            }

            if (ContractSide.SHORT == contractSide) {
                if (newPrice.compareTo(profitPrice) < 0) {
                    backTestOrderService.sellContract(account);
                }
                if (newPrice.compareTo(lossPrice) > 0) {
                    backTestOrderService.sellContract(account);
                }
            }

        }
    }

    /**
     * 做多
     *
     * @param account
     */
    private void longOrder(ContractAccount account) {

        backTestOrderService.buyContract(account, ContractSide.LONG);
    }

    /**
     * 做空
     *
     * @param account
     */
    private void shortOrder(ContractAccount account) {

        //Symbol symbol = account.getSymbol();
        //if (Symbol.GMTUSDT == symbol
        //    || Symbol.APEUSDT == symbol) {
        //    return;
        //}

        backTestOrderService.buyContract(account, ContractSide.SHORT);
    }

    /**
     * 价格是否快速上涨
     *
     * 15分钟内涨幅大于3%
     *
     * @param symbol
     * @return
     */
    private Boolean isPriceRise(Symbol symbol) {

        List<KLine> kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_1, 15);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        BigDecimal beforePrice = kLines.get(0).getClosingPrice();

        //for (KLine kLine : kLines) {
        //
        //    BigDecimal closingPrice = kLine.getClosingPrice();
        //
        //    if (newPrice.divide(closingPrice, 8, RoundingMode.DOWN).doubleValue() > 1.03) {
        //        return true;
        //    }
        //}

        if (newPrice.divide(beforePrice, 8, RoundingMode.DOWN).doubleValue() > 1.029) {
            return true;
        }

        return false;

    }

    /**
     * 价格是否快速下跌
     *
     * 15分钟内跌幅大于3%
     *
     * @param symbol
     * @return
     */
    private Boolean isPriceFall(Symbol symbol) {

        List<KLine> kLines = priceFaceService.queryKLine(symbol, Interval.MINUTE_1, 15);

        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        BigDecimal beforePrice = kLines.get(0).getClosingPrice();

        //for (KLine kLine : kLines) {
        //
        //    BigDecimal closingPrice = kLine.getClosingPrice();
        //
        //    if (newPrice.divide(closingPrice, 8, RoundingMode.UP).doubleValue() < 0.96) {
        //        return true;
        //    }
        //}

        if (newPrice.divide(beforePrice, 8, RoundingMode.UP).doubleValue() < 0.97) {
            return true;
        }

        return false;
    }
}

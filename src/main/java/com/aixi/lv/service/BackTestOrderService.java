package com.aixi.lv.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.ContractSide;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.ContractAccount;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.util.NumUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;
import static com.aixi.lv.strategy.contract.PriceContractStrategy.LONG_LOSS;
import static com.aixi.lv.strategy.contract.PriceContractStrategy.LONG_PROFIT;
import static com.aixi.lv.strategy.contract.PriceContractStrategy.SHORT_LOSS;
import static com.aixi.lv.strategy.contract.PriceContractStrategy.SHORT_PROFIT;

/**
 * @author Js
 */
@Component
@Slf4j
public class BackTestOrderService {

    @Resource
    PriceFaceService priceFaceService;

    /**
     * 回测合约买入
     *
     * @param account
     * @param contractSide
     */
    public void buyContract(ContractAccount account, ContractSide contractSide) {

        Symbol symbol = account.getSymbol();

        // 市场价
        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        // 准备花费的USDT
        BigDecimal buyAmount = account.getHoldAmount().setScale(4, RoundingMode.DOWN);

        // 准备购买的币种数量
        BigDecimal buyQty = buyAmount.divide(newPrice, 8, RoundingMode.HALF_DOWN);


        account.setHoldFlag(true);
        account.setContractSide(contractSide);
        account.setBuyPrice(newPrice);
        account.setHoldQty(buyQty);

        if (ContractSide.LONG == contractSide) {
            account.setProfitPrice(NumUtil.pricePrecision(symbol, newPrice.multiply(new BigDecimal(LONG_PROFIT))));
            account.setLossPrice(NumUtil.pricePrecision(symbol, newPrice.multiply(new BigDecimal(LONG_LOSS))));
        }

        if (ContractSide.SHORT == contractSide) {
            account.setProfitPrice(NumUtil.pricePrecision(symbol, newPrice.multiply(new BigDecimal(SHORT_PROFIT))));
            account.setLossPrice(NumUtil.pricePrecision(symbol, newPrice.multiply(new BigDecimal(SHORT_LOSS))));
        }


        log.info(" 回测交易 | 时间 {} | 账户 {} | 买入 {} | 方向 {} | 当前账户总金额 {}",
            account.getCurBackTestComputeTime(),
            account.getName(),
            symbol,
            contractSide,
            account.getHoldAmount());
    }

    /**
     * 回测合约卖出
     *
     * @param account
     */
    public void sellContract(ContractAccount account) {

        BigDecimal newPrice = priceFaceService.queryNewPrice(account.getSymbol());
        ContractSide contractSide = account.getContractSide();
        BigDecimal buyPrice = account.getBuyPrice();
        BigDecimal holdQty = account.getHoldQty();
        BigDecimal diffAmount = newPrice.subtract(buyPrice).multiply(holdQty).abs();
        String flag = "";


        if (ContractSide.LONG == contractSide) {

            if (newPrice.compareTo(buyPrice) > 0) {
                // 做多盈利
                account.setHoldAmount(account.getHoldAmount().add(diffAmount));
                account.setBackTestProfitTimes(account.getBackTestProfitTimes() + 1);
                flag = "盈利";
            } else {
                // 做多亏损
                account.setHoldAmount(account.getHoldAmount().subtract(diffAmount));
                account.setBackTestLossTimes(account.getBackTestLossTimes() + 1);
                flag = "亏损";
            }

        }

        if (ContractSide.SHORT == contractSide) {

            if (newPrice.compareTo(buyPrice) > 0) {
                // 做空亏损
                account.setHoldAmount(account.getHoldAmount().subtract(diffAmount));
                account.setBackTestLossTimes(account.getBackTestLossTimes() + 1);
                flag = "亏损";
            } else {
                // 做空盈利
                account.setHoldAmount(account.getHoldAmount().add(diffAmount));
                account.setBackTestProfitTimes(account.getBackTestProfitTimes() + 1);
                flag = "盈利";
            }

        }

        account.setHoldFlag(false);
        account.setContractSide(null);
        account.setBuyPrice(null);
        account.setProfitPrice(null);
        account.setLossPrice(null);
        account.setHoldQty(BigDecimal.ZERO);


        log.info(" 回测交易 | 时间 {} | 账户 {} | 卖出 {} | 方向 {} | 当前账户总金额 {} | 本单 {} {}",
            account.getCurBackTestComputeTime(),
            account.getName(),
            account.getSymbol(),
            contractSide,
            account.getHoldAmount(),
            flag,
            diffAmount);
    }

    /**
     * 回测购买
     *
     * @param symbol
     * @param account
     */
    public void buyAction(Symbol symbol, MacdAccount account) {

        // 市场价
        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        // 准备花费的USDT
        BigDecimal buyAmount = account.getCurHoldAmount().setScale(2, RoundingMode.DOWN);

        // 准备购买的币种数量
        BigDecimal buyQty = buyAmount.divide(newPrice, 8, RoundingMode.HALF_DOWN);

        // 手续费
        BigDecimal chargeQty = buyQty.multiply(new BigDecimal("0.00075"));

        // 实际购买的币种数量（已扣除手续费）
        BigDecimal realBuyQty = NumUtil.qtyPrecision(symbol, buyQty.subtract(chargeQty));

        // 实际花费的USDT（已包含手续费）
        BigDecimal costAmount = buyQty.multiply(newPrice);

        account.setCurHoldSymbol(symbol);
        account.setLastBuyPrice(newPrice);
        account.setCurHoldQty(realBuyQty);
        account.setCurHoldAmount(account.getCurHoldAmount().subtract(costAmount));

        BigDecimal totalAmount = account.getCurHoldAmount().add(account.getCurHoldQty().multiply(newPrice));
        log.info(" 回测交易 | 时间 {} | 账户 {} | 买入 {} | 当前账户总金额 {}",
            THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(), account.getName(), symbol, totalAmount);
    }

    /**
     * 回测卖出
     *
     * @param symbol
     * @param account
     * @param title
     */
    public void sellAction(Symbol symbol, MacdAccount account, String title, BigDecimal sellRate) {

        // 市场价
        BigDecimal newPrice = priceFaceService.queryNewPrice(symbol);

        BigDecimal sellQty;
        if (BigDecimal.ONE.equals(sellRate)) {
            // 全卖
            sellQty = account.getCurHoldQty();
        } else {
            // 按比例卖
            sellQty = NumUtil.qtyPrecision(symbol, account.getCurHoldQty().multiply(sellRate));
        }

        BigDecimal sellAmount = newPrice.multiply(sellQty);

        // 手续费
        BigDecimal chargeAmount = sellAmount.multiply(new BigDecimal("0.00075"));

        // 实际卖的金额（已扣除手续费）
        BigDecimal realSellAmount = sellAmount.subtract(chargeAmount);

        // 当初购买花费的金额
        BigDecimal buyAmount = account.getLastBuyPrice().multiply(sellQty);

        if (BigDecimal.ONE.equals(sellRate)) {
            // 全卖
            account.setCurHoldSymbol(null);
            account.setCurHoldQty(BigDecimal.ZERO);
            account.setLastBuyPrice(null);
            account.setReadySellFlag(Boolean.FALSE);
            account.setReadySellTime(null);
        } else {
            // 按比例卖
            account.setCurHoldQty(account.getCurHoldQty().subtract(sellQty));
        }

        // 已扣除手续费后账户增加的金额
        account.setCurHoldAmount(account.getCurHoldAmount().add(realSellAmount));

        if (realSellAmount.compareTo(buyAmount) >= 0) {

            BigDecimal profitAmount = realSellAmount.subtract(buyAmount);
            account.setBackTestTotalProfit(account.getBackTestTotalProfit().add(profitAmount));

            account.setBackTestProfitTimes(
                account.getBackTestProfitTimes() != null ? account.getBackTestProfitTimes() + 1 : 1);

            log.info(" 回测交易 | 时间 {} | 账户 {} | {} {} | 当前账户总金额 {} | 本单交易 盈利 {}",
                THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(), account.getName(), title, symbol,
                account.getCurHoldAmount(), profitAmount);

        } else {

            BigDecimal lossAmount = buyAmount.subtract(realSellAmount);
            account.setBackTestTotalLoss(account.getBackTestTotalLoss().add(lossAmount));

            account.setBackTestLossTimes(
                account.getBackTestLossTimes() != null ? account.getBackTestLossTimes() + 1 : 1);

            log.info(" 回测交易 | 时间 {} | 账户 {} | {} {} | 当前账户总金额 {} | 本单交易 亏损 {}",
                THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime(), account.getName(), title, symbol,
                account.getCurHoldAmount(), lossAmount);
        }
    }
}

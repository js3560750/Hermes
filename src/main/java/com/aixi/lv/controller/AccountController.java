package com.aixi.lv.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.NumUtil;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.aixi.lv.config.ApiKeyConfig.WEB_PASSWORD;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.MACD_ACCOUNT_MAP;

/**
 * @author Js
 * @date 2022/1/11 9:06 下午
 */
@RestController
@RequestMapping("/account")
@Api(tags = "账户服务")
@Slf4j
public class AccountController {

    @Resource
    PriceService priceService;

    /**
     * @return
     */
    @GetMapping("/belong_account_info")
    @ApiOperation("获取归属账户信息")
    public Result belongAccountInfo(String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        List<JSONObject> resultList = Lists.newArrayList();

        Map<String, BigDecimal> belongMap = new TreeMap<>();

        Map<Symbol, BigDecimal> symbolMap = new TreeMap<>();

        for (MacdAccount ac : MACD_ACCOUNT_MAP.values()) {

            BigDecimal totalAmount;
            Symbol curHoldSymbol = ac.getCurHoldSymbol();
            if (curHoldSymbol != null) {
                BigDecimal newPrice = priceService.queryNewPrice(curHoldSymbol);
                // 账户余额 + 持有货币价值
                totalAmount = ac.getCurHoldAmount()
                    .add(ac.getCurHoldQty().multiply(newPrice))
                    .setScale(2, RoundingMode.HALF_DOWN);

                // 币种金额汇总
                if (symbolMap.containsKey(curHoldSymbol)) {
                    symbolMap.put(curHoldSymbol, symbolMap.get(curHoldSymbol).add(totalAmount));
                } else {
                    symbolMap.put(curHoldSymbol, totalAmount);
                }

            } else {
                totalAmount = ac.getCurHoldAmount();
            }

            // 统计归属账户余额
            if (StringUtils.isNotEmpty(ac.getBelongAccount())) {

                String belongAccount = ac.getBelongAccount();

                if (belongMap.containsKey(belongAccount)) {
                    belongMap.put(belongAccount, belongMap.get(belongAccount).add(totalAmount));
                } else {
                    belongMap.put(belongAccount, totalAmount);
                }
            }
        }

        for (Entry<String, BigDecimal> entry : belongMap.entrySet()) {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("归属账户名称", entry.getKey());
            jsonObject.put("账户总额", entry.getValue().stripTrailingZeros().toPlainString());
            resultList.add(jsonObject);
        }

        for (Entry<Symbol, BigDecimal> entry : symbolMap.entrySet()) {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("持有币种名称", entry.getKey());
            jsonObject.put("持有币种总额", entry.getValue().stripTrailingZeros().toPlainString());
            resultList.add(jsonObject);
        }

        return Result.success(resultList);
    }

    /**
     * @return
     */
    @GetMapping("/sub_account_info")
    @ApiOperation("获取子账户信息")
    public Result subAccountInfo(String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        List<JSONObject> resultList = Lists.newArrayList();

        List<MacdAccount> accountList = Lists.newArrayList(MACD_ACCOUNT_MAP.values())
            .stream()
            .sorted(Comparator.comparing(MacdAccount::getName))
            .collect(Collectors.toList());

        for (MacdAccount account : accountList) {

            BigDecimal totalAmount;
            if (account.getCurHoldSymbol() != null) {
                BigDecimal newPrice = priceService.queryNewPrice(account.getCurHoldSymbol());
                // 账户余额 + 持有货币价值
                totalAmount = account.getCurHoldAmount()
                    .add(account.getCurHoldQty().multiply(newPrice))
                    .setScale(2, RoundingMode.HALF_DOWN);
            } else {
                totalAmount = account.getCurHoldAmount();
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("账户名称", account.getName());
            jsonObject.put("账户总额", totalAmount);
            jsonObject.put("当前交易对", account.getSymbolList());
            jsonObject.put("当前持仓", account.getCurHoldSymbol());
            jsonObject.put("卖出标记", account.getReadySellFlag());
            resultList.add(jsonObject);
        }

        return Result.success(resultList);
    }

    @GetMapping("/symbol/add")
    @ApiOperation("添加交易对")
    public Result addSymbol(String accountName, String symbolCode, String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        MacdAccount account = MACD_ACCOUNT_MAP.get(accountName);

        if (account == null) {
            return Result.fail("账户名错误");
        }

        List<Symbol> symbolList = account.getSymbolList();
        symbolList.add(Symbol.getByCode(symbolCode));

        List<Symbol> collect = symbolList.stream().distinct().collect(Collectors.toList());

        account.setSymbolList(collect);

        return Result.success(
            "添加symbol成功，当前账户" + account.getName() + " symbolList = " + JSON.toJSONString(collect));

    }

    @GetMapping("/symbol/remove")
    @ApiOperation("移除交易对")
    public Result removeSymbol(String accountName, String symbolCode, String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        MacdAccount account = MACD_ACCOUNT_MAP.get(accountName);

        if (account == null) {
            return Result.fail("账户名错误");
        }

        List<Symbol> symbolList = account.getSymbolList();
        symbolList.remove(Symbol.getByCode(symbolCode));

        List<Symbol> collect = symbolList.stream().distinct().collect(Collectors.toList());

        account.setSymbolList(collect);

        return Result.success(
            "移除symbol成功，当前账户" + account.getName() + " symbolList = " + JSON.toJSONString(collect));

    }

    @GetMapping("/account/add")
    @ApiOperation("添加账户")
    public Result addAccount(String accountName, List<String> symbolList, Integer amount, String belongAccount,
        String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        List<Symbol> symbols = Lists.newArrayList();

        for (String symbolStr : symbolList) {
            symbols.add(Symbol.getByCode(symbolStr));
        }

        List<Symbol> collect = symbols.stream().distinct().collect(Collectors.toList());

        MacdAccount account = new MacdAccount();
        account.setName(accountName);
        account.setSymbolList(collect);
        account.setCurHoldQty(BigDecimal.ZERO);
        account.setCurHoldAmount(new BigDecimal(amount));

        // 归属账户，多币种组合时，方便统计
        if (StringUtils.isNotEmpty(belongAccount)) {
            account.setBelongAccount(belongAccount);
        }

        MACD_ACCOUNT_MAP.put(account.getName(), account);

        return Result.success(
            "添加账户成功，当前账户" + account.getName() + " symbolList = " + JSON.toJSONString(collect) + " 账户余额 = "
                + amount);
    }

    @GetMapping("/account/remove")
    @ApiOperation("移除账户")
    public Result removeAccount(String accountName, String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        if (!MACD_ACCOUNT_MAP.containsKey(accountName)) {
            return Result.fail("不存在的账户名，账户 = " + accountName);
        }

        MACD_ACCOUNT_MAP.remove(accountName);

        return Result.success("移除账户成功，账户 = " + accountName);
    }

    @GetMapping("/account/reset")
    @ApiOperation("重置账户")
    public Result resetAccount(String resetSymbol, Integer amount, String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        if (amount == null || amount <= 0) {
            return Result.success("amount必须>0");
        }

        Symbol symbol = Symbol.getByCode(resetSymbol);

        for (MacdAccount account : MACD_ACCOUNT_MAP.values()) {

            if (account.getCurHoldSymbol() != null && account.getCurHoldSymbol() == symbol) {

                BigDecimal newPrice = priceService.queryNewPrice(symbol);
                BigDecimal readyQty = new BigDecimal(amount).divide(newPrice, 8, RoundingMode.HALF_DOWN);
                BigDecimal realQty = NumUtil.qtyPrecision(symbol, readyQty);

                account.setCurHoldQty(realQty);
                account.setCurHoldAmount(BigDecimal.ZERO);

            }
        }

        return Result.success("重置账户成功");
    }

    @GetMapping("/account/reset_zero")
    @ApiOperation("重置账户到零")
    public Result resetAccountToZero(String resetSymbol, String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        Symbol symbol = Symbol.getByCode(resetSymbol);

        for (MacdAccount account : MACD_ACCOUNT_MAP.values()) {

            if (account.getCurHoldSymbol() != null && account.getCurHoldSymbol() == symbol) {

                account.setCurHoldSymbol(null);
                account.setLastBuyPrice(BigDecimal.ZERO);
                account.setCurHoldQty(BigDecimal.ZERO);
                account.setCurHoldAmount(BigDecimal.ZERO);
                account.setCurPair(null);
                account.setLastSellTime(null);
                account.setLastSellSymbol(null);

            }
        }

        return Result.success("重置账户到零成功");
    }
}

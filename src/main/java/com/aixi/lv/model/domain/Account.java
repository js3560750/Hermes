package com.aixi.lv.model.domain;

import java.util.List;
import java.util.Optional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.CurrencyType;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 *
 * 账户
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    /**
     * 货币资产
     */
    private List<Asset> assetList;

    /**
     * SPOT 现货账户
     * MARGIN 杠杆账户
     */
    private String accountType;

    /**
     * 是否可以交易
     */
    private Boolean canTrade;

    public static Account parseObject(JSONObject jo) {

        if (jo == null) {
            return null;
        }

        Account account = new Account();

        Boolean canTrade = jo.getBoolean("canTrade");
        account.setCanTrade(canTrade);

        String accountType = jo.getString("accountType");
        account.setAccountType(accountType);

        JSONArray jsonArray = jo.getJSONArray("balances");

        List<JSONObject> balances = jsonArray.toJavaList(JSONObject.class);

        List<Asset> assetList = Lists.newArrayList();

        for (JSONObject balance : balances) {
            String asset = balance.getString("asset");
            Optional<CurrencyType> optional = CurrencyType.getByCode(asset);
            if (optional.isPresent()) {
                Asset currencyAsset = new Asset();
                currencyAsset.setCurrencyType(optional.get());
                currencyAsset.setFreeQty(balance.getBigDecimal("free"));
                currencyAsset.setLockedQty(balance.getBigDecimal("locked"));
                assetList.add(currencyAsset);
            }
        }

        account.setAssetList(assetList);

        return account;
    }
}

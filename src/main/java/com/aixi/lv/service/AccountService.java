package com.aixi.lv.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.CurrencyType;
import com.aixi.lv.model.domain.Account;
import com.aixi.lv.model.domain.Asset;
import com.aixi.lv.util.ApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Js
 */
@Component
@Slf4j
public class AccountService {

    @Resource
    EncryptHttpService encryptHttpService;

    /**
     * 获取账户信息
     *
     * @return
     */
    public Account queryAccountInfo() {

        try {

            String url = ApiUtil.url("/api/v3/account");

            JSONObject params = new JSONObject();
            long timeStamp = System.currentTimeMillis();
            params.put("timestamp", timeStamp);

            JSONObject object = encryptHttpService.getObject(url, params);

            Account account = Account.parseObject(object);

            return account;

        } catch (Exception e) {
            log.error(String.format(" AccountService | queryAccountInfo_fail "), e);
            throw e;
        }
    }

    /**
     * 查询BNB余额
     *
     * @return
     */
    public BigDecimal queryBNBFreeQty() {

        try {

            Account account = this.queryAccountInfo();

            List<Asset> assetList = account.getAssetList();

            for (Asset asset : assetList) {
                if (CurrencyType.BNB == asset.getCurrencyType()) {
                    return asset.getFreeQty();
                }
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error(String.format(" AccountService | queryBNBFreeQty_fail "), e);
            throw e;
        }
    }

}

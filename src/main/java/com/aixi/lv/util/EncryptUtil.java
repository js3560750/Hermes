package com.aixi.lv.util;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.config.ApiKeyConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Js

 */
@Slf4j
public class EncryptUtil {

    /**
     * @param data      要加密的数据，字符串
     * @return
     * @throws Exception
     */
    public static String getSignature(String data) {

        // 密钥
        String secretKey = ApiKeyConfig.SECRET_KEY;

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] array = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
            }

            return sb.toString().toUpperCase();
        } catch (Exception e) {
            log.error(" hmacSHA256 | exception | data=" + JSON.toJSONString(data), e);
            throw new RuntimeException(e);
        }

    }
}

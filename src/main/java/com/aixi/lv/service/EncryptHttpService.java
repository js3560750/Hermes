package com.aixi.lv.service;

import java.net.URI;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.config.ApiKeyConfig;
import com.aixi.lv.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Js
 *
 * 带加密的HTTP服务
 */
@Component
@Slf4j
public class EncryptHttpService {

    @Resource
    RestTemplate restTemplate;

    public JSONObject getObject(String url, JSONObject body) {

        try {

            String completeUrl = this.buildSignatureUrl(url, body);

            URI uri = UriComponentsBuilder.fromUriString(completeUrl).build().toUri();

            RequestEntity<Void> request = RequestEntity.get(uri)
                .header("X-MBX-APIKEY", ApiKeyConfig.API_KEY)
                .accept(MediaType.APPLICATION_JSON)
                .build();

            ResponseEntity<JSONObject> response = restTemplate.exchange(request, JSONObject.class);

            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error(" getObject | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                    JSON.toJSONString(response));
                throw new RuntimeException(" getObject | HttpStatus_error ");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error(String.format(" EncryptHttpService | getObject | url = %s | JSONObject = %s",
                url, JSON.toJSONString(body)), e);
            throw e;
        }
    }

    public JSONArray getArray(String url, JSONObject body) {

        try {

            String completeUrl = this.buildSignatureUrl(url, body);

            URI uri = UriComponentsBuilder.fromUriString(completeUrl).build().toUri();

            RequestEntity<Void> request = RequestEntity.get(uri)
                .header("X-MBX-APIKEY", ApiKeyConfig.API_KEY)
                .accept(MediaType.APPLICATION_JSON)
                .build();

            ResponseEntity<JSONArray> response = restTemplate.exchange(request, JSONArray.class);

            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error(" getArray | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                    JSON.toJSONString(response));
                throw new RuntimeException(" getArray | HttpStatus_error ");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error(String.format(" EncryptHttpService | getArray | url = %s | JSONObject = %s",
                url, JSON.toJSONString(body)), e);
            throw e;
        }
    }

    /**
     * POST 请求
     *
     * @param url
     * @param body
     * @return
     */
    public JSONObject postObject(String url, JSONObject body) {

        try {

            String completeUrl = this.buildSignatureUrl(url, body);

            URI uri = UriComponentsBuilder.fromUriString(completeUrl).build().toUri();

            RequestEntity<Void> request = RequestEntity.post(uri)
                .header("X-MBX-APIKEY", ApiKeyConfig.API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .build();

            ResponseEntity<JSONObject> response = restTemplate.exchange(request, JSONObject.class);

            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error(" postRequest | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                    JSON.toJSONString(response));
                throw new RuntimeException(" postRequest | HttpStatus_error ");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error(String.format(" EncryptHttpService | postObject | url = %s | JSONObject = %s",
                url, JSON.toJSONString(body)), e);
            throw e;
        }
    }

    /**
     * DELETE 请求
     *
     * @param url
     * @param body
     */
    public JSONObject deleteObject(String url, JSONObject body) {

        try {

            String completeUrl = this.buildSignatureUrl(url, body);

            URI uri = UriComponentsBuilder.fromUriString(completeUrl).build().toUri();

            RequestEntity<Void> request = RequestEntity.delete(uri)
                .header("X-MBX-APIKEY", ApiKeyConfig.API_KEY)
                .accept(MediaType.APPLICATION_JSON)
                .build();

            ResponseEntity<JSONObject> response = restTemplate.exchange(request, JSONObject.class);

            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error(" deleteRequest | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                    JSON.toJSONString(response));
                throw new RuntimeException(" deleteRequest | HttpStatus_error ");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error(String.format(" EncryptHttpService | deleteObject | url = %s | JSONObject = %s",
                url, JSON.toJSONString(body)), e);
            throw e;
        }

    }

    /**
     * DELETE 请求
     *
     * @param url
     * @param body
     */
    public JSONArray deleteArray(String url, JSONObject body) {

        try {

            String completeUrl = this.buildSignatureUrl(url, body);

            URI uri = UriComponentsBuilder.fromUriString(completeUrl).build().toUri();

            RequestEntity<Void> request = RequestEntity.delete(uri)
                .header("X-MBX-APIKEY", ApiKeyConfig.API_KEY)
                .accept(MediaType.APPLICATION_JSON)
                .build();

            ResponseEntity<JSONArray> response = restTemplate.exchange(request, JSONArray.class);

            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error(" deleteRequestArray | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                    JSON.toJSONString(response));
                throw new RuntimeException(" deleteRequestArray | HttpStatus_error ");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error(String.format(" EncryptHttpService | deleteArray | url = %s | JSONObject = %s",
                url, JSON.toJSONString(body)), e);
            throw e;
        }
    }

    /**
     * 获得带签名的完整URL
     *
     * @param url
     * @param body
     * @return
     */
    private String buildSignatureUrl(String url, JSONObject body) {

        String param = this.buildParam(body);

        String signature = EncryptUtil.getSignature(param);

        return StringUtils.join(url, "?", param, "&signature=", signature);
    }

    private String buildParam(JSONObject body) {

        if (body == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : body.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            sb.append("&");
        }

        if (sb.length() == 0) {
            return null;
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}

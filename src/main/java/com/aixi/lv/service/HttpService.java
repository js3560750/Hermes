package com.aixi.lv.service;

import java.net.URI;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.util.ApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Js
 */
@Component
@Slf4j
public class HttpService {

    @Resource
    RestTemplate restTemplate;

    /**
     * 测试连通性
     *
     * @return
     */
    public Boolean testConnected() {

        try {

            String url = ApiUtil.url("/api/v3/time");

            JSONObject response = this.getObject(url, null);

            Long serverTime = response.getLong("serverTime");

            if (serverTime == null) {
                return Boolean.FALSE;
            }

        } catch (Exception e) {
            log.error(" HttpService | testConnected_fail | 服务不通");
        }

        return Boolean.TRUE;

    }

    /**
     * GET 请求
     *
     * @param url
     * @param params
     * @return 对象
     */
    public JSONObject getObject(String url, JSONObject params) {

        RequestEntity<Void> request = this.buildGetRequest(url, params);

        ResponseEntity<JSONObject> response = restTemplate.exchange(request, JSONObject.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error(" getObject | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                JSON.toJSONString(response));
            throw new RuntimeException(" getObject | HttpStatus_error ");
        }

        return response.getBody();
    }

    /**
     * GET 请求
     *
     * @param url
     * @param params
     * @return 数组
     */
    public JSONArray getArray(String url, JSONObject params) {

        RequestEntity<Void> request = this.buildGetRequest(url, params);

        ResponseEntity<JSONArray> response = restTemplate.exchange(request, JSONArray.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error(" getArray | HttpStatus_error | request={} | resp={}", JSON.toJSONString(request),
                JSON.toJSONString(response));
            throw new RuntimeException(" getArray | HttpStatus_error ");
        }

        return response.getBody();

    }

    /**
     * 请求参数组装
     *
     * @param url
     * @param params
     * @return
     */
    private RequestEntity<Void> buildGetRequest(String url, JSONObject params) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (params != null) {

            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();

            for (Map.Entry entry : params.entrySet()) {
                paramMap.add(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }

            if (MapUtils.isNotEmpty(paramMap)) {
                builder.queryParams(paramMap);
            }
        }

        URI uri = builder.build().toUri();

        RequestEntity<Void> request = RequestEntity.get(uri)
            .accept(MediaType.APPLICATION_JSON)
            .build();

        return request;
    }

}

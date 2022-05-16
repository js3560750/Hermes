package com.aixi.lv.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.aixi.lv.config.ApiKeyConfig.WEB_PASSWORD;

/**
 * @author Js
 */
@RestController
@RequestMapping("/test")
@Api(tags = "测试服务")
@Slf4j
public class TestController {

    @GetMapping("/hello")
    public String testHello(String js) {

        if (StringUtils.isEmpty(js)) {
            return "js";
        }

        if (!js.equals(WEB_PASSWORD)) {
            return "js";
        }

        return "hello 艾希";
    }
}

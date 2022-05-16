package com.aixi.lv.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.manage.OrderLifeManage;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.service.OrderService;
import com.google.common.base.Preconditions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@RequestMapping("/order")
@Api(tags = "订单服务")
@Slf4j
public class OrderController {

    @Resource
    OrderLifeManage orderLifeManage;

    @Resource
    OrderService orderService;

    /**
     * @return
     */
    @GetMapping("/info")
    @ApiOperation("获取当前所有交易对信息")
    public Result allPairInfo(String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        List<TradePair> allPair = orderLifeManage.getAllPair();

        return Result.success(allPair);
    }

    /**
     * @param symbolCode
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/info/time")
    @ApiOperation("根据时间查询订单信息")
    public Result queryOrderByTime(String symbolCode, String startTime, String endTime, String js) {

        if (StringUtils.isEmpty(js)) {
            return Result.success("js");
        }

        if (!js.equals(WEB_PASSWORD)) {
            return Result.success("js");
        }

        Preconditions.checkArgument(StringUtils.isNotEmpty(symbolCode));
        Preconditions.checkArgument(StringUtils.isNotEmpty(startTime));
        Preconditions.checkArgument(StringUtils.isNotEmpty(endTime));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime start = LocalDateTime.parse(startTime, dtf);
        LocalDateTime end = LocalDateTime.parse(endTime, dtf);

        List<OrderLife> orderLifeList = orderService.queryAllOrder(Symbol.getByCode(symbolCode), start, end);

        return Result.success(orderLifeList);
    }

}

package com.aixi.lv;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.Box;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.service.AccountService;
import com.aixi.lv.service.BoxService;
import com.aixi.lv.service.EncryptHttpService;
import com.aixi.lv.service.HttpService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.strategy.buy.ReBuyStrategy;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

/**
 * @author Js
 */
public class 箱体Test extends BaseTest {

    @Resource
    HttpService httpService;

    @Resource
    EncryptHttpService encryptHttpService;

    @Resource
    BoxService boxService;

    @Resource
    RestTemplate restTemplate;

    @Resource
    MailService mailService;

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    @Resource
    AccountService accountService;

    @Resource
    ReBuyStrategy reBuyStrategy;

    @Test
    public void 找箱体() {

        LocalDateTime endTime = LocalDateTime.of(2022, 1, 5, 23, 30);
        LocalDateTime startTime = endTime.minusHours(24);

        // 查K线
        List<KLine> kLines = priceService.queryKLineByTime(Symbol.ETHUSDT, Interval.MINUTE_15, 96, startTime, endTime);

        // 找箱体
        Result<Box> result = boxService.findBox(kLines);

        if (result.getSuccess()) {
            System.out.println(JSON.toJSONString(result.getData()));
            return;
        }

    }

    @Test
    public void 找箱体2() {



        // 查K线
        List<KLine> kLines = priceService.queryKLine(Symbol.ETHUSDT, Interval.MINUTE_15, 96);

        // 找箱体
        Result<Box> result = boxService.findBox(kLines);

        if (result.getSuccess()) {
            System.out.println(JSON.toJSONString(result.getData()));
            return;
        }

    }

    @Test
    public void V型拐点测试(){

        try{

            //LocalDateTime endTime = LocalDateTime.of(2022, 1, 7, 23, 54);
            //LocalDateTime startTime = endTime.minusMinutes(30);
            //
            //Boolean aBoolean = reBuyStrategy.tradingVolumeBuyTest(Symbol.BTCUSDT, startTime, endTime);
            //
            //System.out.println(aBoolean);

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}

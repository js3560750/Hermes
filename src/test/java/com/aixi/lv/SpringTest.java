package com.aixi.lv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.CurrencyType;
import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.OrderSide;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.Account;
import com.aixi.lv.model.domain.Asset;
import com.aixi.lv.model.domain.Box;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.Result;
import com.aixi.lv.service.AccountService;
import com.aixi.lv.service.BoxService;
import com.aixi.lv.service.EncryptHttpService;
import com.aixi.lv.service.HttpService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.OrderService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.util.ApiUtil;
import com.aixi.lv.util.NumUtil;
import com.aixi.lv.util.RamUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import static com.aixi.lv.config.BackTestConfig.BACK_TEST_DATA_MAP;

/**
 * @author Js
 */
@Slf4j
public class SpringTest extends BaseTest {

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

    @Test
    public void 历史增长倍率检查() {

        for (Symbol symbol : Symbol.values()) {

            try {
                LocalDate saleDate = symbol.getSaleDate();
                LocalDateTime startTime = LocalDateTime.of(saleDate.getYear(), saleDate.getMonth(),
                    saleDate.getDayOfMonth(), 0,
                    0).plusWeeks(2);

                List<KLine> kLines = priceService.queryKLineByTime(symbol, Interval.DAY_1, 1, startTime,
                    startTime.plusDays(1));

                BigDecimal startPrice = kLines.get(0).getClosingPrice();

                BigDecimal newPrice = priceService.queryNewPrice(symbol);

                BigDecimal percent = newPrice.divide(startPrice, 3, RoundingMode.DOWN);

                System.out.println(
                    StringUtils.rightPad(symbol.getCode(), 10) + " | 增长倍率 = " +
                        StringUtils.rightPad(percent.stripTrailingZeros().toPlainString(), 10)
                        + " | 开始价格 = " + startPrice.stripTrailingZeros().toPlainString()
                        + " | 结束价格 = " + newPrice.stripTrailingZeros().toPlainString());
            } catch (Exception e) {
                System.out.println(e);
            }

        }

    }

    @Test
    public void 近1个月增长倍率检查() {

        for (Symbol symbol : Symbol.values()) {

            try {
                LocalDateTime startTime = LocalDateTime.now().minusMonths(1);

                List<KLine> kLines = priceService.queryKLineByTime(symbol, Interval.DAY_1, 1, startTime,
                    startTime.plusDays(1));

                BigDecimal startPrice = kLines.get(0).getClosingPrice();

                BigDecimal newPrice = priceService.queryNewPrice(symbol);

                BigDecimal percent = newPrice.divide(startPrice, 3, RoundingMode.DOWN);

                System.out.println(
                    StringUtils.rightPad(symbol.getCode(), 10) + " | 增长倍率 = " +
                        StringUtils.rightPad(percent.stripTrailingZeros().toPlainString(), 10)
                        + " | 开始价格 = " + startPrice.stripTrailingZeros().toPlainString()
                        + " | 结束价格 = " + newPrice.stripTrailingZeros().toPlainString());
            } catch (Exception e) {
                System.out.println(e);
            }

        }

    }

    @Test
    public void K线图() {

        // 时间最早的排 index=0 , 时间最晚的排 index=size-1
        List<KLine> kLines = priceService.queryKLine(Symbol.BTCUSDT, Interval.MINUTE_1, 15);

        System.out.println(JSON.toJSONString(kLines));

    }

    @Test
    public void K线图by时间() {
        LocalDateTime endTime = LocalDateTime.of(2022, 1, 7, 23, 55);
        LocalDateTime startTime = endTime.minusMinutes(30);

        // 查K线
        List<KLine> kLines = priceService.queryKLineByTime(Symbol.BTCUSDT, Interval.MINUTE_1, 50, startTime, endTime);

        System.out.println(JSON.toJSONString(kLines));
    }

    @Test
    public void 找箱体() {

        // 查K线
        List<KLine> kLines = priceService.queryKLine(Symbol.ETHUSDT, Interval.MINUTE_15, 48);

        // 找箱体
        Result<Box> result = boxService.findBox(kLines);

        if (result.getSuccess()) {
            System.out.println(JSON.toJSONString(result.getData()));
            return;
        }

    }

    @Test
    public void 最新价格() {

        BigDecimal price = priceService.queryNewPrice(Symbol.DOGEUSDT);

        System.out.println(price);

    }

    @Test
    public void 下买单() {

        try {

            OrderLife orderLife = orderService.limitBuyOrder(OrderSide.BUY, Symbol.DOGEUSDT, new BigDecimal("2000"),
                new BigDecimal("0.1"));

            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 市价买单() {

        try {

            OrderLife orderLife = orderService.marketBuyOrder(OrderSide.BUY, Symbol.ETHUSDT, new BigDecimal("23"));

            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 市价卖单() {

        try {

            OrderLife orderLife = orderService.marketSellOrder(OrderSide.SELL, Symbol.DOGEUSDT, new BigDecimal("160"));

            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 下卖单() {

        try {

            OrderLife orderLife = orderService.limitSellOrder(OrderSide.SELL, Symbol.DOGEUSDT, new BigDecimal("200"),
                new BigDecimal("0.2000"));

            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 限价止损卖单() {

        try {

            OrderLife orderLife = orderService.stopLossOrder(OrderSide.SELL, Symbol.DOGEUSDT,
                new BigDecimal("200.0001"),
                new BigDecimal("0.1600"));

            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 限价止盈卖单() {

        try {

            OrderLife orderLife = orderService.takeProfitOrder(OrderSide.SELL, Symbol.DOGEUSDT,
                new BigDecimal("200.002"),
                new BigDecimal("0.2"));

            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 撤销挂单() {

        try {

            Boolean success = orderService.cancelByOrderId(Symbol.DOGEUSDT, 2226020148L);

            System.out.println(success);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 撤销单一交易对的所有挂单() {

        try {

            Boolean aBoolean = orderService.cancelOpenOrder(Symbol.ETHUSDT);

            System.out.println(aBoolean);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void 根据时间查订单() {
        try {

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(23L);
            List<OrderLife> orderLifeList = orderService.queryAllOrder(Symbol.DOGEUSDT, startTime, endTime);
            System.out.println(JSON.toJSONString(orderLifeList));

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void 查当前挂单() {
        try {

            List<OrderLife> orderLifeList = orderService.queryOpenOrder(Symbol.DOGEUSDT);

            System.out.println(orderLifeList);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void 根据ID查订单() {
        try {

            Long orderId = 2251683848L;
            OrderLife orderLife = orderService.queryByOrderId(Symbol.DOGEUSDT, orderId);
            System.out.println(orderLife);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    ///api/v3/account

    @Test
    public void 查询账户信息() {
        try {

            Account account = accountService.queryAccountInfo();

            System.out.println(JSON.toJSONString(account));

            List<Asset> assetList = account.getAssetList();

            for (Asset asset : assetList) {
                if (CurrencyType.BNB == asset.getCurrencyType()) {
                    System.out.println(asset.getFreeQty());
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void 查询交易配置信息() {

        try {

            String url = ApiUtil.url("/api/v3/exchangeInfo");

            JSONObject body = new JSONObject();
            body.put("symbol", Symbol.BTCUSDT.getCode());

            JSONObject response = httpService.getObject(url, body);

            ArrayList list = (ArrayList)((Map)response.getJSONArray("symbols").get(0)).get("filters");

            System.out.println(response);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}

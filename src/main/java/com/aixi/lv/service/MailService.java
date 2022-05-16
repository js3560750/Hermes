package com.aixi.lv.service;

import java.math.BigDecimal;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.model.domain.OrderLife;
import com.aixi.lv.model.domain.TradePair;
import com.aixi.lv.util.NumUtil;
import com.aixi.lv.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.stereotype.Component;

/**
 * @author Js
 */
@Component
@Slf4j
public class MailService {

    @Resource
    PriceService priceService;

    @Resource
    OrderService orderService;

    private static final String NET_163_PASSWORD = "*********************";
    private static final String NET_163_SMTP_HOST = "smtp.163.com";
    private static final Integer NET_163_SMTP_PORT = 465;
    private static final String NET_163_ADDRESS = "*********************@163.com";

    public void sendEmail(String title, String content) {

        try {

            Email email = EmailBuilder.startingBlank()
                .from("Js's Louis Vuitton", NET_163_ADDRESS)
                .to("Js", NET_163_ADDRESS)
                .withSubject(title)
                .withPlainText(content)
                .buildEmail();

            Mailer mailer = MailerBuilder
                .withSMTPServer(NET_163_SMTP_HOST, NET_163_SMTP_PORT, NET_163_ADDRESS, NET_163_PASSWORD)
                .withTransportStrategy(TransportStrategy.SMTPS)
                .withSessionTimeout(30 * 1000)
                .clearEmailAddressCriteria() // turns off email validation
                .withDebugLogging(false)
                .buildMailer();

            mailer.sendMail(email);

        } catch (Exception e) {
            log.error(" sendEmail error ", e);
        }
    }

    /**
     * 成交邮件
     *
     * @param title
     * @param buyOrder
     * @param sellServer
     */
    public void sellDealEmail(String title, OrderLife buyOrder, OrderLife sellServer) {

        String completeTitle = sellServer.getSymbol().getCode() + " " + title;

        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + sellServer.getSymbol().getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");

        BigDecimal sellQty = sellServer.getExecutedQty();

        BigDecimal sellAmount = sellServer.getSellPrice().multiply(sellQty);
        BigDecimal buyAmount = buyOrder.getBuyPrice().multiply(sellQty);

        if (sellAmount.compareTo(buyAmount) >= 0) {
            content.append("盈利金额 : " + sellAmount.subtract(buyAmount).stripTrailingZeros());
        } else {
            content.append("亏损金额 : " + buyAmount.subtract(sellAmount).stripTrailingZeros());
        }
        content.append("\n");
        content.append("\n");
        content.append("卖出价格 : " + NumUtil.cutZero(sellServer.getSellPrice()));
        content.append("\n");
        content.append("卖出金额 : " + NumUtil.cutZero(sellAmount));
        content.append("\n");
        content.append("买入金额 : " + NumUtil.cutZero(buyAmount));
        content.append("\n");

        this.sendEmail(completeTitle, content.toString());
        log.warn("【 卖出成功 】| {} | {}", completeTitle, content);
    }

    /**
     * 交易完结邮件
     *
     * @param title
     * @param pair
     */
    public void tradeDoneEmail(String title, TradePair pair) {

        OrderLife buyOrder = pair.getBuyOrder();
        OrderLife lossOrder = pair.getLossOrder();
        OrderLife firstOrder = pair.getFirstProfitOrder();
        OrderLife secondOrder = pair.getSecondProfitOrder();
        OrderLife thirdOrder = pair.getThirdProfitOrder();
        OrderLife forceOrder = pair.getForceProfitOrder();
        Symbol symbol = buyOrder.getSymbol();

        // 总买入金额
        BigDecimal buyAmount = buyOrder.getCumulativeQuoteQty();

        // 总卖出金额
        BigDecimal sellAmount = BigDecimal.ZERO;
        if (lossOrder != null) {
            OrderLife lossServer = orderService.queryByOrderId(symbol, lossOrder.getOrderId());
            sellAmount = sellAmount.add(lossServer.getCumulativeQuoteQty());
        }
        if (firstOrder != null) {
            OrderLife firstServer = orderService.queryByOrderId(symbol, firstOrder.getOrderId());
            sellAmount = sellAmount.add(firstServer.getCumulativeQuoteQty());
        }
        if (secondOrder != null) {
            OrderLife secondServer = orderService.queryByOrderId(symbol, secondOrder.getOrderId());
            sellAmount = sellAmount.add(secondServer.getCumulativeQuoteQty());
        }
        if (thirdOrder != null) {
            OrderLife thirdServer = orderService.queryByOrderId(symbol, thirdOrder.getOrderId());
            sellAmount = sellAmount.add(thirdServer.getCumulativeQuoteQty());
        }
        if (forceOrder != null) {
            OrderLife forceServer = orderService.queryByOrderId(symbol, forceOrder.getOrderId());
            sellAmount = sellAmount.add(forceServer.getCumulativeQuoteQty());
        }

        BigDecimal newPrice = priceService.queryNewPrice(symbol);

        String completeTitle = buyOrder.getSymbol().getCode() + " " + title;

        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + buyOrder.getSymbol().getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");

        if (sellAmount.compareTo(buyAmount) >= 0) {
            content.append("盈利金额 : " + sellAmount.subtract(buyAmount).stripTrailingZeros());
        } else {
            content.append("亏损金额 : " + buyAmount.subtract(sellAmount).stripTrailingZeros());
        }
        content.append("\n");
        content.append("卖出金额 : " + NumUtil.cutZero(sellAmount));
        content.append("\n");
        content.append("买入金额 : " + NumUtil.cutZero(buyAmount));
        content.append("\n");
        content.append("当前价格 : " + NumUtil.cutZero(newPrice));
        content.append("\n");
        if (buyOrder.getBottomPrice() != null) {
            content.append("箱底价格 : " + NumUtil.cutZero(buyOrder.getBottomPrice()));
            content.append("\n");
        }
        if (buyOrder.getTopPrice() != null) {
            content.append("箱顶价格 : " + NumUtil.cutZero(buyOrder.getTopPrice()));
            content.append("\n");
        }

        this.sendEmail(completeTitle, content.toString());

    }

    /**
     * 买入成交
     *
     * @param title
     * @param buyServer
     */
    public void buyDealMail(String title, OrderLife buyServer) {

        BigDecimal newPrice = priceService.queryNewPrice(buyServer.getSymbol());

        String completeTitle = buyServer.getSymbol().getCode() + " " + title;

        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + buyServer.getSymbol().getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");
        content.append("买入价格 : " + NumUtil.cutZero(buyServer.getBuyPrice()));
        content.append("\n");
        content.append("买入数量 : " + NumUtil.cutZero(buyServer.getExecutedQty()));
        content.append("\n");
        content.append("买入金额 : " + NumUtil.cutZero(buyServer.getBuyPrice().multiply(buyServer.getExecutedQty())));
        content.append("\n");
        content.append("当前价格 : " + NumUtil.cutZero(newPrice));
        content.append("\n");
        content.append("箱底价格 : " + NumUtil.cutZero(buyServer.getBottomPrice()));
        content.append("\n");
        content.append("箱顶价格 : " + NumUtil.cutZero(buyServer.getTopPrice()));
        content.append("\n");
        content.append("任务编号 : " + buyServer.getTaskKey());
        content.append("\n");

        this.sendEmail(completeTitle, content.toString());
        log.warn("【 买入成功 】| {} | {}", completeTitle, content);
    }

    public void macdBuyMail(String title, OrderLife buyServer, MacdAccount account) {

        BigDecimal newPrice = priceService.queryNewPrice(buyServer.getSymbol());

        String completeTitle = buyServer.getSymbol().getCode() + " " + title;

        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + buyServer.getSymbol().getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");
        content.append("买入价格 : " + NumUtil.cutZero(buyServer.getBuyPrice()));
        content.append("\n");
        content.append("买入数量 : " + NumUtil.cutZero(buyServer.getExecutedQty()));
        content.append("\n");
        content.append("买入金额 : " + NumUtil.cutZero(buyServer.getBuyPrice().multiply(buyServer.getExecutedQty())));
        content.append("\n");
        content.append("当前价格 : " + NumUtil.cutZero(newPrice));
        content.append("\n");
        content.append("\n");
        content.append("MACD账户名 : " + account.getName());
        content.append("\n");

        this.sendEmail(completeTitle, content.toString());
        log.warn("【 买入成功 】| {} | {}", completeTitle, content);
    }

    public void macdSellMail(String title, TradePair pair, MacdAccount account) {

        OrderLife buyOrder = pair.getBuyOrder();
        OrderLife lossOrder = pair.getLossOrder();
        Symbol symbol = buyOrder.getSymbol();

        // 总买入金额
        BigDecimal buyAmount = buyOrder.getCumulativeQuoteQty();

        // 总卖出金额
        BigDecimal sellAmount = BigDecimal.ZERO;
        if (lossOrder != null) {
            OrderLife lossServer = orderService.queryByOrderId(symbol, lossOrder.getOrderId());
            sellAmount = sellAmount.add(lossServer.getCumulativeQuoteQty());
        }

        BigDecimal newPrice = priceService.queryNewPrice(symbol);

        String completeTitle = buyOrder.getSymbol().getCode() + " " + title;

        StringBuilder content = new StringBuilder();
        content.append("交易对 : " + buyOrder.getSymbol().getCode());
        content.append("\n");
        content.append("时间 : " + TimeUtil.getCurrentTime());
        content.append("\n");

        if (sellAmount.compareTo(buyAmount) >= 0) {
            content.append("盈利金额 : " + sellAmount.subtract(buyAmount).stripTrailingZeros());
        } else {
            content.append("亏损金额 : " + buyAmount.subtract(sellAmount).stripTrailingZeros());
        }
        content.append("\n");
        content.append("卖出金额 : " + NumUtil.cutZero(sellAmount));
        content.append("\n");
        content.append("买入金额 : " + NumUtil.cutZero(buyAmount));
        content.append("\n");
        content.append("当前价格 : " + NumUtil.cutZero(newPrice));
        content.append("\n");
        content.append("MACD账户名 : " + account.getName());
        content.append("\n");

        this.sendEmail(completeTitle, content.toString());
    }
}

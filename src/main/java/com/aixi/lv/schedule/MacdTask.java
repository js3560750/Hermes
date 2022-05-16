package com.aixi.lv.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.MacdAccount;
import com.aixi.lv.service.AccountService;
import com.aixi.lv.service.MailService;
import com.aixi.lv.service.PriceService;
import com.aixi.lv.strategy.indicator.MacdBuySellStrategy;
import com.aixi.lv.util.TimeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.EMAIL_BUY_SET;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.EMAIL_SELL_SET;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.MACD_ACCOUNT_MAP;
import static com.aixi.lv.strategy.indicator.MacdBuySellStrategy.SELL_WARNING_SET;

/**
 * @author Js
 */
@Component
@EnableScheduling   // 1.开启定时任务
@EnableAsync        // 2.开启多线程
@Profile({"lv", "hermes"})
@Slf4j
public class MacdTask {

    @Resource
    MacdBuySellStrategy macdBuySellStrategy;

    @Resource
    MailService mailService;

    @Resource
    PriceService priceService;

    @Resource
    AccountService accountService;

    private static Boolean sellDetecting = Boolean.FALSE;

    /**
     * 检查手续费余额
     */
    @Async
    @Scheduled(cron = "0 20,50 * * * ? ") // 每小时 第20\50分钟开始执行
    public void checkBNBFreeQty() {

        BigDecimal bnbFreeQty = accountService.queryBNBFreeQty();

        if (bnbFreeQty.compareTo(new BigDecimal("0.3")) < 0) {
            mailService.sendEmail("BNB余额不足", bnbFreeQty.stripTrailingZeros().toPlainString());
        }

    }

    /**
     * 购买准入扫描
     */
    @Async
    @Scheduled(cron = "0 5 * * * ? ")  // 每小时5分
    public void macdHourTask() {

        try {

            if (OPEN) {
                throw new RuntimeException("回测模式没有关闭");
            }

            for (MacdAccount account : MACD_ACCOUNT_MAP.values()) {

                if (account == null) {
                    continue;
                }

                // 主流程
                macdBuySellStrategy.detect(account);
            }

        } catch (Exception e) {
            log.error(" macdHourTask 异常 " + e.getMessage(), e);
            mailService.sendEmail(" macdHourTask 异常", e.getMessage());
            // 终止程序运行
            System.exit(0);
        }
    }

    /**
     * 卖出扫描
     */
    @Async
    @Scheduled(cron = "0/20 * * * * ? ") // 每20秒开始执行
    public void sellTask() {

        try {

            if (OPEN) {
                throw new RuntimeException("回测模式没有关闭");
            }

            // 正在探测中，上一次探测未完成，则直接return
            if (sellDetecting) {
                return;
            }

            sellDetecting = Boolean.TRUE;

            for (MacdAccount account : MACD_ACCOUNT_MAP.values()) {

                if (account == null) {
                    continue;
                }

                // 卖出流程
                macdBuySellStrategy.sellDetect(account);
            }

            sellDetecting = Boolean.FALSE;

        } catch (Exception e) {
            log.error(" sellTask 异常 " + e.getMessage(), e);
            mailService.sendEmail(" sellTask 异常", e.getMessage());
            // 终止程序运行
            System.exit(0);
        }
    }


    /**
     * 定时发送账户信息
     */
    @Async
    @Scheduled(cron = "0 30 12 * * ? ") // 每天中午12点30分发送
    public void sendAccountInfo() {

        MutablePair<Map<String, BigDecimal>, List<JSONObject>> pair = this.printAccountInfo();

        Map<String, BigDecimal> belongMap = pair.getLeft();
        List<JSONObject> subList = pair.getRight();

        StringBuilder content = new StringBuilder();

        content.append("【 归属账户信息 】");
        content.append("\n");
        for (Entry<String, BigDecimal> entry : belongMap.entrySet()) {
            content.append("归属账户名 : " + entry.getKey());
            content.append("归属总额 : " + entry.getValue().stripTrailingZeros().toPlainString());
            content.append("\n");
        }

        content.append("\n");
        content.append("\n");
        content.append("【 子账户信息 】");
        content.append("\n");
        for (JSONObject jo : subList) {
            content.append("子账户名 : " + jo.getString("子账户名"));
            content.append("当前持有 : " + jo.getString("当前持有"));
            content.append("账户金额 : " + jo.getString("账户金额"));
            content.append("\n");
        }

        mailService.sendEmail("每日账户明细", content.toString());

    }

    /**
     * 每小时清理一下卖出预警信息
     */
    @Async
    @Scheduled(cron = "0 50 0/1 * * ?  ") // 每小时 第50分钟开始执行
    public void clearSellWarning() {

        SELL_WARNING_SET.clear();
        EMAIL_SELL_SET.clear();
        EMAIL_BUY_SET.clear();
    }

    /**
     * 打印交易对信息
     */
    @Async
    @Scheduled(cron = "0 15 0/1 * * ?  ") // 每小时 第15分钟开始执行
    public MutablePair<Map<String, BigDecimal>, List<JSONObject>> printAccountInfo() {

        log.info(" 账户信息 | 账户数量 = {} ", MACD_ACCOUNT_MAP.size());

        // 归属账户
        Map<String, BigDecimal> belongMap = new TreeMap<>();

        // 子账户
        List<JSONObject> subAccountInfoList = Lists.newArrayList();

        List<MacdAccount> accountList = Lists.newArrayList(MACD_ACCOUNT_MAP.values())
            .stream()
            .sorted(Comparator.comparing(MacdAccount::getName))
            .collect(Collectors.toList());

        for (MacdAccount ac : accountList) {

            BigDecimal totalAmount;
            if (ac.getCurHoldSymbol() != null) {
                BigDecimal newPrice = priceService.queryNewPrice(ac.getCurHoldSymbol());
                // 账户余额 + 持有货币价值
                totalAmount = ac.getCurHoldAmount()
                    .add(ac.getCurHoldQty().multiply(newPrice))
                    .setScale(2, RoundingMode.HALF_DOWN);
            } else {
                totalAmount = ac.getCurHoldAmount();
            }

            JSONObject jo = new JSONObject();
            jo.put("子账户名", StringUtils.rightPad(ac.getName(), 8));
            jo.put("当前持有",
                StringUtils.rightPad(ac.getCurHoldSymbol() != null ? ac.getCurHoldSymbol().getCode() : "空仓", 10));
            jo.put("账户金额", totalAmount.stripTrailingZeros().toPlainString());
            subAccountInfoList.add(jo);

            log.info(" 子账户信息 | 子账户名 = {} | 账户金额 = {} | 当前持有 = {}",
                jo.getString("子账户名"),
                jo.getString("账户金额"),
                jo.getString("当前持有"));

            // 统计归属账户余额
            if (StringUtils.isNotEmpty(ac.getBelongAccount())) {

                String belongAccount = ac.getBelongAccount();

                if (belongMap.containsKey(belongAccount)) {
                    belongMap.put(belongAccount, belongMap.get(belongAccount).add(totalAmount));
                } else {
                    belongMap.put(belongAccount, totalAmount);
                }
            }
        }

        for (Entry<String, BigDecimal> entry : belongMap.entrySet()) {
            log.info(" 归属账户信息 | 归属账户名 = {} | 归属总额 = {}",
                StringUtils.rightPad(entry.getKey(), 8),
                entry.getValue().stripTrailingZeros().toPlainString());
        }

        return MutablePair.of(belongMap, subAccountInfoList);

    }

}

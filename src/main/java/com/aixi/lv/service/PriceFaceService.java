package com.aixi.lv.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import com.aixi.lv.model.domain.KLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.aixi.lv.config.BackTestConfig.OPEN;

/**
 * @author Js
 *
 * 兼容回测的价格服务
 */
@Component
@Slf4j
public class PriceFaceService extends PriceService {

    @Resource
    private BackTestPriceService backTestPriceService;

    @Override
    public List<KLine> queryKLine(Symbol symbol, Interval interval, Integer limit) {

        if (OPEN) {
            return backTestPriceService.queryKLine(symbol, interval, limit);
        } else {
            return super.queryKLine(symbol, interval, limit);
        }

    }

    @Override
    public List<KLine> queryKLineByTime(Symbol symbol, Interval interval, Integer limit, LocalDateTime startTime,
        LocalDateTime endTime) {

        if (OPEN) {
            return backTestPriceService.queryKLineByTime(symbol, interval, limit, startTime, endTime);
        } else {
            return super.queryKLineByTime(symbol, interval, limit, startTime, endTime);
        }

    }

    @Override
    public BigDecimal queryNewPrice(Symbol symbol) {

        if (OPEN) {
            return backTestPriceService.queryNewPrice(symbol);
        } else {
            return super.queryNewPrice(symbol);
        }

    }
}

package com.aixi.lv.model.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aixi.lv.model.constant.Interval;
import com.aixi.lv.model.constant.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Box {

    /**
     * 箱顶
     */
    private BigDecimal topPrice;

    /**
     * 箱底
     */
    private BigDecimal bottomPrice;

    /**
     * 箱体开始时间
     */
    private LocalDateTime startTime;

    /**
     * 箱体结束时间
     */
    private LocalDateTime endTime;

    /**
     * 时间间隔
     */
    private Interval interval;

    /**
     * 交易对
     */
    private Symbol symbol;

}

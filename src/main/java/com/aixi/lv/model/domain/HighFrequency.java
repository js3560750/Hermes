package com.aixi.lv.model.domain;

import java.math.BigDecimal;

import com.aixi.lv.model.constant.Interval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Js
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HighFrequency {

    /**
     * 高频扫描标记
     * true 执行，false 不执行
     */
    private Boolean scanFlag;

    /**
     * 扫描参考的箱底价格
     */
    private BigDecimal bottomPrice;

    /**
     * 扫描参考的箱顶价格
     */
    private BigDecimal topPrice;

    /**
     * k线间隔
     */
    private Interval interval;

    /**
     * k线数量
     */
    private Integer limit;
}

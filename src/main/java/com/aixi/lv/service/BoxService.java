package com.aixi.lv.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.aixi.lv.model.domain.Box;
import com.aixi.lv.model.domain.KLine;
import com.aixi.lv.model.domain.Result;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;

/**
 * @author Js
 *
 * 找箱体
 */
@Component
@Slf4j
public class BoxService {

    private static final BigDecimal FLOAT_RATE = new BigDecimal("0.001");
    private static final BigDecimal BOTTOM_FLOAT_RATE_LIMIT = new BigDecimal("0.007");
    private static final BigDecimal TOP_FLOAT_RATE_LIMIT = new BigDecimal("0.008");

    private static final BigDecimal TOP_BOTTOM_DIFF_RATE = new BigDecimal("1.009");

    private static final Integer MAX_MATCH_TIMES = 3;

    /**
     * 找箱体
     *
     * @param inputKLines
     * @return
     */
    public Result<Box> findBox(List<KLine> inputKLines) {

        if (CollectionUtils.isEmpty(inputKLines)) {
            return Result.fail("输入K线图为空");
        }

        // 最后一根K线还没走完，先不考虑这根K线
        inputKLines.remove(inputKLines.size() - 1);

        List<KLine> bottomList = Lists.newLinkedList();
        List<KLine> topList = Lists.newLinkedList();

        List<KLine> KLines = inputKLines.stream()
            .sorted(Comparator.comparing(KLine::getOpeningTime).reversed())
            .collect(Collectors.toList());

        for (int i = 1; i < KLines.size() - 1; i++) {
            if (isBottom(KLines, i)) {
                bottomList.add(KLines.get(i));
            }

            if (isTop(KLines, i)) {
                topList.add(KLines.get(i));
            }
        }

        if (CollectionUtils.isEmpty(bottomList) || CollectionUtils.isEmpty(topList)) {
            return Result.fail("头尖或底尖为空");
        }

        // 找箱底
        KLine bottomKLine;
        List<KLine> bottomMatchList;

        List<KLine> tempBottomList = bottomList.stream().collect(Collectors.toList());

        while (true) {

            if (CollectionUtils.isEmpty(tempBottomList) || tempBottomList.size() < 5) {
                return Result.fail("找 箱底 达到最大浮动比例");
            }

            // 找底部
            KLine tempBottomKLine = tempBottomList.get(0);
            for (int i = 0; i < tempBottomList.size(); i++) {
                if (tempBottomList.get(i).getMinPrice().compareTo(tempBottomKLine.getMinPrice()) <= 0) {
                    tempBottomKLine = tempBottomList.get(i);
                }
            }

            Optional<MutablePair<KLine, List<KLine>>> optionalBottom = this.findBottomPrice(tempBottomList,
                tempBottomKLine);

            if (optionalBottom.isPresent()) {
                bottomKLine = optionalBottom.get().getLeft();
                bottomMatchList = optionalBottom.get().getRight();
                break;
            } else {
                // 移除当前，继续找下一个
                Iterator<KLine> iterator = tempBottomList.iterator();
                while (iterator.hasNext()) {
                    KLine next = iterator.next();
                    if (next.equals(tempBottomKLine)) {
                        iterator.remove();
                    }
                }
            }
        }

        // 找箱顶
        KLine topKLine;
        List<KLine> topMatchList;

        List<KLine> tempTopList = topList.stream().collect(Collectors.toList());

        while (true) {

            if (CollectionUtils.isEmpty(tempTopList) || tempTopList.size() < 5) {
                return Result.fail("找 箱顶 达到最大浮动比例");
            }

            // 找顶部
            KLine tempTopKLine = tempTopList.get(0);
            for (int i = 0; i < tempTopList.size(); i++) {
                if (tempTopList.get(i).getMaxPrice().compareTo(tempTopKLine.getMaxPrice()) >= 0) {
                    tempTopKLine = tempTopList.get(i);
                }
            }
            Optional<MutablePair<KLine, List<KLine>>> optionalTop = this.findTopPrice(tempTopList, bottomMatchList,
                tempTopKLine);

            if (optionalTop.isPresent()) {
                topKLine = optionalTop.get().getLeft();
                topMatchList = optionalTop.get().getRight();
                break;
            } else {
                // 移除当前，继续找下一个
                Iterator<KLine> iterator = tempTopList.iterator();
                while (iterator.hasNext()) {
                    KLine next = iterator.next();
                    if (next.equals(tempTopKLine)) {
                        iterator.remove();
                    }
                }
            }
        }

        Box box = new Box();
        box.setBottomPrice(this.getAverageBottomPrice(bottomMatchList));
        box.setTopPrice(this.getAverageTopPrice(topMatchList));
        box.setStartTime(inputKLines.get(0).getOpeningTime());
        box.setEndTime(inputKLines.get(inputKLines.size() - 1).getOpeningTime());

        // 如果箱底箱顶太近了(差值小于1%)，认为没有找到箱体
        BigDecimal topBottomDiffRate = box.getTopPrice().divide(box.getBottomPrice(), 8, BigDecimal.ROUND_HALF_UP);
        if (topBottomDiffRate.compareTo(TOP_BOTTOM_DIFF_RATE) <= 0) {
            return Result.fail(
                "箱底箱顶差值小于0.9% | 差值=" + topBottomDiffRate + " | minPrice=" + box.getBottomPrice() + " | maxPrice="
                    + box.getTopPrice());
        }

        return Result.success(box);

    }

    private Optional<MutablePair<KLine, List<KLine>>> findBottomPrice(List<KLine> bottomList, KLine bottomKLine) {

        List<KLine> bottomMatchList;
        BigDecimal curBottomFloatRate = new BigDecimal("0.001");

        while (true) {

            if (curBottomFloatRate.compareTo(BOTTOM_FLOAT_RATE_LIMIT) > 0) {
                return Optional.empty();
            }

            BigDecimal bottomFloatPrice = bottomKLine.getMinPrice().multiply(
                // 1*(1+rate)
                BigDecimal.ONE.multiply(BigDecimal.ONE.add(curBottomFloatRate))
            );

            Integer matchTimes = 0;
            bottomMatchList = Lists.newArrayList();

            // 找底部匹配线
            Integer lastMatchIndex = 0;
            for (int i = 0; i < bottomList.size(); i++) {

                if (bottomList.get(i).getMinPrice().compareTo(bottomFloatPrice) <= 0
                    // 不要找相邻的
                    && i > (lastMatchIndex + 1)) {

                    matchTimes++;
                    lastMatchIndex = i;
                    bottomMatchList.add(bottomList.get(i));

                    if (MAX_MATCH_TIMES.equals(matchTimes)) {
                        return Optional.of(MutablePair.of(bottomKLine, bottomMatchList));
                    }
                }
            }

            // 加大比例
            curBottomFloatRate = curBottomFloatRate.add(FLOAT_RATE);

        }

    }

    private Optional<MutablePair<KLine, List<KLine>>> findTopPrice(List<KLine> topList,
        List<KLine> bottomMatchList, KLine topKLine) {

        List<KLine> topMatchList;
        BigDecimal curTopFloatRate = new BigDecimal("0.001");

        while (true) {

            if (curTopFloatRate.compareTo(TOP_FLOAT_RATE_LIMIT) > 0) {
                return Optional.empty();
            }

            BigDecimal topFloatPrice = topKLine.getMaxPrice().multiply(
                // 1*(1-rate)
                BigDecimal.ONE.multiply(BigDecimal.ONE.subtract(curTopFloatRate))
            );

            Integer matchTimes = 0;
            topMatchList = Lists.newArrayList();

            // 找顶部匹配线
            Integer lastMatchIndex = 0;
            for (int i = 0; i < topList.size(); i++) {
                if (topList.get(i).getMaxPrice().compareTo(topFloatPrice) >= 0
                    // 不要找相邻的
                    && i > (lastMatchIndex + 1)) {

                    matchTimes++;
                    lastMatchIndex = i;
                    topMatchList.add(topList.get(i));

                    if (MAX_MATCH_TIMES.equals(matchTimes)) {
                        // 检查是否全偏到一侧
                        if (checkIsAtOneSide(topMatchList, bottomMatchList)) {
                            break;
                        } else {
                            return Optional.of(MutablePair.of(topList.get(i), topMatchList));
                        }
                    }
                }
            }

            // 加大比例
            curTopFloatRate = curTopFloatRate.add(FLOAT_RATE);

        }
    }

    /**
     * 获得匹配的箱顶平均价
     *
     * @param topMatchList
     * @return
     */
    private BigDecimal getAverageTopPrice(List<KLine> topMatchList) {

        BigDecimal totalTopPrice = BigDecimal.ZERO;

        for (KLine temp : topMatchList) {
            totalTopPrice = totalTopPrice.add(temp.getMaxPrice());
        }

        return totalTopPrice.divide(new BigDecimal(topMatchList.size()), 8, RoundingMode.HALF_UP);

    }

    private BigDecimal getAverageBottomPrice(List<KLine> bottomMatchList) {

        BigDecimal totalBottomPrice = BigDecimal.ZERO;

        for (KLine temp : bottomMatchList) {
            totalBottomPrice = totalBottomPrice.add(temp.getMinPrice());
        }

        return totalBottomPrice.divide(new BigDecimal(bottomMatchList.size()), 8, RoundingMode.HALF_UP);

    }

    private BigDecimal getMaxBottomPrice(List<KLine> bottomMatchList) {

        BigDecimal max = BigDecimal.ZERO;

        for (KLine temp : bottomMatchList) {
            if (max.compareTo(temp.getMinPrice()) < 0) {
                max = temp.getMinPrice();
            }
        }

        return max;

    }

    /**
     * 检查是否全偏到一边
     *
     * @return
     */
    private Boolean checkIsAtOneSide(List<KLine> topMatchList, List<KLine> bottomMatchList) {

        if (CollectionUtils.isEmpty(topMatchList) || CollectionUtils.isEmpty(bottomMatchList)) {
            throw new RuntimeException(" checkIsAtOneSide | fail | input is null");
        }

        List<KLine> topList = topMatchList.stream()
            .sorted(Comparator.comparing(KLine::getOpeningTime))
            .collect(Collectors.toList());

        KLine topFirst = topList.get(0);
        KLine topLast = topList.get(topList.size() - 1);

        List<KLine> bottomList = bottomMatchList.stream()
            .sorted(Comparator.comparing(KLine::getOpeningTime))
            .collect(Collectors.toList());

        KLine bottomFirst = bottomList.get(0);
        KLine bottomLast = bottomList.get(bottomList.size() - 1);

        if (topFirst.getOpeningTime().isAfter(bottomLast.getOpeningTime())) {
            return Boolean.TRUE;
        }

        if (topLast.getOpeningTime().isBefore(bottomFirst.getOpeningTime())) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;

    }

    /**
     * 是否底尖
     *
     * @param lines
     * @param i
     * @return
     */
    private Boolean isBottom(List<KLine> lines, Integer i) {

        Boolean result = lines.get(i - 1).getMinPrice().compareTo(lines.get(i).getMinPrice()) > 0
            && lines.get(i).getMinPrice().compareTo(lines.get(i + 1).getMinPrice()) < 0;

        return result;
    }

    /**
     * 是否头尖
     *
     * @param lines
     * @param i
     * @return
     */
    private Boolean isTop(List<KLine> lines, Integer i) {

        Boolean result = lines.get(i - 1).getMaxPrice().compareTo(lines.get(i).getMaxPrice()) < 0
            && lines.get(i).getMaxPrice().compareTo(lines.get(i + 1).getMaxPrice()) > 0;

        return result;
    }

}

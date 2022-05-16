package com.aixi.lv.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.aixi.lv.model.constant.Symbol;
import com.google.common.collect.Lists;

/**
 * @author Js

 */
public class CombineUtil {

    /**
     * 获取所有的币种组合
     *
     * @param symbolList
     * @return
     */
    public static List<List<Symbol>> allSymbolCombine(List<Symbol> symbolList) {

        List<List<Symbol>> combineList = Lists.newArrayList();

        List<List<Integer>> allIndexList = allIndexCombine(symbolList.size() - 1);

        for (List<Integer> indexList : allIndexList) {

            List<Symbol> temp = Lists.newArrayList();
            for (Integer index : indexList) {
                temp.add(symbolList.get(index));
            }

            combineList.add(temp);
        }

        return combineList;

    }

    /**
     * 指定币种数量组合
     *
     * @param symbolList
     * @param num
     * @return
     */
    public static List<List<Symbol>> assignSymbolCombine(List<Symbol> symbolList, Integer num) {

        List<List<Symbol>> combineList = Lists.newArrayList();

        List<List<Integer>> allIndexList = combine(symbolList.size() - 1, num);

        for (List<Integer> indexList : allIndexList) {

            List<Symbol> temp = Lists.newArrayList();
            for (Integer index : indexList) {
                temp.add(symbolList.get(index));
            }

            combineList.add(temp);
        }

        return combineList;

    }

    private static List<List<Integer>> allIndexCombine(Integer maxIndex) {

        List<List<Integer>> result = new ArrayList<>();

        Integer size = maxIndex + 1;

        while (size > 2) {
            List<List<Integer>> combine = combine(maxIndex, size);

            result.addAll(combine);

            size--;
        }

        return result;

    }

    /**
     * 获取所有组合
     *
     * @param maxIndex 数组最大的脚标
     * @param k
     * @return
     */
    private static List<List<Integer>> combine(int maxIndex, int k) {

        List<List<Integer>> res = new ArrayList<>();

        Deque<Integer> path = new ArrayDeque<>();
        dfs(maxIndex, k, 0, path, res);
        return res;
    }

    private static void dfs(int n, int k, int begin, Deque<Integer> path, List<List<Integer>> res) {
        // 递归终止条件是：path 的长度等于 k
        if (path.size() == k) {
            res.add(new ArrayList<>(path));
            return;
        }

        // 遍历可能的搜索起点
        for (int i = begin; i <= n; i++) {
            // 向路径变量里添加一个数
            path.addLast(i);
            // 下一轮搜索，设置的搜索起点要加 1，因为组合数理不允许出现重复的元素
            dfs(n, k, i + 1, path, res);
            // 重点理解这里：深度优先遍历有回头的过程，因此递归之前做了什么，递归之后需要做相同操作的逆向操作
            path.removeLast();
        }
    }
}

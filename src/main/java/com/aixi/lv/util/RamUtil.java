package com.aixi.lv.util;

import org.apache.lucene.util.RamUsageEstimator;

/**
 * @author Js

 */
public class RamUtil {

    /**
     * 返回对象内存大小
     *
     * @param object
     * @return
     */
    public static String getRamSize(Object object) {

        long l = RamUsageEstimator.sizeOfObject(object);
        String s = RamUsageEstimator.humanReadableUnits(l);

        return s;
    }
}

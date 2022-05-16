package com.aixi.lv.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


import static com.aixi.lv.config.BackTestConfig.OPEN;
import static com.aixi.lv.config.MacdTradeConfig.THREAD_LOCAL_ACCOUNT;

/**
 * @author Js

 */
public class TimeUtil {

    public static LocalDateTime now() {

        LocalDateTime local;
        if (OPEN) {
            local = THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime();
        } else {
            local = LocalDateTime.now();
        }

        return local;
    }

    public static String getCurrentTime() {

        LocalDateTime local;
        if (OPEN) {
            local = THREAD_LOCAL_ACCOUNT.get().getCurBackTestComputeTime();
        } else {
            local = LocalDateTime.now();
        }

        return local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String getTime(LocalDateTime localDateTime) {

        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static Long localToLong(LocalDateTime localDateTime) {

        return localDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }
}

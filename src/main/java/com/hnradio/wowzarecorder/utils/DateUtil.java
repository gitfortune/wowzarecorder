package com.hnradio.wowzarecorder.utils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtil {

    /**
     *  判断是否是当天
     * @param str 格式为yyyy-mm-dd的时间字符串
     * @return
     */
    public static boolean isToday(String str){
        LocalDate lld1 = LocalDate.now();
        LocalDate lld2 = LocalDate.parse(str);
        if (lld1.equals(lld2)) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前时间是否与给定时间相等
     * @param str
     * @return
     */
    public static boolean equalsWith(String str){
        LocalTime now = LocalTime.now();
        LocalTime parse = LocalTime.parse(str);
        if(now.equals(parse)){
            return true;
        }
        return false;
    }

    /**
     * 计算给定时间的时间间隔,单位 毫秒
     */
    public static long timeDiff(String startTime,String endTime){
        LocalTime startParse = LocalTime.parse(startTime);
        LocalTime endParse = LocalTime.parse(endTime);
        return ChronoUnit.MILLIS.between(startParse, endParse);
//        long seconds = Duration.between(startParse, endParse).getSeconds();
    }

    /**
     * 根据pattern获得相应格式日期，如："20190230"
     * @return
     */
    public static String getDate(String pattern){
        return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
    }

}

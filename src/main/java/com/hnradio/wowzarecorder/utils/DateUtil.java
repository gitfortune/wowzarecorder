package com.hnradio.wowzarecorder.utils;

import java.time.LocalDate;
import java.time.LocalTime;

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

}

package com.navinfo.omqs.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @ClassName:     SystemDateTime.java
 * @author         qj
 * @version        V1.0
 * @Date           2023年4月17日 下午1:56:02
 * @Description:   时间工具类
 */
public class DateTimeUtil {
    // 时间字符串
    private static String systemDate;
    // 全部时间信息
    private static String fullTime;
    // 时间对象
    static Date date = null;

    /**
     * 获取时间日期
     * return
     */
    public static String getDate() {
        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        systemDate = simpleDateFormat.format(date);
        return systemDate;
    }

    /**
     * 时间常量转日期
     * @param time 时间常量
     * return
     */
    public static String getDateFromTime(long time) {
        date = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        systemDate = simpleDateFormat.format(date);
        return systemDate;
    }

    /**
     * 获取时分秒HH:mm:ss
     * return
     */
    public static String getFullTime() {
        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        fullTime = simpleDateFormat.format(date);
        return fullTime;
    }

    /**
     * 格式化时间常量为yyyyMMddHHmmss
     * @param time 时间常量
     * return
     */
    public static String getFullTimeFromTime(long time) {
        date = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        fullTime = simpleDateFormat.format(date);
        return fullTime;
    }

    /**
     * 获取时间信息 yyyy-MM-dd HH:mm:ss
     * return
     */
    public static String getDataTime() {
        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        fullTime = simpleDateFormat.format(date);
        return fullTime;
    }

    /**
     * 简要格式时间HH:mm
     * return
     */
    public static String getShotDataTime() {
        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "HH:mm");
        fullTime = simpleDateFormat.format(date);
        return fullTime;
    }

    /**
     * 获取时间信息 yyyyMMddHHmmss
     * return
     */
    public static String getTime() {
        date = new Date();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        fullTime = simpleDateFormat.format(date);

        return fullTime;
    }


    /**
     * 获取时间信息 yyyyMMddHH
     * return
     */
    public static String getTimeyyyyMMddHH() {
        date = new Date();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHH");

        fullTime = simpleDateFormat.format(date);

        return fullTime;
    }

    /**
     * 获取时间信息 yyyyMMddHHmmssSSS
     * return
     */
    public static String getTimeSSS() {
        Date date = new Date();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        fullTime = simpleDateFormat.format(date);

        return fullTime;
    }

    /**
     * 获取时间信息 yyyy-MM-dd HH:mm:ss:SSS
     * return
     */
    public static String getTimeMill() {
        date = new Date();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

        fullTime = simpleDateFormat.format(date);

        return fullTime;
    }

    /**
     * 获取时间信息 yyyy-MM-dd HH:mm:ss
     * return
     */
    public static Date getNowDateShort() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = formatter.parse(dateString, pos);
        return currentTime_2;
    }

    /**
     * 将yyyyMMddHHmmss转yyyy-MM-dd HH:mm:ss
     * @param date 时间字符串
     * return
     */
    public static String dateToSimpleDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 将yyyyMMddHHmmss转yyyy年MM月dd日
     * @param date 时间字符串
     * return
     */
    public static String dateToChineseDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy年MM月dd日");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss.SSS转换为yyyyMMddHHmmssSSS
     * return
     */
    public static String TimePointSSSToTime(String date) throws ParseException{
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 将yyyy.MM.dd/HH时转yyyy-MM-dd HH:mm:ss
     * @param date 时间字符串
     * return
     */
    public static String dateToSimpleDate2(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd/HH时");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String reTime = format2.format(dDate);
        return reTime;
    }


    /**
     * 将yyyyMMddHHmmss转yyyy.MM.dd/HH时
     * @param date 时间字符串
     * return
     */
    public static String dateToDialogTime(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy.MM.dd/HH时");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 将yyyyMMddHHmmss转yyyy-MM-dd HH:mm:ss:SSS
     * @param date 时间字符串
     * return
     */
    public static String dateToSimpleDateSSS(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 将yyyyMMddHHmmssSSS转yyyy-MM-dd HH:mm:ss:SSS
     * @param date 时间字符串
     * return
     */
    public static String iosDateToSimpleDateSSS(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 将yyyyMMddHHmmssSSS转yyyy-MM-dd HH:mm:ss
     * @param date 时间字符串
     * return
     */
    public static String iosDateToSimpleDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date dDate = format.parse(date);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String reTime = format2.format(dDate);
        return reTime;
    }

    /**
     * 获取当前时间yyyy-MM-dd HH:mm:ss
     * return
     */
    public static Date getNowDate() throws ParseException {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = formatter.parse(dateString);
        return currentTime_2;
    }

    /**
     * 将时间常量转字符串HHmmss.SSS
     * @param l 时间常量
     * return
     */
    public static String getTime(long l) {
        if (l > 0) {
            long utcL = l - 28800;
            SimpleDateFormat sdf = new SimpleDateFormat("HHmmss.SSS");
            return sdf.format(utcL);
        }
        return null;
    }


    /**
     * 将时间常量转字符串yyyyMMddHHmmssSSS
     * @param l 时间常量
     * return
     */
    public static String getDateSimpleTime(long l) {
        if (l > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            return sdf.format(l);
        }
        return null;
    }

    /**
     * 将时间常量转字符串yyyy-MM-dd HH:mm:ss:SSS
     * @param l 时间常量
     * return
     */
    public static String getDateSimpleTimeSSS(long l) {
        if (l > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            return sdf.format(l);
        }
        return null;
    }

    /**
     * 格式化时间
     *
     * @param l
     * @return yyyyMMddHH:mm:ss:SSS
     */
    public static String getDateTimeSSS(long l) {
        if (l > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH:mm:ss:SSS");
            return sdf.format(l);
        }
        return null;
    }


    /**
     * 将yyyyMMddHHmmss转long
     *
     * @param date
     * @return
     */
    public static long getTimeInfo(String date) {
        if (date == null || date.equals(""))
            return 0;
        try {
            String time = dateToSimpleDate(date);
            long timeL = getTime(time);
            return timeL;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss时间转常量
     *
     * @param date
     * @return
     */
    public static long getTime(String date) {
        long changeDate = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date currentDate = formatter.parse(date);
            changeDate = currentDate.getTime();
            if (changeDate != 0) {
                return changeDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss时间转常量
     *
     * @param date
     * @return
     */
    public static long getPicTime(String date) {
        long changeDate = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        try {
            Date currentDate = formatter.parse(date);
            changeDate = currentDate.getTime();
            if (changeDate != 0) {
                return changeDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss.SSS时间转常量
     *
     * @param date
     * @return
     */
    public static long getTimePointSSS(String date) {
        long changeDate = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            Date currentDate = formatter.parse(date);
            changeDate = currentDate.getTime();
            if (changeDate != 0) {
                return changeDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 将yyyyMMddHHmmss时间转常量
     *
     * @param date
     * @return
     */
    public static long getTimePoint(String date) {
        long changeDate = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            Date currentDate = formatter.parse(date);
            changeDate = currentDate.getTime();
            if (changeDate != 0) {
                return changeDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss:SSS时间转常量
     *
     * @param date
     * @return
     */
    public static long getTimeSSS(String date) {
        long changeDate = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        try {
            Date currentDate = formatter.parse(date);
            changeDate = currentDate.getTime();
            if (changeDate != 0) {
                return changeDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 将yyyyMMddHHmmssSSS时间转常量
     *
     * @param date
     * @return
     */
    public static long getTrackTimeSSS(String date) {
        long changeDate = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            Date currentDate = formatter.parse(date);
            changeDate = currentDate.getTime();
            if (changeDate != 0) {
                return changeDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 对比两个时间格式是否超过3000m
     *
     * @param startDate
     *          起始时间
     *@param endDate
     *          结束时间
     * @return  true 是 false 否
     */
    public static boolean isTimeOut(long startDate, long endDate)
            throws ParseException {
        long result = startDate - endDate;
        if (result > 300000 || result < -300000) {
            return true;
        }
        return false;
    }

    /**
     * 对比两个时间格式是否超过3000m
     *
     * @param startDate
     *          起始时间
     *@param endDate
     *          结束时间
     *@param dTime
     *             时间常量
     * @return  true 是 false 否
     */
    public static boolean isTimeOut(long startDate, long endDate, long dTime)
            throws ParseException {
        long result = startDate - endDate;
        if (result > dTime || result < -dTime) {
            return true;
        }
        return false;
    }

    /**
     * 对比两个时间间隔多少天yyyy-MM-dd
     *
     * @param date1
     *          起始时间
     *@param date2
     *          结束时间
     * @return
     *
     */
    public static int getDateSpan(String date1, String date2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date start = formatter.parse(date1);
            Date end = formatter.parse(date2);
            long span = end.getTime() - start.getTime();
            float day = span / ((float) 1000 * 3600 * 24);
            return (int) day;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }


    /**
     * 获取照片的时间
     * return
     */
    public static String getYYYYMMDDDate() {
        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        systemDate = simpleDateFormat.format(date);
        return systemDate;
    }

    /**
     * 获取照片的时间 yyyy年MM月dd日
     * return  YYYYMMDDD
     */
    public static String getYYYYMMDDDateChinese() {
        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        systemDate = simpleDateFormat.format(date);
        return systemDate;
    }

    /**
     * yyyy年MM月dd日 转date
     *
     * @param dateString
     * @return
     */
    public static Date stringYYYYMMDDHHToDate(String dateString) {
        try {
            SimpleDateFormat sim = new SimpleDateFormat("yyyy/MM/dd/HH时");
            Date ticDate = sim.parse(dateString);
            return ticDate;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 当前时间前几天时间
     * @param day
     *          天数
     * return
     */
    public static String getCurrentDateBefore(int day) {
        date = new Date();
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - day);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        systemDate = simpleDateFormat.format(now.getTime());
        return systemDate;
    }

    /**
     * 当前时间前几天时间
     * @param day
     *          天数
     * return
     */
    public static String getCurrentDateAfter(String time,int day) {
        try{
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHH");
            Date dDate = format.parse(time);
            Calendar now = Calendar.getInstance();
            now.setTime(dDate);
            now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            systemDate = simpleDateFormat.format(now.getTime());
        }catch (Exception e){

        }

        return systemDate;
    }

    /**
     * 获取时间
     * @param time
     *          时间
     * return yyyy.MM.dd
     */
    public static String getDateYYMMDD(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            Date dDate = format.parse(time);
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy.MM.dd");
            String reTime = format2.format(dDate);
            return reTime;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取时间
     * @param time
     *          时间
     * return yyyy.MM.dd
     */
    public static String getDateYYMMDDPoint(String time) throws Exception{

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date dDate = format.parse(time);

        SimpleDateFormat format2 = new SimpleDateFormat("yyyy.MM.dd");
        String reTime = format2.format(dDate);

        return reTime;

    }

    /**
     * 获取时间
     * @param time yyyyMMddHHmmss
     * return HH:mm:ss
     */
    public static String getDateHHMMSS(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            Date dDate = format.parse(time);
            SimpleDateFormat format2 = new SimpleDateFormat("HH:mm:ss");
            String reTime = format2.format(dDate);
            return reTime;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前起始时间
     * return yyyyMMddHHmmss
     */
    public static String getCurrentDataStartTimeStr() {

        return getYYYYMMDDDate() + "000000";
    }

    /**
     * 获取当天结束时间
     * return yyyyMMddHHmmss
     */
    public static String getCurrentDataEndTimeStr() {

        SimpleDateFormat format = null;
        Date date = null;
        Calendar myDate = Calendar.getInstance();
        myDate.add(Calendar.DAY_OF_MONTH, 1);
        date = myDate.getTime();
        format = new SimpleDateFormat("yyyyMMdd000000");

        return format.format(date);
    }


    public static int getDateSpanYYMMDDHHMMSS(String date1, String date2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            Date start = formatter.parse(date1);
            Date end = formatter.parse(date2);
            long span = end.getTime() - start.getTime();
            float ss = Math.abs(span);
            return (int) ss;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    public static int getDateSpanIOSYYMMDDHHMMSSSSS(String date1, String date2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            Date start = formatter.parse(date1);
            Date end = formatter.parse(date2);
            long span = end.getTime() - start.getTime();
            float ss = Math.abs(span);
            return (int) ss;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取时间
     * return
     */
    public static String toDateYYMMDDHHSSMM(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            Date dDate = format.parse(time);
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
            String reTime = format2.format(dDate);
            return reTime;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 字符串格式时间转成Date格式
     *
     * @param dateString
     * @return
     */
    public static Date stringToDate(String dateString) {
        try {
            SimpleDateFormat sim = new SimpleDateFormat("yyyyMMddHHmmss");
            Date ticDate = sim.parse(dateString);
            return ticDate;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 字符串格式时间转成Date格式
     *
     * @param dateString
     * @return
     */
    public static Date stringFullTimeToDate(String dateString) {
        try {
            SimpleDateFormat sim = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            Date ticDate = sim.parse(dateString);
            return ticDate;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 判断两个日期相差天数
     * 大的日期放后面
     *
     * @param date1
     * @param date2
     * @return
     */
    public static int differentDays(Date date1, Date date2) {
        int days = (int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24));
        return days;
    }
}

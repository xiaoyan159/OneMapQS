package com.navinfo.omqs.util;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.util.Log;

import com.navinfo.omqs.Constant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 带日志文件输入的，又可控开关的日志调试
 *
 * @author qj
 * @version 1.0
 * @data 2016-8-23
 */
public class CMLog {
    //CrashHandler实例    
    /**
     *
     */
    private static CMLog instance;
    /**
     *
     */
    private static Boolean MYLOG_SWITCH = true; //日志文件总开关
    /**
     *
     */
    private static Boolean MYLOG_WRITE_TO_FILE = true;//日志写入文件开关
    /**
     *
     */
    private static char MYLOG_TYPE = 'v';//输入日志类型，w代表只输出告警信息等，v代表输出所有信息
//    private static String MYLOG_PATH_SDCARD_DIR = FMConstant.USER_DATA_LOG_PATH;// 日志文件在sdcard中的路径
    /**
     *
     */
    private static int SDCARD_LOG_FILE_SAVE_DAYS = 0;// sd卡中日志文件的最多保存天数
    /**
     *
     */
    private static String MYLOGFILEName = "Log.txt";// 本类输出的日志文件名称
    /**
     *
     */
    private static String STACKLOGFILEName = "StackLog.txt";// 本类输出的日志文件名称
    /**
     *
     */
    private static SimpleDateFormat myLogSdf   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//日志的输出格式
    /**
     *
     */
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd-HH");//日志文件格式
    /**
     *
     */
    private static Context mContext;
    /**
     *
     */
    private static boolean flag;
    /**
     *
     */
    private static int count = 0;
    /**
     *
     */
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    /**
     * 获取CrashHandler实例 ,单例模式
     * @return
     */
    public static CMLog getInstance() {
        if (instance == null)
            instance = new CMLog();
        return instance;
    }

    /**
     * 初始化
     * @param context
     */
    public void init(Context context) {
        mContext = context;
    }

    /**
     * @param tag
     * @param msg
     */
    public static void w(String tag, Object msg) {//警告信息
        log(tag, msg.toString(), 'w');
    }

    /**
     * @param tag
     * @param msg
     */
    public static void e(String tag, Object msg) {//错误信息
        log(tag, msg.toString(), 'e');
    }

    /**
     * @param tag
     * @param msg
     */
    public static void d(String tag, Object msg) {//调试信息
        log(tag, msg.toString(), 'd');
    }

    /**
     * @param tag
     * @param msg
     */
    public static void i(String tag, Object msg) {//
        log(tag, msg.toString(), 'i');
    }

    /**
     * @param tag
     * @param msg
     */
    public static void v(String tag, Object msg) {
        log(tag, msg.toString(), 'v');
    }

    /**
     * @param tag
     * @param text
     */
    public static void w(String tag, String text) {
        log(tag, text, 'w');
    }

    /**
     * @param tag
     * @param text
     */
    public static void e(String tag, String text) {
        log(tag, text, 'e');
    }

    /**
     * @param tag
     * @param text
     */
    public static void d(String tag, String text) {
        log(tag, text, 'd');
    }

    /**
     * @param tag
     * @param text
     */
    public static void i(String tag, String text) {
        log(tag, text, 'i');
    }

    /**
     * @param tag
     * @param text
     */
    public static void v(String tag, String text) {
        log(tag, text, 'v');
    }

    /**
     * 根据tag, msg和等级，输出日志
     *
     * @param tag
     * @param msg
     * @param level
     * @return void
     * @since v 1.0
     */
    private static void log(String tag, String msg, char level) {
        if (MYLOG_SWITCH) {
            if ('e' == level && ('e' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) { // 输出错误信息
                Log.e(tag, msg);
            } else if ('w' == level && ('w' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) {
                Log.w(tag, msg);
            } else if ('d' == level && ('d' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) {
                Log.d(tag, msg);
            } else if ('i' == level && ('d' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) {
                Log.i(tag, msg);
            } else {
                Log.v(tag, msg);
            }
            if (MYLOG_WRITE_TO_FILE)
                writeLogtoFile(String.valueOf(level), tag, msg);
        }
    }

    /**
     * 打开日志文件并写入日志
     *
     * @param mylogtype
     * @param tag
     * @param text
     **/
    public static void writeLogtoFile(String mylogtype, String tag, String text) {
        if (!flag&&MYLOG_WRITE_TO_FILE) {
            flag = true;
            try {
                Log.e("qj", "日志写入0");
                // 新建或打开日志文件
                Date nowtime = new Date();
                String needWriteFiel = logfile.format(nowtime);
               // String needWriteMessage = myLogSdf.format(nowtime) + "    " + mylogtype
                String needWriteMessage =  simpleDateFormat.format(nowtime) + "    " + mylogtype
                        + "    " + count + "  " + tag + "    " + text + "\r\n";
                //输出内存使用情况
                ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                MemoryInfo memoryInfo = new MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
/*                needWriteMessage += " 系统剩余内存: " + (memoryInfo.availMem / 1024) + "(KB)\n";
                needWriteMessage += " 系统是否处于低内存运行： " + memoryInfo.lowMemory + "\n";
                needWriteMessage += " 当系统剩余内存低于： " + (memoryInfo.threshold / 1024) + "(KB)\n";*/
                if (new File(Constant.USER_DATA_LOG_PATH).exists() == false) {
                    new File(Constant.USER_DATA_LOG_PATH).mkdirs();
                }
                File file = new File(Constant.USER_DATA_LOG_PATH, needWriteFiel + MYLOGFILEName);
                Log.e("qj", "日志写入1");

                if (!file.exists())
                    file.createNewFile();
                Log.e("qj", "日志写入2");

                FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
                BufferedWriter bufWriter = new BufferedWriter(filerWriter);
                bufWriter.write(needWriteMessage);
                bufWriter.newLine();
                bufWriter.close();
                filerWriter.close();
                Log.e("qj", "日志写入结束");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                flag = false;
            }
        }

        count ++;

        if(count>10000){
            count = 0;
        }
    }

    /**
     * 写入调用栈的信息，包括文件名，行号，接口名称
     *
     * @param extMsg 扩展信息，如果不为NULL，则输出到栈信息之前
     */
    public static void writeStackLogtoFile(String extMsg) {// 新建或打开日志文件
        StackTraceElement[] eles = Thread.currentThread().getStackTrace();
        Date nowtime = new Date();
        String needWriteFiel = logfile.format(nowtime);
        String needWriteMessage = "[Java Stack]:" + myLogSdf.format(nowtime) + "\n";

        if(eles!=null&&eles.length>3){
            needWriteMessage += "\t file name :" + eles[3].getFileName() + "\n";
            needWriteMessage += "\t line num  :" + eles[3].getLineNumber() + "\n";
            needWriteMessage += "\t class name:" + eles[3].getClassName() + "\n";
            needWriteMessage += "\t method    :" + eles[3].getMethodName() + "\n";
        }

        if (extMsg != null && extMsg.length() > 0) {
            needWriteMessage += "\t extMsg    :" + extMsg + "\n";
        }

        if (new File(Constant.USER_DATA_LOG_PATH).exists() == false) {
            new File(Constant.USER_DATA_LOG_PATH).mkdirs();
        }
        File file = new File(Constant.USER_DATA_LOG_PATH, needWriteFiel
                + STACKLOGFILEName);
        try {
            FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
            bufWriter.close();
            filerWriter.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 删除制定的日志文件
     */
    public static void delFile() {// 删除日志文件
        String needDelFiel = logfile.format(getDateBefore());
        File file = new File(Constant.USER_DATA_LOG_PATH, needDelFiel + MYLOGFILEName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 得到现在时间前的几天日期，用来得到需要删除的日志文件名
     * @return
     */
    private static Date getDateBefore() {
        Date nowtime = new Date();
        Calendar now = Calendar.getInstance();
        now.setTime(nowtime);
        now.set(Calendar.DATE, now.get(Calendar.DATE)
                - SDCARD_LOG_FILE_SAVE_DAYS);
        return now.getTime();
    }

    //输出错误日志

    /**
     * 得到现在时间前的几天日期，用来得到需要删除的日志文件名
     * @param e
     * @param TAG
     */
    public static void writeStackLogtoFile(Exception e, String TAG) {
        String errorInfo = "";
        for (int i = 0; i < e.getStackTrace().length; i++) {
            errorInfo += e.getStackTrace()[i];
        }
        writeLogtoFile("异常崩溃", "异常", "异常信息：" + errorInfo);
    }

    /**
     * @param mylogWriteToFile
     */
    public static void setMylogWriteToFile(Boolean mylogWriteToFile) {
        MYLOG_WRITE_TO_FILE = mylogWriteToFile;
    }
}

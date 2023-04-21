package com.navinfo.collect.library.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtils {
    protected static final String XML_NAME = "cjzq_trade";
    protected static final String KEEP_NOTIFICATION_FLAG = "notification_flag";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(XML_NAME, Context.MODE_PRIVATE);
    }

    public static String getMsgNotificationFlag(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(KEEP_NOTIFICATION_FLAG, "1");
    }

    /**
     * 
     * @param context
     * @param flag
     */
    public static void saveMsgNotificationFlag(Context context, String flag) {
        SharedPreferences sp = getSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEEP_NOTIFICATION_FLAG, flag);
        editor.commit();
    }

    /**
     * 保存sp值
     * 
     * @param context
     * @param name
     * @param flag
     */
    public static void saveSpText(Context context, String key, String value) {
        SharedPreferences sp = getSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * 获取值
     * 
     * @param context
     * @param name
     * @return
     */
    public static String getSpText(Context context, String key) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(key, "");
    }

    /**
     * 移除值
     * 
     * @param context
     * @param name
     * @return
     */
    public static void removeSpText(Context context, String name) {
        SharedPreferences sp = getSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(name);
        editor.commit();
    }

    public static String getConnectIp(Context context, String key) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(key, "");
    }

    public static void saveConnectIp(Context context,String key, String ip) {
        SharedPreferences sp = getSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, ip);
        editor.commit();
    }

    public static String getConnectWifiName(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString("connectwifiname", "");
    }

    public static void saveConnectWifiName(Context context, String name) {
        SharedPreferences sp = getSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("connectwifiname", name);
        editor.commit();
    }
}

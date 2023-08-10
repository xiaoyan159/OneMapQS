package com.navinfo.collect.library.utils;

public class RealmDBParamUtils {
    private static int mtaskId = -1;

    public static int getTaskId() {
        return mtaskId;
    }

    public static void setTaskId(int taskId) {
        mtaskId = taskId;
    }
}

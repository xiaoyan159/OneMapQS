package com.navinfo.collect.library.utils;

import com.navinfo.collect.library.enums.DataLayerEnum;

import java.io.File;

import io.realm.RealmConfiguration;

public class MapParamUtils {

    private static int mtaskId = -1;

    private static RealmConfiguration mTaskConfig = null;

    private static DataLayerEnum dataLayerEnum = DataLayerEnum.ONLY_ENABLE_LAYERS;

    public static int getTaskId() {
        return mtaskId;
    }
    public static void setTaskId(int taskId) {
        mtaskId = taskId;
    }

    public static RealmConfiguration getTaskConfig() {
        return mTaskConfig;
    }

    public static void setTaskConfig(RealmConfiguration taskConfig) {
        mTaskConfig = taskConfig;
    }

    public static DataLayerEnum getDataLayerEnum() {
        return dataLayerEnum;
    }

    public static void setDataLayerEnum(DataLayerEnum dataLayerEnum) {
        MapParamUtils.dataLayerEnum = dataLayerEnum;
    }
}

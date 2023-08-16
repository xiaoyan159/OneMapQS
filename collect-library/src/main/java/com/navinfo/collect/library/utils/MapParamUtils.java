package com.navinfo.collect.library.utils;

import com.navinfo.collect.library.enums.DataLayerEnum;

public class MapParamUtils {

    private static int mtaskId = -1;

    private static DataLayerEnum dataLayerEnum = DataLayerEnum.ONLY_ENABLE_LAYERS;

    public static int getTaskId() {
        return mtaskId;
    }

    public static void setTaskId(int taskId) {
        mtaskId = taskId;
    }

    public static DataLayerEnum getDataLayerEnum() {
        return dataLayerEnum;
    }

    public static void setDataLayerEnum(DataLayerEnum dataLayerEnum) {
        MapParamUtils.dataLayerEnum = dataLayerEnum;
    }
}

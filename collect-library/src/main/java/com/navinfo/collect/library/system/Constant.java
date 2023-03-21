package com.navinfo.collect.library.system;

import android.os.Environment;

import com.navinfo.collect.library.map.source.RealmDBTileDataSource;

import org.oscim.tiling.OverzoomTileDataSource;

import java.util.HashMap;
import java.util.Map;

public class Constant {

    public static String SD_PATH = Environment.getExternalStorageDirectory() + "";
    public static String ROOT_PATH = SD_PATH + "/NavinfoCollect";

    public static String PHOTO_PATH = ROOT_PATH + "/image";


    public static double CONVERSION_FACTOR = 1000000d;


    public static void setVisibleTypeMap(Map<String, Boolean> visibleTypeMap) {
        Map<String, Boolean> HD_LAYER_VISIABLE_MAP= new HashMap<>();
        // 只记录不显示的类型
        if (visibleTypeMap!=null&&!visibleTypeMap.isEmpty()) {
            for (Map.Entry<String, Boolean> e:visibleTypeMap.entrySet()) {
                if (!e.getValue()) {
                    HD_LAYER_VISIABLE_MAP.put(e.getKey(), e.getValue());
                }
            }
        }
        HAD_LAYER_INVISIABLE_ARRAY = HD_LAYER_VISIABLE_MAP.keySet().toArray(new String[HD_LAYER_VISIABLE_MAP.keySet().size()]);
    }
    public static String[] HAD_LAYER_INVISIABLE_ARRAY;
    public static final int OVER_ZOOM = 23;
}


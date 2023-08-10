package com.navinfo.collect.library.system;

import android.os.Environment;

import com.navinfo.collect.library.map.source.RealmDBTileDataSource;

import org.oscim.tiling.OverzoomTileDataSource;

import java.util.HashMap;
import java.util.Map;

public class Constant {

    //服务地址
    public static String URL_BASE = "http://cmp-gateway-sp9-port.ayiqdpfs.cloud.app.ncloud.navinfo.com/";

    public static String MAP_PATH = Environment.getExternalStorageDirectory() + "/map/";

    public static void setVisibleTypeMap(Map<String, Boolean> visibleTypeMap) {
        Map<String, Boolean> HD_LAYER_VISIABLE_MAP = new HashMap<>();
        // 只记录不显示的类型
        if (visibleTypeMap != null && !visibleTypeMap.isEmpty()) {
            for (Map.Entry<String, Boolean> e : visibleTypeMap.entrySet()) {
                if (!e.getValue()) {
                    HD_LAYER_VISIABLE_MAP.put(e.getKey(), e.getValue());
                }
            }
        }
        HAD_LAYER_INVISIABLE_ARRAY = HD_LAYER_VISIABLE_MAP.keySet().toArray(new String[HD_LAYER_VISIABLE_MAP.keySet().size()]);
    }

    public static String[] HAD_LAYER_INVISIABLE_ARRAY;
    public static final int OVER_ZOOM = 20;
    public static final int MAX_ZOOM = 20;
    public static final int OMDB_MIN_ZOOM = 15;

    /**
     * 服务器地址
     */
    public static final String SERVER_ADDRESS = "http://fastmap.navinfo.com/";
}


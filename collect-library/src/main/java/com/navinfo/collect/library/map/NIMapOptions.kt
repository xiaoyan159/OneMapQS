package com.navinfo.collect.library.map

import com.navinfo.collect.library.system.Constant
import org.json.JSONObject


data class NIMapOptions(
    val showCompassControl: Boolean = true,//指南针是否显示
    val showZoomControl: Boolean = true, //是否显示zoom按钮
    val zoomLevel: Double = 13.0, /// 地图比例尺初始级别
    val coordinate: NICoordinate = NICoordinate(39.907375, 116.391349),
    val maxZoom: Int = Constant.MAX_ZOOM
) {
    companion object {
        fun fromJson(json: String): NIMapOptions {
            var options: NIMapOptions
            try {
                val json = JSONObject(json)
                val showCompassControl = json.optBoolean("showZoomControl", true)
                val showZoomControl = json.optBoolean("showZoomControl", true)
                val zoomLevel = json.optDouble("zoomLevel", 13.0)
                val geometryObject = json.optJSONObject("coordinate")
                var coor = NICoordinate.fromJson(geometryObject)
                if (coor == null)
                    coor == NICoordinate(39.907375, 116.391349)
                options = NIMapOptions(
                    showCompassControl = showCompassControl,
                    showZoomControl = showZoomControl,
                    zoomLevel = zoomLevel,
                    coordinate = coor!!,
                )
            } catch (e: Exception) {
                options = NIMapOptions()
            }
            return options;
        }

    }
}

data class NICoordinate(val latitude: Double, val longitude: Double) {
    companion object {
        fun fromJson(json: JSONObject): NICoordinate? {
            try {
                return NICoordinate(
                    json.optDouble("latitude"), json.optDouble("longitude")
                )
            } catch (e: Exception) {

            }
            return null;
        }
    }
}
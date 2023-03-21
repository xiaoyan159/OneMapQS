package com.navinfo.collect.library.map.flutter

import com.navinfo.collect.library.map.NIMapOptions
import com.navinfo.collect.library.map.NIMapView
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem

/**
 * 和flutter层进行数据传递时的数据格式转换
 */
class FlutterMapConversion {
    companion object {
        // flutter json 转成原生对象
        /**
         * 地图初始配置信息转换
         */
        fun flutterToNIMapOptions(args: String): NIMapOptions {

            return  NIMapOptions.fromJson(args)
//          return MoshiUtil.fromJson<NIMapOptions>(args)!!;
//            val moshi = Moshi.Builder().build()
//            val jsonAdapter: JsonAdapter<NIMapOptions> = moshi.adapter(NIMapOptions::class.java)
//            return jsonAdapter.fromJson(args)!!
        }


        //原生转给flutter
        /**
         * marker 转json
         */
        fun markerToJson(marker: MarkerItem): String {
            return "{\"id\":\"${marker.title}\",\"description\":\"${marker.description}\",\"geoPoint\":{\"longitude\":${marker.geoPoint.longitude},\"latitude\":${marker.geoPoint.latitude}}}"
        }

        /**
         * line转json
         */
        fun lineToJson(list: List<GeoPoint>): String {
            var jsonArray = JSONArray();
            for (point in list) {
                val jsonObject = JSONObject();
                jsonObject.put("latitude", point.latitude)
                jsonObject.put("longitude", point.longitude)
                jsonObject.put("latitudeE6", point.latitudeE6)
                jsonObject.put("longitudeE6", point.longitudeE6)
                jsonArray.put(jsonObject)
            }

            return jsonArray.toString()
        }

        /**
         * 地图转json
         */
        fun baseMapTypeToJson(list: Array<NIMapView.BASE_MAP_TYPE>): String {
            var jsonArray = JSONArray();
            //循环输出 值
            for (baseMapType in list) {
                val jsonObject = JSONObject();
                jsonObject.put("title", baseMapType.title)
                jsonObject.put("url", baseMapType.url)
                jsonObject.put("tilePath", baseMapType.tilePath)
                jsonArray.put(jsonObject)
            }
            return jsonArray.toString()
        }

        fun toGeoPointMapList(list: List<GeoPoint>): List<*> {
            val newList = mutableListOf<Map<*, *>>()
            for (item in list) {
                newList.add(
                    mapOf(
                        "latitude" to item.latitude,
                        "longitude" to item.longitude,
                        "latitudeE6" to item.latitudeE6,
                        "longitudeE6" to item.longitudeE6,
                    ),
                )
            }
            return newList
        }

    }
}
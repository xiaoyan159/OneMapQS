package com.navinfo.collect.library.data.flutter.flutterhandler

import android.content.Context
import android.util.Log
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.CustomLayerItem
import com.navinfo.collect.library.data.entity.DataLayerItemType
import com.navinfo.collect.library.data.handler.DataLayerHandler
import com.navinfo.collect.library.data.entity.LayerManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONObject

/**
 * 数据图层管理
 */
class FlutterDataLayerHandler(context: Context, dataBase: MapLifeDataBase) :
    DataLayerHandler(context, dataBase) {

    fun createDataLayerTable(call: MethodCall, result: MethodChannel.Result) {
        try {
            if (call.arguments is String) {
                val jsonObject = JSONObject(call.arguments as String)
                val layer = LayerManager()
                layer.id = jsonObject.optString("uuid")
                layer.projectId = jsonObject.optString("projectId")
                layer.geometryType = jsonObject.optInt("geometryType")
                layer.layerName = jsonObject.optString("layerName")
                layer.style = jsonObject.optString("style")
                layer.importTime = jsonObject.optString("import_time")
                layer.describe = jsonObject.optString("describe")
                layer.visibility = jsonObject.optBoolean("visibility")
                val jsonArrayItems = JSONArray(jsonObject.optString("layerItems"))
                layer.bundle = jsonArrayItems.toString()

                var itemList = mutableListOf<CustomLayerItem>();
                for (i in 0 until jsonArrayItems.length()) {
                    val jsonObjectItem: JSONObject = jsonArrayItems[i] as JSONObject;
                    val item = CustomLayerItem(
                        key = jsonObjectItem.optString("key"),
                        type = DataLayerItemType.values()[jsonObjectItem.optInt("type")],
                        title = jsonObjectItem.optString("title"),
                        describe = jsonObjectItem.optString("describe"),
                        itemBean = jsonObjectItem.optString("itemBean"),
                    )
                    itemList.add(item)
                }
//                val list = DataConversion.listMapToCustomLayerItemsList(itemList)
                createTable(layer, itemList) { res, _ ->
                    result.success(res)
                }
            } else {
                result.success(false)
            }
        } catch (e: Throwable) {
            e.message?.let { Log.e("jingo", it) }
            Log.e("jingo", e.stackTraceToString())
            Log.e("jingo", e.toString())
            result.success(false)
        }
    }

    /**
     * 获取全部数据图层信息
     */
    fun getDataLayerList(call: MethodCall, result: MethodChannel.Result) {
        getDataLayerList { list ->
            val newList = mutableListOf<Map<*, *>>()
            for (item in list) {
                val map = mapOf(
                    "uuid" to item.id,
                    "projectId" to item.projectId,
                    "geometryType" to item.geometryType,
                    "layerName" to item.layerName,
                    "import_time" to item.importTime,
                    "describe" to item.describe,
                    "visibility" to item.visibility,
                    "layerItems" to item.bundle,
                    "style" to item.style
                )
                newList.add(map)
            }
            result.success(newList)
        }
    }

    /**
     * 获取某个数据图层
     */
    fun getDataLayer(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is String) {
            getDataLayer(call.arguments as String) { layer ->
                result.success(
                    mapOf(
                        "uuid" to layer.id,
                        "projectId" to layer.projectId,
                        "geometryType" to layer.geometryType,
                        "layerName" to layer.layerName,
                        "import_time" to layer.importTime,
                        "describe" to layer.describe,
                        "visibility" to layer.visibility,
                        "layerItems" to layer.bundle,
                        "style" to layer.style
                    )
                )
            }
        } else {
            result.success("false")
        }

    }
}
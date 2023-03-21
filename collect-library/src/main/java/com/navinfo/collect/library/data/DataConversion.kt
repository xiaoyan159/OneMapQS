package com.navinfo.collect.library.data

import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import android.view.WindowInsetsAnimation.Bounds
import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel
import com.baidu.ai.edge.ui.view.model.OcrViewResultModel
import com.navinfo.collect.library.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * 和flutter层进行数据传递时的数据格式转换
 */
class DataConversion {
    companion object {
        /**
         * 将数据转成map形式-传给flutter使用
         */
        fun toElementMapList(list: List<Element>): List<Map<*, *>> {
            val newList = mutableListOf<Map<*, *>>()
            for (element in list) {
                newList.add(element.toMap())
            }
            return newList
        }

        /**
         * json转数据图层自定义子表的所有字段对象列表
         * （现在用于从数据库中提取数据图层自定义表所有字段信息时，主要用于原生层）
         */
        fun jsonToLayerItemsList(json: String): List<CustomLayerItem> {
            val list = mutableListOf<CustomLayerItem>()
            try {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val itemObject = jsonArray.getJSONObject(i)
                    val itemMap = jsonToCustomLayerItem(itemObject)
                    list.add(itemMap);
                }
            } catch (e: Throwable) {
                e.message?.let { Log.e("jingo", it) }
                Log.e("jingo", e.stackTraceToString())
            }
            return list
        }


        /**
         * 数据图层子表字段配置转Map
         * （主要原生层使用）
         */
        fun customLayerItemsToMapList(list: List<CustomLayerItem>?): List<Map<*, *>> {
            var newList = mutableListOf<Map<*, *>>()
            if (list == null) {
                return newList
            }

            for (item in list) {
                newList.add(item.toMap())
            }
            return newList
        }

        /**
         * 数据图层子表字段配置 json形式的数据 转成对象形式
         * （原生层主要用json，所以主要原生层使用）
         */
        private fun jsonToCustomLayerItem(itemObject: JSONObject): CustomLayerItem {
            return CustomLayerItem(
                key = itemObject.optString("key"),
                title = itemObject.optString("title"),
                type = DataLayerItemType.values()[itemObject.optInt("type")],
//                nullable = itemObject.optBoolean("nullable"),
//                primaryKey = itemObject.optBoolean("primaryKey"),
//                value = itemObject.opt("value"),
//                selectOptions = itemObject.optString("selectOptions"),
//                isMainName = itemObject.optBoolean("isMainName"),
                describe = itemObject.optString("describe"),
//                checkManagerList = mutableListOf()
                itemBean = ""
            )
        }

        /**
         * 数据图层子表字段配置转Map
         * （flutter层喜欢用map，所以主要用于flutter和原生交互使用）
         */
        private fun jsonToCustomLayerItem(itemMap: Map<*, *>): CustomLayerItem {
            val checkMapList = itemMap["checkMangerList"] as List<Map<*, *>>
            val checkManagerList = mutableListOf<CheckManager>()
            for (c in checkMapList) {
                checkManagerList.add(
                    CheckManager(
                        id = (c["id"] as Int).toLong(),
                        type = c["type"] as Int,
                        tag = c["tag"] as String,
                        regexStr = c["regexStr"] as String
                    )
                )
            }
            return CustomLayerItem(
                key = itemMap["key"] as String,
                title = itemMap["title"] as String,
                type = DataLayerItemType.values()[itemMap["type"] as Int],
//                nullable = itemMap["nullable"] as Boolean,
//                primaryKey = itemMap["primaryKey"] as Boolean,
//                value = itemMap["value"] as Any,
//                selectOptions = itemMap["selectOptions"] as String,
//                isMainName = itemMap["isMainName"] as Boolean,
                describe = itemMap["describe"] as String,
//                checkManagerList = checkManagerList
                itemBean = ""
            )
        }

        /**
         * 数据图层 map形式的字段数据集合转成对象集合
         * （主要用于flutter层与原生层数据交互时）
         */
        fun listMapToCustomLayerItemsList(itemList: List<Map<*, *>>): List<CustomLayerItem> {
            val list = mutableListOf<CustomLayerItem>()
            for (itemMap in itemList) {
                list.add(jsonToCustomLayerItem(itemMap));
            }
            return list
        }

        //        /**
//         * layerManager转化成map
//         */
//        fun layerManagerToMap(
//            layerManager: LayerManager,
//            itemsMap: List<Map<String, *>>
//        ): Map<*, *> {
//            return mapOf("layerManager" to layerManager.toMap(), "itemsList" to itemsMap);
//        }
        /**
         * 检查项转map给flutter
         */
        fun toCheckManagerMapList(list: List<CheckManager>): List<Map<*, *>> {
            val newList = mutableListOf<Map<*, *>>()
            for (check in list) {
                newList.add(
                    check.toMap()
                )
            }
            return newList
        }

        fun toOcrList(
            path: String,
            basePolygonResultModels: List<BasePolygonResultModel>
        ): Map<*, *> {
            val newList = mutableMapOf<String, Any>()
            newList["photo_path"] = path
            val list = mutableListOf<String>();
            for (model in basePolygonResultModels) {
                list.add(model.name)
            }
            newList["result"] = list
            return newList
        }

        /**
         * 将ocr识别结果返回给flutter
         */
        suspend fun toFlutterOcrList(
            list: List<Map<String, Any>>,
        ): List<Any> {
            val listR = mutableListOf<Any>()
            val job = MainScope().async(Dispatchers.IO) {
                Log.e("jingo", "OCR图像识别写CSV文件 ${Thread.currentThread().name}")
                for (item in list) {
                    if (item is Map) {
                        val map1 = mutableMapOf<String, Any>()
                        map1["path"] = item["path"]!!
                        map1["width"] = item["width"]!!
                        map1["height"] = item["height"]!!
                        var data = item["data"]
                        if (data is List<*>) {
                            val dataList = mutableListOf<Any>()
                            for (v in data) {
                                val map = mutableMapOf<String, Any>()
                                map["index"] = (v as OcrViewResultModel).index
                                map["bounds"] = rectToMap(v.bounds)
                                map["text"] = v.name
                                map["confidence"] = v.confidence
                                dataList.add(map)
                            }
                            map1["data"] = dataList
                        }
                        listR.add(map1)
                    }
                }
            }
            job.await()
            Log.e("jingo", "ORC 识别结果 ${listR.toString()}")
            return listR
        }

        private fun rectToMap(list: List<Point>): List<List<Int>> {
            val pointList = mutableListOf<List<Int>>()
            for (point in list) {
                val l = mutableListOf<Int>()
                l.add(point.x)
                l.add(point.y)
                pointList.add(l)
            }

            return pointList
        }
    }
}
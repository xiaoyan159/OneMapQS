package com.navinfo.collect.library.data.flutter.flutterhandler

import android.content.Context
import android.util.Log
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.RealmUtils
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.Element
import com.navinfo.collect.library.data.entity.LayerManager
import com.navinfo.collect.library.data.entity.Project
import com.navinfo.collect.library.data.flutter.FlutterDataProtocolKeys
import com.navinfo.collect.library.data.handler.DataElementHandler
import com.navinfo.collect.library.data.search.*
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject

/**
 * 数据操作
 */
class FlutterDataElementHandler(
    context: Context,
    methodChannel: MethodChannel,
    dataBase: MapLifeDataBase
) :
    DataElementHandler(context, dataBase), OnGetSearchDataResultListener {
    val _methodChannel = methodChannel

    init {
        setListener(this)
    }

    /**
     * 保存数据
     */
    fun saveElementData(call: MethodCall, result: MethodChannel.Result) = try {
        if (call.arguments is Map<*, *>) {
            val element = Element.fromMap(call.arguments as MutableMap<String, Any>?);
            val valueMap = call.argument<Map<String, Any>>("values")
            saveData(element, valueMap) { res, error ->
                if (res) {
                    result.success("$res")
                } else {
                    result.success(error)
                }
            }

        } else {
            result.success("数据格式错误")
        }
    } catch (e: Throwable) {
        e.message?.let { Log.e("jingo", it) }
        Log.e("jingo", e.stackTraceToString())
        Log.e("jingo", e.toString())
        result.success("数据格式错误")
    }

    /**
     * 删除数据
     */
    fun deleteElementData(call: MethodCall, result: MethodChannel.Result) = try {
        if (call.arguments is Map<*, *>) {
            val element = Element()
            element.layerId = call.argument("layerId")
            element.id = call.argument("uuid")
            element.geometry = call.argument("geometry")
            element.displayText = call.argument("displayText")
            element.displayStyle = call.argument("displayStyle")
            element.operationTime = call.argument("tOperateDate")
            element.tLifecycle = call.argument<Int>("tLifecycle")!!
            element.tStatus = call.argument<Int>("tStatus")!!
            deleteData(element) { res, message ->
                if (res) {
                    result.success("success")
                } else {
                    result.success(message)
                }
            };

        } else {
            result.success("数据格式错误")
        }
    } catch (e: Throwable) {
        e.message?.let { Log.e("jingo", it) }
        Log.e("jingo", e.stackTraceToString())
        Log.e("jingo", e.toString())
        result.success("数据格式错误")
    }

    /**
     * 复制数据
     */
    fun copyElementData(call: MethodCall, result: MethodChannel.Result) = try {
        if (call.arguments is Map<*, *>) {

        } else {
            result.success("数据格式错误")
        }
    } catch (e: Throwable) {
        e.message?.let { Log.e("jingo", it) }
        Log.e("jingo", e.stackTraceToString())
        Log.e("jingo", e.toString())
        result.success("数据格式错误")
    }


    fun importPbfData2Realm(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is List<*>) {
            val pbfFiles: List<String> = call.arguments as List<String>
            try {
                val importResult = RealmUtils.getInstance().importPbfData(pbfFiles)
                result.success(importResult)
            } catch (exeception: Exception) {
                result.error("-1", exeception.message, exeception)
            }
        }
    }

    /**
     * 捕捉数据
     */
    fun snapElementDataList(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is String) {
            snapElementDataList(call.arguments as String) {
                result.success(DataConversion.toElementMapList(it))
            }
        }
    }

    /**
     * 查询数据深度信息模板
     */
    fun queryElementDeepInfo(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is Map<*, *>) {
            val id = call.argument<String>("id")
            val layerId = call.argument<String>("layerId")
            if (id != null && layerId != null) {
                queryElementDeepInfo(id, layerId) { layerManager, itemList ->
                    if (layerManager != null) {
                        val map = layerManager.toMap()
                        map["layerItems"] = DataConversion.customLayerItemsToMapList(itemList)
                        result.success(map)
                    } else {
                        result.error("-1","没有这条数据的拓展模板","")
                    }
                }
            }
        }
    }

    /**
     * 根据渲染名称查询数据
     */
    fun searchData(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is String) {
            try {
                val jsonObject = JSONObject(call.arguments as String)
                val keyword = jsonObject.getString("keyword")
                val pageNum = jsonObject.getInt("pageNum")
                val pageCapacity = jsonObject.getInt("pageCapacity")
                val startTime = jsonObject.getInt("startTime")
                val endTime = jsonObject.getInt("endTime")
                val projectArray = jsonObject.getJSONArray("projectItemList")
                val layerArray = jsonObject.getJSONArray("layerItemList")
                val fieldArray = jsonObject.getJSONArray("fieldItemList")
                val projectItemList = ArrayList<OptionProjectItem>()
                val layerItemList = ArrayList<OptionLayerItem>()
                val fieldItemList = ArrayList<OptionFieldItem>()
                for (i in 0 until projectArray.length()) {
                    val item = OptionProjectItem(projectArray.getString(i))
                    projectItemList.add(item)
                }
                for (i in 0 until layerArray.length()) {
                    val layerObject = layerArray.getJSONObject(i)
                    val item = OptionLayerItem(
                        layerObject.optString("projectId"),
                        layerObject.optString("layerId")
                    )
                    layerItemList.add(item)
                }
                for (i in 0 until fieldArray.length()) {
                    val fieldObject = fieldArray.getJSONObject(i)
                    val item = OptionFieldItem(
                        fieldObject.optString("projectId"),
                        fieldObject.optString("layerId"),
                        fieldObject.optString("fieldName")
                    )
                    fieldItemList.add(item)
                }
                val option =
                    SearchDataOption.Builder().setKeyword(keyword).setStartTime(startTime)
                        .setEndTime(endTime).setFieldItems(fieldItemList)
                        .setLayerItems(layerItemList).setProjectItems(projectItemList)
                        .setPageNum(pageNum).setPageCapacity(pageCapacity).build()
                searchData(option) {
//                    result.success(DataConversion.toElementMapList(it))
                }
                result.success("");
            } catch (e: Exception) {
                println("$e")
                result.error("-1", "查询参数解析错误", "$e")
            }

        }
    }

    /**
     * 获取检查项列表
     */
    fun queryCheckManagerList(call: MethodCall, result: MethodChannel.Result) {
        queryCheckManagerList() { list ->
            result.success(DataConversion.toCheckManagerMapList(list));
        }
    }


    /**
     * 根据id获取检查项列表
     */
    fun queryCheckManagerListByIds(call: MethodCall, result: MethodChannel.Result) {
        queryCheckManagerList() { list ->
            result.success(DataConversion.toCheckManagerMapList(list));
        }
    }



    override fun onGetElementResult(elementList: List<Element>) {
        _methodChannel.invokeMethod(
            FlutterDataProtocolKeys.DataElementProtocol.kDataSearchData,
            DataConversion.toElementMapList(elementList)
        )
    }

    override fun onGetLayerResult(layer: LayerManager) {
    }

    override fun onGetProjectResult(project: Project) {
    }

    override fun onError(msg: String, keyword: String) {
    }


}
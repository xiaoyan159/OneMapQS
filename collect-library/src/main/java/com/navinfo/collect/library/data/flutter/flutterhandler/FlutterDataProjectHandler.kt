package com.navinfo.collect.library.data.flutter.flutterhandler

import android.content.Context
import android.util.Log
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.handler.DataProjectHandler
import com.navinfo.collect.library.data.entity.Project
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * 项目管理
 */
class FlutterDataProjectHandler(context: Context, dataBase: MapLifeDataBase) :
    DataProjectHandler(context, dataBase) {

    fun saveProject(call: MethodCall, result: MethodChannel.Result) {
        try {
            if (call.arguments is Map<*, *>) {
//                "style": style,
                val project = Project();
                //项目名称

                project.id = call.argument("uuid")
                project.name = call.argument("name")
                project.createTime = call.argument("createTime")
                project.describe = call.argument("describe")
                project.visibility = call.argument<Boolean>("visibility") == false
                project.geometry = call.argument("geometry")
                project.geometryVisibility = call.argument<Boolean>("geometryVisibility") == false

                saveProject(project) { res, _ ->
                    result.success(res)
                };
            } else {
                result.success(false)
            }
        } catch (e: Throwable) {
            e.message?.let { Log.e("jingo", it) };
            Log.e("jingo", e.stackTraceToString());
            Log.e("jingo", e.toString());
            result.success(false)
        }
    }

    fun getProjectList(call: MethodCall, result: MethodChannel.Result) {
        getProjectList { list ->
            val newList = mutableListOf<Map<*, *>>()
            for (item in list) {
                val map = mapOf(
                    "uuid" to item.id,
                    "name" to item.name,
                    "createTime" to item.createTime,
                    "describe" to item.describe,
                    "visibility" to item.visibility,
                    "geometry" to item.geometry,
                    "geometryVisibility" to item.geometryVisibility
                )
                newList.add(map)
            }
            result.success(newList)
        };
    }
}
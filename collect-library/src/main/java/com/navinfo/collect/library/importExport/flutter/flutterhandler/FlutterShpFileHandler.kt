package com.navinfo.collect.library.importExport.flutter.flutterhandler

import android.content.Context
import android.os.Environment
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.importExport.handler.ShpFileHandler
import com.navinfo.collect.library.system.Constant
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterShpFileHandler(
    context: Context,
    methodChannel: MethodChannel,
    dataBase: MapLifeDataBase,
) : ShpFileHandler(context, dataBase) {
    /**
     * 获取shp文件属性信息
     */
    fun getImportShpFileInfo(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is String) {
//                .path + "/yongfeng/yongfengpolygon.shp"
//            val path = call.arguments["path"];
            getImportShpFileInfo("${call.arguments}") { bSuccess, message ->
                if (bSuccess) {
                    result.success(message)
                } else {
                    result.error("1", message, "")
                }
            }

        }
    }

    /**
     * 导入shp文件数据
     */
    fun importShpData(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is Map<*, *>) {
            var path = "";
            var layerId = "";
            if (call.hasArgument("path")) {
                path = call.argument<String>("path").toString()
//                path = Environment.getExternalStorageDirectory()
//                    .path + "/yongfeng/yongfengpolygon.shp"
            }
            if (call.hasArgument("layerId")) {
                layerId = call.argument<String>("layerId").toString()
            }
            if (path.isNotEmpty() && layerId.isNotEmpty()) {
                importShpData("$path", layerId) { bSuccess, message ->
                    if (bSuccess) {
                        result.success(true)
                    } else {
                        result.error("-1", message, "")
                    }
                }
            }
        }
    }

}
package com.navinfo.collect.library.importExport.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.importExport.handler.ImportFileHandler
import com.navinfo.collect.library.system.Constant
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterImportFileHandler(
    context: Context,
    methodChannel: MethodChannel,
    dataBase: MapLifeDataBase,
) : ImportFileHandler(context, dataBase) {
    /**
     * 获取shp文件属性信息
     */
    fun importCheckData(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is String) {
            importCheckFileInfo("${call.arguments}") { bSuccess, message ->
                if (bSuccess) {
                    result.success(message)
                } else {
                    result.error("1", message, "")
                }
            }

        }
    }

}
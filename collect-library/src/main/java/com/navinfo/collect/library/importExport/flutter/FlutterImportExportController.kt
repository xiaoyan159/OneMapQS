package com.navinfo.collect.library.importExport.flutter

import android.content.Context
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.importExport.ImportExportController
import com.navinfo.collect.library.importExport.flutter.flutterhandler.FlutterImportFileHandler
import com.navinfo.collect.library.importExport.flutter.flutterhandler.FlutterShpFileHandler
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterImportExportController(
    id: Int,
    context: Context,
    binaryMessenger: BinaryMessenger,
    dataBase: MapLifeDataBase,
) : ImportExportController(context, dataBase), MethodChannel.MethodCallHandler {

    private val mMethodChannel: MethodChannel = MethodChannel(
        binaryMessenger,
        "com.navinfo.collect/importOrExport_$id"
    )

    private val flutterShpFileHandler: FlutterShpFileHandler =
        FlutterShpFileHandler(context, mMethodChannel, mDataBase)

    private val flutterImportFileHandler: FlutterImportFileHandler =
        FlutterImportFileHandler(context, mMethodChannel, mDataBase)

    init {
        mMethodChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            /**
             * shp文件操作
             */
            //获取shp文件信息
            FlutterImportExportProtocolKeys.ShpFileProtocol.kGetImportShpFileInfo -> {
                flutterShpFileHandler.getImportShpFileInfo(call, result)
            }
            //导入shp数据
            FlutterImportExportProtocolKeys.ShpFileProtocol.kImportShpData -> {
                flutterShpFileHandler.importShpData(call, result)
            }

            //导入检查项数据
            FlutterImportExportProtocolKeys.MainProtocol.kGetImportCheckFileInfo ->{
                flutterImportFileHandler.importCheckData(call,result)
            }
        }
    }
}
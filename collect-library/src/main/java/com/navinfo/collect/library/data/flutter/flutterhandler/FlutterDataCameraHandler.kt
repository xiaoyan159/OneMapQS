package com.navinfo.collect.library.data.flutter.flutterhandler

import android.content.Context
import android.util.Log
import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel
import com.baidu.ai.edge.ui.view.model.OcrViewResultModel
import com.navinfo.collect.FlutterBaseActivity
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.flutter.FlutterDataProtocolKeys
import com.navinfo.collect.library.data.handler.DataCameraHandler
import com.navinfo.collect.library.data.handler.OnOCRBatchListener
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterDataCameraHandler(
    context: Context,
    methodChannel: MethodChannel,
    activity: FlutterBaseActivity,
    dataBase: MapLifeDataBase
) :
    DataCameraHandler(context, activity, dataBase) {
    private val _methodChannel = methodChannel
    fun openCamera(call: MethodCall, result: MethodChannel.Result) {
        super.openCamera()
        result.success(true)
    }

    fun sendOcrResults(path: String, basePolygonResultModels: List<BasePolygonResultModel>) {

        _methodChannel.invokeMethod(
            FlutterDataProtocolKeys.DataCameraProtocol.kDataOCRResults,
            DataConversion.toOcrList(path, basePolygonResultModels)
        )
    }

    fun ocrBatch(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is String) {
            super.ocrBatch(
                call.arguments as String,
                object : OnOCRBatchListener {
                    override fun onProgress(total: Int, current: Int) {
                        Log.e("jingo", "OCRManager 线程名称2 ${Thread.currentThread().name}")
                        Log.e("jingo", "OCR识别 当前第 $current 张,总共 $total 张");
                        val map = mutableMapOf<String, Int>()
                        map["total"] = total
                        map["current"] = current + 1
                        _methodChannel.invokeMethod(
                            FlutterDataProtocolKeys.DataCameraProtocol.kDataOCRBatchProgress,
                            map
                        )
                    }

                    override suspend fun onResult(list: List<Map<String, Any>>) {
                        Log.e("jingo", "OCRManager 线程名称2 ${Thread.currentThread().name}")
                        _methodChannel.invokeMethod(
                            FlutterDataProtocolKeys.DataCameraProtocol.kDataOCRBatchResults,
                            DataConversion.toFlutterOcrList(list)
                        )
                    }

                }
            )
        }
        result.success(true)
    }
}
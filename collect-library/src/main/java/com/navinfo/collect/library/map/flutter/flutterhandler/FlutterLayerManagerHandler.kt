package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.handler.LayerManagerHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * 图层操作控制
 */
class FlutterLayerManagerHandler(context: Context, mapView: NIMapView) :
    LayerManagerHandler(context, mapView) {

    //切换底图
    fun switchRasterTileLayer(call: MethodCall, result: MethodChannel.Result) {
        if (call.hasArgument("url") && call.hasArgument("tilePath")) {

        }
        val url = call.argument<String>("url")
        val tilePath = call.argument<String>("tilePath")
        val cache = call.argument<Boolean>("cache")
        switchRasterTileLayer(url, tilePath, cache)
    }

    fun removeRasterTileLayer(call: MethodCall, result: MethodChannel.Result) {
        if (call.hasArgument("url")) {
            val url = call.argument<String>("url")
            removeRasterTileLayer(url!!);
            result.success(true)
        }
    }

    //获取支持底图内容
    fun getBaseRasterTileLayerList(call: MethodCall, result: MethodChannel.Result) {
        getBaseRasterTileLayerList { baseMapJson ->
            result.success(baseMapJson)
        }
    }


    //编辑线或者面
    fun editLineOrPolygon(call: MethodCall, result: MethodChannel.Result) {
        if (call.hasArgument("type")&&call.hasArgument("geometry")) {
            val type = call.argument<Int>("type")
            val geometry = call.argument<String>("geometry")
            if (type != null) {
                editLineOrPolygon(geometry!!,type)
            }
        }
    }
}
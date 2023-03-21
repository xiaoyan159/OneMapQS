package com.navinfo.collect.library.map.flutter.maphandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.maphandler.MeasureLayerHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * 测距操作控制
 */
class FlutterMeasureLayerHandler(context: Context, mapView: NIMapView) :
    MeasureLayerHandler(context, mapView) {

    /**
     * 给面数据增加一个点，如果是经纬度就用经纬度，如果是屏幕坐标就转换成经纬度
     */
    fun drawLineOrPolygon(call: MethodCall, result: MethodChannel.Result) {

        if (call.hasArgument("type")) {
            val type = call.argument<Int>("type")
            if (type != null) {
                drawLineOrPolygon(type)
            }
        }
    }


    fun clean(call: MethodCall, result: MethodChannel.Result) {
        clean()
        result.success(true)
    }
}
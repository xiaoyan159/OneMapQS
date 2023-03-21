package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.flutter.FlutterMapConversion
import com.navinfo.collect.library.map.handler.LineHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.oscim.core.GeoPoint

/**
 * 线数据操作控制
 */
class FlutterLineHandler(context: Context, mapView: NIMapView) :
    LineHandler(context, mapView) {

    /**
     * 给线数据增加一个点，如果是经纬度就用经纬度，如果是屏幕坐标就转换成经纬度
     */
    fun addDrawLinePoint(call: MethodCall, result: MethodChannel.Result) {
        val list = addDrawLinePoint(
            GeoPoint(
                mMapView.vtmMap.mapPosition.latitude,
                mMapView.vtmMap.mapPosition.longitude
            )
        )
        result.success(FlutterMapConversion.lineToJson(list))
    }

    fun clean(call: MethodCall, result: MethodChannel.Result) {
        clean()
        result.success(true)
    }

    fun addDrawLine(call: MethodCall, result: MethodChannel.Result) {
        clean()
        val pointList = mutableListOf<GeoPoint>();
        if (call.arguments is List<*>) {
            for (item in call.arguments as List<*>) {
                if (item is Map<*, *>) {
                    pointList.add(GeoPoint(item["latitude"] as Double, item["longitude"] as Double))
                }
            }
        }
        addDrawLine(pointList);
        result.success(true)
    }
}
package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.flutter.FlutterMapConversion
import com.navinfo.collect.library.map.handler.PolygonHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.oscim.core.GeoPoint

/**
 * 面据操作控制
 */
class FlutterPolygonHandler(context: Context, mapView: NIMapView) :
    PolygonHandler(context, mapView) {

    /**
     * 给面数据增加一个点，如果是经纬度就用经纬度，如果是屏幕坐标就转换成经纬度
     */
    fun addDrawPolygonPoint(call: MethodCall, result: MethodChannel.Result) {
        val list = addDrawPolygonPoint(
            GeoPoint(
                mMapView.vtmMap.mapPosition.latitude,
                mMapView.vtmMap.mapPosition.longitude
            )
        )
        result.success(FlutterMapConversion.lineToJson(list));
    }
    /**
     * 给面数据增加一个点，如果是经纬度就用经纬度，如果是屏幕坐标就转换成经纬度
     */
    fun addDrawPolygonNiPoint(call: MethodCall, result: MethodChannel.Result) {

        if (call.hasArgument("longitude")&&call.hasArgument("latitude")) {

            val x = call.argument<Double>("longitude")
            val y = call.argument<Double>("latitude")

            val list = addDrawPolygonPoint(GeoPoint(y!!, x!!))

            result.success(FlutterMapConversion.lineToJson(list))
        }
    }

    fun addDrawPolygon(call: MethodCall, result: MethodChannel.Result) {
        clean()
        val pointList = mutableListOf<GeoPoint>();
        if (call.arguments is List<*>) {
            for (item in call.arguments as List<*>) {
                if (item is Map<*, *>) {
                    pointList.add(GeoPoint(item["latitude"] as Double, item["longitude"] as Double))
                }
            }
        }
        addDrawPolygon(pointList);
        result.success(true)
    }

    fun clean(call: MethodCall, result: MethodChannel.Result) {
        clean()
        result.success(true)
    }
}
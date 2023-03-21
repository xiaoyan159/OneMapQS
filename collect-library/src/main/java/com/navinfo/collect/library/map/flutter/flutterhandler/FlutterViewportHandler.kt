package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.handler.ViewportHandler
import com.navinfo.collect.library.utils.GeometryTools
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.oscim.core.GeoPoint

class FlutterViewportHandler(context: Context, mapView: NIMapView) :
    ViewportHandler(context, mapView) {
    /**
     * 设置地图中心偏移量 [-1,1]之间
     */
    fun setMapViewCenter(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is Map<*, *>) {
            val x = call.argument<Double>("xPivot")
            val y = call.argument<Double>("yPivot")
            val mapPosition = mMapView.vtmMap.mapPosition;
            setMapViewCenter(x!!.toFloat(), y!!.toFloat())
            mMapView.vtmMap.animator().animateTo(mapPosition)

        }
    }

    /**
     * 获取几何的外接矩形,返回矩形的左上，右下两个坐标
     */
    fun getBoundingBoxWkt(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is Map<*, *>) {
            val lat = call.argument<Double>("latitude")
            val lon = call.argument<Double>("longitude")
            val snapType = call.argument<Int>("snapType")
            val distance = call.argument<Int>("distance")
            val geoPoint = GeoPoint(lat!!, lon!!);

            val map = getBoundingBoxWkt(geoPoint, GeometryTools.SNAP_TYPE.values()[snapType!!], distance!!)

            result.success(map)

        }
    }

    /**
     * 坐标转屏幕点
     */
    fun toScreenPoint(call: MethodCall, result: MethodChannel.Result) {
        if (call.arguments is Map<*, *>) {
            val lat = call.argument<Double>("latitude")
            val lon = call.argument<Double>("longitude")
            val geoPoint = GeoPoint(lat!!, lon!!);

            val map = toScreenPoint(geoPoint)

            result.success(map)
        }
    }

    /**
     * 屏幕点转坐标
     */
    fun fromScreenPoint(call: MethodCall, result: MethodChannel.Result) {

        print("qj===fromScreenPoint")

        if (call.arguments is Map<*, *>) {
            val px = call.argument<Double>("px")
            val py = call.argument<Double>("py")

            print("qj===px:$px==py:$py")

            if(px!=null&&py!=null){
                val map = fromScreenPoint(px.toFloat(),py.toFloat())

                result.success(map)
            }else{
                result.success("")
            }
        }
    }
}
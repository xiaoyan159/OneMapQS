package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.handler.AnimationHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterAnimationHandler(
    context: Context,
    mapView: NIMapView
) :
    AnimationHandler(context, mapView) {


    /**
     * 使用像素移动地图
     */
    fun animation(call: MethodCall, result: MethodChannel.Result) {
        if (call.hasArgument("xPixel") && call.hasArgument("yPixel")) {
            val x = call.argument<Double>("xPixel")
            val y = call.argument<Double>("yPixel")
            var time = 200;
            if (call.hasArgument("animateDurationMs")) {
                time = call.argument<Int>("animateDurationMs")!!;
            }
            super.animationByPixel(x!!.toInt(), y!!.toInt(), time.toLong())
            result.success(true)
        } else if (call.hasArgument("latitude") && call.hasArgument("longitude")) {
            var time = 200;
            if (call.hasArgument("animateDurationMs")) {
                time = call.argument<Int>("animateDurationMs")!!;
            }
            animationByLonLat(
                call.argument<Double>("latitude") as Double,
                call.argument<Double>("longitude") as Double,
                time.toLong()
            )
            result.success(true)
        } else {
            result.error("-1", "像素坐标错误", "")
        }
    }

    /**
     * 地图缩小
     */
    fun zoomOut(call: MethodCall, result: MethodChannel.Result) {
        super.zoomOut()
        result.success(true)
    }

    /**
     * 地图放大
     */
    fun zoomIn(call: MethodCall, result: MethodChannel.Result) {
        super.zoomIn()
        result.success(true)
    }


}
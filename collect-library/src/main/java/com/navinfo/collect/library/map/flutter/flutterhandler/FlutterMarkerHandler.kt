package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.flutter.FlutterMapConversion
import com.navinfo.collect.library.map.handler.MarkHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem

class FlutterMarkerHandler(context: Context, mapView: NIMapView) :
    MarkHandler(context, mapView) {

    fun addMarker(call: MethodCall, result: MethodChannel.Result) {
        val marker: MarkerItem;
        val description = call.argument<String>("description")
        val title = call.argument<String>("title")
        if (call.hasArgument("xPixel") && call.hasArgument("yPixel")) {
            val x = call.argument<Double>("xPixel")
            val y = call.argument<Double>("yPixel")
            val description = call.argument<String>("description")
            val geoPoint =
                mMapView.vtmMap.viewport().fromScreenPoint(x!!.toFloat(), y!!.toFloat())
            marker = addMarker(geoPoint, title, description)
        } else if (call.hasArgument("longitude") && call.hasArgument("latitude")) {
            val lon = call.argument<Double>("longitude")
            val lat = call.argument<Double>("latitude")

            marker = addMarker(GeoPoint(lat!!, lon!!), title, description)
        } else {
            marker = addMarker(
                GeoPoint(
                    mMapView.vtmMap.mapPosition.latitude,
                    mMapView.vtmMap.mapPosition.longitude
                ), title, description
            )
        }
        result.success(FlutterMapConversion.markerToJson(marker));
    }

    fun removeMarker(call: MethodCall, result: MethodChannel.Result) {
        if (call.hasArgument("title")) {
            val title = call.argument<String>("title")
            removeMarker(title!!);
            result.success(true)
        }
    }

}
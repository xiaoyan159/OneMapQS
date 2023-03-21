package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NILocation
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.handler.LocationLayerHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.oscim.core.GeoPoint

class FlutterLocationLayerHandler(context: Context, mapView: NIMapView) :
    LocationLayerHandler(context, mapView) {

    var bFirst = true;

    /**
     * 设置当前定位信息
     */
    fun updateCurrentLocation(call: MethodCall, result: MethodChannel.Result) {
        var bTracking = true;
        if (call.hasArgument("tracking")) {
            bTracking = call.argument<Boolean>("tracking") == true
        }
        var locationMap = call.argument<Map<*, *>>("location");
        if (locationMap != null) {
            if (locationMap.containsKey("latitude") && locationMap.containsKey("longitude")) {
                val longitude = locationMap["longitude"]
                val latitude = locationMap["latitude"]
                val location = NILocation("GPS");
                if (latitude is Double && longitude is Double) {
                    location.latitude = latitude
                    location.longitude = longitude
                } else {
                    return result.success(false)
                }
                if (locationMap.containsKey("direction")) {
                    val direction = locationMap["direction"]
                    if (direction is Float)
                        location.bearing = direction
                }
                if (locationMap.containsKey("altitude")) {
                    val altitude = locationMap["altitude"]
                    if (altitude is Double)
                        location.altitude = altitude
                }
                if (locationMap.containsKey("radius")) {
                    val radius = locationMap["radius"]
                    if (radius is Float)
                        location.radius = radius
                }
                setCurrentLocation(location)
                if (bFirst || bTracking) {
                    mMapView.vtmMap.animator().animateTo(GeoPoint(latitude, longitude))
                    bFirst = false;
                }

            }
        }
        return result.success(true)
    }
}
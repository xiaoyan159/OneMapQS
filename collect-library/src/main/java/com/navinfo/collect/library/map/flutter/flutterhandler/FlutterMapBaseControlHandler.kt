package com.navinfo.collect.library.map.flutter.flutterhandler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.flutter.FlutterMapProtocolKeys
import com.navinfo.collect.library.map.handler.MapBaseControlHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.oscim.event.Gesture
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.Layer
import org.oscim.map.Map

class FlutterMapBaseControlHandler(
    context: Context,
    methodChannel: MethodChannel,
    mapView: NIMapView
) :
    MapBaseControlHandler(context, mapView) {
    init {
        mMapView.vtmMap.events.bind(Map.UpdateListener { e, _ ->
            when (e) {
                Map.MOVE_EVENT -> methodChannel.invokeMethod(
                    FlutterMapProtocolKeys.MapCallBackProtocol.kMapEventCallback,
                    mapOf("MapEvent" to 0)
                );
                Map.SCALE_EVENT -> methodChannel.invokeMethod(
                    FlutterMapProtocolKeys.MapCallBackProtocol.kMapEventCallback,
                    mapOf("MapEvent" to 1)
                );
                Map.ROTATE_EVENT -> methodChannel.invokeMethod(
                    FlutterMapProtocolKeys.MapCallBackProtocol.kMapEventCallback,
                    mapOf("MapEvent" to 2)
                );
                Map.TILT_EVENT -> methodChannel.invokeMethod(
                    FlutterMapProtocolKeys.MapCallBackProtocol.kMapEventCallback,
                    mapOf("MapEvent" to 3)
                );
            }
        })
        mMapView.layerManager.addLayer(
            "pointSnapLayer",
            MapEventsReceiver(methodChannel, mMapView.vtmMap)
        )

    }


    private class MapEventsReceiver(methodChannel: MethodChannel, map: Map?) : Layer(map),
        GestureListener {
        private val _methodChannel = methodChannel;
        override fun onGesture(g: Gesture, e: MotionEvent): Boolean {
            if (g is Gesture.Tap) {
                val p = mMap.viewport().fromScreenPoint(e.x, e.y)
                _methodChannel.invokeMethod(
                    FlutterMapProtocolKeys.MapCallBackProtocol.kMapOnClickCallback,
                    mapOf(
                        "latitude" to p.latitude,
                        "longitude" to p.longitude,
                        "longitudeE6" to p.longitudeE6,
                        "latitudeE6" to p.latitudeE6,
                    )
                )
            }
            return false
        }
    }

    /**
     * 刷新地图
     */
    fun updateMap(call: MethodCall, result: MethodChannel.Result) {
        var redraw = call.argument<Boolean>("redraw") ?: true
        upDateMap(redraw);
        result.success(true)
    }
}
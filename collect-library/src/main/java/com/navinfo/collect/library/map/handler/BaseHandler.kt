package com.navinfo.collect.library.map.handler

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.navinfo.collect.library.map.NIMapView
import org.oscim.layers.Layer

abstract class BaseHandler(context: AppCompatActivity, mapView: NIMapView) {
    protected val mContext: AppCompatActivity = context
    protected val mMapView: NIMapView = mapView

    fun addLayer(layer: Layer, groupType: NIMapView.LAYER_GROUPS) {
        Log.e("jingo", "增加了图层 ${layer.toString()}")
        mMapView.vtmMap.layers().add(
            layer,
            groupType.groupIndex
        )
    }

    fun removeLayer(layer: Layer) {
        Log.e("jingo", "移除了图层 ${layer.toString()}")
        mMapView.vtmMap.layers().remove(layer)
    }


//    fun setOnMapClickListener(listener: NIMapView.OnMapClickListener) {
//        mMapView.setOnMapClickListener(listener)
//    }
//
//    fun removeOnMapClickListener() {
//        mMapView.setOnMapClickListener(null)
//    }
}
package com.navinfo.collect.library.map.handler

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.navinfo.collect.library.map.NIMapView
import org.oscim.core.GeoPoint
import org.oscim.layers.Layer
import org.oscim.layers.marker.MarkerItem

abstract class BaseHandler(context: AppCompatActivity, mapView: NIMapView) {
    protected val mContext: AppCompatActivity = context
    protected val mMapView: NIMapView = mapView

    fun addLayer(layer: Layer, groupType: NIMapView.LAYER_GROUPS) {
        mMapView.vtmMap.layers().add(
            layer,
            groupType.groupIndex
        )
    }

    fun removeLayer(layer: Layer) {
        mMapView.vtmMap.layers().remove(layer)
    }

    fun setOnMapClickListener(listener: NIMapView.OnMapClickListener) {
        mMapView.setOnMapClickListener(listener)
    }

    fun removeOnMapClickListener() {
        mMapView.setOnMapClickListener(null)
    }

    abstract fun <T> mutableListOf(): MutableList<GeoPoint>
}
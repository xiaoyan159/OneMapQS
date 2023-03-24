package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import org.oscim.layers.Layer

abstract class BaseHandler(context: Context, mapView: NIMapView) {
    protected val mContext: Context = context
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

}
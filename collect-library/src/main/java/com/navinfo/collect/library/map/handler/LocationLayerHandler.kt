package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NILocation
import com.navinfo.collect.library.map.NIMapView

open class LocationLayerHandler(context: Context, mapView:NIMapView) :
    BaseHandler(context, mapView) {

    private var mCurrentLocation: NILocation? = null

    /**
     * 设置当前位置信息
     */
    fun setCurrentLocation(location: NILocation) {
        this.mCurrentLocation = location
        mMapView.layerManager.locationLayer.setPosition(
            location.latitude,
            location.longitude,
            location.radius
        )
    }
}
package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView

open class MapBaseControlHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView) {

    /**
     * 刷新地图
     */
    fun upDateMap(redraw: Boolean = true) {
        if (redraw) {
            mMapView.vtmMap.events.fire(org.oscim.map.Map.CLEAR_EVENT, mMapView.vtmMap.mapPosition)
        }
        mMapView.vtmMap.updateMap(redraw)
    }
}
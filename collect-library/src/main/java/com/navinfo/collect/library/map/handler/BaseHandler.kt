package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView

open class BaseHandler(context:Context, mapView: NIMapView) {
    protected val mContext:Context = context
    protected val mMapView: NIMapView = mapView
}
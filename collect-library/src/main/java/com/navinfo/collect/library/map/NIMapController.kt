package com.navinfo.collect.library.map

import android.content.Context
import com.navinfo.collect.library.data.entity.LayerManager
import com.navinfo.collect.library.map.handler.*
import com.navinfo.collect.library.map.maphandler.MeasureLayerHandler
import com.navinfo.collect.library.map.handler.ViewportHandler

/**
 *  地图控制器
 */

open class NIMapController(
    context: Context,
    options: NIMapOptions? = null,
    mapView: NIMapView? = null,
) {
    private val mContext = context
    var mMapView: NIMapView = mapView ?: NIMapView(mContext, options)

    val layerManagerHandler = LayerManagerHandler(mContext, mMapView)
    val locationLayerHandler by lazy { LocationLayerHandler(mContext, mMapView) }
    val animationHandler by lazy { AnimationHandler(mContext, mMapView) }
    val markerHandle by lazy { MarkHandler(mContext, mMapView) }
    val lineHandler by lazy { LineHandler(mContext, mMapView) }
    val polygonHandler by lazy { PolygonHandler(mContext, mMapView) }
    val viewportHandler by lazy { ViewportHandler(mContext, mMapView) }
    val measureLayerHandler by lazy { MeasureLayerHandler(mContext, mMapView) }

}


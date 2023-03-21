package com.navinfo.collect.library.map

import android.content.Context
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
    internal var mMapView: NIMapView =
        mapView ?: NIMapView(mContext, options)

    val animationHandler: AnimationHandler by lazy { AnimationHandler(mContext, mMapView) }
    val markerHandle: MarkHandler by lazy { MarkHandler(mContext, mMapView) }
    val locationLayerHandler: LocationLayerHandler by lazy {
        LocationLayerHandler(
            mContext,
            mMapView
        )
    };
    val lineHandler: LineHandler by lazy { LineHandler(mContext, mMapView) };
    val polygonHandler: PolygonHandler by lazy { PolygonHandler(mContext, mMapView) };
    val viewportHandler: ViewportHandler by lazy { ViewportHandler(mContext, mMapView) }
    val measureLayerHandler: MeasureLayerHandler by lazy { MeasureLayerHandler(mContext, mMapView) };


    /**
     * 刷新地图
     */
    fun upDateMap(redraw: Boolean = true) {
        if (redraw) {
            mMapView.vtmMap.events.fire(org.oscim.map.Map.CLEAR_EVENT, mMapView.vtmMap.mapPosition)
        }
        mMapView.vtmMap.updateMap(redraw)
    }

    open fun release() {
        mMapView.onDestroy()
    }
}


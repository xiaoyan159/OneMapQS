package com.navinfo.collect.library.map

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.handler.DataNiLocationHandler
import com.navinfo.collect.library.map.handler.*
import com.navinfo.collect.library.map.maphandler.MeasureLayerHandler
import com.navinfo.collect.library.map.handler.ViewportHandler
import com.navinfo.collect.library.system.Constant

/**
 *  地图控制器
 */
class NIMapController {

    lateinit var mMapView: NIMapView
    lateinit var layerManagerHandler: LayerManagerHandler
    lateinit var locationLayerHandler: LocationLayerHandler
    lateinit var animationHandler: AnimationHandler
    lateinit var markerHandle: MarkHandler
    lateinit var lineHandler: LineHandler
    lateinit var polygonHandler: PolygonHandler
    lateinit var viewportHandler: ViewportHandler
    lateinit var measureLayerHandler: MeasureLayerHandler

    fun init(context: AppCompatActivity, mapView: NIMapView, options: NIMapOptions? = null, mapPath: String) {
        Constant.MAP_PATH = mapPath
        layerManagerHandler = LayerManagerHandler(context, mapView)
        locationLayerHandler = LocationLayerHandler(context, mapView)
        animationHandler = AnimationHandler(context, mapView)
        markerHandle = MarkHandler(context, mapView)
        lineHandler = LineHandler(context, mapView)
        polygonHandler = PolygonHandler(context, mapView)
        viewportHandler = ViewportHandler(context, mapView)
        measureLayerHandler = MeasureLayerHandler(context, mapView)
        mMapView = mapView
        mapView.setOptions(options)
        mMapView.vtmMap.viewport().maxZoomLevel = Constant.MAX_ZOOM // 设置地图的最大级别
    }


}


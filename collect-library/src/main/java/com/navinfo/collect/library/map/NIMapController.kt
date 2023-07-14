package com.navinfo.collect.library.map

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.navinfo.collect.library.map.handler.*
import com.navinfo.collect.library.system.Constant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.oscim.core.GeoPoint

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

    val onMapClickFlow = MutableSharedFlow<GeoPoint>()

    fun init(
        context: AppCompatActivity,
        mapView: NIMapView,
        options: NIMapOptions? = null,
        mapPath: String,
        tracePath: String
    ) {
        Constant.MAP_PATH = mapPath
        layerManagerHandler = LayerManagerHandler(context, mapView, tracePath)
        locationLayerHandler = LocationLayerHandler(context, mapView)
        animationHandler = AnimationHandler(context, mapView)
        markerHandle = MarkHandler(context, mapView)
        lineHandler = LineHandler(context, mapView)
        polygonHandler = PolygonHandler(context, mapView)
        viewportHandler = ViewportHandler(context, mapView)
        measureLayerHandler = MeasureLayerHandler(context, mapView)
        mMapView = mapView
        mMapView.setOnMapClickListener {
            context.lifecycleScope.launch {
                onMapClickFlow.emit(it)
            }
        }
        mapView.setOptions(options)
    }


}


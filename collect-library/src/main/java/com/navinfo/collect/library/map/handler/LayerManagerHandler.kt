package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.NIMapView.LAYER_GROUPS
import org.oscim.layers.Layer

/**
 * Layer 操作
 */
open class LayerManagerHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView) {

    //增加RasterTileLayer
    fun switchRasterTileLayer(
        url: String?,
        filePath: String? = "",
        cache: Boolean? = false,
    ): Boolean {
        mMapView.removeBaseMap()
        val layer: Layer =
            mMapView.layerManager.getRasterTileLayer(mContext, url, filePath, cache!!)
        if (layer != null) {
            mMapView.vtmMap.layers().add(layer, LAYER_GROUPS.BASE_RASTER.groupIndex)
            mMapView.vtmMap.clearMap()
            return true
        }
        return false
    }

    //删除RasterTileLayer
    fun removeRasterTileLayer(url: String) {

    }
}
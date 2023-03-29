package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.utils.StringUtil
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem

/**
 * marker 操作
 */
open class MarkHandler(context: Context, mapView:NIMapView) :
    BaseHandler(context, mapView), OnItemGestureListener<MarkerInterface> {

//    //增加marker
//    fun addMarker(
//        geoPoint: GeoPoint,
//        title: String?,
//        description: String? = ""
//    ): MarkerItem {
//        var marker: MarkerItem? = null
//        for (e in mMapView.layerManager.defaultMarkerLayer.itemList) {
//            if (e is MarkerItem && e.title == title) {
//                marker = e;
//                break;
//            }
//        }
//        if (marker == null) {
//            var tempTitle = title;
//            if (tempTitle.isNullOrBlank()) {
//                tempTitle = StringUtil.createUUID();
//            }
//            val marker = MarkerItem(
//                tempTitle,
//                description,
//                geoPoint
//            )
//            mMapView.layerManager.defaultMarkerLayer.addItem(marker);
//            mMapView.vtmMap.updateMap(true)
//            return marker
//        } else {
//            marker.description = description
//            marker.geoPoint = geoPoint
//            mMapView.layerManager.defaultMarkerLayer.removeItem(marker)
//            mMapView.layerManager.defaultMarkerLayer.addItem(marker)
//            mMapView.vtmMap.updateMap(true)
//            return marker
//        }
//    }
//
//    fun removeMarker(title: String) {
//        var marker: MarkerItem? = null
//        for (e in mMapView.layerManager.defaultMarkerLayer.itemList) {
//            if (e is MarkerItem && e.title == title) {
//                marker = e;
//                break;
//            }
//        }
//        if (marker != null) {
//            mMapView.layerManager.defaultMarkerLayer.removeItem(marker)
//            mMapView.vtmMap.updateMap(true)
//        }
//    }
//
    override fun onItemSingleTapUp(index: Int, item: MarkerInterface): Boolean {
        return false
    }

    override fun onItemLongPress(index: Int, item: MarkerInterface): Boolean {
        return false
    }

}
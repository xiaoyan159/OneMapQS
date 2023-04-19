package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import com.navinfo.collect.library.R
import com.navinfo.collect.library.map.GeoPoint
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.utils.StringUtil
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol

/**
 * marker 操作
 */
class MarkHandler(context: AppCompatActivity, mapView: NIMapView) :
    BaseHandler(context, mapView) {

    //    //默认marker图层
    private var mDefaultMarkerLayer: ItemizedLayer

    init {
        //新增marker图标样式
        val mDefaultBitmap =
            AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.marker));

        val markerSymbol = MarkerSymbol(
            mDefaultBitmap,
            MarkerSymbol.HotspotPlace.BOTTOM_CENTER
        );
        //新增marker图层
        mDefaultMarkerLayer = ItemizedLayer(
            mapView.vtmMap,
            ArrayList<MarkerInterface>(),
            markerSymbol,
            object : OnItemGestureListener<MarkerInterface> {
                override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
                    return false
                }

                override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
                    return false
                }

            }
        )
        addLayer(mDefaultMarkerLayer, NIMapView.LAYER_GROUPS.OPERATE);
    }

    //增加marker
    fun addMarker(
        geoPoint: GeoPoint,
        title: String?,
        description: String? = ""
    ) {
        var marker: MarkerItem? = null
        for (e in mDefaultMarkerLayer.itemList) {
            if (e is MarkerItem && e.title == title) {
                marker = e
                break
            }
        }
        if (marker == null) {
            var tempTitle = title;
            if (tempTitle.isNullOrBlank()) {
                tempTitle = StringUtil.createUUID();
            }
            val marker = MarkerItem(
                tempTitle,
                description,
                org.oscim.core.GeoPoint(geoPoint.latitude, geoPoint.longitude)
            )
            mDefaultMarkerLayer.addItem(marker);
            mMapView.vtmMap.updateMap(true)
        } else {
            marker.description = description
            marker.geoPoint = org.oscim.core.GeoPoint(geoPoint.latitude, geoPoint.longitude)
            mDefaultMarkerLayer.removeItem(marker)
            mDefaultMarkerLayer.addItem(marker)
            mMapView.vtmMap.updateMap(true)
        }
    }

    fun removeMarker(title: String) {
        var marker: MarkerItem? = null
        for (e in mDefaultMarkerLayer.itemList) {
            if (e is MarkerItem && e.title == title) {
                marker = e
                break
            }
        }
        if (marker != null) {
            mDefaultMarkerLayer.removeItem(marker)
            mMapView.vtmMap.updateMap(true)
        }
    }

}
package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition

/**
 * 控制地图状态相关操作
 */
open class AnimationHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView) {


    /**
     * 根据屏幕像素移动
     *
     * xPixel： 屏幕横坐标，xPixel：屏幕纵坐标
     */
    fun animationByPixel(xPixel: Int, yPixel: Int, time: Long = 200) {
        val geoPoint =
            mMapView.vtmMap.viewport().fromScreenPoint(xPixel.toFloat(), yPixel.toFloat())
        mMapView.vtmMap.animator().animateTo(time, geoPoint)
    }


    /**
     * 根据经纬度移动
     *
     * latitude： ，longitude：经纬度
     **
     */
    fun animationByLonLat(latitude: Double, longitude: Double, time: Long = 200) {
        mMapView.vtmMap.animator().animateTo(time, GeoPoint(latitude, longitude))
    }

    /**
     * 根据经纬度，zoom值 移动
     *
     * latitude： ，longitude：经纬度
     **
     */
    fun animationByZoom(latitude: Double, longitude: Double, zoomLevel: Int) {
        val mapPosition: MapPosition = mMapView.vtmMap.mapPosition
        if (mapPosition.getZoomLevel() < zoomLevel) {
            mapPosition.setZoomLevel(zoomLevel)
        }
        mapPosition.setPosition(latitude, longitude)
        mMapView.vtmMap.animator().animateTo(300, mapPosition)
    }

    /**
     * 缩小地图
     */
    fun zoomOut() {
        val mapPosition: MapPosition = mMapView.vtmMap.mapPosition
        mapPosition.zoom = mapPosition.zoom - 1
        mMapView.vtmMap.animator().animateTo(mapPosition)
    }

    /**
     * 放大地图
     */
    fun zoomIn() {
        val mapPosition: MapPosition = mMapView.vtmMap.mapPosition
        mapPosition.zoom = mapPosition.zoom + 1
        mMapView.vtmMap.animator().animateTo(mapPosition)
    }

}
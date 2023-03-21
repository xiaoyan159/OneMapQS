package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.utils.GeometryTools
import org.oscim.core.GeoPoint
import org.oscim.core.Point

open class ViewportHandler(context: Context, mapView: NIMapView) : BaseHandler(context, mapView) {

    /**
     * Set pivot horizontal / vertical relative to view center in [-1, 1].
     * e.g. pivotY 0.5 is usually preferred for navigation, moving center to 25% of view height.
     * 地图中心点偏移
     */
    fun setMapViewCenter(xPivot: Float, yPivot: Float) {
        mMapView.vtmMap.viewport().setMapViewCenter(xPivot, yPivot)
    }

    /**
     * 获取几何的外接矩形,返回矩形的左上，右下两个坐标
     * @param snapType 扩展外接矩形的方式，用屏幕像素还是距离
     *  @param distance 距离大小 像素 或 米
     */
    fun getBoundingBoxWkt(
        geoPoint: GeoPoint,
        snapType: GeometryTools.SNAP_TYPE,
        distance: Int
    ): String {
        val array = GeometryTools.getBoundingBox(mMapView.vtmMap, geoPoint, distance, snapType)
        var minX = array[0].x
        var maxX = array[0].x
        var minY = array[0].y
        var maxY = array[0].y
        for (item in array) {
            if (item.x < minX) {
                minX = item.x
            }
            if (item.y < minY) {
                minY = item.y
            }
            if (item.y > maxY) {
                maxY = item.y
            }
            if (item.x > maxX) {
                maxX = item.x
            }
        }
        return "POLYGON(($minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY))"
    }

    /**
     * 获取几何的外接矩形,返回矩形的左上，右下两个坐标
     * @param snapType 扩展外接矩形的方式，用屏幕像素还是距离
     *  @param distance 距离大小 像素 或 米
     */
    fun toScreenPoint(
        geoPoint: GeoPoint
    ): String {
        val point = Point()

        mMapView.vtmMap.viewport().toScreenPoint(geoPoint, false, point)

        return "${point.x},${point.y}"
    }

    /**
     * 获取几何的外接矩形,返回矩形的左上，右下两个坐标
     * @param snapType 扩展外接矩形的方式，用屏幕像素还是距离
     *  @param distance 距离大小 像素 或 米
     */
    fun fromScreenPoint(
        px: Float, py: Float
    ): Map<String, Any> {

        val geo = mMapView.vtmMap.viewport().fromScreenPoint(px,py)

        return mapOf(
            "latitude" to geo.latitude,
            "longitude" to geo.longitude,
            "longitudeE6" to geo.longitudeE6,
            "latitudeE6" to geo.latitudeE6,
        )
    }
}
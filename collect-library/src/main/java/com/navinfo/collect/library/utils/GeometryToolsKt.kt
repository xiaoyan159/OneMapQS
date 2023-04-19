package com.navinfo.collect.library.utils

import com.navinfo.collect.library.system.Constant
import io.realm.RealmSet
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.oscim.core.MercatorProjection

class GeometryToolsKt {
    companion object {
        /**
         * 根据给定的geometry计算其横跨的20级瓦片Y值
         */
        fun getTileYByGeometry(wkt: String, tileYSet: MutableSet<Int?>) {

            val reader = WKTReader()
            val geometry = reader.read(wkt);

            var tileYSet: MutableSet<Int?>? = tileYSet
            val startTime = System.currentTimeMillis()
            if (tileYSet == null) {
                tileYSet = RealmSet()
            }
            tileYSet.clear()
            val envelope = geometry.envelope
            if (envelope != null) {
                val coordinates = envelope.coordinates
                // 最小最大x轴坐标，索引0位最小x值，索引1位最大y值
                if (coordinates != null && coordinates.isNotEmpty()) {
                    val minMaxY = doubleArrayOf(coordinates[0].y, coordinates[0].y)
                    for (coordinate in coordinates) {
                        // 获取最大和最小y的值
                        if (coordinate.y < minMaxY[0]) {
                            minMaxY[0] = coordinate.y
                        }
                        if (coordinate.y > minMaxY[1]) {
                            minMaxY[1] = coordinate.y
                        }
                    }
                    // 分别计算最大和最小x值对应的tile号
                    val tileY0 = MercatorProjection.latitudeToTileY(minMaxY[0], Constant.OVER_ZOOM.toByte())
                    val tileY1 = MercatorProjection.latitudeToTileY(minMaxY[1], Constant.OVER_ZOOM.toByte())
                    val minTileY = if (tileY0 <= tileY1) tileY0 else tileY1
                    val maxTileY = if (tileY0 <= tileY1) tileY1 else tileY0
                    println("getTileYByGeometry$envelope===$minTileY===$maxTileY")

                    for (i in minTileY..maxTileY) {
                        tileYSet.add(i)
                    }
                }
            }
            println("YGeometry-time:" + (System.currentTimeMillis() - startTime))
        }

        /**
         * 根据给定的geometry计算其横跨的20级瓦片X值
         */
        fun getTileXByGeometry(wkt: String, tileXSet: MutableSet<Int?>) {
            val reader = WKTReader()
            val geometry = reader.read(wkt);

            var tileXSet: MutableSet<Int?>? = tileXSet
            val startTime = System.currentTimeMillis()
            if (tileXSet == null) {
                tileXSet = RealmSet()
            }
            tileXSet.clear()
            if (geometry != null) {
                val envelope = geometry.envelope
                if (envelope != null) {
                    val coordinates = envelope.coordinates
                    // 最小最大x轴坐标，索引0位最小x值，索引1位最大x值
                    if (coordinates != null && coordinates.isNotEmpty()) {
                        val minMaxX = doubleArrayOf(coordinates[0].x, coordinates[0].x)
                        for (coordinate in coordinates) {
                            // 获取最大和最小x的值
                            if (coordinate.x < minMaxX[0]) {
                                minMaxX[0] = coordinate.x
                            }
                            if (coordinate.x > minMaxX[1]) {
                                minMaxX[1] = coordinate.x
                            }
                        }
                        // 分别计算最大和最小x值对应的tile号
                        val tileX0 = MercatorProjection.longitudeToTileX(minMaxX[0], Constant.OVER_ZOOM.toByte())
                        val tileX1 = MercatorProjection.longitudeToTileX(minMaxX[1], Constant.OVER_ZOOM.toByte())
                        val minTileX = if (tileX0 <= tileX1) tileX0 else tileX1
                        val maxTileX = if (tileX0 <= tileX1) tileX1 else tileX0
                        println("getTileXByGeometry$envelope$minTileX===$maxTileX")
                        for (i in minTileX..maxTileX) {
                            tileXSet.add(i)
                        }
                    }
                }
            }
            println("XGeometry-time:" + (System.currentTimeMillis() - startTime))
        }

        fun getMasterPoint(wkt: String): String {
            val reader = WKTReader()
            val geometry = reader.read(wkt);
            return "POINT(${geometry.centroid.x} ${
                geometry.centroid.y
            })"
        }
    }
}
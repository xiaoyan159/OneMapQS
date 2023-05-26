package com.navinfo.collect.library.map.layers

import android.graphics.Color
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.utils.GeometryTools
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.oscim.core.GeoPoint
import org.oscim.layers.vector.VectorLayer
import org.oscim.layers.vector.geometries.LineDrawable
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map

/**
 * Created by xiaoxiao on 2018/3/26.
 */
class MultiLinesLayer(map: Map) : VectorLayer(map) {

    private val linkList = mutableListOf<LineDrawable>()

    @Synchronized
    fun addLine(geometry: String, color: Int = Color.BLUE) {
        try {
            val style =
                Style.builder().fillColor(color).fillAlpha(0.5f).strokeColor(color)
                    .strokeWidth(6f).fixed(true).build()
            val lineDrawable =
                LineDrawable(GeometryTools.createGeometry(geometry), style)
            super.add(lineDrawable)
            linkList.add(lineDrawable)
        } catch (e: Exception) {

        }
    }

    @Synchronized
    fun clear() {
        for (item in linkList) {
            super.remove(item)
        }
        linkList.clear()
    }
}
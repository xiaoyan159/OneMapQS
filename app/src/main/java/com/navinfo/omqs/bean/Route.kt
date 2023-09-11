package com.navinfo.omqs.bean

import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import org.locationtech.jts.geom.Geometry
import org.oscim.core.GeoPoint

data class Route(
    val linkId: String,
    var sNode: String = "",
    var eNode: String = "",
    var direct: Int = 0,
    var name: String = "",
    var length: Double = 0.0,
) {
    var pointList: MutableList<GeoPoint> = mutableListOf()
        get() {
            return field
        }
        set(value) {
            length = GeometryTools.getDistance(value)
            field = value
        }
}
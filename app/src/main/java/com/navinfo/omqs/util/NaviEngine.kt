package com.navinfo.omqs.util

import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.bean.Route
import org.locationtech.jts.geom.LineString
import org.oscim.core.GeoPoint

class NaviEngine {
    var geometry: LineString? = null
    var routeList = mutableListOf<Route>()
        get() {
            return field
        }
        set(value) {
            val list = mutableListOf<GeoPoint>()
            list.addAll(value[0].pointList)
            for (i in 1 until value.size) {
                val list2 = value[i].pointList
                list2.removeAt(0)
                list.addAll(list2)
            }
            geometry = GeometryTools.createLineString(list)
            field = value
        }
}
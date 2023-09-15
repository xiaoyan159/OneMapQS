package com.navinfo.omqs.bean

import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.utils.GeometryTools
import org.oscim.core.GeoPoint

data class NaviRoute(
    //我是整条路径中的第几段路径
    var indexInPath: Int = -1,
    //link id
    val linkId: String,
    //起点id
    var sNode: String = "",
    //终点id
    var eNode: String = "",
    //方向
    var direct: Int = 0,
    //道路名称
    var name: String = "",
    //路段总长
    var length: Double = 0.0,
    //当前link在整段路径中的起点
    var startIndexInPath: Int = -1,
    //当前link在整段路径中的终点
    var endIndexIntPath: Int = -1,
    var itemList: MutableList<NaviRouteItem>? = null
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

data class NaviRouteItem(
    var index: Int,
    val data: RenderEntity,
    val linkId: String,
    var distance: Int = -1
)

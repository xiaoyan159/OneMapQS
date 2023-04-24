package com.navinfo.collect.library.map.cluster

import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem

/*
 *com.nmp.map.cluster
 *zhjch
 *2021/12/10
 *10:51
 *说明（）
 */
class ClusterMarkerItem(uid: Any, title: String?, description: String?, geoPoint: GeoPoint) :
    MarkerItem(uid, title, description, geoPoint) {
    var clusterList: List<Int> = ArrayList()
}
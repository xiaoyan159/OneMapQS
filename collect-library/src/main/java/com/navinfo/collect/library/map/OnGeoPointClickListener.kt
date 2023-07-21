package com.navinfo.collect.library.map

import org.oscim.core.GeoPoint

interface OnGeoPointClickListener : BaseClickListener {
    fun onMapClick(tag: String, point: GeoPoint)
}

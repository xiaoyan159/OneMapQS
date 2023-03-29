package com.navinfo.omqs.ui.activity

import android.os.Bundle
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.omqs.R

class MapTestActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_test)

        val mapController = NIMapController(context = this, mapView = findViewById<NIMapView>(R.id.main_activity_map1))
        mapController.locationLayerHandler.startLocation()
    }
}
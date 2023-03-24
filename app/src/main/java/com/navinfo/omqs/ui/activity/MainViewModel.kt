package com.navinfo.omqs.ui.activity

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.NIMapView

class MainViewModel : ViewModel(), DefaultLifecycleObserver {
    private lateinit var mapController: NIMapController

    fun initMap(context: Context, mapView: NIMapView) {
        mapController = NIMapController(context = context, mapView = mapView)

    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapController.locationLayerHandler.startLocation()
    }

    override fun onPause(owner: LifecycleOwner) {
        mapController.mMapView.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mapController.mMapView.onDestroy()
        mapController.locationLayerHandler.stopLocation()
    }

    override fun onResume(owner: LifecycleOwner) {
        mapController.mMapView.onResume()
    }

}
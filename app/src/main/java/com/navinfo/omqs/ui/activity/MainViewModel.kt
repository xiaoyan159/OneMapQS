package com.navinfo.omqs.ui.activity

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.NIMapView

class MainViewModel(val app: Application) : AndroidViewModel(app), DefaultLifecycleObserver {
    /**
     * 地图控制器
     */
    private lateinit var mapController: NIMapController

    /**
     * 初始化地图
     */
    fun initMap(context: Context, mapView: NIMapView) {
        mapController = NIMapController(context = app, mapView = mapView)

    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        //开启定位
        mapController.locationLayerHandler.startLocation()
    }

    override fun onPause(owner: LifecycleOwner) {
        mapController.mMapView.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mapController.mMapView.onDestroy()
        //结束定位
        mapController.locationLayerHandler.stopLocation()
    }

    override fun onResume(owner: LifecycleOwner) {
        mapController.mMapView.onResume()
    }

    /**
     * 点击我的位置，回到我的位置
     */
    fun onClickLocationButton() {
        mapController.locationLayerHandler.animateToCurrentPosition()
    }

}
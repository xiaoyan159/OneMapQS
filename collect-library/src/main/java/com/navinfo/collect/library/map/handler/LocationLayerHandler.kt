package com.navinfo.collect.library.map.handler

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.location.LocationClientOption.LocationMode
import com.navinfo.collect.library.map.GeoPoint
import com.navinfo.collect.library.map.NIMapView
import org.oscim.layers.LocationLayer


class LocationLayerHandler(context: AppCompatActivity, mapView: NIMapView) : BaseHandler(context, mapView) {

    private var mCurrentLocation: BDLocation? = null
    private var bFirst = true
    private val mLocationLayer: LocationLayer = LocationLayer(mMapView.vtmMap)
    private lateinit var locationClient: LocationClient

    init {
        ///添加定位图层到地图，[NIMapView.LAYER_GROUPS.NAVIGATION] 是最上层layer组
        addLayer(mLocationLayer, NIMapView.LAYER_GROUPS.NAVIGATION)
        //初始化定位
        initLocationOption()
    }

    /**
     * 初始化定位参数配置
     */
    private fun initLocationOption() {
        try {

            LocationClient.setAgreePrivacy(true)
            locationClient = LocationClient(mContext.applicationContext)
            //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
            //声明LocationClient类实例并配置定位参数
            val locationOption = LocationClientOption()
            val myLocationListener = MyLocationListener {
                //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
                //以下只列举部分获取经纬度相关（常用）的结果信息
                //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

                //获取纬度信息
                val latitude = it.latitude
                //获取经度信息
                val longitude = it.longitude
                //获取定位精度，默认值为0.0f
                val radius = it.radius
                //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
                val coorType = it.coorType
                //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
                val errorCode = it.locType
                mCurrentLocation = it
                mLocationLayer.setPosition(
                    it.latitude, it.longitude, it.radius
                )
                //第一次定位成功显示当前位置
                if (this.bFirst) {
                    animateToCurrentPosition(16.0)
                }

            }
            //注册监听函数
            locationClient.registerLocationListener(myLocationListener)
            //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            locationOption.locationMode = LocationMode.Hight_Accuracy
            //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            locationOption.setCoorType("gcj02")
            //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
            locationOption.setScanSpan(1000)
            //可选，设置是否需要地址信息，默认不需要
            locationOption.setIsNeedAddress(false)
            //可选，设置是否需要地址描述
            locationOption.setIsNeedLocationDescribe(false)
            //可选，设置是否需要设备方向结果
            locationOption.setNeedDeviceDirect(true)
            //可选，默认false，设置是否当Gnss有效时按照1S1次频率输出Gnss结果
            locationOption.isLocationNotify = true
            //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            locationOption.setIgnoreKillProcess(true)
            //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            locationOption.setIsNeedLocationDescribe(false)
            //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            locationOption.setIsNeedLocationPoiList(false)
            //可选，默认false，设置是否收集CRASH信息，默认收集
            locationOption.SetIgnoreCacheException(false)
            //可选，默认false，设置是否开启卫星定位
            locationOption.isOpenGnss = true
            //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
            locationOption.setIsNeedAltitude(true)
            //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
//        locationOption.setOpenAutoNotifyMode()
            //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
            locationOption.setOpenAutoNotifyMode(
                1000, 1, LocationClientOption.LOC_SENSITIVITY_HIGHT
            )
            //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
            locationClient.locOption = locationOption
        } catch (e: Throwable) {
            Toast.makeText(mContext, "定位初始化失败 $e", Toast.LENGTH_SHORT)
        }
    }

    /**
     * 开启定位
     */
    fun startLocation() {
        //开始定位
        if (!locationClient.isStarted) {
            locationClient.start()
        }
    }

    /**
     * 停止定位
     */
    fun stopLocation() {
        if (locationClient.isStarted) {
            locationClient.stop()
        }
    }

    /**
     * 回到当前位置
     */
    fun animateToCurrentPosition(zoom: Double) {

        mCurrentLocation?.run {
            val mapPosition = mMapView.vtmMap.mapPosition
            mapPosition.zoom = zoom
            mapPosition.setPosition(this.latitude, this.longitude)
            mMapView.vtmMap.animator().animateTo(300, mapPosition)
        }
    }

    fun animateToCurrentPosition() {
        mCurrentLocation?.run {
            val mapPosition = mMapView.vtmMap.mapPosition
            mapPosition.setPosition(this.latitude, this.longitude)
            mMapView.vtmMap.animator().animateTo(300, mapPosition)
        }
    }

    fun getCurrentGeoPoint(): GeoPoint? {
        mCurrentLocation?.let {
            return GeoPoint(it.latitude, it.longitude)
        }
        return null
    }
}

/**
 * 实现定位回调
 */
private class MyLocationListener(callback: (BDLocation) -> Unit) : BDAbstractLocationListener() {
    val call = callback;
    override fun onReceiveLocation(location: BDLocation) {
        call(location)
    }
}
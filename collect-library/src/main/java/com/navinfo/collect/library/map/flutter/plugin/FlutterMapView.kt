package com.navinfo.collect.library.map.flutter.plugin

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.navinfo.collect.library.data.flutter.FlutterDataController
import com.navinfo.collect.library.importExport.flutter.FlutterImportExportController
import com.navinfo.collect.library.map.flutter.FlutterMapController
import com.navinfo.collect.library.map.NIMapView
import io.flutter.plugin.platform.PlatformView


class FlutterMapView(
    mapController: FlutterMapController,
    dataController: FlutterDataController,
    importExportController: FlutterImportExportController,
    lifecycleProxy: LifecycleProxy
) :
    PlatformView, DefaultLifecycleObserver {
    private val mMapController: FlutterMapController = mapController
    private val mDataController: FlutterDataController = dataController
    private val mImportExportController: FlutterImportExportController = importExportController
    private val mapView: NIMapView = mMapController.mMapView
    private var mIsDisposed = false
    private val mLifecycleProxy: LifecycleProxy = lifecycleProxy;

    init {
        var lifecycle: Lifecycle? = mLifecycleProxy.getLifecycle()
        lifecycle?.addObserver(this)
    }

    override fun getView(): View {
        return mapView
    }

    override fun dispose() {

        if (mIsDisposed) {
            return
        }

        mIsDisposed = true

        mMapController.release()
        mDataController.release()

        val lifecycle: Lifecycle? = mLifecycleProxy.getLifecycle()
        lifecycle?.removeObserver(this)
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!mIsDisposed) {
            mapView.onPause()
        }
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!mIsDisposed) {
            mapView.onResume()
        }
        super.onResume(owner)
    }

    override fun onStop(owner: LifecycleOwner) {}

    override fun onDestroy(owner: LifecycleOwner) {
        if (!mIsDisposed) {
            mMapController.release()
            mDataController.release()
//            mapView = null
        }
    }
}
package com.navinfo.collect.library.map.flutter.plugin

import android.content.Context
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import com.navinfo.collect.library.map.NIMapCopyView
import com.navinfo.collect.library.map.NIMapView
import io.flutter.plugin.platform.PlatformView


class FlutterNiMapCopyView(context:Context,niMapView: NIMapView,
    lifecycleProxy: LifecycleProxy
) :
    PlatformView, DefaultLifecycleObserver {
    private val copyView: NIMapCopyView = NIMapCopyView(context,niMapView);
    private var mIsDisposed = false
    private val mLifecycleProxy: LifecycleProxy = lifecycleProxy;

    init {
        var lifecycle: Lifecycle? = mLifecycleProxy.getLifecycle()
        lifecycle?.addObserver(this)
    }

    override fun getView(): View {
        return copyView
    }

    override fun dispose() {

        if (mIsDisposed) {
            return
        }

        mIsDisposed = true
        val lifecycle: Lifecycle? = mLifecycleProxy.getLifecycle()
        lifecycle?.removeObserver(this)
    }
}
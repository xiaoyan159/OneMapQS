package com.navinfo.collect.library.map.flutter.plugin

import android.content.Context
import com.navinfo.collect.library.data.flutter.FlutterDataController
import com.navinfo.collect.library.map.NIMapOptions
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.flutter.FlutterMapConversion
import com.navinfo.collect.library.map.flutter.FlutterMapController
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class FlutterNiMapCopyViewFactory(
    private val messenger: BinaryMessenger,
    lifecycleProxy: LifecycleProxy
) :
    PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    private val binaryMessenger: BinaryMessenger = messenger

    private var mLifecycleProxy: LifecycleProxy = lifecycleProxy

    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        return FlutterNiMapCopyView(context,args as NIMapView, mLifecycleProxy)
    }

}

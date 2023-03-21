package com.navinfo.collect.library.map.flutter.plugin

import android.content.Context
import com.navinfo.collect.FlutterBaseActivity
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.flutter.FlutterDataController
import com.navinfo.collect.library.importExport.flutter.FlutterImportExportController
import com.navinfo.collect.library.map.NIMapOptions
import com.navinfo.collect.library.map.flutter.FlutterMapConversion
import com.navinfo.collect.library.map.flutter.FlutterMapController
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class FlutterMapViewFactory(
    messenger: BinaryMessenger,
    lifecycleProxy: LifecycleProxy,
    activity: FlutterBaseActivity,
) :
    PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    private val binaryMessenger: BinaryMessenger = messenger

    private var mLifecycleProxy: LifecycleProxy = lifecycleProxy

    lateinit var mapController: FlutterMapController
    lateinit var dataController: FlutterDataController
    private val activity: FlutterBaseActivity = activity

    override fun create(context: Context?, id: Int, args: Any?): PlatformView {
        mapController = buildMapController(context!!, id, args!!)
        dataController = buildDataController(context, id, activity)
        val importExportController =
            buildImportExportController(context, id, dataController.mDateBase)
        return FlutterMapView(
            mapController,
            dataController,
            importExportController,
            mLifecycleProxy
        )
    }

    /**
     * 创建地图控制器
     */
    private fun buildMapController(
        context: Context,
        viewId: Int,
        args: Any,
    ): FlutterMapController {

        val options: NIMapOptions = FlutterMapConversion.flutterToNIMapOptions(args.toString())
        return FlutterMapController(viewId, context, binaryMessenger, options)
    }

    /**
     * 创建数据控制器
     */
    private fun buildDataController(
        context: Context,
        viewId: Int,
        activity: FlutterBaseActivity
    ): FlutterDataController {
        return FlutterDataController(viewId, context, binaryMessenger, activity)
    }

    /**
     * 创建文件导入导出
     */
    private fun buildImportExportController(
        context: Context,
        viewId: Int,
        dataBase: MapLifeDataBase
    ): FlutterImportExportController {
        return FlutterImportExportController(viewId, context, binaryMessenger, dataBase)
    }
}

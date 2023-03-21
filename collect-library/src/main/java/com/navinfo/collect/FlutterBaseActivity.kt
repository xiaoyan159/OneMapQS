package com.navinfo.collect

import android.content.Intent
import android.util.Log
import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel
import com.navinfo.collect.library.map.flutter.plugin.FlutterMapViewFactory
import com.navinfo.collect.library.map.flutter.plugin.FlutterMapViewFlutterPlugin
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class FlutterBaseActivity : FlutterActivity(), CoroutineScope by MainScope()    {
    lateinit var factory: FlutterMapViewFactory
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        factory = FlutterMapViewFlutterPlugin.registerWith(flutterEngine, this);
//        FlutterNiMapCopyViewFlutterPlugin.registerWith(flutterEngine, this);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 0x10 && data != null) {
            val path = data.getStringExtra("photo_path")
            val list = data.getParcelableArrayListExtra<BasePolygonResultModel>("result_list")
            Log.e("jingo","OCR java 返回的数据："+ path + list.toString());
            if (path != null && list != null) {
                factory.dataController.flutterDataCameraHandler.sendOcrResults(
                    path,
                    list as List<BasePolygonResultModel>
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //协程销毁
        cancel()
    }

}
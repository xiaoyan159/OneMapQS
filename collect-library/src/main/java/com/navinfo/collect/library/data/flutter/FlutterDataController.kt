package com.navinfo.collect.library.data.flutter

import android.content.Context
import com.navinfo.collect.FlutterBaseActivity
import com.navinfo.collect.library.data.NIDataController
import com.navinfo.collect.library.data.flutter.flutterhandler.*
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterDataController(
    id: Int,
    context: Context,
    binaryMessenger: BinaryMessenger,
    activity: FlutterBaseActivity,
) : NIDataController(context, activity), MethodChannel.MethodCallHandler {

    private val mMethodChannel: MethodChannel = MethodChannel(
        binaryMessenger,
        "com.navinfo.collect/data_$id"
    )

    private val flutterDataLayerHandler: FlutterDataLayerHandler

    private val flutterDataElementHandler: FlutterDataElementHandler

    private val flutterDataProjectHandler: FlutterDataProjectHandler

    private val flutterDataNiLocationHandler: FlutterDataNiLocationHandler

    val flutterDataCameraHandler: FlutterDataCameraHandler

    init {
        mMethodChannel.setMethodCallHandler(this)
        flutterDataLayerHandler = FlutterDataLayerHandler(mContext, mDateBase)
        flutterDataElementHandler = FlutterDataElementHandler(mContext, mMethodChannel, mDateBase)
        flutterDataProjectHandler = FlutterDataProjectHandler(mContext, mDateBase)
        flutterDataNiLocationHandler = FlutterDataNiLocationHandler(mContext, mDateBase)
        flutterDataCameraHandler =
            FlutterDataCameraHandler(mContext, mMethodChannel, activity, mDateBase)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            /**
             * 数据图层操作部分
             */
            //创建数据图层
            FlutterDataProtocolKeys.DateLayerProtocol.kDataCreateDataLayer -> {
                flutterDataLayerHandler.createDataLayerTable(call, result);
            }
            //获取所有数据图层
            FlutterDataProtocolKeys.DateLayerProtocol.kDataGetDataLayerList -> {
                flutterDataLayerHandler.getDataLayerList(call, result);
            }

            //获取某个数据图层
            FlutterDataProtocolKeys.DateLayerProtocol.kDataGetDataLayer -> {
                flutterDataLayerHandler.getDataLayer(call, result);
            }

            /**
             * 数据操作部分
             */
            //保存数据
            FlutterDataProtocolKeys.DataElementProtocol.kDataSaveElementData -> {
                flutterDataElementHandler.saveElementData(call, result)
            }

            //删除数据
            FlutterDataProtocolKeys.DataElementProtocol.kDataDeleteElementData -> {
                flutterDataElementHandler.deleteElementData(call, result)
            }

            //捕捉数据
            FlutterDataProtocolKeys.DataElementProtocol.kDataSnapElementDataList -> {
                flutterDataElementHandler.snapElementDataList(call, result)
            }

            // 导入Pbf数据
            FlutterDataProtocolKeys.DataElementProtocol.kImportPbfData -> {
                flutterDataElementHandler.importPbfData2Realm(call, result)
            }
            //查询数据详细信息模板
            FlutterDataProtocolKeys.DataElementProtocol.kDataQueryElementDeepInfo -> {
                flutterDataElementHandler.queryElementDeepInfo(call, result)
            }
            //数据搜索
            FlutterDataProtocolKeys.DataElementProtocol.kDataSearchData -> {
                flutterDataElementHandler.searchData(call, result)
            }

            /**
             * 项目管理
             */
            //获取项目列表
            FlutterDataProtocolKeys.DataProjectProtocol.kDataGetDataProjectList -> {
                flutterDataProjectHandler.getProjectList(call, result);
            }

            //保存项目
            FlutterDataProtocolKeys.DataProjectProtocol.kDataSaveDataProject -> {
                flutterDataProjectHandler.saveProject(call, result);
            }

            /**
             * 轨迹操作部分
             */
            //保存轨迹数据
            FlutterDataProtocolKeys.DataNiLocationProtocol.kDataSaveNiLocationData -> {
                flutterDataNiLocationHandler.saveNiLocationData(call, result)
            }

            /**
             * 检查项部分
             */
            //获取所有检查项
            FlutterDataProtocolKeys.DataCheckProtocol.kDataGetCheckManagerList -> {
                flutterDataElementHandler.queryCheckManagerList(call, result)
            }
            //根据id获取检查项
            FlutterDataProtocolKeys.DataCheckProtocol.kDataGetCheckManagerListByIds -> {
                flutterDataElementHandler.queryCheckManagerListByIds(call, result)
            }
            //打开摄像头
            FlutterDataProtocolKeys.DataCameraProtocol.kDataOpenCamera -> {
                flutterDataCameraHandler.openCamera(call, result)
            }
            ///ocr批量识别
            FlutterDataProtocolKeys.DataCameraProtocol.kDataOCRBatchResults -> {
                flutterDataCameraHandler.ocrBatch(call, result)
            }
        }

    }

    override fun release() {
        super.release()
        mMethodChannel.setMethodCallHandler(null)
    }
}
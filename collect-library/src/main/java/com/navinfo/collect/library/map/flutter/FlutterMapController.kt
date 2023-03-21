package com.navinfo.collect.library.map.flutter

import android.content.Context
import com.navinfo.collect.FlutterBaseActivity
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.NIMapOptions
import com.navinfo.collect.library.map.flutter.flutterhandler.*
import com.navinfo.collect.library.map.flutter.maphandler.FlutterMeasureLayerHandler
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterMapController(
    id: Int,
    context: Context,
    binaryMessenger: BinaryMessenger,
    options: NIMapOptions,
) : NIMapController(context, options), MethodChannel.MethodCallHandler {
    private val mMethodChannel: MethodChannel = MethodChannel(
        binaryMessenger,
        "com.navinfo.collect/mapView_$id"
    )

    private val flutterMapBaseControlHandler: FlutterMapBaseControlHandler =
        FlutterMapBaseControlHandler(context, mMethodChannel, mMapView)

    private val flutterAnimationHandler: FlutterAnimationHandler by lazy {
        FlutterAnimationHandler(context, mMapView)
    }
    private val flutterMarkerHandler: FlutterMarkerHandler by lazy {
        FlutterMarkerHandler(context, mMapView)
    }

    private val flutterLocationHandler: FlutterLocationLayerHandler by lazy {
        FlutterLocationLayerHandler(context, mMapView)
    }

    private val flutterLineHandler: FlutterLineHandler by lazy {
        FlutterLineHandler(context, mMapView)
    }

    private val flutterPolygonHandler: FlutterPolygonHandler by lazy {
        FlutterPolygonHandler(context, mMapView)
    }

    private val flutterViewportHandler: FlutterViewportHandler by lazy {
        FlutterViewportHandler(context, mMapView)
    }

    private val flutterLayerManagerHandler: FlutterLayerManagerHandler =
        FlutterLayerManagerHandler(context, mMapView)

    init {
        mMethodChannel.setMethodCallHandler(this)
    }

    private val flutterMeasureLayerHandler: FlutterMeasureLayerHandler by lazy {
        FlutterMeasureLayerHandler(context, mMapView)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            /**
             * 地图基础操作部分
             */
            //刷新地图
            FlutterMapProtocolKeys.kMapUpdateMap -> {
                flutterMapBaseControlHandler.updateMap(call, result)
            }

            /**
             * 地图动画效果控制协议
             */
            //根据屏幕像素移动地图
            FlutterMapProtocolKeys.AnimationProtocol.kMapAnimationByPixel -> {
                flutterAnimationHandler.animation(call, result)
            }
            //地图缩小
            FlutterMapProtocolKeys.AnimationProtocol.kMapAnimationZoomOut -> {
                flutterAnimationHandler.zoomOut(call, result)
            }
            //地图放大
            FlutterMapProtocolKeys.AnimationProtocol.kMapAnimationZoomIn -> {
                flutterAnimationHandler.zoomIn(call, result)
            }

            /**
             * 地图Marker控制协议
             */
            //增加或更新marker
            FlutterMapProtocolKeys.MarkerProtocol.kMapAddMarkerMethod -> {
                flutterMarkerHandler.addMarker(call, result)
            }
            //删除marker
            FlutterMapProtocolKeys.MarkerProtocol.kMapRemoveMarkerMethod -> {
                flutterMarkerHandler.removeMarker(call, result)
            }


            /**
             * 地图定位信息控制协议
             */
            //更新定位信息
            FlutterMapProtocolKeys.LocationProtocol.kMapUpdateLocationDataMethod -> {
                flutterLocationHandler.updateCurrentLocation(call, result)
            }

            /**
             * 线数据协议
             */
            //绘制线时的打点
            FlutterMapProtocolKeys.LineProtocol.kMapAddDrawLinePointMethod -> {
                flutterLineHandler.addDrawLinePoint(call, result)
            }
            //清除正在绘制的线
            FlutterMapProtocolKeys.LineProtocol.kMapCleanDrawLineMethod -> {
                flutterLineHandler.clean(call, result)
            }
            //添加线数据
            FlutterMapProtocolKeys.LineProtocol.kMapAddDrawLineMethod -> {
                flutterLineHandler.addDrawLine(call, result)
            }

            /**
             * 面数据协议
             */
            //绘制面时的打点
            FlutterMapProtocolKeys.PolygonProtocol.kMapAddDrawPolygonPointMethod -> {
                flutterPolygonHandler.addDrawPolygonPoint(call, result)
            }
            /**
             * 面数据协议
             */
            //绘制面时的打点
            FlutterMapProtocolKeys.PolygonProtocol.kMapAddDrawPolygonNiPointMethod -> {
                flutterPolygonHandler.addDrawPolygonNiPoint(call, result)
            }

            //绘制面
            FlutterMapProtocolKeys.PolygonProtocol.kMapAddDrawPolygonMethod -> {
                flutterPolygonHandler.addDrawPolygon(call, result)
            }

            //清除正在绘制的面
            FlutterMapProtocolKeys.PolygonProtocol.kMapCleanDrawPolygonMethod -> {
                flutterPolygonHandler.clean(call, result)
            }

            /**
             * 视窗操作
             */
            //设置地图中心点的偏移量
            FlutterMapProtocolKeys.ViewportProtocol.kMapViewportSetViewCenterMethod -> {
                flutterViewportHandler.setMapViewCenter(call, result)
            }
            //获取几何扩展后的外接矩形
            FlutterMapProtocolKeys.ViewportProtocol.kMapViewportGetBoundingBoxMethod -> {
                flutterViewportHandler.getBoundingBoxWkt(call, result)
            }

            //坐标转屏幕点
            FlutterMapProtocolKeys.ViewportProtocol.kMapViewportToScreenPointMethod -> {
                flutterViewportHandler.toScreenPoint(call, result)
            }

            //屏幕点转坐标
            FlutterMapProtocolKeys.ViewportProtocol.kMapViewportFromScreenPointMethod -> {
                flutterViewportHandler.fromScreenPoint(call, result)
            }

            /**
             * 底图切换控制协议
             */
            //切换底图
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapSwitchRasterTileLayerMethod -> {
                flutterLayerManagerHandler.switchRasterTileLayer(call, result)
            }

            //获取支持的底图
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapGetRasterTileLayerListMethod -> {
                flutterLayerManagerHandler.getBaseRasterTileLayerList(call, result)
            }

            /**
             * 底图切换控制协议
             */
            //切换底图
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapSwitchRasterTileLayerMethod -> {
                flutterLayerManagerHandler.switchRasterTileLayer(call, result)
            }

            //获取支持的底图
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapGetRasterTileLayerListMethod -> {
                flutterLayerManagerHandler.getBaseRasterTileLayerList(call, result)
            }

            //绘制线或者面
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapDrawLineOrPolygonMethod -> {
                flutterMeasureLayerHandler.drawLineOrPolygon(call, result);
            }

            //清理绘制线或者面
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapCleanDrawLineOrPolygonMethod -> {
                flutterMeasureLayerHandler.clean();
            }

            //绘制线或者面
            FlutterMapProtocolKeys.LayerManagerProtocol.kMapEditLineOrPolygonMethod -> {
                flutterLayerManagerHandler.editLineOrPolygon(call, result)
            }
        }
    }

    override fun release() {
        super.release()

        mMethodChannel.setMethodCallHandler(null)
    }
}
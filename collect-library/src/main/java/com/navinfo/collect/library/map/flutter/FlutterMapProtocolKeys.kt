package com.navinfo.collect.library.map.flutter

object FlutterMapProtocolKeys {

    ///刷新地图
    const val kMapUpdateMap = "flutter_nimap/base/UpdateMap";

    /**
     * 地图动画控制协议
     */
    object AnimationProtocol {
        // 按像素移动地图中心点
        const val kMapAnimationByPixel = "flutter_nimap/animation/AnimationByPixel"

        ///地图缩小
        const val kMapAnimationZoomOut = "flutter_nimap/animation/AnimationZoomOut";

        ///地图放大
        const val kMapAnimationZoomIn = "flutter_nimap/animation/AnimationZoomIn";


    }

    /**
     * marker控制协议
     */
    object MarkerProtocol {
        // 添加marker
        const val kMapAddMarkerMethod = "flutter_nimap/marker/addMarker"

        // 删除marker
        const val kMapRemoveMarkerMethod = "flutter_nimap/marker/removeMarker";
    }

    /**
     * 定位信息控制协议
     */
    object LocationProtocol {
        // 动态更新我的位置数据
        const val kMapUpdateLocationDataMethod = "flutter_nimap/location/updateLocationData"
    }

    /**
     * 线数据控制协议
     */
    object LineProtocol {
        // 线数据打点
        const val kMapAddDrawLinePointMethod = "flutter_nimap/line/addDrawLinePoint";

        // 清除画线功能
        const val kMapCleanDrawLineMethod = "flutter_nimap/line/cleanDrawLine"

        // 添加线数据
        const val kMapAddDrawLineMethod = "flutter_nimap/line/addDrawLine"
    }

    /**
     * 线数据控制协议
     */
    object PolygonProtocol {
        // 线数据打点
        const val kMapAddDrawPolygonPointMethod = "flutter_nimap/Polygon/addDrawPolygonPoint";
        // 线数据打点
        const val kMapAddDrawPolygonNiPointMethod = "flutter_nimap/Polygon/addDrawPolygonNiPoint";
        // 添加面数据
        const val kMapAddDrawPolygonMethod = "flutter_nimap/Polygon/addDrawPolygon"
        // 清除画线功能
        const val kMapCleanDrawPolygonMethod = "flutter_nimap/Polygon/cleanDrawPolygon"
    }

    /**
     * 地图view操作
     */
    object ViewportProtocol {
        // 设置地图中心点
        const val kMapViewportSetViewCenterMethod = "flutter_nimap/Viewport/SetViewCenter";
        //获取几何扩展后的外接矩形
        const val kMapViewportGetBoundingBoxMethod = "flutter_nimap/Viewport/GetBoundingBox";
        //坐标转屏幕点
        const val kMapViewportToScreenPointMethod = "flutter_nimap/Viewport/ToScreenPoint";
        //屏幕点转坐标
        const val kMapViewportFromScreenPointMethod = "flutter_nimap/Viewport/FromScreenPoint";
    }

    /**
     * 原生地图通知flutter命令
     */
    object MapCallBackProtocol {
        // 用户操作地图，状态回调
        const val kMapEventCallback = "flutter_nimap/mapCallBack/kMapEventCallback";

        // 用户单击地图，回调点击坐标
        const val kMapOnClickCallback = "flutter_nimap/mapCallBack/kMapOnClickCallback";

    }

    /**
     * LayerManager控制协议
     */
    object LayerManagerProtocol {
        // 切换RasterTileLayer
        const val kMapSwitchRasterTileLayerMethod = "flutter_nimap/LayerManager/switchRasterTileLayer";

        // 移除RasterTileLayer
        const val kMapRemoveRasterTileLayerMethod = "flutter_nimap/LayerManager/removeRasterTileLayer"

        // 获取RasterTileLayer
        const val kMapGetRasterTileLayerListMethod = "flutter_nimap/LayerManager/getRasterTileLayerList"

        // 绘制线或者面
        const val kMapDrawLineOrPolygonMethod = "flutter_nimap/LayerManager/drawLineOrPolygon"

        // 清理绘制线或者面
        const val kMapCleanDrawLineOrPolygonMethod = "flutter_nimap/LayerManager/cleanDrawLineOrPolygon"

        // 编辑线或者面
        const val kMapEditLineOrPolygonMethod = "flutter_nimap/LayerManager/editLineOrPolygon"

    }
}

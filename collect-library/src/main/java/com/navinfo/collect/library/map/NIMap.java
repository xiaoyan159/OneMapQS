package com.navinfo.collect.library.map;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.navinfo.collect.library.utils.CacheTileProgress;
import com.navinfo.collect.library.utils.TileDownloader;
import com.yanzhenjie.kalle.Kalle;
import com.yanzhenjie.kalle.download.Download;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.UrlTileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 定义 NavinfoMap 地图对象的操作方法与接口
 */
public class NIMap {
    /**
     *
     */
    private Map map;
    /**
     * 地图控件
     */
    private NIMapView mMapView;
    /**
     * 指北针显隐
     */
    private boolean enableCompassImage = true;

    /**
     * 用户位置显示图层
     * */
//    private NaviLocationLayer locationLayer;
    /**
     * 构造函数
     */
    public NIMap(NIMapView niMapView) {
        this.mMapView = niMapView;
        this.map = mMapView.getVtmMap();
    }

    /**
     * 获取地图的当前状态
     *
     * @return
     */
    public Map getVtmMap() {
        return map;
    }

    /**
     * 获取地图最大缩放级别
     *
     * @return
     */
    public float getMaxZoomLevel() {
        return map.viewport().getMaxZoomLevel();
    }

    /**
     * 获取地图最小缩放级别
     *
     * @return
     */
    public float getMinZoomLevel() {

        return map.viewport().getMinZoomLevel();
    }

    /**
     * 设置指南针是否显示
     *
     * @param enable
     */
    public void setCompassEnable(boolean enable) {
        this.enableCompassImage = enable;
        if (mMapView != null && mMapView.getCompassImage() != null) {
            mMapView.getCompassImage().setVisibility(enable ? View.VISIBLE : View.GONE);
            mMapView.getCompassImage().setEnabled(enable);
        }
    }

    /**
     * 获取指北针显隐控制
     *
     * @return true 显示 false 隐藏
     */
    public boolean isEnableCompassImage() {
        return enableCompassImage;
    }

    /**
     * 设置指南针自定义图标
     *
     * @param icon
     */
    public void setCompassIcon(Bitmap icon) {
        if (mMapView != null && mMapView.getCompassImage() != null) {
            mMapView.getCompassImage().setImageBitmap(icon);
        }
    }


    /**
     * 设置地图显示大小等级
     *
     * @param level
     */
    public void setFontSizeLevel(int level) {

    }

    /**
     * 设置地图最大以及最小缩放级别
     *
     * @param max
     * @param min
     */
    public void setMaxAndMinZoomLevel(int max, int min) {
        map.viewport().setMaxZoomLevel(max);
        map.viewport().setMinZoomLevel(min);
    }

    /**
     * 放大
     *
     * @param animate 是否动画过渡
     */
    public void zoomIn(boolean animate) {
        MapPosition mapPosition = map.getMapPosition();
        mapPosition.setZoom(mapPosition.getZoom() + 1);
        if (animate) {
            map.animator().animateTo(mapPosition);
        } else {
            map.setMapPosition(mapPosition);
        }
    }

    /**
     * 缩小地图
     *
     * @param animate 是否动画过渡
     */
    public void zoomOut(boolean animate) {
        MapPosition mapPosition = map.getMapPosition();
        mapPosition.setZoom(mapPosition.getZoom() - 1);
        if (animate) {
            map.animator().animateTo(mapPosition);
        } else {
            map.setMapPosition(mapPosition);
        }
    }

    /**
     * 设置定位数据, 只有先允许定位图层后设置数据才会生效，参见 setMyLocationEnabled(boolean)
     *
     * @param data
     */
//    public void setMyLocationData(Location data) {
//        if (locationLayer == null) {
//            return;
//        }
//        locationLayer.setPosition(data.getLatitude(), data.getLongitude(), data.getAccuracy());
//    }

//    public void setMyLocationData(double lat, double lon, float accuracy) {
//        if (locationLayer == null) {
//            return;
//        }
//        locationLayer.setPosition(lat, lon, accuracy);
//    }

    /**
     * 设置是否允许定位图层
     *
     * @param enabled
     */
//    public void setMyLocationEnabled(Context mContext, boolean enabled) {
//        initLocaitonLayer(mContext);
//        locationLayer.setEnabled(enabled);
//    }

//    private void initLocaitonLayer(Context mContext) {
//        if (map == null) {
//            throw new IllegalStateException("map不可用，无法显示当前位置！");
//        }
//        if (locationLayer == null) {
//            locationLayer = new NaviLocationLayer(mContext, map);
//        }
//    }

    /**
     * 设置地图
     * */
    public void setMapPosition(double latitude, double longitude, int zoomLevel) {
        double scale = 1 << zoomLevel;
        getVtmMap().setMapPosition(latitude, longitude, scale);
    }

    public void animateMapPosition(double latitude, double longitude, int zoomLevel, int duration) {
        if (duration < 0) {
            duration = 500;
        }
        if (zoomLevel <= 0) {
            zoomLevel = getVtmMap().getMapPosition().zoomLevel;
        }
        double scale = 1 << zoomLevel;
        MapPosition mapPosition = new MapPosition(latitude, longitude, scale);
        getVtmMap().animator().animateTo(duration, mapPosition);
    }

    /**
     * 下载离线矢量地图
     * */
    public void downloadVectorMap(String url, DownloadProgress downloadProgress) {
        Kalle.Download.get(url).directory(NILayerManager.defaultDir).onProgress(new Download.ProgressBar() {
            @Override
            public void onProgress(int progress, long byteCount, long speed) {
                downloadProgress.onProgress(progress, byteCount, speed);
            }
        });
    }

    /**
     * 缓存urlTilesource对应的数据
     * */
    public List<FutureTask> cacheUrlTileMap(Rect rect, int minZoomLevel, int maxZoomLevel, CacheTileProgress progress) {
        List<Layer> layerList = getVtmMap().layers();
        List<UrlTileSource> urlTileSourceList = new ArrayList<>();
        if (layerList!=null&&!layerList.isEmpty()) {
            for (int i = 0; i < layerList.size(); i++) {
                Layer layer = layerList.get(i);
                if (layer instanceof BitmapTileLayer && ((BitmapTileLayer) layer).getTileSource() instanceof UrlTileSource) {
                    UrlTileSource urlTileSource = (UrlTileSource) ((BitmapTileLayer) layer).getTileSource();
                    urlTileSourceList.add(urlTileSource);
                }
            }
        }
        // 根据rect获取对应的地理坐标
        GeoPoint leftTopGeoPoint = map.viewport().fromScreenPoint(rect.left, rect.top);
        GeoPoint rightBottomGeoPoint = map.viewport().fromScreenPoint(rect.right, rect.bottom);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        List<FutureTask> futureTaskList = new ArrayList<>();
        progress.setLayerCount(urlTileSourceList.size());
        for (int i = 0; i < urlTileSourceList.size(); i++) {
            UrlTileSource urlTileSource = urlTileSourceList.get(i);
            int finalI = i;
            progress.setLayerId(i);
            Callable callable = TileDownloader.getInstance().downloadRasterTile(urlTileSource, leftTopGeoPoint, rightBottomGeoPoint, (byte) minZoomLevel, (byte) maxZoomLevel, progress);
            FutureTask futureTask = new FutureTask(callable);
            futureTaskList.add(futureTask);
        }

        if (futureTaskList!=null&&!futureTaskList.isEmpty()){
            for (int i = 0; i < futureTaskList.size(); i++) {
                scheduledExecutorService.submit(futureTaskList.get(i));
            }
            scheduledExecutorService.shutdown();
        }
        return futureTaskList;
    }

    public void cancelCacheTileMap() {
        TileDownloader.getInstance().setCanDownloadRasterTile(false);
    }

    public interface DownloadProgress {

        Download.ProgressBar DEFAULT = new Download.ProgressBar() {
            @Override
            public void onProgress(int progress, long byteCount, long speed) {
            }
        };

        /**
         * Download onProgress changes.
         */
        void onProgress(int progress, long byteCount, long speed);
    }

    /**
     * 设置地图单击事件监听者
     *
     * @param listener
     */
    public void setOnMapClickListener(OnMapClickListener listener) {
        mMapView.setOnMapClickListener(listener);
    }

    /**
     * 设置地图双击事件监听者
     *
     * @param listener
     */
    public void setOnMapDoubleClickListener(OnMapDoubleClickListener listener) {
        mMapView.setOnMapDoubleClickListener(listener);
    }

    /**
     * 设置地图长按事件监听者
     *
     * @param listener
     */
    public void setOnMapLongClickListener(OnMapLongClickListener listener) {
        mMapView.setOnMapLongClickListener(listener);
    }

    /**
     * @param listener
     */
    public void setOnMapTouchListener(OnMapTouchListener listener) {
        mMapView.setOnMapTouchListener(listener);
    }

    /**
     * 地图单击事件监听接口
     */
    public static interface OnMapClickListener {
        /**
         * 地图单击事件回调函数
         *
         * @param point
         */
        void onMapClick(GeoPoint point);

        /**
         * 地图内 Poi 单击事件回调函数
         *
         * @param poi
         */
        void onMapPoiClick(GeoPoint poi);
    }

    /**
     * 地图双击事件监听接口
     */
    public static interface OnMapDoubleClickListener {

        /**
         * 地图双击事件监听回调函数
         *
         * @param point
         */
        void onMapDoubleClick(GeoPoint point);

    }

    /**
     * 地图长按事件监听接口
     */
    public static interface OnMapLongClickListener {
        /**
         * 地图长按事件监听回调函数
         *
         * @param point
         */
        void onMapLongClick(GeoPoint point);
    }

    /**
     * 用户触摸地图时回调接口
     */
    public static interface OnMapTouchListener {
        /**
         * 当用户触摸地图时回调函数
         *
         * @param event
         */
        void onTouch(MotionEvent event);
    }

}

package com.navinfo.collect.library.map;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.navinfo.collect.library.R;
import com.navinfo.collect.library.map.layers.NaviMapScaleBar;
import com.navinfo.collect.library.map.source.MapLifeDBTileSource;
import com.navinfo.collect.library.map.source.MapLifeNiLocationTileDataSource;
import com.navinfo.collect.library.map.source.MapLifeNiLocationTileSource;
import com.navinfo.collect.library.system.Constant;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;
import org.oscim.android.theme.AssetsRenderTheme;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.tile.vector.labeling.LabelTileLoaderHook;
import org.oscim.map.Map;
import org.oscim.renderer.GLViewport;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;

import java.io.File;

/**
 * 一个显示地图的视图（View）。它负责从服务端获取地图数据。它将会捕捉屏幕触控手势事件
 */
public final class NIMapView extends RelativeLayout {
    /**
     * VTM地图
     */
    private MapView mapView;
    /**
     * NavinfoMap 地图对象的操作方法与接口
     */
//    private NIMap map;

    /**
     * 定位图标
     */
    protected ImageView compassImage;

    /**
     * logo图标
     */
    protected ImageView logoImage;

    /**
     * 图片旋转
     */
    private NIRotateAnimation mRotateAnimation;

    /**
     * 之前的旋转角度
     */
    private float mLastRotateZ = 0;

    /**
     * 缩放按钮
     */
    private ImageView zoomInImage, zoomOutImage;
    private View zoomLayout;

    /**
     * 地图状态设置
     */
    private NIMapOptions options;
    /**
     * 地图图层管理器
     */
    private NILayerManager mLayerManager;
    private Layer baseRasterLayer, defaultVectorTileLayer, defaultVectorLabelLayer, gridLayer;
    protected Context mContext;

    private MapPreferences mPrefs;
    protected String mapFilePath = Constant.ROOT_PATH+"/map";
    protected GroupLayer baseGroupLayer; // 用于盛放所有基础底图的图层组，便于统一管理

    public NIMapView(Context context, NIMapOptions options) {
        this(context, null, 0);
        this.options = options;
        initOptions();
    }

    /**
     * 地图的单击事件监听
     */
    private NIMap.OnMapClickListener mapClickListener;

    /**
     * 地图的双击事件监听
     */
    private NIMap.OnMapDoubleClickListener mapDoubleClickListener;

    /**
     * 地图的长按事件监听
     */
    private NIMap.OnMapLongClickListener mapLongClickListener;

    /**
     * 地图的触摸事件
     */
    private NIMap.OnMapTouchListener touchListener;

    public NIMapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NIMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NIMapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        View rootView = LayoutInflater.from(context).inflate(R.layout.base_map_layout, this, true);
        this.mContext = context;

        mapView = rootView.findViewById(R.id.base_map_view);
//        map = new NIMap(this);
        compassImage = rootView.findViewById(R.id.navinfo_map_compass);
        initMapGroup(); // 初始化图层组

        mLayerManager = new NILayerManager(context, getVtmMap());

        logoImage = rootView.findViewById(R.id.navinfo_map_logo);
        mRotateAnimation = new NIRotateAnimation(compassImage);
        mPrefs = new MapPreferences(NIMapView.class.getName(), mContext);
        getVtmMap().events.bind(new Map.UpdateListener() {
            @Override
            public void onMapEvent(Event e, MapPosition mapPosition) {
                //旋转
                if (mLastRotateZ != mapPosition.bearing) {
                    mRotateAnimation.startRotationZ(mLastRotateZ, mapPosition.bearing);
                    mLastRotateZ = mapPosition.bearing;
                }

                //增加控制联动效果
//                if (map != null && map.isEnableCompassImage()) {
                //2D,正北隐藏
                if (compassImage.getVisibility() != View.VISIBLE && (mapPosition.tilt != 0 || mapPosition.bearing != 0)) {
                    compassImage.setVisibility(View.VISIBLE);
                } else if (compassImage.getVisibility() == View.VISIBLE && mapPosition.tilt == 0 && mapPosition.bearing == 0) {
                    compassImage.clearAnimation();
                    compassImage.setVisibility(View.GONE);
                }
//                } else {
//                    compassImage.clearAnimation();
//                    compassImage.setVisibility(View.GONE);
//                }
            }
        });

        // 增加比例尺图层
        NaviMapScaleBar naviMapScaleBar = new NaviMapScaleBar(getVtmMap());
        naviMapScaleBar.initScaleBarLayer(GLViewport.Position.BOTTOM_LEFT, 25, 60);

        if (gridLayer == null) {
            gridLayer = new TileGridLayer(getVtmMap());
            getVtmMap().layers().add(gridLayer, LAYER_GROUPS.ALLWAYS_SHOW_GROUP.groupIndex);
            gridLayer.setEnabled(false);
        }

        compassImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MapPosition mapPosition = getVtmMap().getMapPosition();
                mapPosition.setBearing(0);
                mapPosition.setTilt(0);
                getVtmMap().animator().animateTo(300, mapPosition);
            }
        });

        zoomInImage = rootView.findViewById(R.id.navinfo_map_zoom_in);
        zoomInImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                zoomIn(arg0);
            }
        });

        zoomOutImage = rootView.findViewById(R.id.navinfo_map_zoom_out);
        zoomOutImage.setImageResource(R.drawable.icon_map_zoom_out);
        zoomOutImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                zoomOut(arg0);
            }
        });

        mapView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (touchListener != null) {
                    touchListener.onTouch(event);
                }
                return false;
            }
        });

        zoomLayout = rootView.findViewById(R.id.navinfo_map_zoom_layer);
        initMap();
    }

    /**
     * 地图初始化参数
     */
    private void initOptions() {
        if (options != null) {
            if (options.getShowZoomControl()) {
                zoomLayout.setVisibility(VISIBLE);
            } else {
                zoomLayout.setVisibility(GONE);
            }
            MapPosition mapPosition = getVtmMap().getMapPosition();
            mapPosition.setZoom(options.getZoomLevel());
            mapPosition.setPosition(options.getCoordinate().getLatitude(), options.getCoordinate().getLongitude());
            getVtmMap().animator().animateTo(100, mapPosition);
        }
    }


    /**
     * 初始化地图
     */
    private void initMap() {
        switchBaseMapType(BASE_MAP_TYPE.CYCLE_MAP);
        switchTileVectorLayerTheme(MAP_THEME.DEFAULT);
        initVectorTileLayer();
        initMapLifeSource();
    }

    /**
     * 增加作业渲染
     */
    LabelLayer labelLayer;
    /**
     * 作业数据图层
     */
    VectorTileLayer vectorTileLayer;
    /**
     * 作业数据渲染图层
     */
    MapLifeDBTileSource mapLifeDBTileSource;
    /**
     * 轨迹渲染图层
     */
    MapLifeNiLocationTileSource mapLifeNiLocationTileSource;
    /**
     * 轨迹数据图层
     */
    VectorTileLayer vectorNiLocationTileLayer;
    /**
     * 增加作业渲染
     */
    LabelLayer labelNiLocationLayer;

    public void initMapLifeSource() {
        mapLifeDBTileSource = new MapLifeDBTileSource(this.mContext, Constant.ROOT_PATH + "/coremap.db");
        mapLifeNiLocationTileSource = new MapLifeNiLocationTileSource(this.mContext,Constant.ROOT_PATH + "/coremap.db");
        vectorTileLayer = new VectorTileLayer(mapView.map(), mapLifeDBTileSource);
        vectorNiLocationTileLayer = new VectorTileLayer(mapView.map(),mapLifeNiLocationTileSource);
        mapView.map().layers().add(vectorNiLocationTileLayer,LAYER_GROUPS.VECTOR_TILE.groupIndex);
        mapView.map().layers().add(vectorTileLayer,LAYER_GROUPS.VECTOR_TILE.groupIndex);
        labelLayer = new LabelLayer(mapView.map(), vectorTileLayer, new LabelTileLoaderHook(), 15);
        mapView.map().layers().add(labelLayer,LAYER_GROUPS.VECTOR_TILE.groupIndex);
        labelNiLocationLayer = new LabelLayer(mapView.map(), vectorNiLocationTileLayer, new LabelTileLoaderHook(), 15);
        mapView.map().layers().add(labelNiLocationLayer,LAYER_GROUPS.VECTOR_TILE.groupIndex);

        switchTileVectorLayerTheme(null);
        mapView.map().updateMap(true);
        MapPosition mapPosition = new MapPosition();
        mapPosition.setZoomLevel(18);
        mapPosition.setPosition(40.091692296269144, 116.26712172082546);
        //116.26712172082546 40.091692296269144
        mapView.map().animator().animateTo(mapPosition);
    }

    public void dataLayerUpdate() {
        switchTileVectorLayerTheme(null);
    }

    private void initMapGroup() {
        for (LAYER_GROUPS groups : LAYER_GROUPS.values()) {
            getVtmMap().layers().addGroup(groups.groupIndex);
        }
    }

    /**
     * 切换基础底图样式
     */
    public void switchBaseMapType(BASE_MAP_TYPE type) {
        if (baseRasterLayer != null) {
            getVtmMap().layers().remove(baseRasterLayer);
            baseRasterLayer = null;
            getVtmMap().updateMap();
        }
        baseRasterLayer = mLayerManager.getRasterTileLayer(mContext, type.url, type.tilePath, true);
        getVtmMap().layers().add(baseRasterLayer, LAYER_GROUPS.BASE_RASTER.groupIndex);
        getVtmMap().updateMap();
    }

    /**
     * 移除基础底图样式
     */
    public void removeBaseMap() {
        if (baseRasterLayer != null) {
            getVtmMap().layers().remove(baseRasterLayer);
            baseRasterLayer = null;
            getVtmMap().updateMap();
        }
    }

    public void initVectorTileLayer(){
        if (baseGroupLayer == null) {
            baseGroupLayer = new GroupLayer(getVtmMap());
        }
        for (Layer layer : baseGroupLayer.layers) {
            getVtmMap().layers().remove(layer);
        }
        baseGroupLayer.layers.clear();

        File baseMapFolder = new File(mapFilePath);
        if (!baseMapFolder.exists()) {
            return;
        }

        File[] mapFileList = baseMapFolder.listFiles();

        if (mapFileList != null && mapFileList.length > 0) {

            MultiMapFileTileSource multiMapFileTileSource = new MultiMapFileTileSource();

            for (File mapFile : mapFileList) {

                if (!mapFile.exists() || !mapFile.getName().endsWith(".map")) {
                    continue;
                }

                MapFileTileSource mTileSource = new MapFileTileSource();

                mTileSource.setPreferredLanguage("zh");

                if (mTileSource.setMapFile(mapFile.getAbsolutePath())) {
                    multiMapFileTileSource.add(mTileSource);
                }

            }

            VectorTileLayer baseMapLayer = new OsmTileLayer(getVtmMap());
            baseMapLayer.setTileSource(multiMapFileTileSource);

            baseGroupLayer.layers.add(baseMapLayer);

            if (getTheme(null) != null)
                baseMapLayer.setTheme(getTheme(null));

            baseGroupLayer.layers.add(new BuildingLayer(getVtmMap(), baseMapLayer));
            baseGroupLayer.layers.add(new LabelLayer(getVtmMap(), baseMapLayer));

            for (Layer layer : baseGroupLayer.layers) {
                if (layer instanceof LabelLayer) {
                    getVtmMap().layers().add(layer, LAYER_GROUPS.VECTOR.groupIndex);
                } else {
                    getVtmMap().layers().add(layer, LAYER_GROUPS.BASE_VECTOR.groupIndex);
                }
            }
        }
    }

    //获取渲染资源
    public IRenderTheme getTheme(final String styleId) {
        AssetsRenderTheme theme = new AssetsRenderTheme(mContext.getAssets(), null, "default.xml");
        if (styleId == null || "".equals(styleId.trim())) {
            switch (2) {
                case 0:
                    theme = new AssetsRenderTheme(mContext.getAssets(), null, "default.xml");
                    break;
                case 1:
                    theme = new AssetsRenderTheme(mContext.getAssets(), null, "osmarender.xml");
                    break;
                case 2:
                    theme = new AssetsRenderTheme(mContext.getAssets(), null, "tronrender.xml");
                    break;
            }

        }

        return ThemeLoader.load(theme);
    }

    public void addDefaultVectorTileLayer(MAP_THEME theme) {
        if (defaultVectorTileLayer != null) {
            getVtmMap().layers().remove(defaultVectorTileLayer);
            defaultVectorTileLayer = null;
            getVtmMap().updateMap();
        }
        defaultVectorTileLayer = mLayerManager.getDefaultVectorLayer(true);
        getVtmMap().layers().add(defaultVectorTileLayer, LAYER_GROUPS.VECTOR_TILE.groupIndex);
        defaultVectorLabelLayer = new LabelLayer(getVtmMap(), (VectorTileLayer) defaultVectorTileLayer);
        getVtmMap().layers().add(defaultVectorLabelLayer, LAYER_GROUPS.VECTOR_TILE.groupIndex);
        if (theme != null) {
            switchTileVectorLayerTheme(theme);
        }
        getVtmMap().updateMap();
    }

    public void setBaseRasterVisiable(boolean visiable) {
        if (baseRasterLayer != null) {
            baseRasterLayer.setEnabled(visiable);
            getVtmMap().updateMap();
        }
    }

    /**
     * 基础
     */
    public enum BASE_MAP_TYPE {
        OPEN_STREET_MAP("Open Street Map", "http://a.tile.openstreetmap.org", "/{Z}}/{X}/{Y}.png"), // openStreetMap底图
        CYCLE_MAP("Cycle Map", "http://c.tile.opencyclemap.org/cycle", "/{Z}}/{X}/{Y}.png"), // cyclemap底图
        S_MAP("SMap", "http://smap.navinfo.com/gateway/smap-raster-map/raster/basemap/tile", "z={Z}&x={X}&y={Y}"), // cyclemap底图
        TRANSPORT_MAP("Transport Map", "http://b.tile2.opencyclemap.org/transport", "/{Z}}/{X}/{Y}.png"); // TransportMap底图
        String title;
        String url;
        String tilePath;

        BASE_MAP_TYPE(String title, String url, String tilePath) {
            this.title = title;
            this.url = url;
            this.tilePath = tilePath;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getTilePath() {
            return tilePath;
        }
    }

    /**
     * 网格图层是否显示
     */
    public void setGridLayerVisiable(boolean visiable) {
        if (gridLayer != null) {
            gridLayer.setEnabled(visiable);
            getVtmMap().updateMap();
        }
    }

    /**
     * 图层组定义
     */
    public enum LAYER_GROUPS {
        BASE_RASTER(0)/*栅格底图*/, BASE_VECTOR(1)/*矢量底图*/,
        RASTER_TILE(2)/*栅格网格*/, VECTOR_TILE(3)/*矢量网格*/,
        VECTOR(4)/*矢量图层组*/, OTHER(5)/*其他图层*/,
        ALLWAYS_SHOW_GROUP(6), OPERATE(7)/*操作图层组*/;
        int groupIndex;

        LAYER_GROUPS(int groupIndex) {
            this.groupIndex = groupIndex;
        }

        public int getGroupIndex() {
            return groupIndex;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    public enum GRAVITY {
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM
    }

    /**
     * 用户重载这个方法时必须调用父类的这个方法 用于NIMapView保存地图状态
     *
     * @param context
     * @param bundle
     */
    public void onCreate(Context context, Bundle bundle) {

    }

    /**
     * 当Activity暂停的时候调用地图暂停
     */
    public void onPause() {
        if (mapView != null) {
            if (mPrefs != null)
                mPrefs.save(getVtmMap());
            mapView.onPause();
        }
    }

    /**
     * 当Activity唤醒时调用地图唤醒
     */
    public void onResume() {
        if (mapView != null) {
            if (mPrefs != null)
                mPrefs.load(getVtmMap());
            mapView.onResume();
        }
    }

    /**
     * 用户重载这个方法时必须调用父类的这个方法 用于NIMapView保存地图状态
     *
     * @param bundle
     */
    public void onSaveInstanceState(Bundle bundle) {

    }


    /**
     * 当Activity销毁时调用地图的销毁
     */
    public void onDestroy() {
        try{
            if (mapView != null) {
                mapView.onDestroy();
            }
        }catch (Exception e){

        }
    }


    /**
     * @param child
     * @param params
     */
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
    }

    /**
     * 从NIMapView中移除一个子View
     *
     * @param view
     */
    public void removeView(View view) {
        super.removeView(view);
    }


    /**
     * 获取地图控制器
     *
     * @return
     */
//    public NIMap getMap() {
//        return map;
//    }

    /**
     * 获取VTM-Map
     *
     * @return
     */
    public Map getVtmMap() {
        if (mapView != null)
            return mapView.map();
        return null;
    }

    /**
     * 获取当前地图级别对应比例尺大小
     *
     * @return
     */
    public int getMapLevel() {

        if (mapView != null && mapView.map() != null)
            return mapView.map().getMapPosition().getZoomLevel();

        return 0;
    }

    /**
     * @param view
     */
    public void zoomIn(View view) {
        if (view != null) {
            if (view.isEnabled()) {
                MapPosition mapPosition = mapView.map().getMapPosition();
                mapPosition.setZoom(mapPosition.getZoom() + 1);
                mapView.map().animator().animateTo(mapPosition);
//                map.zoomIn(true);
            }
            view.setEnabled(false);
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.setEnabled(true);
                }
            }, 300);
        }
    }

    /**
     * @param view
     */
    public void zoomOut(View view) {
        if (view != null) {
            if (view.isEnabled()) {
                MapPosition mapPosition = mapView.map().getMapPosition();
                mapPosition.setZoom(mapPosition.getZoom() - 1);
                mapView.map().animator().animateTo(mapPosition);

//                map.zoomOut(true);
            }
            view.setEnabled(false);
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.setEnabled(true);
                }
            }, 300);
        }
    }


    /**
     * 设置MotionEvent
     *
     * @param event
     */
    public void setUpViewEventToNIMapView(MotionEvent event) {

    }

    /**
     * 设置缩放按钮位置
     *
     * @param position 按钮位置
     */
    public void setZoomControlPosition(GRAVITY position) {
        if (zoomLayout != null) {
            setLayoutParams(position, zoomLayout);
        }
    }

    /**
     * 设置缩放按钮位置
     *
     * @param position 按钮位置
     */
    public void setLogoPosition(GRAVITY position) {
        if (logoImage != null) {
            setLayoutParams(position, logoImage);
        }
    }

    /**
     * 设置缩放按钮位置
     *
     * @param position 按钮位置
     */
    public void setCompassPosition(GRAVITY position) {
        if (compassImage != null) {
            setLayoutParams(position, compassImage);
        }
    }


    /**
     * 根据GRAVITY生成对应的layoutParams
     *
     * @param position 按钮相对于父布局的位置
     * @param view     被设置显示位置的view
     */
    private void setLayoutParams(GRAVITY position, View view) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        if (layoutParams.getRules() != null) {
            for (int i = 0; i < layoutParams.getRules().length; i++) {
                layoutParams.removeRule(i);
            }
        }
        switch (position) {
            case LEFT_TOP:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                break;
            case RIGHT_TOP:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                break;
            case LEFT_BOTTOM:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            case RIGHT_BOTTOM:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
        }
        view.setLayoutParams(layoutParams);
    }

    /**
     * 设置是否显示缩放控件
     *
     * @param show
     */
    public void showZoomControls(boolean show) {
        if (zoomLayout != null) {
            if (show) {
                zoomLayout.setVisibility(VISIBLE);
            } else {
                zoomLayout.setVisibility(GONE);
            }
        }
    }

    /**
     * 设置是否显示缩放控件
     *
     * @param show
     */
    public void showCompass(boolean show) {
//        if (map != null) {
//            map.setCompassEnable(show);
//        }
        if (compassImage != null) {
            compassImage.setVisibility(show ? View.VISIBLE : View.GONE);
            compassImage.setEnabled(show);
        }
    }

    /**
     * 获取指北针
     */
    protected ImageView getCompassImage() {
        return compassImage;
    }


    public enum MAP_THEME {
        DEFAULT(0), NEWTRON(1), OSMAGRAY(2),
        OSMARENDER(3), TRONRENDER(4);
        int themeId;

        MAP_THEME(int themeId) {
            this.themeId = themeId;
        }

        public MAP_THEME getMapTheme(int themeId) {
            MAP_THEME theme = DEFAULT;
            switch (themeId) {
                case 1:
                    theme = NEWTRON;
                    break;
                case 2:
                    theme = OSMAGRAY;
                    break;
                case 3:
                    theme = OSMARENDER;
                    break;
                case 4:
                    theme = TRONRENDER;
                    break;
            }
            return theme;
        }
    }

    public void switchTileVectorLayerTheme(MAP_THEME styleId) {
        // 如果不包含vectorlayer，则设置theme无效
        boolean bHis = false;
        for (Layer layer : getVtmMap().layers()) {
            if (layer instanceof VectorTileLayer) {
                bHis = true;
                break;
            }
        }
        if (!bHis) {
            getVtmMap().updateMap(true);
            return;
        }

        if (styleId == null) {
            getVtmMap().setTheme(new AssetsRenderTheme(mContext.getAssets(), "", "navdefault.xml"), true);
        } else {
            switch (styleId) {
                case NEWTRON:
                    getVtmMap().setTheme(VtmThemes.NEWTRON, true);
                    break;
                case OSMAGRAY:
                    getVtmMap().setTheme(VtmThemes.OSMAGRAY, true);
                    break;
                case OSMARENDER:
                    getVtmMap().setTheme(VtmThemes.OSMARENDER, true);
                    break;
                case TRONRENDER:
                    getVtmMap().setTheme(VtmThemes.TRONRENDER, true);
                    break;
                default:
                    getVtmMap().setTheme(new AssetsRenderTheme(mContext.getAssets(), "", "editormarker.xml"), true);
                    break;
            }
        }
        getVtmMap().updateMap();
    }

    public NILayerManager getLayerManager() {
        return mLayerManager;
    }

    // 地图点击事件对应的图层
    private class MapEventsReceiver extends Layer implements GestureListener {

        public MapEventsReceiver(Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, org.oscim.event.MotionEvent e) {
            GeoPoint geoPoint = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
            if (g instanceof Gesture.Tap) { // 单击事件
                if (mapClickListener != null) {
                    mapClickListener.onMapClick(geoPoint);
                }
            } else if (g instanceof Gesture.DoubleTap) { // 双击
                if (mapDoubleClickListener != null) {
                    mapDoubleClickListener.onMapDoubleClick(geoPoint);
                }
            } else if (g instanceof Gesture.LongPress) { // 长按
                if (mapLongClickListener != null) {
                    mapLongClickListener.onMapLongClick(geoPoint);
                }
            }
            setOnMapClickListener(new NIMap.OnMapClickListener() {
                @Override
                public void onMapClick(GeoPoint point) {

                }

                @Override
                public void onMapPoiClick(GeoPoint poi) {

                }
            });
            return false;
        }
    }

    /**
     * 设置地图的点击事件
     */
    public void setOnMapClickListener(@Nullable NIMap.OnMapClickListener listener) {
        this.mapClickListener = listener;
    }

    /**
     * 设置地图的双击事件
     * 注：默认情况下，双击会自动放大地图
     */
    public void setOnMapDoubleClickListener(@Nullable NIMap.OnMapDoubleClickListener listener) {
        this.mapDoubleClickListener = listener;
    }

    /**
     * 设置地图长按事件
     *
     * @param listener
     */
    public void setOnMapLongClickListener(@Nullable NIMap.OnMapLongClickListener listener) {
        this.mapLongClickListener = listener;
    }

    /**
     * 设置地图的触摸事件
     *
     * @param listener
     */
    public void setOnMapTouchListener(@Nullable NIMap.OnMapTouchListener listener) {
        this.touchListener = listener;
    }
}

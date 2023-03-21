package com.navinfo.collect.library.map;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Environment;
import android.text.TextPaint;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.gdx.maps.MapGroupLayer;
import com.navinfo.collect.library.R;
import com.navinfo.collect.library.map.layers.NIPolygonLayer;
import com.navinfo.collect.library.map.source.NavinfoMapRastorTileSource;
import com.navinfo.collect.library.map.source.NavinfoMultiMapFileTileSource;
import com.navinfo.collect.library.utils.DistanceUtil;
import com.navinfo.collect.library.utils.GeometryTools;
import com.navinfo.collect.library.utils.StringUtil;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.layers.LocationLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.tiling.source.OkHttpEngine;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class NILayerManager implements ItemizedLayer.OnItemGestureListener {
    private Map vtmMap;
    public static String defaultDir = Environment.getExternalStorageDirectory() + "/" + "NavinfoCollect";
    private String defaultLocalMapPath = defaultDir + "/maps/";
    //图层管理
    private java.util.Map<String, Layer> layersMap = new HashMap<String, Layer>();
    //默认marker图层
    private ItemizedLayer mDefaultMarkerLayer;
    //定位图层
    private LocationLayer mLocationLayer;
    private NIPolygonLayer mPolygonLayer;
    private List<MarkerItem> mPathMakers = new ArrayList<>();
    private Location mCurrentLocation; // 当前位置信息

    public static final String MARQUEE_MARKER_LAYER = "MarqueeMarker";

    private Context mCon;

    private java.util.Map<String, Layer> vectorLayerMap; // 维护vector图层的map数据，key为图层名称（一般对应为矢量数据的数据源），value为图层

    public NILayerManager(Context context, Map vtmMap) {
        this.vtmMap = vtmMap;
        this.mCon = context;

        //新增marker图标样式
        AndroidBitmap mDefaultBitmap =
                new AndroidBitmap(BitmapFactory.decodeResource(context.getResources(), R.mipmap.marker));
        MarkerSymbol markerSymbol = new MarkerSymbol(mDefaultBitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        //新增marker图层
        mDefaultMarkerLayer = new ItemizedLayer(
                vtmMap,
                new ArrayList<MarkerInterface>(),
                markerSymbol,
                this
        );
        addLayer("defaultMarkerLayer", mDefaultMarkerLayer, NIMapView.LAYER_GROUPS.VECTOR.ordinal());

        //定位图层
        mLocationLayer = new LocationLayer(vtmMap);
        addLayer("locationLayer", mLocationLayer, NIMapView.LAYER_GROUPS.ALLWAYS_SHOW_GROUP.ordinal());

        if (this.vectorLayerMap == null) {
            this.vectorLayerMap = new HashMap<>();
        }
    }

    public Layer getRasterTileLayer(Context mContext, String url, String tilePath, boolean useCache) {
        if (this.vtmMap == null) {
            throw new IllegalStateException("无法获取到map对象");
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        NavinfoMapRastorTileSource mTileSource = NavinfoMapRastorTileSource.builder(url).tilePath(tilePath).httpFactory(new OkHttpEngine.OkHttpFactory(builder)).build();
        // 如果使用缓存
        if (useCache) {
            File cacheDirectory = new File(defaultDir, "tiles-raster");
            int cacheSize = 300 * 1024 * 1024; // 300 MB
            Cache cache = new Cache(cacheDirectory, cacheSize);
            builder.cache(cache);
        }

//        mTileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory(builder));
//        mTileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "vtm-android-example"));
//        mTileSource.setCache(new TileCache(mContext, defaultDir, url.substring(url.indexOf(":")+1)));

        BitmapTileLayer rasterLayer = new BitmapTileLayer(this.vtmMap, mTileSource);
        return rasterLayer;
    }

    // 初始化请求在线底图数据
    public Layer getDefaultVectorLayer(boolean useCache) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (useCache) {
            // Cache the tiles into file system
            File cacheDirectory = new File(defaultDir, "tiles-vector");
            int cacheSize = 200 * 1024 * 1024; // 200 MB
            Cache cache = new Cache(cacheDirectory, cacheSize);
            builder.cache(cache);
        }

        NavinfoMultiMapFileTileSource tileSource = NavinfoMultiMapFileTileSource.builder()
                .apiKey("4wTLZyXcQym31pxC_HGy7Q") // Put a proper API key
                .httpFactory(new OkHttpEngine.OkHttpFactory(builder))
                //.locale("en")
                .build();
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("token", "eyJhbGciOiJIUzI1NiJ9.eyJjbGllbnRJZCI6MTAzLCJ1c2VyTmFtZSI6ImFkbWluIiwidXNlcklkIjoxLCJ1c2VyR3JvdXAiOiLnoJTlj5Hpg6giLCJvcmdJZCI6MSwiaWF0IjoxNjMwOTk4MzI5LCJleHAiOjE2MzEwODQ3Mjl9.0wFm8mAA9dCC2FmZj-u1dhxTFDRYx8AqVnh2C88hitk");
        tileSource.setHttpRequestHeaders(headerMap);
        VectorTileLayer l = new VectorTileLayer(this.vtmMap, tileSource);
        return l;
    }

    public void addLayer(String tag, Layer layer) {
        if (!layersMap.containsKey(tag)) {
            layersMap.put(tag, layer);
            vtmMap.layers().add(layer);
        }
    }


    public void addLayer(String tag, Layer layer, int group) {
        if (!layersMap.containsKey(tag)) {
            layersMap.put(tag, layer);
            vtmMap.layers().add(layer, group);
        }
    }

    public void addLayer(int index, String tag, Layer layer) {
        if (!layersMap.containsKey(tag)) {
            layersMap.put(tag, layer);
            vtmMap.layers().add(index, layer);
        }
    }

    public boolean containsLayer(String tag) {
        return layersMap.containsKey(tag);
    }

    public Layer getLayer(String tag) {
        return layersMap.get(tag);
    }

    public void removeLayer(String tag) {
        if (layersMap.containsKey(tag)) {
            Layer layer = layersMap.remove(tag);
            vtmMap.layers().remove(layer);
        }
    }

    /**
     * 返回默认marker图层
     *
     * @return mDefaultMarkerLayer
     */
    public ItemizedLayer getDefaultMarkerLayer() {
        return mDefaultMarkerLayer;
    }

    /**
     * 返回定位图层
     *
     * @return mLocationLayer
     */
    public LocationLayer getLocationLayer() {
        return mLocationLayer;
    }


    @Override
    public boolean onItemSingleTapUp(int index, Object item) {
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, Object item) {
        return false;
    }

    /**
     * 定位
     * @param lon
     * @param lat
     * @param time
     */
    public void jumpToPosition(double lon, double lat, long time) {
        MapPosition mapPosition = vtmMap.getMapPosition();
        mapPosition.setPosition(lat, lon);
        vtmMap.animator().animateTo(time, mapPosition);
    }

    /**
     * 定位
     * @param lon
     * @param lat
     * @param zoomLevel
     */
    public void jumpToPosition(double lon, double lat, int zoomLevel) {
        MapPosition mapPosition = vtmMap.getMapPosition();
        if (mapPosition.getZoomLevel() < zoomLevel) {
            mapPosition.setZoomLevel(zoomLevel);
        }
        mapPosition.setPosition(lat, lon);
        vtmMap.animator().animateTo(300, mapPosition);
    }

    public void addMarker2MarkerLayer(MarkerInterface markerItem, Bitmap defaultBitmap, String layerName, int layerGroup) {
        if (markerItem == null) {
            return;
        }
        if (vectorLayerMap != null) {
            if (!vectorLayerMap.containsKey(layerName) || vectorLayerMap.get(layerName) == null) {
                MarkerSymbol symbol = new MarkerSymbol(defaultBitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
                vectorLayerMap.put(layerName, new ItemizedLayer(vtmMap, symbol));
            }

            ItemizedLayer itemizedLayer = (ItemizedLayer) vectorLayerMap.get(layerName);
            itemizedLayer.addItem(markerItem);
            if (!vtmMap.layers().contains(itemizedLayer)) {
                vtmMap.layers().add(itemizedLayer, layerGroup);
            }
            itemizedLayer.update();
        }
    }

    public void addMarker2MarkerLayer(MarkerInterface markerItem, Bitmap defaultBitmap, String layerName, int layerGroup, ItemizedLayer.OnItemGestureListener listener) {
        if (markerItem == null) {
            return;
        }
        if (vectorLayerMap != null) {
            if (!vectorLayerMap.containsKey(layerName) || vectorLayerMap.get(layerName) == null) {
                MarkerSymbol symbol = new MarkerSymbol(defaultBitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
                vectorLayerMap.put(layerName, new ItemizedLayer(vtmMap, new ArrayList<MarkerInterface>(), symbol, listener));
            }

            ItemizedLayer itemizedLayer = (ItemizedLayer) vectorLayerMap.get(layerName);
            itemizedLayer.addItem(markerItem);
            if (!vtmMap.layers().contains(itemizedLayer)) {
                vtmMap.layers().add(itemizedLayer, layerGroup);
            }
            itemizedLayer.update();
        }
    }

    //删除marker
    public void removeMarker(MarkerItem markerItem) {
        if (vectorLayerMap != null && vectorLayerMap.containsKey("Marker")) {
            ItemizedLayer itemizedLayer = (ItemizedLayer) vectorLayerMap.get("Marker");
            if (itemizedLayer.getItemList() != null && itemizedLayer.getItemList().size() > 0) {
                itemizedLayer.removeItem(markerItem);
            }
        }
    }

    /**
     * @param markerItem marker
     * @param layerName  图层
     */
    public void removeMarker(MarkerItem markerItem, String layerName) {
        if (vectorLayerMap != null && layerName != null && vectorLayerMap.containsKey(layerName)) {
            ItemizedLayer itemizedLayer = (ItemizedLayer) vectorLayerMap.get(layerName);
            if (itemizedLayer.getItemList() != null && itemizedLayer.getItemList().size() > 0) {
                itemizedLayer.removeItem(markerItem);
            }
        }
    }


    /**
     * 在地图中心增加marker
     */
    public void showEditMapGraphical(String geometry, int type) {

    }

    //增加marker
    public MarkerItem addMarker(GeoPoint geopoint, Bitmap bitmap) {
        if (geopoint != null && bitmap != null) {

            MarkerItem markerItem = new MarkerItem(StringUtil.Companion.createUUID(), "", geopoint.getLatitude() + "," + geopoint.getLongitude(), geopoint);

            MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);

            markerItem.setMarker(markerSymbol);

            addMarker2MarkerLayer(markerItem, bitmap, "", NIMapView.LAYER_GROUPS.OTHER.ordinal());

            return markerItem;
        }

        return null;
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

}

package com.navinfo.collect.map;

import android.content.Context;
import android.os.Environment;

import com.navinfo.collect.library.map.source.NavinfoMapRastorTileSource;
import com.navinfo.collect.library.map.source.NavinfoMultiMapFileTileSource;

import org.oscim.android.MapPreferences;
import org.oscim.android.cache.TileCache;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.DateTime;
import org.oscim.backend.DateTimeAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.gdx.AndroidGL;
import org.oscim.gdx.AndroidGL30;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.poi3d.Poi3DLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.buildings.S3DBLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class NILayerManager {
    private Map vtmMap;
    public static String defaultDir = Environment.getExternalStorageDirectory() + "/" + "EditorMark";
    private String defaultLocalMapPath = defaultDir + "/maps/";

    public NILayerManager(Map vtmMap) {
        this.vtmMap = vtmMap;
    }

    public Layer getRasterTileLayer(Context mContext, String url, String tilePath, boolean useCache) {
        if (this.vtmMap == null) {
            throw new IllegalStateException("无法获取到map对象");
        }
        NavinfoMapRastorTileSource mTileSource = NavinfoMapRastorTileSource.builder(url).httpFactory(new OkHttpEngine.OkHttpFactory()).tilePath(tilePath).build();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 如果使用缓存
        if (useCache) {
            File cacheDirectory = new File(defaultDir, "tiles-raster");
            int cacheSize = 300 * 1024 * 1024; // 300 MB
            Cache cache = new Cache(cacheDirectory, cacheSize);
            builder.cache(cache);
        }

        mTileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory(builder));
        mTileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "vtm-android-example"));
        mTileSource.setCache(new TileCache(mContext, defaultDir, url.substring(url.indexOf(":")+1)));

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
}

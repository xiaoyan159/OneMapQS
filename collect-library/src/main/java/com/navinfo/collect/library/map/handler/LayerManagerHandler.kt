package com.navinfo.collect.library.map.handler

import android.content.Context
import android.os.Environment
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.NIMapView.LAYER_GROUPS
import com.navinfo.collect.library.map.source.NavinfoMapRastorTileSource
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.oscim.layers.Layer
import org.oscim.layers.LocationLayer
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory
import java.io.File

/**
 * Layer 操作
 */
class LayerManagerHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView) {
    lateinit var mLocationLayer: LocationLayer
    private var baseRasterLayer: Layer? = null

    init {
        initMap()
    }

    /**
     * 初始化地图
     */
    private fun initMap() {
        switchBaseMapType(BASE_MAP_TYPE.CYCLE_MAP)

//        initVectorTileLayer()
//        initMapLifeSource()
    }


    /**
     * 切换基础底图样式
     */
    fun switchBaseMapType(type: BASE_MAP_TYPE) {
        if (baseRasterLayer != null) {
            mMapView.vtmMap.layers().remove(baseRasterLayer)
            baseRasterLayer = null
            mMapView.vtmMap.updateMap()
        }
        baseRasterLayer = getRasterTileLayer(type.url, type.tilePath, true)
        addLayer(baseRasterLayer!!, LAYER_GROUPS.BASE)
        mMapView.updateMap()
    }

    private fun getRasterTileLayer(
        url: String?,
        tilePath: String?,
        useCache: Boolean
    ): Layer {
        val builder = OkHttpClient.Builder()
        val mTileSource =
            NavinfoMapRastorTileSource.builder(url).tilePath(tilePath)
                .httpFactory(OkHttpFactory(builder)).build()
        // 如果使用缓存
        if (useCache) {
            val cacheDirectory: File =
                File(Environment.getExternalStorageState() + "/" + "lalalal", "tiles-raster")
            val cacheSize = 300 * 1024 * 1024 // 300 MB
            val cache = Cache(cacheDirectory, cacheSize.toLong())
            builder.cache(cache)
        }

//        mTileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory(builder));
//        mTileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "vtm-android-example"));
//        mTileSource.setCache(new TileCache(mContext, defaultDir, url.substring(url.indexOf(":")+1)));
        return BitmapTileLayer(mMapView.vtmMap, mTileSource)
    }
}

/**
 * 基础
 */
enum class BASE_MAP_TYPE(// TransportMap底图
    var title: String, var url: String, var tilePath: String
) {
    OPEN_STREET_MAP(
        "Open Street Map",
        "http://a.tile.openstreetmap.org",
        "/{Z}}/{X}/{Y}.png"
    ),  // openStreetMap底图
    CYCLE_MAP(
        "Cycle Map",
        "http://c.tile.opencyclemap.org/cycle",
        "/{Z}}/{X}/{Y}.png"
    ),  // cyclemap底图
    S_MAP(
        "SMap",
        "http://smap.navinfo.com/gateway/smap-raster-map/raster/basemap/tile",
        "z={Z}&x={X}&y={Y}"
    ),  // cyclemap底图
    TRANSPORT_MAP(
        "Transport Map",
        "http://b.tile2.opencyclemap.org/transport",
        "/{Z}}/{X}/{Y}.png"
    );

}
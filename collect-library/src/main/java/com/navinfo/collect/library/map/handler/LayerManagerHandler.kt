package com.navinfo.collect.library.map.handler

import android.content.Context
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.source.NavinfoMapRastorTileSource
import com.navinfo.collect.library.map.source.NavinfoMultiMapFileTileSource
import com.navinfo.collect.library.system.Constant
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.oscim.layers.GroupLayer
import org.oscim.layers.Layer
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory
import org.oscim.tiling.source.mapfile.MapFileTileSource
import java.io.File

/**
 * Layer 操作
 */
class LayerManagerHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView) {
    private var baseGroupLayer // 用于盛放所有基础底图的图层组，便于统一管理
            : GroupLayer? = null

    init {
        initMap()
    }

    /**
     * 初始化地图
     */
    private fun initMap() {

        loadBaseMap()
        mMapView.switchTileVectorLayerTheme(NIMapView.MAP_THEME.DEFAULT)
        mMapView.vtmMap.updateMap()
//        initVectorTileLayer()
//        initMapLifeSource()
    }


    /**
     * 切换基础底图样式
     */
    fun loadBaseMap() {

        if (baseGroupLayer == null) {
            baseGroupLayer = GroupLayer(mMapView.vtmMap)
            addLayer(baseGroupLayer!!, NIMapView.LAYER_GROUPS.BASE)
        }
        baseGroupLayer?.let {
            for (layer in it.layers) {
                removeLayer(layer)
            }
            it.layers.clear()
            val builder = OkHttpClient.Builder()
            val urlTileSource: NavinfoMultiMapFileTileSource =
                NavinfoMultiMapFileTileSource.builder()
                    .httpFactory(OkHttpFactory(builder)) //.locale("en")
                    .build()

            // Cache the tiles into file system
            val cacheDirectory = File(Constant.MAP_PATH, "cache")
            val cacheSize = 200 * 1024 * 1024 // 10 MB
            val cache = Cache(cacheDirectory, cacheSize.toLong())
            builder.cache(cache)

//        val headerMap = HashMap<String, String>()
//        headerMap["token"] = ""//Constant.TOKEN
//        urlTileSource.setHttpRequestHeaders(headerMap)
            val baseLayer = VectorTileLayer(mMapView.vtmMap, urlTileSource)

            val baseMapFolder = File("${Constant.MAP_PATH}offline")

            if (baseMapFolder.exists()) {
                val dirFileList = baseMapFolder.listFiles()
                if (dirFileList != null && dirFileList.isNotEmpty()) {
                    for (mapFile in dirFileList) {
                        if (!mapFile.isFile || !mapFile.name.endsWith(".map")) {
                            continue
                        }
                        val mTileSource = MapFileTileSource()
                        mTileSource.setPreferredLanguage("zh")
                        if (mTileSource.setMapFile(mapFile.absolutePath)) {
                            urlTileSource.add(mTileSource)
                        }
                    }
                }
                baseLayer.tileSource = urlTileSource
                it.layers.add(baseLayer)
                it.layers.add(BuildingLayer(mMapView.vtmMap, baseLayer))
                it.layers.add(LabelLayer(mMapView.vtmMap, baseLayer))
                for (layer in it.layers) {
                    addLayer(layer, NIMapView.LAYER_GROUPS.BASE)
                }
                mMapView.updateMap()
            }
        }
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
            val cacheDirectory =
                File(Constant.MAP_PATH, "cache")
            val cacheSize = 300 * 1024 * 1024 // 300 MB
            val cache = Cache(cacheDirectory, cacheSize.toLong())
            builder.cache(cache)
        }

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
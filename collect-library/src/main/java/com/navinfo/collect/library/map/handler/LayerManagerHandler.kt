package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.navinfo.collect.library.R
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.cluster.ClusterMarkerItem
import com.navinfo.collect.library.map.cluster.ClusterMarkerRenderer
import com.navinfo.collect.library.map.layers.MyItemizedLayer
import com.navinfo.collect.library.map.source.MapLifeNiLocationTileSource
import com.navinfo.collect.library.map.source.NavinfoMultiMapFileTileSource
import com.navinfo.collect.library.system.Constant
import com.navinfo.collect.library.utils.GeometryTools
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.locationtech.jts.geom.Geometry
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Paint
import org.oscim.core.GeoPoint
import org.oscim.layers.GroupLayer
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerRendererFactory
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.layers.tile.vector.labeling.LabelTileLoaderHook
import org.oscim.map.Map.UpdateListener
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory
import org.oscim.tiling.source.mapfile.MapFileTileSource
import java.io.File
import java.util.*

/**
 * Layer 操作
 */
open class LayerManagerHandler(context: AppCompatActivity, mapView: NIMapView,tracePath: String) : BaseHandler(context, mapView) {
    private var baseGroupLayer // 用于盛放所有基础底图的图层组，便于统一管理
            : GroupLayer? = null
    protected val mTracePath:String = tracePath

    /**
     * 轨迹渲染图层
     */
    private lateinit var mapLifeNiLocationTileSource: MapLifeNiLocationTileSource

    /**
     * 轨迹数据图层
     */
    private lateinit var vectorNiLocationTileLayer: VectorTileLayer

    /**
     * 增加作业渲染
     */
    private lateinit var labelNiLocationLayer: LabelLayer


    init {
        initMap()
    }

    /**
     * 初始化地图
     */
    private fun initMap() {

        loadBaseMap()

        mapLifeNiLocationTileSource = MapLifeNiLocationTileSource(mContext, mTracePath)

        vectorNiLocationTileLayer = VectorTileLayer(mMapView.vtmMap, mapLifeNiLocationTileSource)

        labelNiLocationLayer = LabelLayer(mMapView.vtmMap, vectorNiLocationTileLayer, LabelTileLoaderHook(), 15)

        if(vectorNiLocationTileLayer!=null){
            addLayer(vectorNiLocationTileLayer,NIMapView.LAYER_GROUPS.BASE)
        }
        if(labelNiLocationLayer!=null){
            addLayer(labelNiLocationLayer, NIMapView.LAYER_GROUPS.BASE)
        }

        vectorNiLocationTileLayer.isEnabled = false
        labelNiLocationLayer.isEnabled = false

        mMapView.switchTileVectorLayerTheme(NIMapView.MAP_THEME.DEFAULT)


        mMapView.updateMap()
//        initMapLifeSource()

    }


    /**
     * 切换基础底图样式
     */
    fun loadBaseMap() {
        //给地图layer分组
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
                //增加基础路网图层
                it.layers.add(baseLayer)
                //增加建筑图层
                it.layers.add(BuildingLayer(mMapView.vtmMap, baseLayer))
                //增加文字图层
                it.layers.add(LabelLayer(mMapView.vtmMap, baseLayer))
                for (layer in it.layers) {
                    addLayer(layer, NIMapView.LAYER_GROUPS.BASE)
                }
            }
        }
        mMapView.switchTileVectorLayerTheme(NIMapView.MAP_THEME.DEFAULT)
        mMapView.updateMap()
    }


    //显示轨迹图层
    fun showNiLocationLayer() {
        vectorNiLocationTileLayer.isEnabled = true
        labelNiLocationLayer.isEnabled = true
    }

    //隐藏轨迹图层
    fun hideNiLocationLayer() {
        vectorNiLocationTileLayer.isEnabled = false
        labelNiLocationLayer.isEnabled = false
    }
}


/**
 * 基础
 */
enum class BASE_MAP_TYPE(// TransportMap底图
    var title: String, var url: String, var tilePath: String
) {
    OPEN_STREET_MAP(
        "Open Street Map", "http://a.tile.openstreetmap.org", "/{Z}}/{X}/{Y}.png"
    ),  // openStreetMap底图
    CYCLE_MAP(
        "Cycle Map", "http://c.tile.opencyclemap.org/cycle", "/{Z}}/{X}/{Y}.png"
    ),  // cyclemap底图
    S_MAP(
        "SMap",
        "http://smap.navinfo.com/gateway/smap-raster-map/raster/basemap/tile",
        "z={Z}&x={X}&y={Y}"
    ),  // cyclemap底图
    TRANSPORT_MAP(
        "Transport Map", "http://b.tile2.opencyclemap.org/transport", "/{Z}}/{X}/{Y}.png"
    );

}
package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
import org.oscim.layers.marker.*
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.layers.tile.vector.labeling.LabelTileLoaderHook
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
     * 默认文字颜色
     */
    private val mDefaultTextColor = "#4E55AF"

    /**
     * 文字画笔
     */

    private lateinit var paint: Paint

    //画布
    private lateinit var canvas: org.oscim.backend.canvas.Canvas
    private lateinit var itemizedLayer: MyItemizedLayer
    private lateinit var markerRendererFactory: MarkerRendererFactory
    private val markerItemsNames = mutableListOf<MarkerInterface>()

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

    /**
     * 文字大小
     */
    private val NUM_13 = 13

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

        //初始化之间数据图层
        initQsRecordDataLayer()

        mMapView.vtmMap.updateMap()


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
                mMapView.updateMap()
            }
        }
    }

    /**
     * 初始话质检数据图层
     */
    private fun initQsRecordDataLayer() {
        canvas = CanvasAdapter.newCanvas()
        paint = CanvasAdapter.newPaint()
        paint.setTypeface(Paint.FontFamily.DEFAULT, Paint.FontStyle.NORMAL)
        paint.setTextSize(NUM_13 * CanvasAdapter.getScale())
        paint.strokeWidth = 2 * CanvasAdapter.getScale()
        paint.color = Color.parseColor(mDefaultTextColor)
        val bitmapPoi: Bitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.map_icon_blue2
            )
        )
        val symbol = MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.BOTTOM_CENTER)
        markerRendererFactory = MarkerRendererFactory { markerLayer ->
            object : ClusterMarkerRenderer(
                mContext,
                markerLayer,
                symbol,
                ClusterStyle(
                    org.oscim.backend.canvas.Color.WHITE,
                    org.oscim.backend.canvas.Color.BLUE
                )
            ) {
//                override fun getClusterBitmap(size: Int): Bitmap? {
//                    return super.getclusterbitmap(size)
//                }
            }
        }

        var resId = R.mipmap.map_icon_point_add

        mContext.lifecycleScope.launch(Dispatchers.IO) {
            var list = mutableListOf<QsRecordBean>()
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                val objects = realm.where<QsRecordBean>().findAll()
                list = realm.copyFromRealm(objects)
            }
            realm.close()

            itemizedLayer =
                MyItemizedLayer(
                    mMapView.vtmMap,
                    mutableListOf(),
                    markerRendererFactory,
                    object : MyItemizedLayer.OnItemGestureListener {
                        override fun onItemSingleTapUp(
                            layer: MyItemizedLayer?,
                            list: MutableList<Int>?,
                            nearest: Int
                        ): Boolean {
                            return true
                        }

                        override fun onItemLongPress(
                            layer: MyItemizedLayer?,
                            list: MutableList<Int>?,
                            nearest: Int
                        ): Boolean {
                            return true
                        }
                    })

            for (item in list) {
                val bitmap: Bitmap = createTextMarkerBitmap(mContext, item.description, resId)
                if (item.t_lifecycle != 2) {
                    val geometry: Geometry? = GeometryTools.createGeometry(item.geometry)
                    if (geometry != null) {
                        var geoPoint: GeoPoint? = null
                        if (geometry.geometryType != null) {
                            when (geometry.geometryType.uppercase(Locale.getDefault())) {
                                "POINT" -> geoPoint =
                                    GeoPoint(geometry.coordinate.y, geometry.coordinate.x)
//                                "LINESTRING" -> {
//                                    val lineString = geometry as LineString
//                                    if (lineString != null && lineString.coordinates.size > 0) {
//                                        geoPoint = GeoPoint(
//                                            lineString.coordinates[0].y,
//                                            lineString.coordinates[0].x
//                                        )
//                                    }
//                                    val drawableLine: Drawable =
//                                        convertGeometry2Drawable(geometry, lineStyle)
//                                    if (drawableLine != null) {
//                                        dataVectorLayer.add(drawableLine)
//                                    }
//                                }
//                                "POLYGON" -> {
//                                    val polygon = geometry as Polygon
//                                    if (polygon != null && polygon.coordinates.size > 0) {
//                                        geoPoint = GeoPoint(
//                                            polygon.coordinates[0].y,
//                                            polygon.coordinates[0].x
//                                        )
//                                    }
//                                    val drawablePolygon: Drawable =
//                                        convertGeometry2Drawable(geometry, polygonStyle)
//                                    if (drawablePolygon != null) {
//                                        dataVectorLayer.add(drawablePolygon)
//                                    }
//                                }
                            }
                        }
                        if (geoPoint != null) {
                            var geoMarkerItem: MarkerItem
//                            if (item.getType() === 1) {
                            geoMarkerItem = ClusterMarkerItem(
                                1, item.id, item.description, geoPoint
                            )
//                            } else {
//                                geoMarkerItem = MarkerItem(
//                                    ePointTemp.getType(),
//                                    ePointTemp.getId(),
//                                    ePointTemp.getStyleText(),
//                                    geoPoint
//                                )
//                            }
                            markerItemsNames.add(geoMarkerItem)
                            val markerSymbol =
                                MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER)
                            geoMarkerItem.marker = markerSymbol
                        }
                    }
                }
            }
            itemizedLayer.addItems(markerItemsNames)
            addLayer(itemizedLayer, NIMapView.LAYER_GROUPS.OPERATE)
            withContext(Dispatchers.Main) {
                itemizedLayer.map().updateMap(true)
            }
        }
    }

    /**
     * 文字和图片拼装，文字换行
     *
     * @param context
     * @param text
     * @param resId
     * @return
     */
    private fun createTextMarkerBitmap(context: Context, text: String, resId: Int): Bitmap {
        var text: String? = text
        return if (text == null || text.trim { it <= ' ' }.isEmpty()) {
            val drawable = ResourcesCompat.getDrawable(context.resources, resId, null)
            val originBitmap = android.graphics.Bitmap.createBitmap(
                drawable!!.intrinsicWidth,
                drawable.intrinsicHeight * 2,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val androidCanvas = Canvas(originBitmap)
            val startX = (originBitmap.width - drawable.intrinsicWidth) / 2
            drawable.setBounds(
                startX, 0, startX + drawable.intrinsicWidth, drawable.intrinsicHeight
            )
            drawable.draw(androidCanvas)
            val bitmap: Bitmap = AndroidBitmap(originBitmap)
            canvas.setBitmap(bitmap)
            bitmap
        } else {
            val drawable = ResourcesCompat.getDrawable(context.resources, resId, null)
            val textList: MutableList<String> = ArrayList()
            val fontSize: Float = NUM_13 * CanvasAdapter.getScale()
            paint.setTextSize(fontSize)
            var maxWidth = 0f
            //最多4行，一行7个
            if (text.trim { it <= ' ' }.length > 24) {
                val size = (drawable!!.intrinsicHeight / 4).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                if (text.trim { it <= ' ' }.length > 28) text = text.substring(0, 26) + "..."
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 7)
                textList.add(temp2)
                text = text.substring(7)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                val temp3 = text.substring(0, 7)
                textList.add(temp3)
                text = text.substring(7)
                newWidth = paint.getTextWidth(temp3)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 21) {
                val size = (drawable!!.intrinsicHeight / 4).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 6)
                textList.add(temp1)
                text = text.substring(6)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 6)
                textList.add(temp2)
                text = text.substring(6)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                val temp3 = text.substring(0, 6)
                textList.add(temp3)
                text = text.substring(6)
                newWidth = paint.getTextWidth(temp3)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 18) {
                val size = (drawable!!.intrinsicHeight / 3).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 7)
                textList.add(temp2)
                text = text.substring(7)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 18) {
                val size = (drawable!!.intrinsicHeight / 3).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 7)
                textList.add(temp2)
                text = text.substring(7)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 14) {
                val size = (drawable!!.intrinsicHeight / 3).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 6)
                textList.add(temp1)
                text = text.substring(6)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 6)
                textList.add(temp2)
                text = text.substring(6)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 12) {
                val size = (drawable!!.intrinsicHeight / 2).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                textList.add(text)
                val newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 10) {
                val size = (drawable!!.intrinsicHeight / 2).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 6)
                textList.add(temp1)
                text = text.substring(6)
                maxWidth = paint.getTextWidth(temp1)
                textList.add(text)
                val newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 7) {
                val size = (drawable!!.intrinsicHeight / 2).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 5)
                textList.add(temp1)
                text = text.substring(5)
                maxWidth = paint.getTextWidth(temp1)
                textList.add(text)
                val newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else {
                val size = drawable!!.intrinsicHeight.toFloat()
                if (size < fontSize) paint.setTextSize(size)
                textList.add(text)
                maxWidth = paint.getTextWidth(text)
            }
            paint.color = Color.parseColor(mDefaultTextColor)
            val originBitmap = android.graphics.Bitmap.createBitmap(
                if (drawable.intrinsicWidth > maxWidth) drawable.intrinsicWidth else maxWidth.toInt(),
                drawable.intrinsicHeight * 2,
                android.graphics.Bitmap.Config.ARGB_4444
            )
            val androidCanvas = Canvas(originBitmap)
            val startX = (originBitmap.width - drawable.intrinsicWidth) / 2
            drawable.setBounds(
                startX, 0, startX + drawable.intrinsicWidth, drawable.intrinsicHeight
            )
            drawable.draw(androidCanvas)
            val bitmap: Bitmap = AndroidBitmap(originBitmap)
            canvas.setBitmap(bitmap)
            var startHeight = (drawable.intrinsicHeight + paint.getTextHeight(text)).toInt()
            for (txt in textList) {
                canvas.drawText(
                    txt, (bitmap.width - paint.getTextWidth(txt)) / 2, startHeight.toFloat(), paint
                )
                startHeight += paint.getTextHeight(txt).toInt()
            }
            bitmap
        }
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
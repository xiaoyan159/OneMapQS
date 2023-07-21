package com.navinfo.collect.library.map.handler

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.navinfo.collect.library.R
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.map.BaseClickListener
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.layers.MultiLinesLayer
import com.navinfo.collect.library.map.layers.OmdbTaskLinkLayer
import com.navinfo.collect.library.utils.GeometryTools
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.vector.PathLayer
import org.oscim.layers.vector.geometries.Style

class LineHandler(context: AppCompatActivity, mapView: NIMapView) : BaseHandler(context, mapView) {


    /**
     * 高亮线图层，同时只高亮一条线，如线选择
     */
    private val mDefaultPathLayer: PathLayer by lazy {
        //高亮线绘制线 样式
        val defaultLineStyle = Style.builder()
            .stippleColor(context.resources.getColor(R.color.draw_line_blue2_color))
            .strokeWidth(10f)
            .fillColor(context.resources.getColor(R.color.teal_200))
            .fillAlpha(0.5f)
            .strokeColor(context.resources.getColor(R.color.teal_200))
            .fixed(true).build()

        val layer = PathLayer(mMapView.vtmMap, defaultLineStyle)
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_LINE)
        layer
    }


    /**
     * 路口高亮
     */
    val linksLayer by lazy {
        val layer = MultiLinesLayer(mapView.vtmMap)
        addLayer(layer, NIMapView.LAYER_GROUPS.VECTOR)
        layer
    }

    /**
     * 任务线图层
     */
    private val omdbTaskLinkLayer: OmdbTaskLinkLayer by lazy {
        val layer = OmdbTaskLinkLayer(
            mMapView.vtmMap,
            Style.builder()
//            .stippleColor(context.resources.getColor(R.color.draw_line_red_color, null))
                .fillColor(context.resources.getColor(R.color.draw_line_red_color))
                .fillAlpha(0.5f)
                .strokeColor(context.resources.getColor(R.color.draw_line_red_color))
                .strokeWidth(8f)
                .fixed(true).build()
        )
        addLayer(layer, NIMapView.LAYER_GROUPS.VECTOR)
        layer
    }

    /**
     * 任务线的marker，新增link会有marker
     */
    private val omdbTaskMarkerLayer: ItemizedLayer by lazy {

        //新增marker图标样式
        val mDefaultBitmap =
            AndroidBitmap(
                context.resources.openRawResource(R.raw.icon_task_link_marker), 48, 48, 100
            )
        val markerSymbol = MarkerSymbol(
            mDefaultBitmap,
            MarkerSymbol.HotspotPlace.CENTER
        )
        val layer = ItemizedLayer(
            mapView.vtmMap,
            ArrayList(),
            markerSymbol,
            object : OnItemGestureListener<MarkerInterface> {
                override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
                    val tag = mMapView.listenerTagList.last()
                    val listenerList = mMapView.listenerList[tag]
                    if (listenerList != null) {
                        for (listener in listenerList) {
                            if (listener is OnTaskLinkItemClickListener) {
                                if (item is MarkerItem) {
                                    listener.onTaskLink(tag, item.title)
                                }
                                break
                            }
                        }
                    }
                    return false
                }

                override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
                    return false
                }

            }
        )
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }


    /**
     * 高亮一条线
     */
    fun showLine(geometry: String) {
        try {
            mDefaultPathLayer.clearPath()
            mDefaultPathLayer.setPoints(GeometryTools.getGeoPoints(geometry))
            mDefaultPathLayer.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(mContext, "高亮路线失败 ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 取消高亮线
     */
    fun removeLine() {
        mDefaultPathLayer.clearPath()
        mDefaultPathLayer.isEnabled = false
    }

    /**
     * 移除所有任务高亮线
     */
    fun removeAllTaskLine() {
        omdbTaskLinkLayer.removeAll()
        omdbTaskMarkerLayer.removeAllItems()
    }

    /**
     * 显示所有任务高亮线
     */
    fun showTaskLines(hadLinkDvoList: List<HadLinkDvoBean>) {
        for (link in hadLinkDvoList) {
            if (link.linkStatus == 3) {
                val pointList = GeometryTools.getGeoPoints(link.geometry)
                val geoPoint = if (pointList.size < 3) {
                    pointList[0]
                } else {
                    pointList[1]
                }
                val marker = MarkerItem(
                    link.linkPid,
                    "",
                    geoPoint
                )
                omdbTaskMarkerLayer.addItem(marker)
            }
        }
        omdbTaskLinkLayer.addLineList(hadLinkDvoList)
    }

    /**
     * 增加一条任务高亮线
     */
    fun addTaskLink(linkBean: HadLinkDvoBean) {
        for (marker in omdbTaskMarkerLayer.itemList) {
            if ((marker as MarkerItem).title == linkBean.linkPid) {
                omdbTaskMarkerLayer.removeItem(marker)
                break
            }
        }
        if (linkBean.linkStatus == 3) {
            val pointList = GeometryTools.getGeoPoints(linkBean.geometry)
            val geoPoint = if (pointList.size < 3) {
                pointList[0]
            } else {
                pointList[1]
            }
            val marker = MarkerItem(
                linkBean.linkPid,
                "",
                geoPoint
            )
            omdbTaskMarkerLayer.addItem(marker)
        }
        omdbTaskLinkLayer.removeLine(linkBean.linkPid)
        omdbTaskLinkLayer.addLine(linkBean)
        omdbTaskLinkLayer.update()
        mMapView.vtmMap.updateMap(true)
    }

    /**
     * 增加一条任务高亮线
     */
    fun removeTaskLink(linkBeanId: String) {
        for (marker in omdbTaskMarkerLayer.itemList) {
            if ((marker as MarkerItem).title == linkBeanId) {
                omdbTaskMarkerLayer.removeItem(marker)
                break
            }
        }
        omdbTaskLinkLayer.removeLine(linkBeanId)
        omdbTaskLinkLayer.update()
        mMapView.vtmMap.updateMap(true)
    }
}

interface OnTaskLinkItemClickListener : BaseClickListener {
    fun onTaskLink(tag: String, taskLinkId: String)
}
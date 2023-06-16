package com.navinfo.collect.library.map.layers

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.navinfo.collect.library.R
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.utils.GeometryTools
import org.locationtech.jts.geom.Geometry
import org.oscim.layers.vector.VectorLayer
import org.oscim.layers.vector.geometries.Drawable
import org.oscim.layers.vector.geometries.LineDrawable
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map

class OmdbTaskLinkLayer(map: Map, private var style: Style) : VectorLayer(map) {
    private val lineMap = HashMap<String, Drawable>()

    private var selectDrawable: Drawable? = null

    private val selectStyle =
        Style.builder().fillColor(Color.GREEN).strokeColor(Color.GREEN)
            .strokeWidth(10f).fixed(false).build()


    @Synchronized
    fun addLine(hadLinkDvoBean: HadLinkDvoBean, style: Style = this.style) {
        if (!lineMap.containsKey(hadLinkDvoBean.linkPid)) {
            // 添加geometry到图层上
            val lineDrawable =
                LineDrawable(GeometryTools.createGeometry(hadLinkDvoBean.geometry), style)
            super.add(lineDrawable)
            lineMap[hadLinkDvoBean.linkPid] = lineDrawable
        }
    }

    @Synchronized
    fun showSelectLine(hadLinkDvoBean: HadLinkDvoBean) {
        if (selectDrawable != null) super.remove(selectDrawable)
        selectDrawable =
            LineDrawable(GeometryTools.createGeometry(hadLinkDvoBean.geometry), selectStyle)
        super.add(selectDrawable)
    }

    @Synchronized
    fun clearSelectLine() {
        if (selectDrawable != null) {
            super.remove(selectDrawable)
            selectDrawable = null
        }
    }

    @Synchronized
    fun addLineList(hadLinkDvoBeanList: List<HadLinkDvoBean>, style: Style = this.style) {
        hadLinkDvoBeanList.forEach {
            addLine(it, style)
        }
        update()
    }

    fun removeLine(linkPid: String): Boolean {
        if (lineMap.containsKey(linkPid)) {
            super.remove(lineMap[linkPid])
            lineMap.remove(linkPid)
        }
        return false
    }

    fun removeLine(geometry: Geometry) {
        super.remove(geometry)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setLineColor(color: Color) {
        this.style =
            Style.builder().fillColor(color.toArgb()).fillAlpha(0.5f).strokeColor(color.toArgb())
                .strokeWidth(8f).fixed(true).build()
    }

    @Synchronized
    fun removeAll() {
        for ((key, value) in lineMap) {
            super.remove(value)
        }
        lineMap.clear()
        clearSelectLine()
        update()
    }
}
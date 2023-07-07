package com.navinfo.collect.library.map.layers

import android.graphics.Color
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.utils.GeometryTools
import org.oscim.layers.vector.VectorLayer
import org.oscim.layers.vector.geometries.Drawable
import org.oscim.layers.vector.geometries.LineDrawable
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map


class NoteLineLayer(map: Map) : VectorLayer(map) {
    private val lineMap = HashMap<String, MutableList<Drawable>>()


    private var selectDrawable: Drawable? = null

    private val selectStyle =
        Style.builder().fillColor(Color.GREEN).strokeColor(Color.GREEN)
            .strokeWidth(10f).fixed(false).build()


    @Synchronized
    fun showNoteBeanLines(noteBean: NoteBean) {
        removeNoteBeanLines(noteBean)
        val list = mutableListOf<Drawable>()
        for (item in noteBean.list) {
            val lineDrawable =
                LineDrawable(GeometryTools.createGeometry(item.geometry), getStyle(item.style))
            add(lineDrawable)
            list.add(lineDrawable)
        }
        lineMap[noteBean.id] = list
        update()
    }


    @Synchronized
    fun removeNoteBeanLines(noteBean: NoteBean) {
        if (lineMap.containsKey(noteBean.id)) {
            for (drawable in lineMap[noteBean.id]!!) {
                remove(drawable)
            }
            lineMap.remove(noteBean.id)
        }
        update()
    }


    private fun getStyle(style: String): Style {
//        if (style.startsWith("4")) {
//            canvasStyle = CanvasView.CanvasStyle.RAILWAY_LINE
//        } else if (style.startsWith("5")) {
//            if (style.contains("cde3ac")) {
//                canvasStyle = CanvasView.CanvasStyle.GREENLAND_LINE
//            } else if (style.contains("abcaff")) {
//                canvasStyle = CanvasView.CanvasStyle.WATER_LINE
//            } else if (style.contains("fffe98")) {
//                canvasStyle = CanvasView.CanvasStyle.PARKING_LINE
//            }
//        } else {
//            val s: String = style.substring(0, 1)
//            if (TextUtils.equals(s, "2")) {
//                canvasStyle = CanvasView.CanvasStyle.STRAIGHT_LINE
//            } else if (TextUtils.equals(s, "3")) {
//                canvasStyle = CanvasView.CanvasStyle.RECT_LINE
//            } else if (TextUtils.equals(s, "6")) {
//                canvasStyle = CanvasView.CanvasStyle.POLY_LINE
//            } else if (TextUtils.equals(s, "7")) {
//                canvasStyle = CanvasView.CanvasStyle.ELLIPSE_LINE
//            } else if (TextUtils.equals(s, "9")) {
//                canvasStyle = CanvasView.CanvasStyle.CIRCULAR_POINT
//            } else if (TextUtils.equals(s, "1")) {
//                canvasStyle = CanvasView.CanvasStyle.FREE_LINE
//            }
        val width = style.substring(1, 3).toFloat()
        var colorStr: String = style.substring(3, style.length)
        colorStr = if (colorStr.length == 6) {
            "#ff$colorStr"
        } else {
            "#ff000000"
        }
//        val color = colorStr.toLong(16).toInt()
        return Style.builder().fillColor(colorStr).fillAlpha(0.5f).strokeColor(colorStr)
            .strokeWidth(width).fixed(true).build()
    }

    fun removeAll() {
        for ((_, value) in lineMap) {
            for (item in value) {
                remove(item)
            }
        }
        lineMap.clear()
        update()
    }
}
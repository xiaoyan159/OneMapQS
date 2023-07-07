package com.navinfo.omqs.ui.fragment.note

import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.data.entity.SketchAttachContent
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.ui.fragment.note.CanvasView.CanvasStyle
import io.realm.RealmList
import org.locationtech.jts.geom.Coordinate
import org.oscim.backend.canvas.Color
import org.oscim.core.GeoPoint
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author zhjch
 * @version V1.0
 * @ClassName: CanvasViewHelper
 * @Date 2016/5/16
 * @Description: ${TODO}(用一句话描述该文件做什么)
 */
object CanvasViewHelper {
    private const val mD2I = 3600000
    fun createNoteBean(
        controller: NIMapController,
        mCurrentPaths: List<CanvasView.DrawPath>,
    ): NoteBean {
        val noteBean = NoteBean(UUID.randomUUID().toString())
        if (mCurrentPaths.isNotEmpty()) {
            val list: RealmList<SketchAttachContent> = RealmList<SketchAttachContent>()
            noteBean.list = list
            for (index in mCurrentPaths.indices) {
                val dp: CanvasView.DrawPath = mCurrentPaths[index]
                val geo = SketchAttachContent(UUID.randomUUID().toString())
                val pointList = dp.pointList ?: continue
                if (dp.style === CanvasStyle.GREENLAND_LINE || dp.style === CanvasStyle.WATER_LINE || dp.style === CanvasStyle.PARKING_LINE) {
                    val geoPointList = mutableListOf<GeoPoint>()
                    for (i in pointList.indices) {
                        val point = pointList[i]
                        val geoPoint: GeoPoint = controller.viewportHandler.fromScreenPoint(
                            point
                        )
                        geoPointList.add(geoPoint)
                        if (index == 0 && i == 0) {
                            noteBean.guideGeometry =
                                GeometryTools.createGeometry(geoPoint).toText()
                        }
                    }
                    geo.style = createLineStyle(dp.style, dp.width, dp.color)
                    geo.geometry = GeometryTools.createPolygon(geoPointList).toText()
                } else if (dp.style === CanvasStyle.CIRCULAR_POINT) {
                    val point = pointList[0]
                    val geoPoint: GeoPoint = controller.viewportHandler.fromScreenPoint(point)
                    geo.style = createLineStyle(dp.style, dp.width, dp.color)
                    geo.geometry = GeometryTools.createGeometry(geoPoint).toText()
                    noteBean.guideGeometry = geo.geometry
                } else if (dp.style === CanvasStyle.ELLIPSE_LINE) {
                    dp.rect?.let {
                        val pointLT = Point(it.left, it.top)
                        val pointRB = Point(it.right, it.bottom)
                        val geoPointLT: GeoPoint =
                            controller.viewportHandler.fromScreenPoint(pointLT)
                        val geoPointRB: GeoPoint =
                            controller.viewportHandler.fromScreenPoint(pointRB)
                        val minX: Double
                        val maxX: Double
                        val minY: Double
                        val maxY: Double
                        if (geoPointLT.longitude < geoPointRB.longitude) {
                            minX = (geoPointLT.longitude * mD2I)
                            maxX = (geoPointRB.longitude * mD2I)
                        } else {
                            minX = (geoPointRB.longitude * mD2I)
                            maxX = (geoPointLT.longitude * mD2I)
                        }
                        if (geoPointLT.latitude < geoPointRB.latitude) {
                            minY = (geoPointLT.latitude * mD2I)
                            maxY = (geoPointRB.latitude * mD2I)
                        } else {
                            minY = (geoPointRB.latitude * mD2I)
                            maxY = (geoPointLT.latitude * mD2I)
                        }
                        val xR = (maxX - minX) / 2
                        val yR = (maxY - minY) / 2
                        var a = 0.0
                        var tempX = xR * cos(a) + xR + minX
                        val tempY = yR * sin(a) + yR + minY
                        val firstX = tempX
                        val geoPointList = mutableListOf<GeoPoint>()
                        geoPointList.add(GeoPoint(tempX / mD2I, tempY / mD2I))
                        var bLeft = false
                        var bRight = false
                        var zeng = 0.1
                        if (controller.mMapView.mapLevel >= 20) {
                            zeng = 0.2
                        }
                        while (!bLeft || !bRight) {
                            a += zeng
                            val x1 = (xR * cos(a) + xR + minX).toInt().toDouble()
                            val y1 = (yR * sin(a) + yR + minY).toInt().toDouble()
                            if (!bLeft && x1 > tempX) {
                                bLeft = true
                            }
                            if (!bRight && bLeft && x1 <= tempX) {
                                bRight = true
                                geoPointList.add(
                                    GeoPoint(
                                        firstX / mD2I,
                                        tempY / mD2I
                                    )
                                )
                            } else {
                                tempX = x1
                                geoPointList.add(GeoPoint(x1 / mD2I, y1 / mD2I))
                            }
                        }
                        if (index == 0) {
                            noteBean.guideGeometry =
                                GeometryTools.createGeometry(geoPointList[0]).toText()
                        }
                        geo.style = createLineStyle(dp.style, dp.width, dp.color)
                        geo.geometry = GeometryTools.createLineString(geoPointList).toText()
                    }
                } else {
                    val geoPointList = mutableListOf<GeoPoint>()
                    for (i in pointList.indices) {
                        val point = pointList[i]
                        val geoPoint: GeoPoint =
                            controller.viewportHandler.fromScreenPoint(point)
                        geoPointList.add(geoPoint)
                        if (index == 0 && i == 0) {
                            noteBean.guideGeometry =
                                GeometryTools.createGeometry(geoPoint).toText()
                        }
                    }
                    geo.style = createLineStyle(dp.style, dp.width, dp.color)
                    geo.geometry = GeometryTools.createLineString(geoPointList).toText()
                }
                list.add(geo)
            }
        }
        return noteBean
    }

    fun createDrawPaths(
        controller: NIMapController,
        att: NoteBean
    ): MutableList<CanvasView.DrawPath> {
        val contents: List<SketchAttachContent> = att.list
        val drawPaths: MutableList<CanvasView.DrawPath> = mutableListOf()
        var width = 5
        var canvasStyle = CanvasStyle.FREE_LINE
        var color = Color.BLACK
        for (geo in contents) {
            var max_x = 0
            var max_y = 0
            var min_x = 0
            var min_y = 0
            val style = geo.style
            if (!TextUtils.isEmpty(style) && style.length > 3) {
                try {
                    if (style.startsWith("4")) {
                        canvasStyle = CanvasStyle.RAILWAY_LINE
                    } else if (style.startsWith("5")) {
                        if (style.contains("cde3ac")) {
                            canvasStyle = CanvasStyle.GREENLAND_LINE
                        } else if (style.contains("abcaff")) {
                            canvasStyle = CanvasStyle.WATER_LINE
                        } else if (style.contains("fffe98")) {
                            canvasStyle = CanvasStyle.PARKING_LINE
                        }
                    } else {
                        val s = style.substring(0, 1)
                        if (TextUtils.equals(s, "2")) {
                            canvasStyle = CanvasStyle.STRAIGHT_LINE
                        } else if (TextUtils.equals(s, "3")) {
                            canvasStyle = CanvasStyle.RECT_LINE
                        } else if (TextUtils.equals(s, "6")) {
                            canvasStyle = CanvasStyle.POLY_LINE
                        } else if (TextUtils.equals(s, "7")) {
                            canvasStyle = CanvasStyle.ELLIPSE_LINE
                        } else if (TextUtils.equals(s, "9")) {
                            canvasStyle = CanvasStyle.CIRCULAR_POINT
                        } else if (TextUtils.equals(s, "1")) {
                            canvasStyle = CanvasStyle.FREE_LINE
                        }
                        width = style.substring(1, 3).toInt()
                        var colorStr = style.substring(3, style.length)
                        if (colorStr.length == 6) {
                            colorStr = "ff$colorStr"
                        } else if (colorStr.length == 8) {
                        } else {
                            colorStr = "ff000000"
                        }
                        color = colorStr.toLong(16).toInt()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val path = Path()
                val pointList: MutableList<Point> = ArrayList()
                if (canvasStyle === CanvasStyle.GREENLAND_LINE || canvasStyle === CanvasStyle.WATER_LINE || canvasStyle === CanvasStyle.PARKING_LINE) {
//                    val polygonGeometry: PolygonGeometry = geo.geo as PolygonGeometry
//                    if (polygonGeometry != null) {
//                        val xyz: Array<Array<DoubleArray>> = polygonGeometry.getCoordinates()
//                        if (xyz != null && xyz.isNotEmpty() && xyz[0].size > 1) {
//                            var geoPoint: GeoPoint? = GeoPoint(xyz[0][0][0], xyz[0][0][1])
//                            val movePoint: Point = .geoToScreen(geoPoint)
//                            max_x = movePoint.x
//                            max_y = movePoint.y
//                            min_x = movePoint.x
//                            min_y = movePoint.y
//                            path.reset()
//                            path.moveTo(movePoint.x.toFloat(), movePoint.y.toFloat())
//                            pointList.add(Point(movePoint.x, movePoint.y))
//                            for (i in 1 until xyz[0].size) {
//                                val x_y = xyz[0][i]
//                                if (x_y != null) {
//                                    geoPoint = GeoPoint(x_y[0], x_y[1])
//                                    val point: Point = projection.geoToScreen(geoPoint)
//                                    if (point.x > max_x) {
//                                        max_x = point.x
//                                    }
//                                    if (point.x < min_x) {
//                                        min_x = point.x
//                                    }
//                                    if (point.y > max_y) {
//                                        max_y = point.y
//                                    }
//                                    if (point.y < min_y) {
//                                        min_y = point.y
//                                    }
//                                    path.lineTo(point.x.toFloat(), point.y.toFloat())
//                                    pointList.add(point)
//                                }
//                            }
//                            path.close()
//                        }
//                    }
//                    val drawPath =
//                        CanvasView.DrawPath(pointList[0], path, width, color, canvasStyle)
//                    val rect = Rect(min_x, min_y, max_x, max_y)
//                    drawPath.rect = rect
//                    drawPath.pointList = pointList
//                    drawPaths.add(drawPath)
                } else if (canvasStyle === CanvasStyle.CIRCULAR_POINT) {
//                    val pointGeometry: PointGeometry = geo.geo as PointGeometry
//                    if (pointGeometry != null && pointGeometry.getCoordinates() != null) {
//                        val geoPoint: GeoPoint = GeoPoint(
//                            pointGeometry.getCoordinates().get(0),
//                            pointGeometry.getCoordinates().get(1)
//                        )
//                        val movePoint: Point = projection.geoToScreen(geoPoint)
//                        pointList.add(movePoint)
//                        val drawPath = DrawPath(movePoint, path, width, color, canvasStyle)
//                        val rect = Rect(
//                            movePoint.x - width - 20,
//                            movePoint.y - width - 20,
//                            movePoint.x + width + 20,
//                            movePoint.y + width + 20
//                        )
//                        drawPath.rect = rect
//                        drawPath.pointList = pointList
//                        drawPaths.add(drawPath)
//                    }
                } else if (canvasStyle === CanvasStyle.ELLIPSE_LINE) {
//                    val lineGeometry = GeometryTools.createGeometry(geo.geometry)
//                    if (lineGeometry != null) {
//                        val xys: Array<out Coordinate> = lineGeometry.coordinates
//                        if (xys != null && xys.size > 1) {
//                            var geoPoint: GeoPoint? = GeoPoint(xys[0].y, xys[0].x)
//                            val movePoint: Point = projection.geoToScreen(geoPoint)
//                            max_x = movePoint.x
//                            max_y = movePoint.y
//                            min_x = movePoint.x
//                            min_y = movePoint.y
//                            path.reset()
//                            path.moveTo(movePoint.x.toFloat(), movePoint.y.toFloat())
//                            pointList.add(Point(movePoint.x, movePoint.y))
//                            for (i in 1 until xys.size) {
//                                val x_y = xys[i]
//                                geoPoint = GeoPoint(x_y[0], x_y[1])
//                                val point: Point = projection.geoToScreen(geoPoint)
//                                if (point.x > max_x) {
//                                    max_x = point.x
//                                }
//                                if (point.x < min_x) {
//                                    min_x = point.x
//                                }
//                                if (point.y > max_y) {
//                                    max_y = point.y
//                                }
//                                if (point.y < min_y) {
//                                    min_y = point.y
//                                }
//                                pointList.add(point)
//                            }
//                            path.addOval(
//                                RectF(
//                                    min_x.toFloat(),
//                                    min_y.toFloat(),
//                                    max_x.toFloat(),
//                                    max_y.toFloat()
//                                ), Path.Direction.CW
//                            )
//                        }
//                    }
//                    val drawPath =
//                        CanvasView.DrawPath(pointList[0], path, width, color, canvasStyle)
//                    val rect = Rect(min_x, min_y, max_x, max_y)
//                    drawPath.rect = rect
//                    drawPath.pointList = pointList
//                    drawPaths.add(drawPath)
                } else {
                    val lineGeometry = GeometryTools.createGeometry(geo.geometry)
                    if (lineGeometry != null) {
                        val xys: Array<out Coordinate> = lineGeometry.coordinates
                        if (xys.size > 1) {
                            var geoPoint = GeoPoint(xys[0].y, xys[0].x)
                            val movePoint: Point =
                                controller.viewportHandler.toScreenPoint(geoPoint)
                            max_x = movePoint.x
                            max_y = movePoint.y
                            min_x = movePoint.x
                            min_y = movePoint.y
                            path.reset()
                            path.moveTo(movePoint.x.toFloat(), movePoint.y.toFloat())
                            pointList.add(Point(movePoint.x, movePoint.y))
                            for (i in 1 until xys.size) {
                                val x_y = xys[i]
                                geoPoint = GeoPoint(x_y.y, x_y.x)
                                val point: Point =
                                    controller.viewportHandler.toScreenPoint(geoPoint)
                                if (point.x > max_x) {
                                    max_x = point.x
                                }
                                if (point.x < min_x) {
                                    min_x = point.x
                                }
                                if (point.y > max_y) {
                                    max_y = point.y
                                }
                                if (point.y < min_y) {
                                    min_y = point.y
                                }
                                if (canvasStyle === CanvasStyle.FREE_LINE) {
                                    val dx = abs(point.x - movePoint.x).toFloat()
                                    val dy = abs(point.y - movePoint.y).toFloat()
                                    if (dx >= 4 || dy >= 4) {
                                        path.quadTo(
                                            movePoint.x.toFloat(),
                                            movePoint.y.toFloat(),
                                            ((point.x + movePoint.x) / 2).toFloat(),
                                            ((point.y + movePoint.y) / 2).toFloat()
                                        ) //源代码是这样写的，可是我没有弄明白，为什么要这样？
                                        movePoint.x = point.x
                                        movePoint.y = point.y
                                    }
                                } else {
                                    path.lineTo(point.x.toFloat(), point.y.toFloat())
                                }
                                pointList.add(point)
                            }
                        }
                    }
                    val drawPath =
                        CanvasView.DrawPath(pointList[0], path, width, color, canvasStyle)
                    val rect = Rect(min_x, min_y, max_x, max_y)
                    drawPath.rect = rect
                    drawPath.pointList = pointList
                    drawPaths.add(drawPath)
                }
            }
        }
        return drawPaths
    }

    private fun createLineStyle(canvasStyle: CanvasStyle, width: Int, color: Int): String {
        val style = StringBuilder()
        if (canvasStyle === CanvasStyle.RAILWAY_LINE) {
            return "4060070c004ffffff16"
        } else if (canvasStyle === CanvasStyle.GREENLAND_LINE) {
            return "50200b050cde3ac"
        } else if (canvasStyle === CanvasStyle.WATER_LINE) {
            return "50200b050abcaff"
        } else if (canvasStyle === CanvasStyle.PARKING_LINE) {
            return "502a6a6a6fffe98"
        }
        if (canvasStyle === CanvasStyle.STRAIGHT_LINE) {
            style.append("2")
        } else if (canvasStyle === CanvasStyle.RECT_LINE) {
            style.append("3")
        } else if (canvasStyle === CanvasStyle.POLY_LINE) {
            style.append("6")
        } else if (canvasStyle === CanvasStyle.ELLIPSE_LINE) {
            style.append("7")
        } else if (canvasStyle === CanvasStyle.CIRCULAR_POINT) {
            style.append("9")
        } else {
            style.append("1")
        }
        if (width < 10) {
            style.append("0")
        }
        style.append(width.toString())
        try {
            var colorString = Integer.toHexString(color).toString()
            if (colorString.length == 8) {
                colorString = TextUtils.substring(colorString, 2, 8)
            }
            style.append(colorString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return style.toString()
    }
}
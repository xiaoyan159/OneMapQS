package com.navinfo.omqs.db

import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.utils.GeometryTools
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry


class ImportPreProcess {
    /**
     * 预处理所需要的函数
     * */
    fun foo(renderEntity: RenderEntity): RenderEntity {
        println("foo")
        renderEntity.properties["foo"] = "bar"
        return renderEntity
    }

    /**
     * 计算指定数据指定方向的坐标
     * */
    fun translateRight(renderEntity: RenderEntity): RenderEntity {
        // 获取当前renderEntity的geometry
        val geometry = renderEntity.wkt
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var point = Coordinate(geometry?.coordinate)
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            val angle = if(renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get("angle")?.toDouble()!!
            radian = Math.toRadians(angle)
        } else if (Geometry.TYPENAME_LINESTRING == geometry?.geometryType) {
            val p1: Coordinate = geometry.coordinates.get(geometry.coordinates.size - 2)
            val p2: Coordinate = geometry.coordinates.get(geometry.coordinates.size - 1)
            // 计算线段的方向
            radian = Angle.angle(p1, p2)
            point = p2
        }

        // 计算偏移距离
        val dx: Double = GeometryTools.convertDistanceToDegree(5.0, geometry?.coordinate?.y!!) * Math.cos(radian)
        val dy: Double = GeometryTools.convertDistanceToDegree(5.0, geometry?.coordinate?.y!!) * Math.sin(radian)

        // 计算偏移后的点
        val coord =
            Coordinate(point.getX() + dy, point.getY() - dx)

        // 将这个点记录在数据中
        val geometryTranslate: Geometry = GeometryTools.createGeometry(doubleArrayOf(coord.x, coord.y))
        renderEntity.geometry = geometryTranslate.toString()
        return renderEntity
    }

    fun addAngleFromGeometry(renderEntity: RenderEntity): String {
        if (!renderEntity.properties.containsKey("angle")) {
            if (renderEntity.wkt!=null) {
                val geometry = renderEntity.wkt
                var angle: String = "90"
                if (geometry?.numPoints!!>=2) {
                    val p1: Coordinate = geometry?.coordinates?.get(geometry.coordinates.size - 2)!!
                    val p2: Coordinate = geometry?.coordinates?.get(geometry.coordinates.size - 1)!!
                    // 弧度转角度
                    angle = Math.toDegrees(Angle.angle(p1, p2)).toString()
                } else {
                    angle = "90"
                }
                // 计算线段的方向
                renderEntity.properties["angle"] = angle
                return angle
            }
        }
        return "0"
    }
}
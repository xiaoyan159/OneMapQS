package com.navinfo.omqs.db

import android.util.Log
import com.navinfo.collect.library.data.entity.ReferenceEntity
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.utils.GeometryTools
import io.realm.Realm
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.oscim.core.GeoPoint


class ImportPreProcess {
    val code2NameMap = Code2NameMap()
    lateinit var cacheRdLink: Map<String?, RenderEntity>
    val defaultTranslateDistance = 3.0

    fun checkCircleRoad(renderEntity: RenderEntity): Boolean {
        val linkInId  = renderEntity.properties["linkIn"]
        val linkOutId  = renderEntity.properties["linkOut"]
        // 根据linkIn和linkOut获取对应的link数据
        val linkInEntity = cacheRdLink[linkInId]
        val linkOutEntity = cacheRdLink[linkOutId]
        Log.d("checkCircleRoad", "LinkInEntity: ${linkInId}- ${linkInEntity?.properties?.get("snodePid")}，LinkOutEntity: ${linkOutId}- ${linkOutEntity?.properties?.get("enodePid")}")
        // 查询linkIn的sNode和linkOut的eNode是否相同，如果相同，认为数据是环形路口，返回false
        if (linkInEntity!=null&&linkOutEntity!=null) {
            if (linkInEntity.properties["snodePid"] == linkOutEntity.properties["enodePid"] || linkInEntity.properties["enodePid"] == linkOutEntity.properties["snodePid"]
                || linkInEntity.properties["snodePid"] == linkOutEntity.properties["snodePid"]|| linkInEntity.properties["enodePid"] == linkOutEntity.properties["enodePid"])
            return false
        }
        return true
    }
    /**
     * 计算指定数据指定方向的坐标
     * @param direction 判断当前数据是否为逆向，给定的应该是一个a=b的表达式，a为对应的properties的key，b为对应的值
     * */
    fun translateRight(renderEntity: RenderEntity, direction: String = "") {
        // 获取当前renderEntity的geometry
        val geometry = renderEntity.wkt
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var point = Coordinate(geometry?.coordinate)
        var isReverse = false // 是否为逆向
        if (direction.isNotEmpty()) {
            val paramDirections = direction.split("=")
            if (paramDirections.size>=2 && renderEntity.properties[paramDirections[0].trim()] == paramDirections[1].trim()) {
                isReverse = true;
            }
        }
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) { // angle为与正北方向的顺时针夹角
            var angle = if(renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get("angle")?.toDouble()!!
//            if (isReverse) {
//                angle += 180
//            }
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle=(450-angle)%360
            radian = Math.toRadians(angle)
        } else if (Geometry.TYPENAME_LINESTRING == geometry?.geometryType) {
            var coordinates = geometry.coordinates
            if (isReverse) {
                coordinates = coordinates.reversedArray()
            }
            val p1: Coordinate = coordinates.get(coordinates.size - 2)
            val p2: Coordinate = coordinates.get(coordinates.size - 1)
            // 计算线段的方向
            radian = Angle.angle(p1, p2)
            point = p1
        }

        // 计算偏移距离
        val dx: Double = GeometryTools.convertDistanceToDegree(defaultTranslateDistance, geometry?.coordinate?.y!!) * Math.cos(radian)
        val dy: Double = GeometryTools.convertDistanceToDegree(defaultTranslateDistance, geometry?.coordinate?.y!!) * Math.sin(radian)

        // 计算偏移后的点
        val coord =
            Coordinate(point.getX() + dy, point.getY() - dx)

        // 记录偏移后的点位或线数据，如果数据为线时，记录的偏移后数据为倒数第二个点右移后，方向与线的最后两个点平行同向的单位向量
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            val geometryTranslate: Geometry = GeometryTools.createGeometry(doubleArrayOf(coord.x, coord.y))
            renderEntity.geometry = geometryTranslate.toString()
        } else {
            val coorEnd = Coordinate(coord.x+dx, coord.y+dy)
            val geometryTranslate: Geometry = GeometryTools.createLineString(arrayOf(coord, coorEnd))
            renderEntity.geometry = geometryTranslate.toString()
        }
    }

    /**
     * 向方向对应的反方向偏移
     * */
    fun translateBack(renderEntity: RenderEntity, direction: String = "") {
        // 获取当前renderEntity的geometry
        val geometry = renderEntity.wkt
        var isReverse = false // 是否为逆向
        if (direction.isNotEmpty()) {
            val paramDirections = direction.split("=")
            if (paramDirections.size>=2 && renderEntity.properties[paramDirections[0].trim()] == paramDirections[1].trim()) {
                isReverse = true;
            }
        }
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var point = Coordinate(geometry?.coordinate)
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            var angle = if(renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get("angle")?.toDouble()!!
//            if (isReverse) {
//                angle += 180
//            }
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle=(450-angle)%360
            radian = Math.toRadians(angle)
        } else if (Geometry.TYPENAME_LINESTRING == geometry?.geometryType) {
            var coordinates = geometry.coordinates
            if (isReverse) {
                coordinates = coordinates.reversedArray()
            }
            val p1: Coordinate = coordinates.get(coordinates.size - 2)
            val p2: Coordinate = coordinates.get(coordinates.size - 1)
            // 计算线段的方向
            radian = Angle.angle(p1, p2)
            point = p2
        }

        // 计算偏移距离
        val dx: Double = GeometryTools.convertDistanceToDegree(defaultTranslateDistance, geometry?.coordinate?.y!!) * Math.cos(radian)
        val dy: Double = GeometryTools.convertDistanceToDegree(defaultTranslateDistance, geometry?.coordinate?.y!!) * Math.sin(radian)

        // 计算偏移后的点
        val coord =
            Coordinate(point.getX() - dx, point.getY() - dy)

        // 将这个点记录在数据中
        val geometryTranslate: Geometry = GeometryTools.createGeometry(doubleArrayOf(coord.x, coord.y))
        renderEntity.geometry = geometryTranslate.toString()
    }

    /**
     * 生成偏移后数据的起终点参考线
     * */
    fun generateS2EReferenceLine(renderEntity: RenderEntity) {
        // 获取当前renderEntity的geometry，该坐标为偏移后坐标，即为终点
        val translateGeometry = renderEntity.wkt
        val startGeometry = GeometryTools.createGeometry(renderEntity.properties["geometry"])

        var pointEnd = translateGeometry!!.coordinates[translateGeometry.numPoints-1] // 获取这个geometry对应的结束点坐标
        var pointStart = startGeometry!!.coordinates[startGeometry.numPoints-1] // 获取这个geometry对应的结束点坐标
        if (translateGeometry.geometryType == Geometry.TYPENAME_LINESTRING) { // 如果是线数据，则取倒数第二个点作为偏移的起止点
            pointEnd = translateGeometry!!.coordinates[translateGeometry.numPoints-2] // 获取这个geometry对应的结束点坐标
        }
        if (startGeometry.geometryType == Geometry.TYPENAME_LINESTRING) { // 如果是线数据，则取倒数第二个点作为偏移的起止点
            pointStart = startGeometry!!.coordinates[startGeometry.numPoints-2] // 获取这个geometry对应的结束点坐标
        }

        // 将这个起终点的线记录在数据中
        val startEndReference = ReferenceEntity()
        startEndReference.renderEntityId = renderEntity.id
        startEndReference.name = "${renderEntity.name}参考线"
        startEndReference.table = renderEntity.table
        // 起终点坐标组成的线
        startEndReference.geometry = GeometryTools.createLineString(arrayOf<Coordinate>(pointStart, pointEnd)).toString()
        startEndReference.properties["qi_table"] = renderEntity.table
        startEndReference.properties["type"] = "s_2_e"
        Realm.getDefaultInstance().insert(startEndReference)
    }

    /**
     * 生成与对应方向相同的方向线，用以绘制方向箭头
     * */
    fun generateDirectReferenceLine(renderEntity: RenderEntity, direction: String = "") {
        // 根据数据或angle计算方向对应的角度和偏移量
        val geometry = renderEntity.wkt
        var isReverse = false // 是否为逆向
        if (direction.isNotEmpty()) {
            val paramDirections = direction.split("=")
            if (paramDirections.size>=2 && renderEntity.properties[paramDirections[0].trim()] == paramDirections[1].trim()) {
                isReverse = true
            }
        }
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var point = Coordinate(geometry?.coordinate)
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            point = Coordinate(geometry?.coordinate)
            var angle = if(renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get("angle")?.toDouble()!!
//            if (isReverse) {
//                angle += 180
//            }
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle=(450-angle)%360
            radian = Math.toRadians(angle)
        } else if (Geometry.TYPENAME_LINESTRING == geometry?.geometryType) {
            var coordinates = geometry.coordinates
            if (isReverse) {
                coordinates = coordinates.reversedArray()
            }
            val p1: Coordinate = coordinates.get(coordinates.size - 2)
            val p2: Coordinate = coordinates.get(coordinates.size - 1)
            // 计算线段的方向
            radian = Angle.angle(p1, p2)
            point = p1
        }

        // 计算偏移距离
        val dx: Double = GeometryTools.convertDistanceToDegree(defaultTranslateDistance, geometry?.coordinate?.y!!) * Math.cos(radian)
        val dy: Double = GeometryTools.convertDistanceToDegree(defaultTranslateDistance, geometry?.coordinate?.y!!) * Math.sin(radian)

        val coorEnd = Coordinate(point.getX() + dx, point.getY() + dy)

        val angleReference = ReferenceEntity()
        angleReference.renderEntityId = renderEntity.id
        angleReference.name = "${renderEntity.name}参考方向"
        angleReference.table = renderEntity.table
        // 与原有方向指向平行的线
        angleReference.geometry = GeometryTools.createLineString(arrayOf(point, coorEnd)).toString()
        angleReference.properties["qi_table"] = renderEntity.table
        angleReference.properties["type"] = "angle"
        Realm.getDefaultInstance().insert(angleReference)
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

    /**
     * 解析车道边线数据二级属性
     * */
    fun unpackingLaneBoundary(renderEntity: RenderEntity) {
        var shape:JSONObject = JSONObject(mapOf(
            "lateralOffset" to 0,
            "markType" to 1,
            "markColor" to 0,
            "markMaterial" to 1,
            "markSeqNum" to 1,
            "markWidth" to 10,
            "markingCount" to 1
        ))
        if (renderEntity.code == 2013&&!renderEntity.properties["shapeList"].isNullOrEmpty()&&renderEntity.properties["shapeList"]!="null") {
            // 解析shapeList，将数组中的属性放会properties
            val shapeList = JSONArray(renderEntity.properties["shapeList"])
            for (i in 0 until shapeList.length()) {
                shape = shapeList.getJSONObject(i)
                if (shape.optInt("lateralOffset", 0) == 0) {
                    break
                }
            }
        }
        for (key in shape.keys()) {
            renderEntity.properties[key] = shape[key].toString()
        }
    }

    /**
     * 解析车信数据二级属性
     * */
    fun unpackingLaneInfo(renderEntity: RenderEntity) {
        if (renderEntity.code == 4601) {
            if (!renderEntity.properties["laneinfoGroup"].isNullOrEmpty()&&renderEntity.properties["laneinfoGroup"]!="null") {
                // 解析laneinfoGroup，将数组中的属性放会properties
                val laneinfoGroup = JSONArray(renderEntity.properties["laneinfoGroup"].toString().replace("{", "[").replace("}", "]"))
                // 分别获取两个数组中的数据，取第一个作为主数据，另外两个作为辅助渲染数据
                val laneInfoDirectArray = JSONArray(laneinfoGroup[0].toString())
                val laneInfoTypeArray = JSONArray(laneinfoGroup[1].toString())

                for (i in 0 until laneInfoDirectArray.length()) {
                    // 根据后续的数据生成辅助表数据
                    val referenceEntity = ReferenceEntity()
                    referenceEntity.renderEntityId = renderEntity.id
                    referenceEntity.name = "${renderEntity.name}参考方向"
                    referenceEntity.table = renderEntity.table
                    // 与原数据使用相同的geometry
                    referenceEntity.geometry = renderEntity.geometry.toString()
                    referenceEntity.properties["qi_table"] = renderEntity.table
                    referenceEntity.properties["currentDirect"] = laneInfoDirectArray[i].toString().split(",").distinct().joinToString("_")
                    referenceEntity.properties["currentType"] = laneInfoTypeArray[i].toString().split(",").distinct().joinToString("_")
                    Realm.getDefaultInstance().insert(referenceEntity)
                }
            }
        }
    }



    /**
     * 生成默认道路名数据
     * */
    fun generateRoadName(renderEntity: RenderEntity) {
        // LinkName的真正名称数据，是保存在properties的shapeList中的，因此需要解析shapeList数据
        var shape :JSONObject? = null
        if (renderEntity.properties.containsKey("shapeList")) {
            val shapeListJsonArray: JSONArray = JSONArray(renderEntity.properties["shapeList"])
            for (i in 0 until shapeListJsonArray.length()) {
                val shapeJSONObject = shapeListJsonArray.getJSONObject(i)
                if (shapeJSONObject["nameClass"]==1) {
                    if (shape == null) {
                        shape = shapeJSONObject
                    }
                    // 获取第一官方名
                    //("名称分类"NAME_CLASS =“1 官方名”，且名称序号SEQ_NUM 最小者）
                    if (shapeJSONObject["seqNum"].toString().toInt()< shape!!["seqNum"].toString().toInt()) {
                        shape = shapeJSONObject
                    }
                }
            }
        }
        // 获取最小的shape值，将其记录增加记录在properties的name属性下
        if(shape!=null) {
            renderEntity.properties["name"] = shape["name"].toString()
        } else {
            renderEntity.properties["name"] = ""
        }
    }

    /**
     * 生成电子眼对应的渲染名称
     * */
    fun generateElectronName(renderEntity: RenderEntity) {
        // 解析电子眼的kind，将其转换为渲染的简要名称
        var shape :JSONObject? = null
        if (renderEntity.properties.containsKey("kind")) {
            renderEntity.properties["name"] = code2NameMap.electronEyeKindMap[renderEntity.properties["kind"].toString().toInt()]
        } else {
            renderEntity.properties["name"] = ""
        }
    }

    /**
     * 生成默认路口数据的参考数据
     * */
    fun generateIntersectionReference(renderEntity: RenderEntity) {
        // 路口数据的其他点位，是保存在nodeList对应的数组下
        if (renderEntity.properties.containsKey("nodeList")) {
            val nodeListJsonArray: JSONArray = JSONArray(renderEntity.properties["nodeList"])
            for (i in 0 until nodeListJsonArray.length()) {
                val nodeJSONObject = nodeListJsonArray.getJSONObject(i)
                val intersectionReference = ReferenceEntity()
                intersectionReference.renderEntityId = renderEntity.id
                intersectionReference.name = "${renderEntity.name}参考点"
                intersectionReference.table = renderEntity.table
                // 与原有方向指向平行的线
                intersectionReference.geometry = GeometryTools.createGeometry(nodeJSONObject["geometry"].toString()).toString()
                intersectionReference.properties["qi_table"] = renderEntity.table
                intersectionReference.properties["type"] = "node"
                Realm.getDefaultInstance().insert(intersectionReference)
            }
        }
    }
}
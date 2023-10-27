package com.navinfo.omqs.db

import android.util.Log
import com.google.gson.Gson
import com.navinfo.collect.library.data.entity.LinkRelation
import com.navinfo.collect.library.data.entity.ReferenceEntity
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StrZipUtil
import com.navinfo.omqs.Constant
import io.realm.Realm
import io.realm.RealmModel
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTWriter
import org.oscim.core.GeoPoint
import java.util.*


class ImportPreProcess {
    val code2NameMap = Code2NameMap()

    //    lateinit var cacheRdLink: Map<String?, RenderEntity>
    val defaultTranslateDistance = 3.0
    val testFlag: Boolean = false
    var realm: Realm? = null
    val gson = Gson()
    fun checkCircleRoad(renderEntity: RenderEntity): Boolean {
        val linkInId = renderEntity.properties["linkIn"]
        val linkOutId = renderEntity.properties["linkOut"]
//        // 根据linkIn和linkOut获取对应的link数据
//        val linkInEntity = cacheRdLink[linkInId]
//        val linkOutEntity = cacheRdLink[linkOutId]
        realm?.let {
            val linkInEntity = it.where(RenderEntity::class.java)
                .equalTo("code", DataCodeEnum.OMDB_RD_LINK.code)
                .and().equalTo("linkPid", linkInId)
                .findFirst()
            val linkOutEntity = it.where(RenderEntity::class.java)
                .equalTo("code", DataCodeEnum.OMDB_RD_LINK.code)
                .and().equalTo("linkPid", linkOutId)
                .findFirst()

            Log.d(
                "checkCircleRoad",
                "LinkInEntity: ${linkInId}- ${linkInEntity?.properties?.get("snodePid")}，LinkOutEntity: ${linkOutId}- ${
                    linkOutEntity?.properties?.get("enodePid")
                }"
            )
            // 查询linkIn的sNode和linkOut的eNode是否相同，如果相同，认为数据是环形路口，返回false
            if (linkInEntity != null && linkOutEntity != null) {
                if (linkInEntity.properties["snodePid"] == linkOutEntity.properties["enodePid"] || linkInEntity.properties["enodePid"] == linkOutEntity.properties["snodePid"] || linkInEntity.properties["snodePid"] == linkOutEntity.properties["snodePid"] || linkInEntity.properties["enodePid"] == linkOutEntity.properties["enodePid"]) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 计算指定数据指定方向的坐标
     * @param direction 判断当前数据是否为逆向，给定的应该是一个a=b的表达式，a为对应的properties的key，b为对应的值
     * */
    fun translateRight(renderEntity: RenderEntity, direction: String = "") {
        if (testFlag) {
            return
        }
        // 获取当前renderEntity的geometry
        val geometry = renderEntity.wkt
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向

        var isReverse = false // 是否为逆向
        if (direction.isNotEmpty()) {
            val paramDirections = direction.split("=")
            if (paramDirections.size >= 2 && renderEntity.properties[paramDirections[0].trim()] == paramDirections[1].trim()) {
                isReverse = true;
            }
        }
        // 如果是正向，则取最后一个点作为渲染图标的位置
        var point = geometry!!.coordinates[geometry!!.coordinates.size - 1]
        if (isReverse) {
            // 逆向的话取第一个点作为渲染图标的位置
            point = geometry.coordinates[0]
        }

        // 如果数据属性中存在angle，则使用该值，否则需要根据line中的数据进行计算
        if (renderEntity?.properties?.get(
                "angle"
            ) != null
        ) {
            // 带有angle字段的数据，也有可能是线，需要判断是否需要根据指定字段判断数据是否为逆向

            var angle = renderEntity?.properties?.get("angle")?.toDouble()!!
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle = ((450 - angle) % 360)
            radian = Math.toRadians(angle)
        } else {
            if (Geometry.TYPENAME_LINESTRING == geometry?.geometryType) {
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
        }

        // 计算偏移距离
        val dx: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.cos(radian)
        val dy: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.sin(radian)

        // 计算偏移后的点
        val coord =
            Coordinate(point.getX() + dy, point.getY() - dx)

        // 记录偏移后的点位或线数据，如果数据为线时，记录的偏移后数据为最后一个点右移后，方向与线的最后两个点平行同向的单位向量
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            val geometryTranslate: Geometry =
                GeometryTools.createGeometry(doubleArrayOf(coord.x, coord.y))
            renderEntity.geometry = geometryTranslate.toString()
        } else {
            val coorEnd = Coordinate(coord.x + dx, coord.y + dy)
            val geometryTranslate: Geometry =
                GeometryTools.createLineString(arrayOf(coord, coorEnd))
            renderEntity.geometry = geometryTranslate.toString()
        }
    }

    /**
     * 向方向对应的反方向偏移
     * */
    fun translateBack(renderEntity: RenderEntity, direction: String = "") {
        if (testFlag) {
            return
        }
        // 获取当前renderEntity的geometry
        val geometry = renderEntity.wkt
        var isReverse = false // 是否为逆向
        if (direction.isNotEmpty()) {
            val paramDirections = direction.split("=")
            if (paramDirections.size >= 2 && renderEntity.properties[paramDirections[0].trim()] == paramDirections[1].trim()) {
                isReverse = true;
            }
        }
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var point = Coordinate(geometry?.coordinate)
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            var angle =
                if (renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get(
                    "angle"
                )?.toDouble()!!
//            if (isReverse) {
//                angle += 180
//            }
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle = ((450 - angle) % 360)
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
        val dx: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.cos(radian)
        val dy: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.sin(radian)

        // 计算偏移后的点
        val coord =
            Coordinate(point.getX() - dx, point.getY() - dy)

        // 将这个点记录在数据中
        val geometryTranslate: Geometry =
            GeometryTools.createGeometry(doubleArrayOf(coord.x, coord.y))
        renderEntity.geometry = geometryTranslate.toString()
    }

    /**
     * 生成偏移后数据的起终点参考线
     * */
    fun generateS2EReferenceLine(renderEntity: RenderEntity) {
        if (testFlag) {
            return
        }
        // 获取当前renderEntity的geometry，该坐标为偏移后坐标，即为终点
        val translateGeometry = renderEntity.wkt
        val startGeometry = GeometryTools.createGeometry(renderEntity.properties["geometry"])

        var pointEnd =
            translateGeometry!!.coordinates[translateGeometry.numPoints - 1] // 获取这个geometry对应的结束点坐标
        var pointStart =
            startGeometry!!.coordinates[startGeometry.numPoints - 1] // 获取这个geometry对应的结束点坐标
        if (translateGeometry.geometryType == Geometry.TYPENAME_LINESTRING) { // 如果是线数据，则取倒数第二个点作为偏移的起止点
            pointEnd =
                translateGeometry.coordinates[translateGeometry.numPoints - 2] // 获取这个geometry对应的结束点坐标
        }
        if (startGeometry.geometryType == Geometry.TYPENAME_LINESTRING) { // 如果是线数据，则取倒数第二个点作为偏移的起止点
            pointStart =
                startGeometry.coordinates[startGeometry.numPoints - 2] // 获取这个geometry对应的结束点坐标
        }

        // 将这个起终点的线记录在数据中
        val startEndReference = ReferenceEntity()
//        startEndReference.renderEntityId = renderEntity.id
        startEndReference.name = "${renderEntity.name}参考线"
        startEndReference.table = renderEntity.table
        startEndReference.zoomMin = renderEntity.zoomMin
        startEndReference.zoomMax = renderEntity.zoomMax
        startEndReference.taskId = renderEntity.taskId
        startEndReference.enable = renderEntity.enable
        // 起终点坐标组成的线
        startEndReference.geometry =
            GeometryTools.createLineString(arrayOf<Coordinate>(pointStart, pointEnd)).toString()
        startEndReference.properties["qi_table"] = renderEntity.table
        startEndReference.properties["type"] = "s_2_e"
        val listResult = mutableListOf<ReferenceEntity>()
        startEndReference.propertiesDb = StrZipUtil.compress(
            gson.toJson(startEndReference.properties).toString()
        )
        listResult.add(startEndReference)
        insertData(listResult)
    }

    /**
     * 生成车道类型起终点参考数据
     * */
    fun generateLaneTypeAccessS2ERefPoint(renderEntity: RenderEntity) {
        // 如果车道类型非常规车道(第0bit的数据)，则需要生成辅助数据
        if (renderEntity.properties["laneType"]!!.toInt()>0) {
            val geometry = GeometryTools.createGeometry(renderEntity.properties["geometry"])

            val pointEnd = geometry!!.coordinates[geometry.numPoints - 1] // 获取这个geometry对应的结束点坐标
            val pointStart = geometry!!.coordinates[0] // 获取这个geometry对应的起点
            val listResult = mutableListOf<ReferenceEntity>()

            // 将这个起终点的线记录在数据中
            val startReference = ReferenceEntity()
//            startReference.renderEntityId = renderEntity.id
            startReference.name = "${renderEntity.name}参考点"
            startReference.code = renderEntity.code
            startReference.table = renderEntity.table
            startReference.zoomMin = renderEntity.zoomMin
            startReference.zoomMax = renderEntity.zoomMax
            startReference.taskId = renderEntity.taskId
            startReference.enable = renderEntity.enable

            // 起点坐标
            startReference.geometry =
                GeometryTools.createGeometry(GeoPoint(pointStart.y, pointStart.x)).toString()
            startReference.properties["qi_table"] = renderEntity.table
            startReference.properties["type"] = "s_2_p"
            startReference.properties["geometry"] = startReference.geometry
            listResult.add(startReference)

            val endReference = ReferenceEntity()
//            endReference.renderEntityId = renderEntity.id
            endReference.name = "${renderEntity.name}参考点"
            endReference.code = renderEntity.code
            endReference.table = renderEntity.table
            endReference.zoomMin = renderEntity.zoomMin
            endReference.zoomMax = renderEntity.zoomMax
            endReference.taskId = renderEntity.taskId
            endReference.enable = renderEntity.enable

            // 终点坐标
            endReference.geometry =
                GeometryTools.createGeometry(GeoPoint(pointEnd.y, pointEnd.x)).toString()
            endReference.properties["qi_table"] = renderEntity.table
            endReference.properties["type"] = "e_2_p"
            endReference.properties["geometry"] = endReference.geometry

            listResult.add(endReference)
            insertData(listResult)
        }
    }

    fun generateS2EReferencePoint(
        renderEntity: RenderEntity,
        proKey: String = "",
        table: String = ""
    ) {
        val geometry = GeometryTools.createGeometry(renderEntity.properties["geometry"])

        val pointEnd = geometry!!.coordinates[geometry.numPoints - 1] // 获取这个geometry对应的结束点坐标
        val pointStart = geometry!!.coordinates[0] // 获取这个geometry对应的起点
        val listResult = mutableListOf<ReferenceEntity>()

        // 将这个起终点的线记录在数据中
        val startReference = ReferenceEntity()
//        startReference.renderEntityId = renderEntity.id
        startReference.name = "${renderEntity.name}参考点"
        startReference.code = renderEntity.code
        startReference.table = renderEntity.table
        startReference.zoomMin = renderEntity.zoomMin
        startReference.zoomMax = renderEntity.zoomMax
        startReference.taskId = renderEntity.taskId
        startReference.enable = renderEntity.enable

        // 起点坐标
        startReference.geometry =
            GeometryTools.createGeometry(GeoPoint(pointStart.y, pointStart.x)).toString()
        startReference.properties["qi_table"] = renderEntity.table
        Log.e("qj", "generateS2EReferencePoint===$table===$proKey")
        if (renderEntity.table == table) {
            Log.e("qj", "generateS2EReferencePoint===开始")
            if (renderEntity.properties.containsKey(proKey)) {
                if (renderEntity.properties[proKey] != "") {
                    startReference.properties["type"] = "s_2_p_${renderEntity.properties[proKey]}"
                } else {
                    startReference.properties["type"] = "s_2_p_0"
                }
                Log.e("qj", "generateS2EReferencePoint===s_2_p_${renderEntity.properties[proKey]}")
            }
        } else {
            startReference.properties["type"] = "s_2_p"
            Log.e("qj", "generateS2EReferencePoint===s_2_p${renderEntity.name}")
        }

        Log.e("qj", "generateS2EReferencePoint===${startReference.geometry}")

        startReference.properties["geometry"] = startReference.geometry
        startReference.propertiesDb = StrZipUtil.compress(
            gson.toJson(startReference.properties).toString()
        )
        listResult.add(startReference)

        Log.e("qj", "generateS2EReferencePoint===1")

        val endReference = ReferenceEntity()
//        endReference.renderEntityId = renderEntity.id
        endReference.name = "${renderEntity.name}参考点"
        endReference.code = renderEntity.code
        endReference.table = renderEntity.table
        endReference.zoomMin = renderEntity.zoomMin
        endReference.zoomMax = renderEntity.zoomMax
        endReference.taskId = renderEntity.taskId
        endReference.enable = renderEntity.enable

        Log.e("qj", "generateS2EReferencePoint===2")

        // 终点坐标
        endReference.geometry =
            GeometryTools.createGeometry(GeoPoint(pointEnd.y, pointEnd.x)).toString()
        Log.e("qj", "generateS2EReferencePoint===3")
        endReference.properties["qi_table"] = renderEntity.table
        if (renderEntity.table == table) {
            if (renderEntity.properties.containsKey(proKey)) {
                if (renderEntity.properties[proKey] != "") {
                    endReference.properties["type"] = "e_2_p_${renderEntity.properties[proKey]}"
                } else {
                    endReference.properties["type"] = "e_2_p_0"
                }
            }
        } else {
            endReference.properties["type"] = "e_2_p"
            Log.e("qj", "generateS2EReferencePoint===e_2_p${renderEntity.name}")
        }
        endReference.properties["geometry"] = endReference.geometry
        endReference.propertiesDb = StrZipUtil.compress(
            gson.toJson(endReference.properties).toString()
        )
        listResult.add(endReference)
        Log.e("qj", "generateS2EReferencePoint===4")
        insertData(listResult)
    }

    /**
     * 生成与对应方向相同的方向线，用以绘制方向箭头
     * */
    fun generateDirectReferenceLine(
        renderEntity: RenderEntity,
        direction: String = "",
        distance: String = ""
    ) {
        // 根据数据或angle计算方向对应的角度和偏移量
        val geometry = renderEntity.wkt
        var isReverse = false // 是否为逆向
        if (direction.isNotEmpty()) {
            val paramDirections = direction.split("=")
            if (paramDirections.size >= 2 && renderEntity.properties[paramDirections[0].trim()] == paramDirections[1].trim()) {
                isReverse = true
            }
        }
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var pointStartArray = mutableListOf<Coordinate>()
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            val point = Coordinate(geometry?.coordinate)
            pointStartArray.add(point)
            var angle =
                if (renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get(
                    "angle"
                )?.toDouble()!!
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle = ((450 - angle) % 360)
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
            pointStartArray.add(p1)
        } else if (Geometry.TYPENAME_POLYGON == geometry?.geometryType) {
            // 记录下面数据的每一个点位
            pointStartArray.addAll(geometry.coordinates)
            // 获取当前的面数据对应的方向信息
            var angle = if (renderEntity?.properties?.get("angle") == null) {
                if (renderEntity?.properties?.get("heading") == null) {
                    0.0
                } else {
                    renderEntity?.properties?.get("heading")?.toDouble()!!
                }
            } else renderEntity?.properties?.get("angle")?.toDouble()!!

            angle = ((450 - angle) % 360)
            radian = Math.toRadians(angle)
        }

        // 计算偏移距离
        var dx: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.cos(radian)
        var dy: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.sin(radian)
        if (distance.isNotEmpty()) {
            dx = GeometryTools.convertDistanceToDegree(
                distance.toDouble(),
                geometry?.coordinate?.y!!
            ) * Math.cos(radian)
            dy = GeometryTools.convertDistanceToDegree(
                distance.toDouble(),
                geometry?.coordinate?.y!!
            ) * Math.sin(radian)
        }
        val listResult = mutableListOf<ReferenceEntity>()

        for (pointStart in pointStartArray) {
            val coorEnd = Coordinate(pointStart.getX() + dx, pointStart.getY() + dy, pointStart.z)

            val angleReference = ReferenceEntity()
//            angleReference.renderEntityId = renderEntity.id
            angleReference.name = "${renderEntity.name}参考方向"
            angleReference.table = renderEntity.table
            angleReference.zoomMin = renderEntity.zoomMin
            angleReference.zoomMax = renderEntity.zoomMax
            angleReference.taskId = renderEntity.taskId
            angleReference.enable = renderEntity.enable
            // 与原有方向指向平行的线
            angleReference.geometry =
                WKTWriter(3).write(GeometryTools.createLineString(arrayOf(pointStart, coorEnd)))
            angleReference.properties["qi_table"] = renderEntity.table
            angleReference.properties["type"] = "angle"
            angleReference.propertiesDb = StrZipUtil.compress(
                gson.toJson(angleReference.properties).toString()
            )
            listResult.add(angleReference)
        }
        insertData(listResult)
    }

    fun addAngleFromGeometry(renderEntity: RenderEntity): String {
        if (!renderEntity.properties.containsKey("angle")) {
            if (renderEntity.wkt != null) {
                val geometry = renderEntity.wkt
                var angle: String = "90"
                if (geometry?.numPoints!! >= 2) {
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

        if (renderEntity.code == DataCodeEnum.OMDB_LANE_MARK_BOUNDARYTYPE.code && !renderEntity.properties["shapeList"].isNullOrEmpty() && renderEntity.properties["shapeList"] != "null") {
            // 解析shapeList，将数组中的属性放会properties
            val shapeList = JSONArray(renderEntity.properties["shapeList"])
            val boundaryType = renderEntity.properties["boundaryType"]
            val listResult = mutableListOf<RenderEntity>()
            if (boundaryType != null) {
                var isExistOffSet0 = false
                //只处理标线类型,App要求值渲染1、2、6、8
                if (boundaryType.toInt() == 2) {
                    for (i in 0 until shapeList.length()) {
                        var shape = shapeList.getJSONObject(i)
                        val lateralOffset = shape.optInt("lateralOffset", 0)
                        //999999时不应用也不渲染
                        if (lateralOffset != 999999) {
                            //需要做偏移处理
                            if (lateralOffset != 0) {

                                when (shape.optInt("markType", 0)) {
                                    1, 2, 6, 8 -> {
                                        val renderEntityTemp = RenderEntity()
                                        for (key in shape.keys()) {
                                            renderEntityTemp.properties[key] = shape[key].toString()
                                        }
                                        renderEntityTemp.properties["qi_table"] =
                                            renderEntity.properties["qi_table"]
                                        renderEntityTemp.properties["qi_code"] =
                                            renderEntity.properties["qi_code"]
                                        renderEntityTemp.properties["qi_zoomMin"] =
                                            renderEntity.properties["qi_zoomMin"]
                                        renderEntityTemp.properties["qi_zoomMax"] =
                                            renderEntity.properties["qi_zoomMax"]
                                        renderEntityTemp.properties["name"] =
                                            renderEntity.properties["name"]
                                        renderEntityTemp.properties["qi_name"] =
                                            renderEntity.properties["qi_name"]
                                        renderEntityTemp.properties["boundaryType"] =
                                            renderEntity.properties["boundaryType"]
                                        renderEntityTemp.properties["featureClass"] =
                                            renderEntity.properties["featureClass"]
                                        renderEntityTemp.properties["featurePid"] =
                                            renderEntity.properties["featurePid"]

                                        renderEntityTemp.code = renderEntity.code
                                        renderEntityTemp.table = renderEntity.table
                                        renderEntityTemp.name = renderEntity.name
                                        renderEntityTemp.zoomMin = renderEntity.zoomMin
                                        renderEntityTemp.zoomMax = renderEntity.zoomMax
                                        renderEntityTemp.enable = renderEntity.enable
                                        renderEntityTemp.taskId = renderEntity.taskId
                                        renderEntityTemp.catchEnable = renderEntity.catchEnable
                                        var dis = -lateralOffset.toDouble() / 100000000
                                        //最小值取10厘米，否正渲染太近无法显示
                                        if (dis > 0 && dis < 0.000005) {
                                            dis = 0.000005
                                            Log.d("lateralOffset", "$dis")
                                        } else if (dis > -0.000005 && dis < 0) {
                                            dis = -0.000005
                                            Log.d("lateralOffset", "$dis")
                                        }
                                        renderEntityTemp.geometry = GeometryTools.computeLine(
                                            dis,
                                            renderEntity.geometry
                                        )
                                        listResult.add(renderEntityTemp)
                                    }
                                }
                            } else {
                                isExistOffSet0 = true
                                //遍历赋值
                                for (key in shape.keys()) {
                                    renderEntity.properties[key] = shape[key].toString()
                                }
                            }
                        }
                    }
                    //如果不存在偏移的数据时，数据本身不渲染同时也不进行捕捉
                    if (!isExistOffSet0) {
                        renderEntity.catchEnable = 0
                    }
                    if (listResult.size > 0) {
                        insertData(listResult)
                    }
                }
            }
        }
    }

    /**
     * 解析车信数据二级属性
     * */
    fun unpackingLaneInfo(renderEntity: RenderEntity) {
        if (renderEntity.code == "4601") {
            if (!renderEntity.properties["laneinfoGroup"].isNullOrEmpty() && renderEntity.properties["laneinfoGroup"] != "null") {
                // 解析laneinfoGroup，将数组中的属性放会properties
                val laneinfoGroup = JSONArray(
                    renderEntity.properties["laneinfoGroup"].toString().replace("{", "[")
                        .replace("}", "]")
                )
                // 分别获取两个数组中的数据，取第一个作为主数据，另外两个作为辅助渲染数据
                val laneInfoDirectArray = JSONArray(laneinfoGroup[0].toString())
                val laneInfoTypeArray = JSONArray(laneinfoGroup[1].toString())
                val listResult = mutableListOf<ReferenceEntity>()

                for (i in 0 until laneInfoDirectArray.length()) {
                    // 根据后续的数据生成辅助表数据
                    val referenceEntity = ReferenceEntity()
//                    referenceEntity.renderEntityId = renderEntity.id
                    referenceEntity.name = "${renderEntity.name}参考方向"
                    referenceEntity.table = renderEntity.table
                    referenceEntity.enable = renderEntity.enable
                    referenceEntity.taskId = renderEntity.taskId
                    referenceEntity.zoomMin = renderEntity.zoomMin
                    referenceEntity.zoomMax = renderEntity.zoomMax
                    // 与原数据使用相同的geometry
                    referenceEntity.geometry = renderEntity.geometry
                    referenceEntity.properties["qi_table"] = renderEntity.table
                    referenceEntity.properties["currentDirect"] =
                        laneInfoDirectArray[i].toString().split(",").distinct().joinToString("_")
                    referenceEntity.properties["currentType"] =
                        laneInfoTypeArray[i].toString()
                    val type =
                        if (referenceEntity.properties["currentType"] == "0") "normal" else if (referenceEntity.properties["currentType"] == "1") "extend" else "bus"
                    referenceEntity.properties["symbol"] =
                        "assets:omdb/4601/${type}/1301_${referenceEntity.properties["currentDirect"]}.svg"
                    Log.d("unpackingLaneInfo", referenceEntity.properties["symbol"].toString())
                    referenceEntity.propertiesDb = StrZipUtil.compress(
                        gson.toJson(referenceEntity.properties).toString()
                    )
                    listResult.add(referenceEntity)
                }
                insertData(listResult)
            }
        }
    }

    /**
     * 生成默认道路名数据
     * */
    fun generateRoadText(renderEntity: RenderEntity) {
        // 根据类型进行文字转换
        if (renderEntity.code != null) {

            var type = renderEntity.properties["sa"]

            if (type != null && type == "1") {
                renderEntity.properties["name"] = "SA"
                renderEntity.properties["type"] = "1"
            } else {
                type = renderEntity.properties["pa"]
                if (type != null && type == "1") {
                    renderEntity.properties["type"] = "2"
                    Log.e("qj", "generateRoadText===2")
                } else {
                    type = renderEntity.properties["frontage"]
                    if (type != null && type == "1") {
                        renderEntity.properties["name"] = "FRONTAGE"
                        renderEntity.properties["type"] = "3"
                    } else {
                        type = renderEntity.properties["mainSideAccess"]
                        if (type != null && type == "1") {
                            renderEntity.properties["name"] = "MAIN"
                            renderEntity.properties["type"] = "4"
                        }
                    }
                }
            }
        }
    }


    /**
     * 生成默认道路名数据
     * */
    fun generateRoadName(renderEntity: RenderEntity) {
        // LinkName的真正名称数据，是保存在properties的shapeList中的，因此需要解析shapeList数据
        var shape: JSONObject? = null
        if (renderEntity.properties.containsKey("shapeList")) {
            val shapeListJsonArray: JSONArray = JSONArray(renderEntity.properties["shapeList"])
            for (i in 0 until shapeListJsonArray.length()) {
                val shapeJSONObject = shapeListJsonArray.getJSONObject(i)
                if (shapeJSONObject["nameClass"] == 1) {
                    if (shape == null) {
                        shape = shapeJSONObject
                    }
                    // 获取第一官方名
                    //("名称分类"NAME_CLASS =“1 官方名”，且名称序号SEQ_NUM 最小者）
                    if (shapeJSONObject["seqNum"].toString().toInt() < shape!!["seqNum"].toString()
                            .toInt()
                    ) {
                        shape = shapeJSONObject
                    }
                }
            }
        }
        // 获取最小的shape值，将其记录增加记录在properties的name属性下
        if (shape != null) {
            renderEntity.properties["name"] = shape.optString("name", "")
        } else {
            renderEntity.properties["name"] = ""
        }
    }

    /**
     * 生成电子眼对应的渲染名称
     * */
    fun generateElectronName(renderEntity: RenderEntity) {
        // 解析电子眼的kind，将其转换为渲染的简要名称
        var shape: JSONObject? = null
        if (renderEntity.properties.containsKey("kind")) {
            renderEntity.properties["name"] =
                code2NameMap.electronEyeKindMap[renderEntity.properties["kind"].toString().toInt()]
        } else {
            renderEntity.properties["name"] = ""
        }
    }

    /**
     * 生成车道中心线面宽度
     * */
    fun generateAddWidthLine(renderEntity: RenderEntity) {
        var newTime = 0L
        // 添加车道中心面渲染原则，根据车道宽度进行渲染
        val angleReference = ReferenceEntity()
       // angleReference.renderEntityId = renderEntity.id
        angleReference.name = "${renderEntity.name}车道中线面"
        angleReference.table = renderEntity.table
        Log.e("jingo", "几何转换开始")
        angleReference.geometry =
            GeometryTools.createGeometry(renderEntity.geometry).buffer(0.000010)
                .toString()//GeometryTools.computeLine(0.000035,0.000035,renderEntity.geometry)
        Log.e("jingo", "几何转换结束")
        angleReference.properties["qi_table"] = renderEntity.table
        angleReference.properties["widthProperties"] = "3"
        angleReference.zoomMin = renderEntity.zoomMin
        angleReference.zoomMax = renderEntity.zoomMax
        angleReference.taskId = renderEntity.taskId
        angleReference.enable = renderEntity.enable
        val listResult = mutableListOf<ReferenceEntity>()
        angleReference.propertiesDb = StrZipUtil.compress(
            gson.toJson(angleReference.properties).toString()
        )
        listResult.add(angleReference)
        insertData(listResult)

    }


    /**
     * 生成默认路口数据的参考数据
     * */
    fun generateIntersectionReference(renderEntity: RenderEntity) {
        // 路口数据的其他点位，是保存在nodeList对应的数组下
        if (renderEntity.properties.containsKey("nodeList")) {
            val nodeListJsonArray: JSONArray = JSONArray(renderEntity.properties["nodeList"])
            val listResult = mutableListOf<ReferenceEntity>()

            for (i in 0 until nodeListJsonArray.length()) {
                val nodeJSONObject = nodeListJsonArray.getJSONObject(i)
                val intersectionReference = ReferenceEntity()
//                intersectionReference.renderEntityId = renderEntity.id
                intersectionReference.name = "${renderEntity.name}参考点"
                intersectionReference.code = renderEntity.code
                intersectionReference.table = renderEntity.table
                intersectionReference.zoomMin = renderEntity.zoomMin
                intersectionReference.zoomMax = renderEntity.zoomMax
                intersectionReference.taskId = renderEntity.taskId
                intersectionReference.enable = renderEntity.enable
                // 与原有方向指向平行的线
                intersectionReference.geometry =
                    GeometryTools.createGeometry(nodeJSONObject["geometry"].toString()).toString()
                intersectionReference.properties["qi_table"] = renderEntity.table
                intersectionReference.properties["type"] = "node"
                intersectionReference.propertiesDb = StrZipUtil.compress(
                    gson.toJson(intersectionReference.properties).toString()
                )
                listResult.add(intersectionReference)
            }
            insertData(listResult)
        }
    }

    /**
     * 生成默认路口数据的参考数据
     * */
    fun generateIntersectionDynamic(renderEntity: RenderEntity) {
        // 路口数据的其他点位，是保存在nodeList对应的数组下
        if (renderEntity.properties.containsKey("type")) {
            if (renderEntity.properties["type"] == "0") {
                renderEntity.properties["typesrc"] = "assets:symbols/dot_blue_dark.svg"
            } else {
                renderEntity.properties["typesrc"] = "assets:symbols/volcano.svg"
            }
        }
    }

    /**
     * 处理杆状物的高程数据
     * */
    fun normalizationPoleHeight(renderEntity: RenderEntity) {
        // 获取杆状物的高程数据
        val geometry = renderEntity.wkt
        if (geometry != null) {
//            var minHeight = Double.MAX_VALUE
//            var maxHeight = Double.MIN_VALUE
//            for (coordinate in geometry.coordinates) {
//                if (coordinate.z < minHeight) {
//                    minHeight = coordinate.z
//                }
//                if (coordinate.z > maxHeight) {
//                    maxHeight = coordinate.z
//                }
//            }
//            for (coordinate in geometry.coordinates) {
//                if (coordinate.z == minHeight) {
//                    coordinate.z = 0.0
//                }
//                if (coordinate.z == maxHeight) {
//                    coordinate.z = 40.0
//                }
//            }
//            renderEntity.geometry =
//                WKTWriter(3).write(GeometryTools.createLineString(geometry.coordinates))

            renderEntity.geometry = GeometryTools.createGeometry(
                GeoPoint(
                    geometry.coordinates[0].y,
                    geometry.coordinates[0].x
                )
            ).toString()
        }
    }

    /**
     * 处理交通标牌的高程数据
     * */
    fun normalizationTrafficSignHeight(renderEntity: RenderEntity) {
        // 获取交通标牌的高程数据
        val geometry = renderEntity.wkt
        if (geometry != null) {
            // 获取所有的高程信息，计算高程的中位数，方便对高程做定制化处理
            var midHeight = 0.0
            var countHeight = 0.0
            for (coordinate in geometry.coordinates) {
                countHeight += coordinate.z
            }
            midHeight = countHeight / geometry.coordinates.size

            // 对高程数据做特殊处理
            for (coordinate in geometry.coordinates) {
                if (coordinate.z >= midHeight) {
                    coordinate.z = 40.0
                } else {
                    coordinate.z = 30.0
                }
            }
            renderEntity.geometry =
                WKTWriter(3).write(GeometryTools.getPolygonGeometry(geometry.coordinates))
        }
    }

    /**
     * 生成动态src字段-辅助图层，一般用于显示带角度的图标
     * @param renderEntity 要被处理的RenderEntity
     * @param prefix 图片的前缀（一般还需要指定图片对应的文件夹）
     * @param suffix 图片的后缀（根据codeName获取到的code后，匹配图片的后缀，还包含code码后的其他字符串内容）
     * @param codeName 数据对应的code字段的字段名
     * */
    fun obtainReferenceDynamicSrc(
        renderEntity: RenderEntity,
        prefix: String,
        suffix: String,
        codeName: String
    ) {
        if (codeName.isNullOrBlank()) {
            return
        }

        // 根据数据或angle计算方向对应的角度和偏移量
        val geometry = renderEntity.wkt
        var radian = 0.0 // geometry的角度，如果是点，获取angle，如果是线，获取最后两个点的方向
        var pointStartArray = mutableListOf<Coordinate>()
        if (Geometry.TYPENAME_POINT == geometry?.geometryType) {
            val point = Coordinate(geometry?.coordinate)
            pointStartArray.add(point)
            var angle =
                if (renderEntity?.properties?.get("angle") == null) 0.0 else renderEntity?.properties?.get(
                    "angle"
                )?.toDouble()!!
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle = ((450 - angle) % 360)
            radian = Math.toRadians(angle)
        } else if (Geometry.TYPENAME_LINESTRING == geometry?.geometryType) {
            var coordinates = geometry.coordinates
            val p1: Coordinate = coordinates.get(coordinates.size - 2)
            val p2: Coordinate = coordinates.get(coordinates.size - 1)
            // 计算线段的方向
            radian = Angle.angle(p1, p2)
            pointStartArray.add(p1)
        } else if (Geometry.TYPENAME_POLYGON == geometry?.geometryType) {
            // 记录下面数据的每一个点位
            pointStartArray.addAll(geometry.coordinates)
            // 获取当前的面数据对应的方向信息
            var angle = if (renderEntity?.properties?.get("angle") == null) {
                if (renderEntity?.properties?.get("heading") == null) {
                    0.0
                } else {
                    renderEntity?.properties?.get("heading")?.toDouble()!!
                }
            } else renderEntity?.properties?.get("angle")?.toDouble()!!

            angle = ((450 - angle) % 360)
            radian = Math.toRadians(angle)
        }

        // 计算偏移距离
        var dx: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.cos(radian)
        var dy: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            geometry?.coordinate?.y!!
        ) * Math.sin(radian)
        val listResult = mutableListOf<ReferenceEntity>()

        for (pointStart in pointStartArray) {
            val coorEnd = Coordinate(pointStart.getX() + dx, pointStart.getY() + dy, pointStart.z)

            val dynamicSrcReference = ReferenceEntity()
//            dynamicSrcReference.renderEntityId = renderEntity.id
            dynamicSrcReference.name = "${renderEntity.name}动态icon"
            dynamicSrcReference.table = renderEntity.table
            dynamicSrcReference.zoomMin = renderEntity.zoomMin
            dynamicSrcReference.zoomMax = renderEntity.zoomMax
            dynamicSrcReference.taskId = renderEntity.taskId
            dynamicSrcReference.enable = renderEntity.enable
            // 与原有方向指向平行的线
            dynamicSrcReference.geometry =
                WKTWriter(3).write(GeometryTools.createLineString(arrayOf(pointStart, coorEnd)))
            dynamicSrcReference.properties["qi_table"] = renderEntity.table
            dynamicSrcReference.properties["type"] = "dynamicSrc"
            val code = renderEntity.properties[codeName]
            dynamicSrcReference.properties["src"] = "${prefix}${code}${suffix}"
            dynamicSrcReference.propertiesDb = StrZipUtil.compress(
                gson.toJson(dynamicSrcReference.properties).toString()
            )
            listResult.add(dynamicSrcReference)
        }
        insertData(listResult)
    }

    private fun insertData(list: List<RealmModel>) {
        realm?.let {
            if (list != null && list.isNotEmpty()) {
                it.copyToRealm(list)
            }
        }
    }

    /**
     * 向当前renderEntity中添加动态属性
     * */
    fun obtainDynamicSrc(
        renderEntity: RenderEntity,
        prefix: String,
        suffix: String,
        codeName: String
    ) {
        val code = renderEntity.properties[codeName]
        renderEntity.properties["src"] = "${prefix}${code}${suffix}"
    }

    /**
     * 获取当前数据的中心点坐标
     * */
    fun obtainTrafficSignCenterPoint(renderEntity: RenderEntity) {
        // 获取中心坐标点，将中心坐标作为数据的新的geometry位置
        val centerPoint = renderEntity.wkt?.centroid
        // 根据heading方向自动生成新的Geometry
        var radian = 0.0
        val pointStart = Coordinate(centerPoint!!.x, centerPoint.y)
        var angle =
            if (renderEntity?.properties?.get("heading") == null) 0.0 else renderEntity?.properties?.get(
                "heading"
            )?.toDouble()!!
        // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
        angle = ((450 - angle) % 360)
        radian = Math.toRadians(angle)

        // 计算偏移距离
        var dx: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            centerPoint.y
        ) * Math.cos(radian)
        var dy: Double = GeometryTools.convertDistanceToDegree(
            defaultTranslateDistance,
            centerPoint.y
        ) * Math.sin(radian)
        val listResult = mutableListOf<ReferenceEntity>()

        val coorEnd = Coordinate(pointStart.getX() + dx, pointStart.getY() + dy, pointStart.z)
//        renderEntity.geometry = WKTWriter(3).write(GeometryTools.createLineString(arrayOf(pointStart, coorEnd)))
        renderEntity.geometry =
            GeometryTools.createGeometry(GeoPoint(centerPoint!!.y, centerPoint.x)).toString()
        val code = renderEntity.properties["signType"]
        renderEntity.properties["src"] = "assets:omdb/appendix/1105_${code}_0.svg"
    }

    /**
     * 获取上方障碍物中心点坐标
     *
     * */
    fun getPolygonCenterPoint(renderEntity: RenderEntity, containsDirect: Boolean = false) {
        // 获取中心坐标点，将中心坐标作为数据的新的geometry位置
        val centerPoint = renderEntity.wkt?.centroid
        if (containsDirect) {
            // 根据heading方向自动生成新的Geometry
            var radian = 0.0
            val pointStart = Coordinate(centerPoint!!.x, centerPoint.y)
            var angle =
                if (renderEntity?.properties?.get("heading") == null) 0.0 else renderEntity?.properties?.get(
                    "heading"
                )?.toDouble()!!
            // angle角度为与正北方向的顺时针夹角，将其转换为与X轴正方向的逆时针夹角，即为正东方向的夹角
            angle = ((450 - angle) % 360)
            radian = Math.toRadians(angle)

            // 计算偏移距离
            var dx: Double = GeometryTools.convertDistanceToDegree(
                defaultTranslateDistance,
                centerPoint.y
            ) * Math.cos(radian)
            var dy: Double = GeometryTools.convertDistanceToDegree(
                defaultTranslateDistance,
                centerPoint.y
            ) * Math.sin(radian)
            val listResult = mutableListOf<ReferenceEntity>()

            val coorEnd = Coordinate(pointStart.getX() + dx, pointStart.getY() + dy, pointStart.z)
            renderEntity.geometry =
                WKTWriter(3).write(GeometryTools.createLineString(arrayOf(pointStart, coorEnd)))
        } else {
            renderEntity.geometry =
                GeometryTools.createGeometry(GeoPoint(centerPoint!!.y, centerPoint.x)).toString()
        }
    }

    /**
     * 生成通行车辆类型Lane的渲染名称字段
     * */
    fun generateLaneAccessType(renderEntity: RenderEntity): Boolean {
        if (renderEntity.properties.containsKey("accessCharacteristic")) {
            // 解析accessCharacteristic，判断是否存在指定属性
            val accessCharacteristic = renderEntity.properties["accessCharacteristic"].toString().toInt()
            var str = ""
            if (accessCharacteristic.and(4)>0) {
                str += "公"
            }
            if (accessCharacteristic.and(8)>0) {
                if (str.isNotEmpty()) {
                    str += "|"
                }
                str += "多"
            }
            if (accessCharacteristic.and(64)>0) {
                if (str.isNotEmpty()) {
                    str += "|"
                }
                str += "行"
            }
            if (accessCharacteristic.and(128)>0) {
                if (str.isNotEmpty()) {
                    str += "|"
                }
                str += "自"
            }
            if (str.isNotEmpty()) {
                renderEntity.properties["name"] = str
                return true
            }
        }
        return false
    }

    /**
     * 生成车道点限速的名称
     * */
    fun obtainLaneSpeedLimitName(renderEntity: RenderEntity) {
        if (renderEntity.properties.containsKey("maxSpeed")&&renderEntity.properties.containsKey("minSpeed")) {
            renderEntity.properties["ref"] = "${renderEntity.properties["maxSpeed"]}|${renderEntity.properties["minSpeed"]}"
        }
    }
}
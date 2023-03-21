package com.navinfo.collect.library.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.protobuf.GeneratedMessageV3
import com.navinfo.collect.library.data.entity.GeometryFeatureEntity
import com.navinfo.collect.library.data.entity.LayerEntity
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.*
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.geom.util.AffineTransformationFactory
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.streams.toList

@RequiresApi(Build.VERSION_CODES.N)
class NavinfoPbfFileUtils: GisFileUtils {
    /*
    * 解析Pbf文件
    * */
    override fun parserGisFile(file: File): List<GeometryFeatureEntity> {
        val geometryEntityList = ArrayList<GeometryFeatureEntity>()
        if (!file?.isFile || !file.exists()) {
            return geometryEntityList;
        }
        // 解析pbf文件
        val pbfData = NavinfoPbfData.readPbfData(FileInputStream(file))

        // 解析roadLink道路线
        geometryEntityList.addAll(parserRoadLink(pbfData.roadlinkList, file, "道路线"))
        // 解析roadLink上的道路方向
        geometryEntityList.addAll(parserRoadDirect(pbfData.roadlinkList, file, "道路方向"))
        // 解析roadLink的桥属性
        geometryEntityList.addAll(parserRoadBrunnel(pbfData.roadlinkList, file, "桥隧道"))
        // 解析roadLink的移动式桥属性
        geometryEntityList.addAll(parserRoadMovBrg(pbfData.roadlinkList, file, "移动式桥"))
        // 解析hadLaneLink车道中心线
        geometryEntityList.addAll(parserHadLaneLink(pbfData.hadlanelinkList, file, "车道中心线"))
        // 解析hadLaneMarkLink车道边线
        geometryEntityList.addAll(parserHadLaneMarkLink(pbfData.hadlanemarklinkList, file, "车道边线"))
        // 解析hadLaneMarkLink.traversal车道边线可跨越性
        geometryEntityList.addAll(parserHadLaneMarkLinkTraversal(pbfData.hadlanemarklinkList, file, "车道边线可跨越性"))
        // 解析hadLaneMarkLink.marking车道边线划线类型
        geometryEntityList.addAll(parserHadLaneMarkLinkBoundary(pbfData.hadlanemarklinkList, file, "车道边线非标线类型"))
        // 解析speedLimitGen固定限速
        geometryEntityList.addAll(parserSpeedLimitGen(pbfData.rdspeedlimitgenList, file, "固定限速"))
        // 解析speedLimitDepend条件限速
        geometryEntityList.addAll(parserSpeedLimitDepend(pbfData.rdspeedlimitdependList, file, "条件限速"))
        // 解析speedLimitVar可变限速
        geometryEntityList.addAll(parserSpeedLimitVar(pbfData.rdspeedlimitvarList, file, "可变限速"))
        return geometryEntityList;
    }

    /**
     * 处理道路线RoadLink
     * */
    private fun parserRoadLink(roadLinkList: List<Roadlink.RoadLink>, file: File, layerName: String= "道路线", layerTableName: String = "ROAD_LINK"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (roadLinkList?.isEmpty()) {
            return featureEntityList
        }
        for (roadLink in roadLinkList) {
            val geometry = NavinfoPbfData.createGeometry(roadLink.geometry)
            val geometryFeatureEntity =
                convertDefaultHDEntity(geometry, roadLink, layerName, layerTableName, file)
            featureEntityList.add(geometryFeatureEntity)
        }

        return featureEntityList;
    }

    /**
     * 处理车道中心线HadLaneLink
     * */
    private fun parserHadLaneLink(hadLaneLinkList: List<Hadlanelink.HadLaneLink>, file: File, layerName: String= "车道中心线", layerTableName: String = "HAD_LANE_LINK"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (hadLaneLinkList?.isEmpty()) {
            return featureEntityList
        }
        for (hadLaneLink in hadLaneLinkList) {
            val geometry = NavinfoPbfData.createGeometry(hadLaneLink.geometry)
            val geometryFeatureEntity =
                convertDefaultHDEntity(geometry, hadLaneLink, layerName, layerTableName, file)
            featureEntityList.add(geometryFeatureEntity)
        }
        return featureEntityList;
    }

    /**
     * 处理speedLimitGen固定限速
     * */
    private fun parserSpeedLimitGen(speedLimitGenList: List<Rdspeedlimitgen.RdSpeedlimitGen>, file: File, layerName: String= "固定限速", layerTableName: String = "SPEED_LIMIT_GEN"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (speedLimitGenList?.isEmpty()) {
            return featureEntityList
        }
        for (speedLimitGen in speedLimitGenList) {
            val geometry = NavinfoPbfData.createGeometry(speedLimitGen.geometry)
            val geometryFeatureEntity =
                convertDefaultHDEntity(geometry, speedLimitGen, layerName, layerTableName, file)
            geometryFeatureEntity.otherProperties["SPEED_FLAG"] = speedLimitGen.speedFlag.toString()
            featureEntityList.add(geometryFeatureEntity)
        }
        return featureEntityList;
    }

    /**
     * 处理speedLimitDepend条件限速
     * */
    private fun parserSpeedLimitDepend(speedLimitDenpendList: List<Rdspeedlimitdepend.RdSpeedlimitDepend>, file: File, layerName: String= "条件限速", layerTableName: String = "SPEED_LIMIT_DEPEND"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (speedLimitDenpendList?.isEmpty()) {
            return featureEntityList
        }
        for (speedLimitDepend in speedLimitDenpendList) {
            val geometry = NavinfoPbfData.createGeometry(speedLimitDepend.geometry)
            val geometryFeatureEntity =
                convertDefaultHDEntity(geometry, speedLimitDepend, layerName, layerTableName, file)
            geometryFeatureEntity.otherProperties["SPEED_FLAG"] = speedLimitDepend.speedFlag.toString()
            featureEntityList.add(geometryFeatureEntity)
        }
        return featureEntityList;
    }

    /**
     * 处理speedLimitDepend条件限速
     * */
    private fun parserSpeedLimitVar(speedLimitVarList: List<Rdspeedlimitvar.RdSpeedlimitVar>, file: File,
                                    layerName: String= "可变限速", layerTableName: String = "SPEED_LIMIT_VAR"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (speedLimitVarList?.isEmpty()) {
            return featureEntityList
        }
        for (speedLimitVar in speedLimitVarList) {
            val geometry = NavinfoPbfData.createGeometry(speedLimitVar.geometry)
            val geometryFeatureEntity =
                convertDefaultHDEntity(geometry, speedLimitVar, layerName, layerTableName, file)
            geometryFeatureEntity.otherProperties["SPEED_FLAG"] = speedLimitVar.speedFlag.toString()
            featureEntityList.add(geometryFeatureEntity)
        }
        return featureEntityList;
    }

    /**
     * 处理车道边线HadLaneMarkLink
     * */
    private fun parserHadLaneMarkLink(hadLaneMarkLinkList: List<Hadlanemarklink.HadLaneMarkLink>, file: File,
                                      layerName: String= "车道边线", layerTableName: String = "HAD_LANE_MARK_LINK"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (hadLaneMarkLinkList?.isEmpty()) {
            return featureEntityList
        }
        for (hadLaneMarkLink in hadLaneMarkLinkList) {
            val geometry = NavinfoPbfData.createGeometry(hadLaneMarkLink.geometry)
            val geometryFeatureEntity =
                convertDefaultHDEntity(geometry, hadLaneMarkLink, layerName, layerTableName, file)
            featureEntityList.add(geometryFeatureEntity)
        }

        return featureEntityList;
    }
    /**
     * 处理车道边线HadLaneMarkLinkTraversal
     * */
    private fun parserHadLaneMarkLinkTraversal(hadLaneMarkLinkList: List<Hadlanemarklink.HadLaneMarkLink>, file: File,
                                      layerName: String= "车道边线可跨越性", layerTableName: String = "HAD_LANE_MARK_LINK_TRAVERSAL"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (hadLaneMarkLinkList?.isEmpty()) {
            return featureEntityList
        }
        // 解析道路边线可跨越性
        for (hadLaneMarkLink in hadLaneMarkLinkList) {
//            for (traversal in hadLaneMarkLink.traversalList) {
//                // 根据给出的起终点计算切割点在线上的index
//                val startPoint3d = traversal.paPosition.sIndex
//                val endPoint3d = traversal.paPosition.eIndex
//                val subIndex:List<Map<String, Any>> = List()
//                val sortedIndex =subIndex.sortedBy { item->item["startIndex"].toString().toInt() }
//                for ((index, map) in sortedIndex.withIndex()) {
//
//                }
//            }
            for (traversal in hadLaneMarkLink.traversalList) {
                val startPoint3d = traversal.paPosition.sIndex
                val endPoint3d = traversal.paPosition.eIndex
                val traversalLineString = subLine(hadLaneMarkLink.geometry, startPoint3d, endPoint3d)
                val geometryFeatureEntity =
                    convertDefaultHDEntity(traversalLineString, traversal, layerName, layerTableName, file)
                geometryFeatureEntity.otherProperties["TYPE"] = traversal.type.toString()
                featureEntityList.add(geometryFeatureEntity)
            }
        }

        return featureEntityList;
    }
    /**
     * 处理车道边线HadLaneMarkLinkBoundary
     * */
    private fun parserHadLaneMarkLinkBoundary(hadLaneMarkLinkList: List<Hadlanemarklink.HadLaneMarkLink>, file: File,
                                      layerName: String= "车道边线非标线类型", layerTableName: String = "HAD_LANE_MARK_LINK_BOUNDARY"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (hadLaneMarkLinkList?.isEmpty()) {
            return featureEntityList
        }
        // 解析道路边线可跨越性
        for (hadLaneMarkLink in hadLaneMarkLinkList) {
            for (boundary in hadLaneMarkLink.boundaryList) {
                val startPoint3d = boundary.paPosition.sIndex
                val endPoint3d = boundary.paPosition.eIndex
                val boundaryLineString = subLine(hadLaneMarkLink.geometry, startPoint3d, endPoint3d)
                val geometryFeatureEntity =
                    convertDefaultHDEntity(boundaryLineString, boundary, layerName, layerTableName, file)
                if (boundary.boundaryType.type!=2) {
                    geometryFeatureEntity.otherProperties["BOUNDARY_TYPE"] = boundary.boundaryType.type.toString()
                    featureEntityList.add(geometryFeatureEntity)
                } else {
                    if (boundary.markingList!=null&&boundary.markingList.isNotEmpty()) {
                        for (mark in boundary.markingList) {
                            val geometryFeatureEntity =
                                convertDefaultHDEntity(boundaryLineString, mark, "车道边线标线类型", "HAD_LANE_MARK_LINK_MARKING", file)
                            geometryFeatureEntity.otherProperties["BOUNDARY_TYPE"] = "2"
                            geometryFeatureEntity.otherProperties["MARK_TYPE"] = mark.markType.toString()
                            geometryFeatureEntity.otherProperties["MARK_COLOR"] = mark.markColor.toString()
                            // 根据mark的横向偏移量重新设置geometry值
                            val lineString = boundaryLineString as LineString
                            var degree = Angle.toDegrees(Angle.angle(lineString.startPoint.coordinate, lineString.endPoint.coordinate))
                            degree = degree+90
                            val transformation:AffineTransformation = AffineTransformationFactory.createFromControlVectors(
                                Coordinate(0.toDouble(), 0.toDouble()), Coordinate(sin(degree) *(mark.lateralOffset*7e-9).toDouble(), cos(degree)*mark.lateralOffset*7e-9.toDouble())
                            )
                            val transformGeometry = transformation.transform(boundaryLineString)
                            geometryFeatureEntity.geometry = transformGeometry.toString()
                            featureEntityList.add(geometryFeatureEntity)
                        }
                    }
                }
            }
        }

        return featureEntityList;
    }

    /**
     * 处理道路方向
     * */
    private fun parserRoadDirect(roadLinkList: List<Roadlink.RoadLink>, file: File,
                                 layerName: String= "道路方向", layerTableName: String = "ROAD_LINK_DIRECT"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (roadLinkList?.isEmpty()) {
            return featureEntityList
        }
        for (roadLink in roadLinkList) {
            val geometry = NavinfoPbfData.createGeometry(roadLink.geometry)
            // 解析道路方向
            for (dirct in roadLink.directList) {
                val startPoint3d = dirct.paPosition.sIndex
                val endPoint3d = dirct.paPosition.eIndex
                val directLineString = subLine(roadLink.geometry, startPoint3d, endPoint3d)
                val geometryFeatureEntity =
                    convertDefaultHDEntity(directLineString, dirct, layerName, layerTableName, file)
                geometryFeatureEntity.otherProperties["VALUE"] = dirct.value.toString()
                featureEntityList.add(geometryFeatureEntity)
            }
        }

        return featureEntityList;
    }

    /**
     * 处理道路的桥属性
     * */
    private fun parserRoadBrunnel(roadLinkList: List<Roadlink.RoadLink>, file: File,
                                  layerName: String= "桥隧道", layerTableName: String = "ROAD_LINK_BRUNNEL"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (roadLinkList?.isEmpty()) {
            return featureEntityList
        }
        for (roadLink in roadLinkList) {
            // 解析桥属性IS_BRUNNEL
            for (brunnel in roadLink.isBrunnelList) {
                val startPoint3d = brunnel.paPosition.sIndex
                val endPoint3d = brunnel.paPosition.eIndex
                val brunnelLineString = subLine(roadLink.geometry, startPoint3d, endPoint3d)
                val geometryFeatureEntity =
                    convertDefaultHDEntity(brunnelLineString, brunnel, layerName, layerTableName, file)
                geometryFeatureEntity.otherProperties["TYPE"] = brunnel.type.toString()
                featureEntityList.add(geometryFeatureEntity)
            }
        }

        return featureEntityList;
    }
    /**
     * 处理道路的移动式桥属性
     * */
    private fun parserRoadMovBrg(roadLinkList: List<Roadlink.RoadLink>, file: File,
                                 layerName: String= "移动式桥", layerTableName: String = "ROAD_LINK_MOVBRG"): List<GeometryFeatureEntity> {
        val featureEntityList = ArrayList<GeometryFeatureEntity>()
        if (roadLinkList?.isEmpty()) {
            return featureEntityList
        }
        for (roadLink in roadLinkList) {
            // 解析移动式桥属性MOVBRG
            for (movbrg in roadLink.movbrgList) {
                val startPoint3d = movbrg.paPosition.sIndex
                val endPoint3d = movbrg.paPosition.eIndex
                val movbrgLineString = subLine(roadLink.geometry, startPoint3d, endPoint3d)
                val geometryFeatureEntity =
                    convertDefaultHDEntity(movbrgLineString, movbrg, layerName, layerTableName, file)
                featureEntityList.add(geometryFeatureEntity)
            }
        }

        return featureEntityList;
    }



    /**
     * 读取指定的HD元素数据
     * */
    private fun convertDefaultHDEntity(geometry: Geometry, generatedMsg: GeneratedMessageV3, layerName: String, layerTableName: String, file: File): GeometryFeatureEntity {
        // 处理roadLink
        val layerEntity = LayerEntity()
        layerEntity.layerName = layerName
        layerEntity.layerTableName = layerTableName
        layerEntity.fromDataName = file.name
        // 转换geometry和属性信息为数据库字段
        val renderMap = obtainRenderMap(layerTableName)
        val featureEntity =
            NavinfoPbfData.createGeometryEntity(geometry, generatedMsg.allFields, renderMap)
        featureEntity.name = layerName
        featureEntity.layerEntity = layerEntity
        return featureEntity
    }

    private fun obtainRenderMap(kind: String): Map<String, String> {
        val renderPropMap = HashMap<String, String>()
        renderPropMap.put("navinfo_hd", kind)
        return renderPropMap
    }

    /**
     * 根据给定的起终点截取LineString3D
     * */
    private fun subLine(line: com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString3d,
                        sPoint: com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d,
                        ePoint: com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d): Geometry {
        var sIndex = 0
        var eIndex = 0
        for ((index, linePoint) in line.linestringPointsList.withIndex()) {
            if (sIndex <= eIndex&&linePoint.equals(sPoint)) {
                sIndex = index
            } else if (linePoint.equals(ePoint)) {
                eIndex = index
                break
            }
        }
        if (eIndex>sIndex&&eIndex<line.linestringPointsList.size) {
            // 转换为geometry
            // 注意，因为sublist取的尾数为参数-1，所以eIndex需要加1
            val coordinateArrays: List<Coordinate> = line.linestringPointsList.subList(sIndex, eIndex+1).stream().map { Coordinate(it.latLon.longitudeDegrees, it.latLon.latitudeDegrees, it.elevation.z) }.toList()
            val toTypedArray = coordinateArrays.toTypedArray()
            if (toTypedArray.size == 1) {
                toTypedArray[1] = toTypedArray[0].copy()
            }
//            println("toTypedArray点数：${toTypedArray.size}，sIndex：${sIndex}，eIndex：${eIndex}")
            return GeometryTools.getLineStrinGeo(toTypedArray)
        }
        // 如果没能截取到子线段，则返回第一个点组成的线
        return GeometryTools.getLineStrinGeo(
                arrayOf(
                    Coordinate(line.linestringPointsList.get(0).latLon.longitudeDegrees, line.linestringPointsList.get(0).latLon.latitudeDegrees, line.linestringPointsList.get(0).elevation.z),
                    Coordinate(line.linestringPointsList.get(1).latLon.longitudeDegrees, line.linestringPointsList.get(1).latLon.latitudeDegrees, line.linestringPointsList.get(1).elevation.z))
        )
    }
}
package com.navinfo.omqs.db

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.RenderEntity.Companion.LinkTable
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.buffer.BufferOp
import org.locationtech.spatial4j.context.SpatialContext
import org.locationtech.spatial4j.distance.DistanceUtils
import org.oscim.core.GeoPoint
import org.oscim.core.MercatorProjection
import javax.inject.Inject
import kotlin.streams.toList

@RequiresApi(Build.VERSION_CODES.N)
class RealmOperateHelper() {
    @Inject
    lateinit var niMapController: NIMapController

    /**
     * 根据当前点位查询匹配的Link数据
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param sort 是否需要排序
     * */
    suspend fun queryLink(
        point: Point,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE,
        sort: Boolean = true
    ): MutableList<RenderEntity> {
        val result = mutableListOf<RenderEntity>()
        withContext(Dispatchers.IO) {
            val polygon = getPolygonFromPoint(point, buffer, bufferType)
            // 根据polygon查询相交的tile号
            val tileXSet = mutableSetOf<Int>()
            tileXSet.toString()
            GeometryToolsKt.getTileXByGeometry(polygon.toString(), tileXSet)
            val tileYSet = mutableSetOf<Int>()
            GeometryToolsKt.getTileYByGeometry(polygon.toString(), tileYSet)

            // 对tileXSet和tileYSet查询最大最小值
            val xStart = tileXSet.stream().min(Comparator.naturalOrder()).orElse(null)
            val xEnd = tileXSet.stream().max(Comparator.naturalOrder()).orElse(null)
            val yStart = tileYSet.stream().min(Comparator.naturalOrder()).orElse(null)
            val yEnd = tileYSet.stream().max(Comparator.naturalOrder()).orElse(null)
            // 查询realm中对应tile号的数据
            val realm = Realm.getDefaultInstance()
            val realmList = realm.where(RenderEntity::class.java)
                .equalTo("table", "OMDB_RD_LINK")
                .and()
                .rawPredicate("tileX>=$xStart and tileX<=$xEnd and tileY>=$yStart and tileY<=$yEnd")
                .findAll()
            // 将获取到的数据和查询的polygon做相交，只返回相交的数据
            val dataList = realm.copyFromRealm(realmList)
            val queryResult = dataList?.stream()?.filter {
                polygon.intersects(it.wkt)
            }?.toList()

            queryResult?.let {
                if (sort) {
                    result.addAll(sortRenderEntity(point, it))
                } else {
                    result.addAll(it)
                }
            }

        }
        return result
    }


    suspend fun queryLink(
        linkPid: String,
    ): RenderEntity? {
        var link: RenderEntity? = null
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val realmR = realm.where(RenderEntity::class.java)
                .equalTo("table", "OMDB_RD_LINK")
                .and()
                .rawPredicate("properties['${LinkTable.linkPid}']=$linkPid")
                .findFirst()
            if (realmR != null) {
                link = realm.copyFromRealm(realmR)
            }
        }
        return link
    }

    /**
     * 根据当前点位查询匹配的除Link外的其他要素数据
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param sort 是否需要排序
     * */
    suspend fun queryElement(
        point: Point,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE,
        sort: Boolean = true
    ): MutableList<RenderEntity> {
        val result = mutableListOf<RenderEntity>()
        withContext(Dispatchers.IO) {
            val polygon = getPolygonFromPoint(point, buffer, bufferType)
            // 根据polygon查询相交的tile号
            val tileXSet = mutableSetOf<Int>()
            tileXSet.toString()
            GeometryToolsKt.getTileXByGeometry(polygon.toString(), tileXSet)
            val tileYSet = mutableSetOf<Int>()
            GeometryToolsKt.getTileYByGeometry(polygon.toString(), tileYSet)

            // 对tileXSet和tileYSet查询最大最小值
            val xStart = tileXSet.stream().min(Comparator.naturalOrder()).orElse(null)
            val xEnd = tileXSet.stream().max(Comparator.naturalOrder()).orElse(null)
            val yStart = tileYSet.stream().min(Comparator.naturalOrder()).orElse(null)
            val yEnd = tileYSet.stream().max(Comparator.naturalOrder()).orElse(null)
            // 查询realm中对应tile号的数据
            val realmList = Realm.getDefaultInstance().where(RenderEntity::class.java)
                .notEqualTo("table", "OMDB_RD_LINK")
                .and()
                .rawPredicate("tileX>=$xStart and tileX<=$xEnd and tileY>=$yStart and tileY<=$yEnd")
                .findAll()
            // 将获取到的数据和查询的polygon做相交，只返回相交的数据
            val queryResult = realmList?.stream()?.filter {
                polygon.intersects(it.wkt)
            }?.toList()
            queryResult?.let {
                result.addAll(queryResult)
            }
            if (sort) {
                result.clear()
                result.addAll(sortRenderEntity(point, result))
            }
        }
        return result
    }

    /**
     * 根据linkPid查询关联的要素（除去Link数据）
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param sort 是否需要排序
     * */
    suspend fun queryLinkByLinkPid(linkPid: String): MutableList<RenderEntity> {
        val result = mutableListOf<RenderEntity>()
        withContext(Dispatchers.IO) {
            val realmList = Realm.getDefaultInstance().where(RenderEntity::class.java)
                .notEqualTo("table", "OMDB_RD_LINK")
                .and()
                .equalTo("properties['${LinkTable.linkPid}']", linkPid)
                .findAll()
            result.addAll(realmList)
        }
        return result
    }

    /**
     * 根据给定的点位对数据排序
     * @param point 点位经纬度信息
     * @param unSortList 未排序的数据
     * @return 排序后的数据
     * */
    fun sortRenderEntity(point: Point, unSortList: List<RenderEntity>): List<RenderEntity> {
        val sortList = unSortList.stream().sorted { renderEntity, renderEntity2 ->
            val near = point.distance(renderEntity.wkt) - point.distance(renderEntity2.wkt)
            if (near < 0) -1 else 1
        }.toList()
        return sortList
    }

    private fun getPolygonFromPoint(
        point: Point,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE
    ): Polygon {
        // 首先计算当前点位的buffer组成的geometry
        val wkt: Polygon = if (bufferType == BUFFER_TYPE.METER) { // 如果单位是米
            val distanceDegrees = GeometryTools.convertDistanceToDegree(buffer, point.y)
            // 计算外扩矩形
            BufferOp.bufferOp(point, distanceDegrees) as Polygon
        } else { // 如果单位是像素，需要根据当前屏幕像素计算出经纬度变化
            val currentMapScale = niMapController.mMapView.vtmMap.mapPosition.scale
            // 转换为屏幕坐标
            val pixelPoint =
                MercatorProjection.getPixelWithScale(
                    GeoPoint(point.y, point.x),
                    currentMapScale
                )
            // 将屏幕坐标外扩指定距离
            // 计算外扩矩形
            val envelope = Envelope(
                MercatorProjection.pixelXToLongitudeWithScale(
                    pixelPoint.x - buffer,
                    currentMapScale
                ),
                MercatorProjection.pixelXToLongitudeWithScale(
                    pixelPoint.x + buffer,
                    currentMapScale
                ),
                MercatorProjection.pixelYToLatitudeWithScale(
                    pixelPoint.y - buffer,
                    currentMapScale
                ),
                MercatorProjection.pixelYToLatitudeWithScale(
                    pixelPoint.y + buffer,
                    currentMapScale
                ),
            )
            // 将Envelope对象转换为Polygon对象
            val geometryFactory = GeometryFactory()
            val coordinates = arrayOfNulls<Coordinate>(5)
            coordinates[0] = Coordinate(envelope.minX, envelope.minY)
            coordinates[1] = Coordinate(envelope.minX, envelope.maxY)
            coordinates[2] = Coordinate(envelope.maxX, envelope.maxY)
            coordinates[3] = Coordinate(envelope.maxX, envelope.minY)
            coordinates[4] = coordinates[0]
            geometryFactory.createPolygon(coordinates)
        }

        Log.d("queryLink", wkt.toString())
        return wkt
    }
}

enum class BUFFER_TYPE(val index: Int) {
    METER(0)/*米*/, PIXEL(1)/*像素*/;

    fun getBufferTypeByIndex(index: Int): BUFFER_TYPE {
        for (item in BUFFER_TYPE.values()) {
            if (item.index == index) {
                return item;
            }
        }
        return METER
    }
}

private val DEFAULT_BUFFER: Double = 15.0
private val DEFAULT_BUFFER_TYPE = BUFFER_TYPE.METER

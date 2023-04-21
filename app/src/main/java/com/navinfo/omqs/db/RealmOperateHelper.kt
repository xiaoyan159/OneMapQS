package com.navinfo.omqs.db

import android.util.Log
import com.navinfo.collect.library.data.entity.RenderEntity
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


class RealmOperateHelper() {
    @Inject
    lateinit var niMapController: NIMapController
    /**
     * 根据当前点位查询匹配的Link数据
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param order 是否需要排序
     * */
    suspend fun queryLink(point: Point, buffer: Double = DEFAULT_BUFFER, bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE, order: Boolean = false): MutableList<RenderEntity> {
        withContext(Dispatchers.IO) {
            val polygon = getPolygonFromPoint(point, buffer, bufferType)
            // 根据polygon查询相交的tile号
            val tileXSet = mutableSetOf<Int>()
            GeometryToolsKt.getTileXByGeometry(polygon.toString(), tileXSet)
            val tileYSet = mutableSetOf<Int>()
            GeometryToolsKt.getTileYByGeometry(polygon.toString(), tileYSet)

            // 查询realm中对应tile号的数据
            Realm.getDefaultInstance().where(RenderEntity::class.java).equalTo("table", "HAD_LINK")
        }
        return mutableListOf()
    }

    private fun getPolygonFromPoint(point: Point, buffer: Double = DEFAULT_BUFFER, bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE): Polygon {
        // 首先计算当前点位的buffer组成的geometry
        val wkt: Polygon = if (bufferType == BUFFER_TYPE.METER) { // 如果单位是米
            // 计算米和地球角度之间的关系，在Spatial4J中，经度和纬度的单位是度，而不是米。因此，将距离从米转换为度需要使用一个转换因子，这个转换因子是由地球的周长和360度之间的比例计算得出的。
            // 在这个例子中，使用的转换因子是111000.0，这是因为地球的周长约为40075公里，而每个经度的距离大约是地球周长的1/360，因此每个经度的距离约为111.32公里
            val distanceDegrees = DistanceUtils.dist2Degrees(buffer, DistanceUtils.EARTH_MEAN_RADIUS_KM) * 111000.0
            // 计算外扩矩形
            BufferOp.bufferOp(point, distanceDegrees) as Polygon
        } else { // 如果单位是像素，需要根据当前屏幕像素计算出经纬度变化
            val currentMapScale = niMapController.mMapView.vtmMap.mapPosition.scale
            // 转换为屏幕坐标
            val pixelPoint = MercatorProjection.getPixelWithScale(GeoPoint(point.y, point.x), currentMapScale)
            // 将屏幕坐标外扩指定距离
            // 计算外扩矩形
            val envelope = Envelope(
                MercatorProjection.pixelXToLongitudeWithScale(pixelPoint.x - buffer, currentMapScale),
                MercatorProjection.pixelXToLongitudeWithScale(pixelPoint.x + buffer, currentMapScale),
                MercatorProjection.pixelYToLatitudeWithScale(pixelPoint.y - buffer, currentMapScale),
                MercatorProjection.pixelYToLatitudeWithScale(pixelPoint.y + buffer, currentMapScale),
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
    fun getBufferTypeByIndex(index: Int): BUFFER_TYPE{
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
package com.navinfo.omqs.db

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.RenderEntity.Companion.LinkTable
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.collect.library.utils.MapParamUtils
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.buffer.BufferOp
import org.oscim.core.GeoPoint
import org.oscim.core.MercatorProjection
import javax.inject.Inject
import kotlin.streams.toList

class RealmOperateHelper() {
    @Inject
    lateinit var niMapController: NIMapController

    private var isUpdate: Boolean = false

    /**
     * 根据当前点位查询匹配的Link数据
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param sort 是否需要排序
     * */
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun queryLink(
        point: GeoPoint,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE,
        sort: Boolean = true
    ): MutableList<RenderEntity> {
        val result = mutableListOf<RenderEntity>()
        val polygon = getPolygonFromPoint(
            GeometryTools.createPoint(point.longitude, point.latitude),
            buffer,
            bufferType
        )
        // 根据polygon查询相交的tile号
        val tileXSet = mutableSetOf<Int>()

        GeometryToolsKt.getTileXByGeometry(polygon.toString(), tileXSet)
        val tileYSet = mutableSetOf<Int>()
        GeometryToolsKt.getTileYByGeometry(polygon.toString(), tileYSet)

        // 对tileXSet和tileYSet查询最大最小值
        val xStart = tileXSet.stream().min(Comparator.naturalOrder()).orElse(null)
        val xEnd = tileXSet.stream().max(Comparator.naturalOrder()).orElse(null)
        val yStart = tileYSet.stream().min(Comparator.naturalOrder()).orElse(null)
        val yEnd = tileYSet.stream().max(Comparator.naturalOrder()).orElse(null)
        // 查询realm中对应tile号的数据
        val realm = getRealmDefaultInstance()
        val realmList =
            getRealmTools(RenderEntity::class.java, false)
                .equalTo("table", "OMDB_RD_LINK")
                .greaterThanOrEqualTo("tileX", xStart)
                .lessThanOrEqualTo("tileX", xEnd)
                .greaterThanOrEqualTo("tileY", yStart)
                .lessThanOrEqualTo("tileY", yEnd)
                .findAll()
        // 将获取到的数据和查询的polygon做相交，只返回相交的数据
        val dataList = realm.copyFromRealm(realmList)
        val queryResult = dataList?.stream()?.filter {
            polygon.intersects(it.wkt)
        }?.toList()

        queryResult?.let {
            if (sort) {
                result.addAll(
                    sortRenderEntity(
                        GeometryTools.createPoint(
                            point.longitude,
                            point.latitude
                        ), it
                    )
                )
            } else {
                result.addAll(it)
            }
        }
        return result
    }

    /**
     * 根据当前点位查询匹配的Link数据
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param sort 是否需要排序
     * */
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun queryLine(
        point: GeoPoint,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE,
        table: String,
        sort: Boolean = true
    ): MutableList<RenderEntity> {
        val result = mutableListOf<RenderEntity>()
        val polygon = getPolygonFromPoint(
            GeometryTools.createPoint(point.longitude, point.latitude),
            buffer,
            bufferType
        )
        // 根据polygon查询相交的tile号
        val tileXSet = mutableSetOf<Int>()

        GeometryToolsKt.getTileXByGeometry(polygon.toString(), tileXSet)
        val tileYSet = mutableSetOf<Int>()
        GeometryToolsKt.getTileYByGeometry(polygon.toString(), tileYSet)

        // 对tileXSet和tileYSet查询最大最小值
        val xStart = tileXSet.stream().min(Comparator.naturalOrder()).orElse(null)
        val xEnd = tileXSet.stream().max(Comparator.naturalOrder()).orElse(null)
        val yStart = tileYSet.stream().min(Comparator.naturalOrder()).orElse(null)
        val yEnd = tileYSet.stream().max(Comparator.naturalOrder()).orElse(null)
        // 查询realm中对应tile号的数据
        val realm = getRealmDefaultInstance()
        val realmList = getRealmTools(RenderEntity::class.java, true)
            .equalTo("table", table)
            .greaterThanOrEqualTo("tileX", xStart)
            .lessThanOrEqualTo("tileX", xEnd)
            .greaterThanOrEqualTo("tileY", yStart)
            .lessThanOrEqualTo("tileY", yEnd)
            .findAll()
        // 将获取到的数据和查询的polygon做相交，只返回相交的数据
        val dataList = realm.copyFromRealm(realmList)
        val queryResult = dataList?.stream()?.filter {
            polygon.intersects(it.wkt)
        }?.toList()

        queryResult?.let {
            if (sort) {
                result.addAll(
                    sortRenderEntity(
                        GeometryTools.createPoint(
                            point.longitude,
                            point.latitude
                        ), it
                    )
                )
            } else {
                result.addAll(it)
            }
        }
        return result
    }

    suspend fun captureTaskLink(
        point: GeoPoint,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE,
    ): HadLinkDvoBean? {

        val polygon = getPolygonFromPoint(
            GeometryTools.createPoint(point.longitude, point.latitude),
            buffer,
            bufferType
        )

        val realm = getRealmDefaultInstance()
        val realmList = getRealmTools(HadLinkDvoBean::class.java, false).findAll()
        var linkBean: HadLinkDvoBean? = null
        var nearLast: Double = 99999.99
        for (link in realmList) {
            if (polygon.intersects(GeometryTools.createGeometry(link.geometry))) {
                val near = point.distance(GeometryTools.createGeoPoint(link.geometry))
                if (near < nearLast) {
                    nearLast = near
                    linkBean = link
                }
            }
        }
        if (linkBean != null)
            return realm.copyFromRealm(linkBean)
        return null
    }

    suspend fun queryLink(linkPid: String): RenderEntity? {
        var link: RenderEntity? = null
        val realm = getRealmDefaultInstance()
        val realmR =
            getRealmTools(RenderEntity::class.java, true).equalTo("table", "OMDB_RD_LINK")
                .equalTo("properties['${LinkTable.linkPid}']", linkPid).findFirst()
        if (realmR != null) {
            link = realm.copyFromRealm(realmR)
        }
        return link
    }

    /**
     * 根据markid查询获取对应数据
     * @param markId
     * */
    suspend fun queryQcRecordBean(markId: String): QsRecordBean? {
        var qsRecordBean: QsRecordBean? = null
        val realm = getRealmDefaultInstance()
        val realmR = getRealmTools(QsRecordBean::class.java, false)
            .equalTo("table", "QsRecordBean").equalTo("id", markId).findFirst()
        if (realmR != null) {
            qsRecordBean = realm.copyFromRealm(realmR)
        }
        return qsRecordBean
    }

    suspend fun queryLinkToMutableRenderEntityList(linkPid: String): MutableList<RenderEntity>? {
        val resultList = mutableListOf<RenderEntity>()

        val realm = getRealmDefaultInstance()

        val realmR = getRealmTools(RenderEntity::class.java, true)
            .equalTo("properties['${LinkTable.linkPid}']", linkPid).findAll()

        val dataList = realm.copyFromRealm(realmR)

        dataList.forEach {
            resultList.add(it)
        }

        return resultList
    }

    /**
     * 根据当前点位查询匹配的除Link外的其他要素数据
     * @param point 点位经纬度信息
     * @param buffer 点位的外扩距离
     * @param bufferType 点位外扩距离的单位： 米-Meter，像素-PIXEL
     * @param sort 是否需要排序
     * */
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun queryElement(
        point: Point,
        buffer: Double = DEFAULT_BUFFER,
        bufferType: BUFFER_TYPE = DEFAULT_BUFFER_TYPE,
        catchAll: Boolean = true,
        sort: Boolean = true
    ): MutableList<RenderEntity> {
        val result = mutableListOf<RenderEntity>()
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
        val realm = getRealmDefaultInstance()
        var realmList = mutableListOf<RenderEntity>()
        if(catchAll){
            // 查询realm中对应tile号的数据
            realmList = getRealmTools(RenderEntity::class.java, false)
                .greaterThanOrEqualTo("tileX", xStart)
                .lessThanOrEqualTo("tileX", xEnd)
                .greaterThanOrEqualTo("tileY", yStart)
                .lessThanOrEqualTo("tileY", yEnd)
                .findAll()
        }else{
            // 查询realm中对应tile号的数据
            realmList = getRealmTools(RenderEntity::class.java, false)
                .lessThan("catchEnable", 1)
                .greaterThanOrEqualTo("tileX", xStart)
                .lessThanOrEqualTo("tileX", xEnd)
                .greaterThanOrEqualTo("tileY", yStart)
                .lessThanOrEqualTo("tileY", yEnd)
                .findAll()
        }
        // 将获取到的数据和查询的polygon做相交，只返回相交的数据
        val queryResult = realmList?.stream()?.filter {
            polygon.intersects(it.wkt)
        }?.toList()
        queryResult?.let {
            if (sort) {
                result.addAll(sortRenderEntity(point, realm.copyFromRealm(it)))
            } else {
                result.addAll(realm.copyFromRealm(it))
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
        val realm = getRealmDefaultInstance()
        val realmList = getRealmTools(RenderEntity::class.java, false)
            .notEqualTo("table", DataCodeEnum.OMDB_RD_LINK.name)
            .equalTo("properties['${LinkTable.linkPid}']", linkPid)
            .findAll()
        result.addAll(realm.copyFromRealm(realmList))
        return result
    }

    /**
     * 根据给定的点位对数据排序
     * @param point 点位经纬度信息
     * @param unSortList 未排序的数据
     * @return 排序后的数据
     * */
    @RequiresApi(Build.VERSION_CODES.N)
    fun sortRenderEntity(point: Point, unSortList: List<RenderEntity>): List<RenderEntity> {
        val sortList = unSortList.stream().sorted { renderEntity, renderEntity2 ->
            point.distance(renderEntity.wkt).compareTo(point.distance(renderEntity2.wkt))
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


    fun <E : RealmModel> getRealmTools(clazz: Class<E>, enableSql: Boolean): RealmQuery<E> {
        return if (MapParamUtils.getDataLayerEnum() != null) {

            var sql = "taskId=${MapParamUtils.getTaskId()}"

            if (enableSql) {
                sql =
                    " enable${MapParamUtils.getDataLayerEnum().sql} and taskId=${MapParamUtils.getTaskId()}"
            }

            getRealmDefaultInstance().where(clazz).rawPredicate(sql)

        } else {
            getRealmDefaultInstance().where(clazz)
                .rawPredicate(" taskId=${MapParamUtils.getTaskId()}")
        }
    }

    fun getRealmDefaultInstance(): Realm {
        if (isUpdate) {
            Log.e("jingo", "数据库更新")
            Realm.getDefaultInstance().refresh()
            isUpdate = false;
        }
        return Realm.getDefaultInstance()
    }

    fun updateRealmDefaultInstance() {
        isUpdate = true
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

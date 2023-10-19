package com.navinfo.omqs.util

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.utils.FootAndDistance
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.db.RealmOperateHelper
import io.realm.Realm
import org.oscim.core.GeoPoint
import java.time.LocalDate
import java.time.LocalDateTime

class NaviEngineNew(
    private val realmOperateHelper: RealmOperateHelper,
) {
    /**
     * 要查询的link基本信息列表
     */
    private val QUERY_KEY_LINK_INFO_LIST = arrayOf(
        DataCodeEnum.OMDB_RD_LINK.name,
        DataCodeEnum.OMDB_LINK_DIRECT.name,
        DataCodeEnum.OMDB_LINK_NAME.name,
    )


    private val locationList = mutableListOf<NiLocation>()


    suspend fun bindingRoute(
        niLocation: NiLocation? = null,
        taskBean: TaskBean,
        geoPoint: GeoPoint,
        realm:Realm
    ) {
//        val geoPoint = GeoPoint(niLocation.latitude, niLocation.longitude)
        var latestRoute: HadLinkDvoBean? = null
        var lastDis = -1.0

        for (link in taskBean.hadLinkDvoList) {
            val linkGeometry = GeometryTools.createGeometry(link.geometry)
            val footAndDistance = GeometryTools.pointToLineDistance(geoPoint, linkGeometry)
            val meterD = footAndDistance.getMeterDistance()
            if (meterD < 15 && (lastDis < 0 || lastDis > meterD)) {
                latestRoute = link
                lastDis = meterD
            }
        }


        latestRoute?.let {
            var lastTime = System.currentTimeMillis()

            var nowTime = System.currentTimeMillis()
            Log.e("jingo","打开数据库 ${nowTime - lastTime}")
            lastTime = nowTime
            val res2 =
                realm.where(RenderEntity::class.java).`in`("table", QUERY_KEY_LINK_INFO_LIST)
                    .equalTo("properties['linkPid']", it.linkPid).findAll()
            nowTime = System.currentTimeMillis()
            Log.e("jingo", "第一种 耗时 ${nowTime - lastTime}")
            if (res2 != null) {
                for (entity in res2) {
                    when (entity.code) {
                        DataCodeEnum.OMDB_RD_LINK.code -> {
                            val snodePid = entity.properties["snodePid"]
                            if (snodePid != null) {
                            } else {
                            }
                            val enodePid = entity.properties["enodePid"]
                            if (enodePid != null) {
                            } else {
                            }
                        }
                        DataCodeEnum.OMDB_LINK_DIRECT.code -> {
                            val direct = entity.properties["direct"]
                            if (direct != null) {
                            }
                        }
                        DataCodeEnum.OMDB_LINK_NAME.code -> {
//                            var name = realm.copyFromRealm(res4)
                        }
                    }
                }

            }

        }

    }
}
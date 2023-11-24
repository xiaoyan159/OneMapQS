//package com.navinfo.omqs.util
//
//import android.util.Log
//import com.navinfo.collect.library.data.entity.*
//import com.navinfo.collect.library.enums.DataCodeEnum
//import com.navinfo.collect.library.utils.GeometryTools
//import com.navinfo.omqs.bean.NaviRoute
//import com.navinfo.omqs.db.RealmOperateHelper
//import io.realm.Realm
//import org.oscim.core.GeoPoint
//
//class NaviEngineNew(
//    private val realmOperateHelper: RealmOperateHelper,
//    var naviOption: NaviOption = NaviOption()
//) {
//    /**
//     * 要查询的link基本信息列表
//     */
//    private val QUERY_KEY_LINK_INFO_LIST = arrayOf(
//        DataCodeEnum.OMDB_RD_LINK.name,
//        DataCodeEnum.OMDB_LINK_DIRECT.name,
//        DataCodeEnum.OMDB_LINK_NAME.name,
//    )
//
//    var latestRoute: HadLinkDvoBean? = null
//
//    private val locationList = mutableListOf<NiLocation>()
//    var lastDis = 9999999.0
//
//    /**
//     * 局部匹配时的路段
//     */
//    var tempRoutList = mutableListOf<NaviRoute>()
//
//    suspend fun bindingRoute(
//        niLocation: NiLocation? = null,
//        taskBean: TaskBean,
//        geoPoint: GeoPoint,
//    ): Boolean {
//        val realm = realmOperateHelper.getSelectTaskRealmInstance()
//        if (latestRoute == null) {
//
//            val time = System.currentTimeMillis()
//            for (link in taskBean.hadLinkDvoList) {
//                val linkGeometry = GeometryTools.createGeometry(link.geometry)
//                val footAndDistance = GeometryTools.pointToLineDistance(geoPoint, linkGeometry)
//                val meterD = footAndDistance.getMeterDistance()
//                if (lastDis > meterD) {
//                    if (meterD < 15)
//                        latestRoute = link
//                    lastDis = meterD
//                }
//            }
//            Log.e("jingo", "定位匹配 ${System.currentTimeMillis() - time} $lastDis")
//            latestRoute?.let {
//                val naviRoute = getNaviRouteByLinkPid(realm, it.linkPid)
//                if (naviRoute != null) {
//                    tempRoutList.add(naviRoute)
//                    var bDirectOk = false
//                    //反方向调转方向
//                    if (naviRoute.direct == 2) {
//                        bDirectOk = true
//                    }
//                    if (naviRoute.direct == 3) {
//                        bDirectOk = true
//                        naviRoute.pointList.reverse()
//                        val sNode = naviRoute.eNode
//                        naviRoute.eNode = naviRoute.sNode
//                        naviRoute.sNode = sNode
//                    }
//                    var length = naviRoute.length
//                    //是不是没有能连接的路了
//                    var bHisNextNode = true
//                    while (length < naviOption.farthestDisplayDistance + 2000 && bHisNextNode) {
//                        val currentRoute = tempRoutList.last()
//                        if (bDirectOk) {
//                            val listPid =
//                                realm.where(LinkRelation::class.java)
//                                    .beginGroup()
//                                    .equalTo("sNodeId", currentRoute.eNode).or()
//                                    .equalTo("eNodeId", currentRoute.eNode)
//                                    .endGroup().notEqualTo("linkPid", currentRoute.linkId).findAll()
//                            if (listPid.isNotEmpty()) {
//                                var bHisNextNode2 = false
//                                for (linkPid in listPid) {
//                                    val nextRoute = getNaviRouteByLinkPid(realm, linkPid as String)
//                                    if (nextRoute != null) {
//                                        //顺方向，snode 链接 enode
//                                        if (nextRoute.sNode == currentRoute.eNode && nextRoute.direct == 2) {
//                                            var bInHadList = false
//                                            for (link in taskBean.hadLinkDvoList) {
//                                                if (link.linkPid == nextRoute.linkId) {
//                                                    bInHadList = true
//                                                    tempRoutList.add(nextRoute)
//                                                    break
//                                                }
//                                            }
//                                            if (bInHadList) {
//                                                break
//                                            }
//                                        } else if (nextRoute.eNode == currentRoute.eNode && nextRoute.direct == 3) {
//                                            var bInHadList = false
//                                            for (link in taskBean.hadLinkDvoList) {
//                                                if (link.linkPid == nextRoute.linkId) {
//                                                    bInHadList = true
//                                                    nextRoute.pointList.reverse()
//                                                    val sNode = nextRoute.eNode
//                                                    nextRoute.eNode = nextRoute.sNode
//                                                    nextRoute.sNode = sNode
//                                                    tempRoutList.add(nextRoute)
//                                                    break
//                                                }
//                                            }
//                                            if (bInHadList) {
//                                                break
//                                            }
//                                        } else {
//                                            if (nextRoute.sNode == currentRoute.eNode) {
//
//                                            }
//                                        }
//                                    }
//                                }
//                            } else {
//                                bHisNextNode = false
//                            }
//                        }
//                    }
//                } else {
//                    "查询不到link的基础属性 ${it.linkPid}"
//                    realm.close()
//                    return false
//                }
//            }
//        }
//        realm.close()
//        return true
//    }
//
//    private fun getNaviRouteByLinkPid(realm: Realm, linkPid: String): NaviRoute? {
//        val naviRoute = NaviRoute(linkId = linkPid)
//        val res2 =
//            realm.where(RenderEntity::class.java).`in`("table", QUERY_KEY_LINK_INFO_LIST)
//                .equalTo("linkPid", linkPid).findAll()
//        if (res2 != null) {
//            for (entity in res2) {
//                when (entity.code) {
//                    //获取snode enode
//                    DataCodeEnum.OMDB_RD_LINK.code -> {
//                        if (entity.linkRelation != null) {
//                            if (entity.linkRelation!!.eNodeId == null || entity.linkRelation!!.sNodeId == null) {
//                                "读取不到link的Node点，${linkPid}"
//                                return null
//                            } else {
//                                naviRoute.eNode = entity.linkRelation!!.eNodeId!!
//                                naviRoute.sNode = entity.linkRelation!!.sNodeId!!
//                            }
//                        }
//                    }
//                    //获取方向，geometry
//                    DataCodeEnum.OMDB_LINK_DIRECT.code -> {
//                        val direct = entity.properties["direct"]
//                        if (direct != null)
//                            naviRoute.direct = direct.toInt()
//                        else {
//                            "读取不到link的方向，${linkPid}"
//                            return null
//                        }
//                        naviRoute.pointList = GeometryTools.getGeoPoints(entity.geometry)
//                        naviRoute.length = GeometryTools.getDistance(naviRoute.pointList)
//                    }
//                    //获取名称
//                    DataCodeEnum.OMDB_LINK_NAME.code -> {
//                        naviRoute.name = realm.copyFromRealm(entity)
//                    }
//                }
//            }
//        } else {
//            return null
//        }
//        return naviRoute
//    }
//}
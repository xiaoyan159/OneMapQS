package com.navinfo.omqs.util

import android.util.Log
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.bean.NaviRoute
import com.navinfo.omqs.bean.NaviRouteItem
import com.navinfo.omqs.db.RealmOperateHelper
import io.realm.Realm
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.oscim.core.GeoPoint

interface OnNaviEngineCallbackListener {
    fun planningPathStatus(code: NaviStatus)

    //    fun planningPathError(errorCode: NaviStatus, errorMessage: String)
    suspend fun bindingResults(route: NaviRoute?, list: List<NaviRouteItem>)
}

enum class NaviStatus {
    NAVI_STATUS_PATH_PLANNING, //路径规划中
    NAVI_STATUS_PATH_ERROR_NODE,//node点缺失
    NAVI_STATUS_PATH_ERROR_DIRECTION,//缺少方向
    NAVI_STATUS_PATH_ERROR_BLOCKED,//路径不通
    NAVI_STATUS_PATH_SUCCESS,//路径规划成功
    NAVI_STATUS_DISTANCE_OFF,//距离偏离
    NAVI_STATUS_DIRECTION_OFF,//方向偏离
}


data class NaviOption(
    /**
     * 偏离距离 单位：米
     */
    var deviationDistance: Int = 15,

    /**
     * 偏离次数上限
     */
    var deviationCount: Int = 5,

//    /**
//     * 局部匹配时，没走过的路段还记录1000米
//     */
//    var nextRouteDistance: Int = 1000,

    /**
     * 最远显示距离 米
     */
    var farthestDisplayDistance: Int = 550,
)

class NaviEngine(
    private val niMapController: NIMapController,
    private val realmOperateHelper: RealmOperateHelper,
    val callback: OnNaviEngineCallbackListener,
    var naviOption: NaviOption = NaviOption()
) {

    /**
     * 要查询的要素列表
     */
    private val QUERY_KEY_ITEM_LIST = arrayOf(
        DataCodeEnum.OMDB_ELECTRONICEYE.name,
        DataCodeEnum.OMDB_SPEEDLIMIT.name,
        DataCodeEnum.OMDB_SPEEDLIMIT_COND.name,
        DataCodeEnum.OMDB_SPEEDLIMIT_VAR.name,
        DataCodeEnum.OMDB_TRAFFICLIGHT.name,
//        DataCodeEnum.OMDB_RESTRICTION.name,
        DataCodeEnum.OMDB_LANEINFO.name,
        DataCodeEnum.OMDB_TRAFFIC_SIGN.name,
        DataCodeEnum.OMDB_WARNINGSIGN.name,
        DataCodeEnum.OMDB_TOLLGATE.name
    )

    /**
     * 要查询的link基本信息列表
     */
//    private val QUERY_KEY_LINK_INFO_LIST = arrayOf(
//        DataCodeEnum.OMDB_RD_LINK.name,
//        DataCodeEnum.OMDB_LINK_DIRECT.name,
//        DataCodeEnum.OMDB_LINK_NAME.name,
//    )

//    /**
//     * 偏离距离 单位：米
//     */
//    private val DEVIATION_DISTANCE = 15
//
//    /**
//     * 偏离次数上限
//     */
//    private val DEVIATION_COUNT = 5
//
    /**
     *  局部匹配时，走过的路段还记录100米
     */
    private val PASSED_ROUTE_DISTANCE = 100
//
//    /**
//     * 局部匹配时，没走过的路段还记录1000米
//     */
//    private val NEXT_ROUTE_DISTANCE = 1000
//
//    /**
//     * 最远显示距离 米
//     */
//    private val FARTHEST_DISPLAY_DISTANCE = 550

    /**
     * 绑定失败次数
     */
    private var errorCount = 0

    /**
     * 当前邦定的那段route
     */
    private var routeIndex = -1

    /**
     * 当前绑定的link上哪个point
     */
    private var footIndex = -1

    /**
     * 绑定的垂足坐标
     */
    private var footPoint: GeoPoint? = null

    /**
     * 上一次定位绑到路线的距离
     */
    private var lastDistance = -1

    /**
     * 整条路的几何
     */
    var geometry: LineString? = null

    /**
     * 临时路径
     */
    var tempGeometry: LineString? = null

    /**
     * 定位点集合
     */
    private var locationList: MutableList<NiLocation> = mutableListOf()

    /**
     * 局部匹配时的路段
     */
    var tempRoutList = mutableListOf<NaviRoute>()


    /**
     * 所有路段集合
     */
    var routeList = mutableListOf<NaviRoute>()
        get() {
            return field
        }
        set(value) {
            val list = mutableListOf<GeoPoint>()
            if (value.size > 0) {
                val fRoute = value[0]
                //第一个路段加入
                list.addAll(fRoute.pointList)
                //起始点位置
                fRoute.startIndexInPath = 0
                var startPoint = fRoute.pointList.size - 1
                //终点位置
                fRoute.endIndexIntPath = startPoint
                fRoute.indexInPath = 0

                for (i in 1 until value.size) {
                    val route = value[i]
                    route.startIndexInPath = startPoint
                    if (route.itemList != null) {
                        for (naviItem in route.itemList!!) {
                            naviItem.index += startPoint
                        }
                    }
                    startPoint += route.pointList.size - 1
                    route.endIndexIntPath = startPoint
                    route.indexInPath = i
                    val list2 = ArrayList(route.pointList.toList())
                    list2.removeAt(0)
                    list.addAll(list2)
                }
                geometry = GeometryTools.createLineString(list)
            }
            field = value
        }

    /**
     *  计算路径
     */
    suspend fun planningPath(taskBean: TaskBean) {
        callback.planningPathStatus(NaviStatus.NAVI_STATUS_PATH_PLANNING)
        val pathList = mutableListOf<NaviRoute>()
        val realm = realmOperateHelper.getSelectTaskRealmInstance()
        for (link in taskBean.hadLinkDvoList) {
            //测线不参与导航
            if (link.linkStatus == 3) {
                continue
            }
            val route = NaviRoute(
                linkId = link.linkPid,
            )

            route.pointList = GeometryTools.getGeoPoints(link.geometry)
            var time = System.currentTimeMillis()
            val res = realm.where(RenderEntity::class.java)
                .equalTo("table", DataCodeEnum.OMDB_RD_LINK.name).equalTo("linkPid", link.linkPid)
                .findFirst()

            Log.e("jingo","查询link时间 ${System.currentTimeMillis() - time}")

            if (res?.linkRelation != null) {
                if (res.linkRelation!!.sNodeId == null) {
                    callback.planningPathStatus(
                        NaviStatus.NAVI_STATUS_PATH_ERROR_NODE
                    )
                    return
                } else {
                    route.sNode = res.linkRelation!!.sNodeId!!
                }
                if (res.linkRelation!!.eNodeId == null) {
                    callback.planningPathStatus(
                        NaviStatus.NAVI_STATUS_PATH_ERROR_NODE
                    )
                    return
                } else {
                    route.eNode = res.linkRelation!!.eNodeId!!
                }

                route.direct = res.linkRelation!!.direct
//                route.name = res.linkRelation!!.linkName
                time = System.currentTimeMillis()
                val otherLinks = realm.where(RenderEntity::class.java)
                    .equalTo("table", DataCodeEnum.OMDB_RD_LINK.name)
                    .notEqualTo("linkPid", route.linkId).beginGroup()
                    .`in`("linkRelation.sNodeId", arrayOf(route.sNode, route.eNode)).or()
                    .`in`("linkRelation.eNodeId", arrayOf(route.sNode, route.eNode)).endGroup()
                    .findAll()
                Log.e("jingo","拓扑道路时间 ${System.currentTimeMillis() - time}  共${otherLinks.size}条")
            } else {
                callback.planningPathStatus(
                    NaviStatus.NAVI_STATUS_PATH_ERROR_NODE
                )
                return
            }
            pathList.add(route)
        }

        //用来存储最终的导航路径
        val newRouteList = mutableListOf<NaviRoute>()

        //比对路径排序用的
        val tempRouteList = pathList.toMutableList()

        //先找到一根有方向的link，确定起终点
        var routeStart: NaviRoute? = null
        for (i in tempRouteList.indices) {
            val route = pathList[i]
            //只要时单方向的就行
            if (route.direct == 2 || route.direct == 3) {
                routeStart = route
                tempRouteList.removeAt(i)
                break
            }
        }

        if (routeStart == null) {
            routeStart = tempRouteList[0]
            tempRouteList.removeAt(0)
        }

        var sNode = ""
        var eNode = ""
//如果sNode，eNode是顺方向，geometry 不动，否则反转
        if (routeStart.direct == 3) {
            routeStart.pointList.reverse()
            sNode = routeStart.eNode
            eNode = routeStart.sNode
        } else {
            sNode = routeStart.sNode
            eNode = routeStart.eNode
        }
        newRouteList.add(routeStart)
        var bBreak = true
        while (bBreak) {
            //先找其实link的后续link
            var bHasNext = false
            for (route in tempRouteList) {
                //如果是link 的e 对下个link的s，方向不用动，否则下个link的geometry反转
                if (route.sNode != "" && eNode == route.sNode) {
                    newRouteList.add(route)
                    tempRouteList.remove(route)
                    eNode = route.eNode
                    bHasNext = true
                    break
                } else if (route.eNode != "" && eNode == route.eNode) {
                    route.pointList.reverse()
                    newRouteList.add(route)
                    tempRouteList.remove(route)
                    eNode = route.sNode
                    bHasNext = true
                    break
                }
            }
            //先找其实link的起始link
            var bHasLast = false
            for (route in tempRouteList) {
                //如果是link 的s 对上个link的e，方向不用动，否则下个link的geometry反转
                if (route.eNode != "" && sNode == route.eNode) {
                    newRouteList.add(0, route)
                    tempRouteList.remove(route)
                    sNode = route.sNode
                    bHasLast = true
                    break
                } else if (route.sNode != "" && sNode == route.sNode) {
                    route.pointList.reverse()
                    newRouteList.add(0, route)
                    tempRouteList.remove(route)
                    sNode = route.eNode
                    bHasLast = true
                    break
                }
            }
            if (tempRouteList.size == 0) {
                bBreak = false
            } else {
                if (!bHasLast && !bHasNext) {
                    bBreak = false
                    callback.planningPathStatus(
                        NaviStatus.NAVI_STATUS_PATH_ERROR_BLOCKED
                    )
                    realm.close()
                    return
                }
            }
        }

        val itemMap: MutableMap<GeoPoint, MutableList<RenderEntity>> = mutableMapOf()
//查询每根link上的关联要素
        for (route in newRouteList) {
            itemMap.clear()
            //常规点限速
            val res =
                realm.where(RenderEntity::class.java).equalTo("linkPid", route.linkId).and().`in`(
                        "table", QUERY_KEY_ITEM_LIST
                    ).findAll()
            if (res.isNotEmpty()) {
//                Log.e("jingo", "道路查询预警要素 ${route.linkId} ${res.size}条数据")
                for (r in res) {
//                    Log.e("jingo", "道路查询预警要素 ${r.name}")
                    insertItemToRoute(realm, route, r, itemMap)
                }
            }
            //对路径上的要素进行排序
            if (itemMap.isNotEmpty()) {
                route.itemList = mutableListOf()
                for (i in route.pointList.indices) {
                    val point = route.pointList[i]
                    if (itemMap.containsKey(point)) {
                        val rEList = itemMap[point]
                        for (item in rEList!!) {
                            val naviRouteItem = NaviRouteItem(i, item, route.linkId)
                            route.itemList!!.add(naviRouteItem)
                        }
                        itemMap.remove(point)
                    }
                }
                route.itemList!!.sortBy { it.index }
            }
        }
        realm.close()
        routeList = newRouteList
        callback.planningPathStatus(NaviStatus.NAVI_STATUS_PATH_SUCCESS)

    }

    /**
     * 将要素绑定到路径上
     */
    private fun insertItemToRoute(
        realm: Realm,
        route: NaviRoute,
        res: RenderEntity?,
        itemMap: MutableMap<GeoPoint, MutableList<RenderEntity>>
    ) {
        if (res != null) {
            val geometry = GeometryTools.createGeometry(res.geometry)
            if (geometry is Point) {
                //获取一个垂足点
                val footAndDistance = GeometryTools.pointToLineDistance(
                    GeoPoint(
                        geometry.coordinate.y, geometry.coordinate.x
                    ), GeometryTools.createLineString(route.pointList)
                )
                val point = GeoPoint(
                    footAndDistance.getCoordinate(0).y, footAndDistance.getCoordinate(0).x
                )
                //测试marker
//                niMapController.markerHandle.addMarker(point, res.id, res.name)
                route.pointList.add(footAndDistance.footIndex + 1, point)
                if (itemMap.containsKey(point)) {
                    itemMap[point]!!.add(realm.copyFromRealm(res))
                } else {
                    itemMap[point] = mutableListOf(realm.copyFromRealm(res))
                }
            } else if (geometry is LineString) {
                //如果是线型数据，取路径的最后一个点
                val endPoint = route.pointList.last()
                if (itemMap.containsKey(endPoint)) {
                    itemMap[endPoint]!!.add(realm.copyFromRealm(res))
                } else {
                    itemMap[endPoint] = mutableListOf(realm.copyFromRealm(res))
                }
            }
        }
    }

    /**
     * 绑定道路
     */
    suspend fun bindingRoute(location: NiLocation?, point: GeoPoint) {
        if (geometry != null) {
            //还没有绑定到路径的时候
            if (routeIndex < 0) {
                val pointPairDistance = GeometryTools.pointToLineDistance(point, geometry)
                //定义垂线
                //定位点到垂足距离不超过30米
                if (pointPairDistance.getMeterDistance() < naviOption.deviationDistance) {
                    footIndex = pointPairDistance.footIndex
//                    Log.e(
//                        "jingo",
//                        "当前绑定到了整条路线的第 ${footIndex} 点 ${pointPairDistance.getMeterDistance()} "
//                    )
                    val lastRouteIndex = routeIndex
                    for (i in routeList.indices) {
                        val route = routeList[i]
                        if (route.startIndexInPath <= footIndex && route.endIndexIntPath >= footIndex) {
                            routeIndex = route.indexInPath
//                            Log.e(
//                                "jingo",
//                                "当前绑定到了整条路线id ${route.linkId} "
//                            )
//                            niMapController.lineHandler.showLine(route.pointList)
                            footPoint = GeoPoint(
                                pointPairDistance.getCoordinate(0).y,
                                pointPairDistance.getCoordinate(0).x
                            )
//                            val listPoint = mutableListOf(point, footPoint!!)
//                            niMapController.lineHandler.showLine(
//                                listPoint
//                            )
                            if (lastRouteIndex != routeIndex) {
                                createTempPath()
                            }
                            errorCount = 0
                            break
                        }
                    }
                } else {
                    deviationUp()
                }
            } else if (tempGeometry != null && tempRoutList.isNotEmpty()) {
                val pointPairDistance = GeometryTools.pointToLineDistance(point, tempGeometry)
                //定义垂线
                //定位点到垂足距离不超过30米
                if (pointPairDistance.getMeterDistance() < naviOption.deviationDistance) {
                    footIndex = pointPairDistance.footIndex + tempRoutList[0].startIndexInPath
//                    Log.e("jingo", "局部 当前绑定到了整条路线的第 $footIndex 点")
                    val lastRouteIndex = routeIndex
                    for (i in tempRoutList.indices) {
                        val route = tempRoutList[i]
                        if (route.startIndexInPath <= footIndex && route.endIndexIntPath >= footIndex) {
                            routeIndex = route.indexInPath
//                            Log.e(
//                                "jingo",
//                                "局部 当前绑定到了整条路线id ${route.linkId} "
//                            )
//                            niMapController.lineHandler.showLine(route.pointList)
                            footPoint = GeoPoint(
                                pointPairDistance.getCoordinate(0).y,
                                pointPairDistance.getCoordinate(0).x
                            )

//                            val listPoint = mutableListOf(point, footPoint!!)
//                            niMapController.lineHandler.showLine(
//                                listPoint
//                            )
                            matchingItem()
                            if (lastRouteIndex != routeIndex) {
                                createTempPath()
                            }
                            errorCount = 0
                            break
                        }
                    }
                } else {
                    deviationUp()
                }
            }
        }
    }

    /**
     *  匹配要素
     *  @point:定位点
     */
    private suspend fun matchingItem() {

        if (routeIndex > -1 && tempRoutList.isNotEmpty() && tempGeometry != null) {
//            Log.e("jingo", "当前${routeIndex} ${tempRoutList[0].startIndexInPath} $footIndex")
            //道路前方一定距离范围内的要素信息
            val bindingItemList = mutableListOf<NaviRouteItem>()
            //定位点到要素的路径距离
            var distance = 0.0
            //计算要素路径距离的点集合
            val disPoints = mutableListOf(footPoint!!)
            //下一个要素的起点游标
            var tempIndex = footIndex - tempRoutList[0].startIndexInPath + 1
            var currentRoute: NaviRoute? = null
            for (route in tempRoutList) {
//                if (route.itemList != null) {
//                    Log.e("jingo", "${route.linkId}我有${route.itemList!!.size}个要素 ")
//                }
                if (route.indexInPath < routeIndex) continue
                if (route.indexInPath == routeIndex) {
                    currentRoute = route
                }
                if (route.itemList != null && route.itemList!!.isNotEmpty()) {
                    for (naviItem in route.itemList!!) {
//                        Log.e(
//                            "jingo",
//                            "我是：${naviItem.data.name} 我的点位 ${naviItem.index} 垂足点位 $footIndex"
//                        )
                        if (naviItem.index > footIndex) {
                            val rightI = naviItem.index - tempRoutList[0].startIndexInPath + 1
                            for (i in tempIndex until rightI) {
                                val geo = tempGeometry!!.coordinates[i]
                                disPoints.add(GeoPoint(geo.y, geo.x))
                            }
                            tempIndex = rightI + 1
                            distance = GeometryTools.getDistance(disPoints)
//                            Log.e("jingo", "我的距离${distance} 下一个${tempIndex} 位置${rightI}")
                            if (distance < naviOption.farthestDisplayDistance && distance > -1) {
                                naviItem.distance = distance.toInt()
                                bindingItemList.add(naviItem)
                            } else {
                                break
                            }
                        }
                    }
                    if (distance >= naviOption.farthestDisplayDistance) {
                        break
                    }
                }
            }
            callback.bindingResults(currentRoute, bindingItemList)
        }
    }

    /**
     * 创建临时局部路径
     */
    private fun createTempPath() {
        tempRoutList.clear()
        tempGeometry = null
        if (routeIndex > -1 && routeIndex < routeList.size && footPoint != null) {
            val route = routeList[routeIndex]
            //当前垂足点在这个路段中是第几个坐标
            tempRoutList.add(route)
            var distance = 0.0
            //已经走过的路是否有100米
            var i = routeIndex - 1
            while (i >= 0 && distance < PASSED_ROUTE_DISTANCE) {
                val routeT = routeList[i]
                tempRoutList.add(0, routeT)
                distance += routeT.length
                i--
            }
            distance = 0.0
            //没走过的路是否有1000米
            var j = routeIndex + 1
            val nextDis = naviOption.farthestDisplayDistance + 500
            while (j < routeList.size && distance < nextDis) {
                val routeT = routeList[j]
                tempRoutList.add(routeT)
                distance += routeT.length
                j++
            }
        }
        val list = mutableListOf<GeoPoint>()
        val fRoute = tempRoutList[0]
        //第一个路段加入
        list.addAll(fRoute.pointList)

        for (i in 1 until tempRoutList.size) {
            val route = tempRoutList[i]
            val list2 = ArrayList(route.pointList.toList())
            list2.removeAt(0)
            list.addAll(list2)
        }
        tempGeometry = GeometryTools.createLineString(list)
    }

    /**
     * 判断是否完全偏离
     */
    private fun deviationUp() {
        errorCount++
        if (errorCount >= naviOption.deviationCount) {
            callback.planningPathStatus(NaviStatus.NAVI_STATUS_DISTANCE_OFF)
            bindingReset()
        }
    }

    /**
     * 绑定重置
     */
    private fun bindingReset() {
        //绑定失败次数
        errorCount = 0
        //当前邦定的那段route
        routeIndex = -1
        //当前绑定的link上哪个point
        footIndex = -1
        //上一次定位绑到路线的距离
        lastDistance = -1
        //垂足坐标
        footPoint = null
        //定位点集合
        locationList.clear()
    }

}
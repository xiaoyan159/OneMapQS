package com.navinfo.omqs.util

import android.util.Log
import com.navinfo.collect.library.data.entity.*
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
    suspend fun planningPathStatus(code: NaviStatus, message: String = "", linkId: String? = null, geometry: String? = null)

    //    fun planningPathError(errorCode: NaviStatus, errorMessage: String)
    suspend fun bindingResults(route: NaviRoute?, list: List<NaviRouteItem>)

    suspend fun voicePlay(text: String): Boolean
}

enum class NaviStatus {
    NAVI_STATUS_PATH_PLANNING, //路径规划中
    NAVI_STATUS_PATH_ERROR_BLOCKED,//路径不通
    NAVI_STATUS_PATH_SUCCESS,//路径规划成功
    NAVI_STATUS_DISTANCE_OFF,//距离偏离
    NAVI_STATUS_DIRECTION_OFF,//方向偏离
    NAVI_STATUS_DATA_ERROR,//数据错误
    NAVI_STATUS_NO_START_OR_END,//没有设置起终点
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
        DataCodeEnum.OMDB_CLM_LANEINFO.name,
        DataCodeEnum.OMDB_TRAFFIC_SIGN.name,
        DataCodeEnum.OMDB_WARNINGSIGN.name,
        DataCodeEnum.OMDB_TOLLGATE.name
    )

    /**
     * 要查询的link基本信息列表
     */
    private val QUERY_KEY_LINK_INFO_LIST = arrayOf(
        DataCodeEnum.OMDB_RD_LINK.name,
        DataCodeEnum.OMDB_LINK_DIRECT.name,
        DataCodeEnum.OMDB_LINK_NAME.name,
        DataCodeEnum.OMDB_RD_LINK_KIND.name,
        DataCodeEnum.OMDB_LINK_SPEEDLIMIT.name
    )

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
    private var geometry: LineString? = null

    /**
     * 临时路径
     */
    private var tempGeometry: LineString? = null

    /**
     * 定位点集合
     */
    private var locationList: MutableList<NiLocation> = mutableListOf()

    /**
     * 局部匹配时的路段
     */
    private var tempRoutList = mutableListOf<NaviRoute>()

    private var currentRoadName = ""

    /**
     * 所有路段集合
     */
    private var routeList = mutableListOf<NaviRoute>()
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
     * 查询，反转link
     */
    private suspend fun findNaviRouteByLinkId(realm: Realm, linkId: String, startNodeId: String? = null, endNodeId: String? = null): NaviRoute? {
        val res = realm.where(RenderEntity::class.java).`in`("table", QUERY_KEY_LINK_INFO_LIST)
            .equalTo("linkPid", linkId).findAll()
        if (res != null) {
            /**
             * 是不是有node点
             */
            var bHasNode = false
            var bHasDir = false
            var bHasName = false
            var bHasKind = false
            val route = NaviRoute(
                linkId = linkId,
            )

            for (entity in res) {
                when (entity.code) {
                    DataCodeEnum.OMDB_RD_LINK.code -> {
                        bHasNode = true
                        val snodePid = entity.properties["snodePid"]
                        if (snodePid != null) {
                            route.sNode = snodePid
                        } else {
                            bHasNode = false
                        }
                        val enodePid = entity.properties["enodePid"]
                        if (enodePid != null) {
                            route.eNode = enodePid
                        } else {
                            bHasNode = false
                        }
                        route.pointList = GeometryTools.getGeoPoints(entity.geometry)
                    }
                    DataCodeEnum.OMDB_LINK_DIRECT.code -> {
                        val direct = entity.properties["direct"]
                        if (direct != null) {
                            bHasDir = true
                            route.direct = direct.toInt()
                        }
                    }
                    DataCodeEnum.OMDB_LINK_NAME.code -> {
                        bHasName = true
                        route.name = realm.copyFromRealm(entity)
                    }
                    DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> {
                        route.speedLimit = "${entity.properties["maxSpeed"]}"
                    }
                    DataCodeEnum.OMDB_RD_LINK_KIND.code -> {
                        val kind = entity.properties["kind"]
                        if (kind != null) {
                            bHasKind = true
                            route.kind = kind
                        }
                    }
                }
            }
            if (!bHasNode) {
                callback.planningPathStatus(NaviStatus.NAVI_STATUS_DATA_ERROR, "link缺少node数据", linkId)
                return null
            }
            if (!bHasDir) {
                callback.planningPathStatus(NaviStatus.NAVI_STATUS_DATA_ERROR, "link缺少方向数据", linkId)
                return null
            }
            if (!bHasKind) {
                callback.planningPathStatus(NaviStatus.NAVI_STATUS_DATA_ERROR, "link缺少种别数据", linkId)
                return null
            }
            //根据起终点反转方向
            if (startNodeId != null) {
                if (startNodeId == route.eNode) {
                    //顺方向，起点和终点还不一致的
                    if (route.direct == 2) {
                        callback.planningPathStatus(NaviStatus.NAVI_STATUS_DATA_ERROR, "link为顺方向，与行进方向不符，请检查数据", linkId)
                        return null
                    } else {
                        route.pointList.reverse()
                        route.eNode = route.sNode
                        route.sNode = startNodeId
                    }
                }
            } else if (endNodeId != null) {
                if (endNodeId == route.sNode) {
                    //顺方向，起点和终点还不一致的
                    if (route.direct == 2) {
                        callback.planningPathStatus(NaviStatus.NAVI_STATUS_DATA_ERROR, "link为顺方向，与行进方向不符，请检查数据", linkId)
                        return null
                    } else {
                        route.pointList.reverse()
                        val tempNode = route.sNode
                        route.sNode = route.eNode
                        route.eNode = tempNode
                    }
                }
            }
            return route
        }
        return null
    }

    /**
     *  计算路径
     */
    suspend fun planningPath(taskBean: TaskBean) {
        callback.planningPathStatus(NaviStatus.NAVI_STATUS_PATH_PLANNING)
        val pathList = mutableListOf<NaviRoute>()
        val realm = realmOperateHelper.getSelectTaskRealmInstance()
        //没有设置起终点
        if (taskBean.navInfo == null || (taskBean.navInfo!!.naviStartLinkId.isEmpty() || taskBean.navInfo!!.naviEndLinkId.isEmpty())) {
            callback.planningPathStatus(NaviStatus.NAVI_STATUS_NO_START_OR_END)
            return
        }
        /**
         * 是否起算路径结束
         */
        var bFullPath = true

        /**
         * 起点线在哪个位置
         */
        var pathImportIndex = 0
        val hadLinkDvoListTemp: MutableList<HadLinkDvoBean> = taskBean.hadLinkDvoList.toMutableList()
        while (bFullPath) {
            if (pathList.isEmpty()) {
                val startRoute =
                    findNaviRouteByLinkId(realm = realm, linkId = taskBean.navInfo!!.naviStartLinkId, startNodeId = taskBean.navInfo!!.naviStartNode)
                        ?: return
                pathList.add(startRoute)
                pathImportIndex = pathList.size

                val endRoute =
                    findNaviRouteByLinkId(realm = realm, linkId = taskBean.navInfo!!.naviEndLinkId, endNodeId = taskBean.navInfo!!.naviEndNode)
                        ?: return
                pathList.add(endRoute)
            }
            val leftRoute = pathList[pathImportIndex - 1]
            val rightRout = pathList[pathImportIndex]
            //如果左侧nodeid和右侧nodeid 一直了，说明整个路径联通了
            if (leftRoute.eNode == rightRout.sNode) {
                bFullPath = false
                break
            } else {
                //查询左侧node的拓扑link
                val nodeLinks = realm.where(LinkRelation::class.java)
                    .beginGroup()
                    .equalTo("sNodeId", leftRoute.eNode).or()
                    .equalTo("eNodeId", leftRoute.eNode)
                    .endGroup().notEqualTo("linkPid", leftRoute.linkId).findAll()
                val leftNodeLinks = nodeLinks.toMutableList()
                if (leftNodeLinks.isEmpty()) {
                    callback.planningPathStatus(
                        NaviStatus.NAVI_STATUS_PATH_ERROR_BLOCKED,
                        "该link终点方向，没有拓扑link，请检查数据",
                        leftRoute.linkId,
                        GeometryTools.getLineString(leftRoute.pointList)
                    )
                    bFullPath = false
                    return
                } else {
                    /**
                     * 是否起点和终点已经闭合，规划结束
                     */
                    var bPathOver = false

                    /**
                     * 任务link中，是不是有这段路的下一条link
                     */
                    var bHasNextLink = false
                    for (link in leftNodeLinks) {
                        if (link!!.linkPid == rightRout.linkId) {
                            bPathOver = true
                        } else {
                            //记录其他拓扑关系
                            leftRoute.otherTopologyLinks.add(link.linkPid)
                        }

                        //找出哪条拓扑link是下一条
                        if (!bPathOver && !bHasNextLink) {
                            val iterator = hadLinkDvoListTemp.iterator()
                            while (iterator.hasNext()) {
                                val linkBean = iterator.next()
                                if (!linkBean.isNavi) {
                                    iterator.remove()
                                    continue
                                }
                                if (linkBean.linkPid == link.linkPid) {
                                    bHasNextLink = true
                                    val route = findNaviRouteByLinkId(realm = realm, linkId = linkBean.linkPid, startNodeId = leftRoute.eNode)
                                    if (route == null) {
                                        return
                                    } else {
                                        //插入左侧最后一根
                                        pathList.add(pathImportIndex, route)
                                        pathImportIndex++
                                    }
                                    break
                                }
                            }
                        }
                    }
                    if (!bHasNextLink) {
                        callback.planningPathStatus(
                            code = NaviStatus.NAVI_STATUS_PATH_ERROR_BLOCKED,
                            "路径不通,找不到下一根link",
                            leftRoute.linkId,
                            GeometryTools.getLineString(leftRoute.pointList)
                        )
                        bFullPath = false
                        return
                    }
                    if (bPathOver) {
                        bFullPath = false
                        break
                    }
                }
            }
        }

        //比对路径排序用的
        val tempRouteList = pathList.toMutableList()
        //先找到一根有方向的link，确定起终点
        var routeStart: NaviRoute? = null

        val itemMap: MutableMap<GeoPoint, MutableList<RenderEntity>> = mutableMapOf()
        //查询每根link上的关联要素
        for (i in pathList.indices) {
            val route = pathList[i]
            Log.e("jingo", "获取 插入要素 $i 总共 ${pathList.size}")
            itemMap.clear()
            //常规点限速
            val res = realm.where(RenderEntity::class.java)
                .equalTo("linkPid", route.linkId).and().`in`(
                    "table",
                    QUERY_KEY_ITEM_LIST
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
        routeList = pathList
        callback.planningPathStatus(NaviStatus.NAVI_STATUS_PATH_SUCCESS)

//        val pointList = mutableListOf<GeoPoint>()
//        for (l in pathList) {
//            pointList.addAll(l.pointList)
//        }
//        withContext(Dispatchers.IO) {
//            niMapController.lineHandler.showLine(GeometryTools.getLineString(pointList))
//        }
//        callback.planningPathStatus(NaviStatus.NAVI_STATUS_PATH_SUCCESS)
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
            var bGoToPlay = false
            for (route in tempRoutList) {
//                if (route.itemList != null) {
//                    Log.e("jingo", "${route.linkId}我有${route.itemList!!.size}个要素 ")
//                }
                if (route.indexInPath < routeIndex)
                    continue
                if (route.indexInPath == routeIndex) {
                    currentRoute = route
                    val voice = createRoadInfoVoiceText(route)
                    if (voice != null)
                        callback.voicePlay(voice)
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
                                if (!naviItem.isVoicePlayed && !bGoToPlay) {
                                    naviItem.voiceText = createRenderEntityVoiceText(naviItem.data, naviItem.distance)
                                    if (naviItem.voiceText.isNotEmpty() && callback.voicePlay(naviItem.voiceText)) {
                                        naviItem.isVoicePlayed = true
                                        bGoToPlay = true
                                    }
                                }
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
    private suspend fun deviationUp() {
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

    /**
     * 道路属性语音
     */
    private fun createRoadInfoVoiceText(route: NaviRoute): String? {
        if (route.name != null && route.name!!.properties["name"] != currentRoadName) {
            currentRoadName = "${route.name!!.properties["name"]}"
            return "进入${currentRoadName},限速${route.speedLimit}"
        }
        return null
    }


    /**
     * 要素语音内容
     */
    private fun createRenderEntityVoiceText(renderEntity: RenderEntity, distance: Int): String {
        val stringBuffer = StringBuffer()
        stringBuffer.append("前方")
        if (distance < 50) {

        } else if (distance < 150) {
            stringBuffer.append("100米")
        } else if (distance < 200) {
            stringBuffer.append("150米")
        } else if (distance < 250) {
            stringBuffer.append("200米")
        } else if (distance < 350) {
            stringBuffer.append("300米")
        } else if (distance < 450) {
            stringBuffer.append("400米")
        } else if (distance < 550) {
            stringBuffer.append("500米")
        } else if (distance < 1500) {
            stringBuffer.append("1公里")
        } else {
            val number = distance % 1000.0
            stringBuffer.append("${"0.1f".format(number)}公里")
        }
        when (renderEntity.code) {
            DataCodeEnum.OMDB_ELECTRONICEYE.code -> {
                val maxSpeed = renderEntity.properties["maxSpeed"]
                stringBuffer.append("有限速${maxSpeed}标牌")
            }
            DataCodeEnum.OMDB_SPEEDLIMIT.code,
            DataCodeEnum.OMDB_SPEEDLIMIT_COND.code,
            DataCodeEnum.OMDB_SPEEDLIMIT_VAR.code -> {
                val maxSpeed = renderEntity.properties["maxSpeed"]
                stringBuffer.append("有限速${maxSpeed}标牌")
            }
            DataCodeEnum.OMDB_WARNINGSIGN.code -> {
                val typeCode = renderEntity.properties["typeCode"]
                stringBuffer.append(typeCode)
            }
            DataCodeEnum.OMDB_TOLLGATE.code -> {
                stringBuffer.append("经过收费站")
            }
            else -> {
                stringBuffer.append("有")
                stringBuffer.append("${DataCodeEnum.findTableNameByCode(renderEntity.code)}")
            }
        }

        return stringBuffer.toString()
    }

}
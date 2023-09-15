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
import io.realm.Realm
import io.realm.RealmQuery
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.oscim.core.GeoPoint

public interface OnNaviEngineCallbackListener {
    fun planningPathSuccess()
    fun planningPathError(errorCode: Int, errorMessage: String)
    fun bindingResults(list: List<NaviRouteItem>)
}


class NaviEngine(val niMapController: NIMapController, val callback: OnNaviEngineCallbackListener) {


    private val QUERY_KEY_LIST = arrayOf(
        DataCodeEnum.OMDB_SPEEDLIMIT.name,
        DataCodeEnum.OMDB_SPEEDLIMIT_COND.name,
        DataCodeEnum.OMDB_SPEEDLIMIT_VAR.name,
        DataCodeEnum.OMDB_TRAFFICLIGHT.name,
        DataCodeEnum.OMDB_RESTRICTION.name,
        DataCodeEnum.OMDB_LANEINFO.name,
        DataCodeEnum.OMDB_TRAFFIC_SIGN.name,
        DataCodeEnum.OMDB_WARNINGSIGN.name,
        DataCodeEnum.OMDB_TOLLGATE.name
    )

    /**
     * 偏离距离 单位：米
     */
    private val DEVIATION_DISTANCE = 150000

    /**
     * 偏离次数上限
     */
    private val DEVIATION_COUNT = 3

    /**
     *  局部匹配时，走过的路段还记录100米
     */
    private val PASSED_ROUTE_DISTANCE = 100

    /**
     * 局部匹配时，没走过的路段还记录1000米
     */
    private val NEXT_ROUTE_DISTANCE = 1000

    /**
     * 最远显示距离 米
     */
    private val FARTHEST_DISPLAY_DISTANCE = 550

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
            field = value
        }

    /**
     *  计算路径
     */
    suspend fun planningPath(taskBean: TaskBean) {

        val pathList = mutableListOf<NaviRoute>()
        val realm = Realm.getDefaultInstance()
        for (link in taskBean.hadLinkDvoList) {
            //测线不参与导航
            if (link.linkStatus == 3) {
                continue
            }
            val route = NaviRoute(
                linkId = link.linkPid,
            )

            route.pointList = GeometryTools.getGeoPoints(link.geometry)
            //查询每条link的snode，enode
            val res1 = realm.where(RenderEntity::class.java)
                .equalTo("table", DataCodeEnum.OMDB_RD_LINK.name).and()
                .equalTo("properties['linkPid']", link.linkPid).findFirst()
            res1?.let {

                val snodePid = it.properties["snodePid"]
                if (snodePid != null) {
                    route.sNode = snodePid
                }
                val enodePid = it.properties["enodePid"]
                if (enodePid != null) {
                    route.eNode = enodePid
                }
            }
            //查询每条link的方向
            val res2 = realm.where(RenderEntity::class.java)
                .equalTo("table", DataCodeEnum.OMDB_LINK_DIRECT.name).and()
                .equalTo("properties['linkPid']", link.linkPid).findFirst()
            res2?.let {
                val direct = it.properties["direct"]
                if (direct != null) {
                    route.direct = direct.toInt()
                }
            }
            //查询每条link的名称
            val res3 = realm.where(RenderEntity::class.java)
                .equalTo("table", DataCodeEnum.OMDB_LINK_NAME.name).and()
                .equalTo("properties['linkPid']", link.linkPid).findFirst()
            res3?.let {
                route.name = "${it.properties["name"]}"
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
        if (routeStart != null) {
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
                        callback.planningPathError(1, "路径不连通！")
                        realm.close()
                        return
                    }
                }
            }
        }
        val itemMap: MutableMap<GeoPoint, MutableList<RenderEntity>> = mutableMapOf()
        //查询每根link上的关联要素
        for (route in newRouteList) {
            //常规点限速
            var res = realm.where(RenderEntity::class.java)
                .equalTo("properties['linkPid']", route.linkId).and().`in`(
                    "table",
                    QUERY_KEY_LIST
                ).findAll()
            if (res != null) {
                Log.e("jingo", "道路查询预警要素 ${route.linkId} ${res.size}条数据")
                for (r in res) {
                    Log.e("jingo", "道路查询预警要素 ${r.name}")
                    insertItemToRoute(realm, route, r, itemMap)
                }
            }
//            //条件点限速
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_SPEEDLIMIT_COND.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //可变点限速
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_SPEEDLIMIT_VAR.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //交通灯
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_TRAFFICLIGHT.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //普通交限
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_RESTRICTION.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //车信
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_LANEINFO.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //交通标牌
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_TRAFFIC_SIGN.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //警示信息
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_WARNINGSIGN.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
//            //OMDB_TOLLGATE
//            res = realm.where(RenderEntity::class.java)
//                .equalTo("table", DataCodeEnum.OMDB_TOLLGATE.name).and()
//                .equalTo("properties['linkPid']", route.linkId).findAll()
//            if(res != null){
//                for(r in res)
//                    insertItemToRoute(realm, route, r, itemMap)
//            }
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
                    }
                }
                route.itemList!!.sortBy { it.index }
            }
        }
        realm.close()
        routeList = newRouteList
        callback.planningPathSuccess()
        niMapController.lineHandler.showLine(geometry!!.toText())
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
                niMapController.markerHandle.addMarker(point, res.id, res.name)
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
    fun bindingRoute(location: NiLocation?, point: GeoPoint) {
        if (geometry != null) {
            //还没有绑定到路径的时候
            if (routeIndex < 0) {
                val pointPairDistance = GeometryTools.pointToLineDistance(point, geometry)
                //定义垂线
                //定位点到垂足距离不超过30米
                if (pointPairDistance.getMeterDistance() < DEVIATION_DISTANCE) {
                    footIndex = pointPairDistance.footIndex
                    Log.e(
                        "jingo",
                        "当前绑定到了整条路线的第 $footIndex 点 ${pointPairDistance.getMeterDistance()} "
                    )
                    val lastRouteIndex = routeIndex
                    for (i in routeList.indices) {
                        val route = routeList[i]
                        if (route.startIndexInPath <= footIndex && route.endIndexIntPath >= footIndex) {
                            routeIndex = route.indexInPath
                            Log.e(
                                "jingo",
                                "当前绑定到了整条路线id ${route.linkId} "
                            )
                            niMapController.lineHandler.showLine(route.pointList)
                            footPoint = GeoPoint(
                                pointPairDistance.getCoordinate(0).y,
                                pointPairDistance.getCoordinate(0).x
                            )
                            val listPoint = mutableListOf(point, footPoint!!)
                            niMapController.lineHandler.showLine(
                                listPoint
                            )

                            if (lastRouteIndex != routeIndex) {
                                createTempPath()
                            }
                            matchingItem()
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
                if (pointPairDistance.getMeterDistance() < DEVIATION_DISTANCE) {
                    footIndex = pointPairDistance.footIndex + tempRoutList[0].startIndexInPath
                    Log.e("jingo", "局部 当前绑定到了整条路线的第 $footIndex 点")
                    val lastRouteIndex = routeIndex
                    for (i in tempRoutList.indices) {
                        val route = tempRoutList[i]
                        if (route.startIndexInPath <= footIndex && route.endIndexIntPath >= footIndex) {
                            routeIndex = route.indexInPath
                            Log.e(
                                "jingo",
                                "局部 当前绑定到了整条路线id ${route.linkId} "
                            )
                            niMapController.lineHandler.showLine(route.pointList)
                            footPoint = GeoPoint(
                                pointPairDistance.getCoordinate(0).y,
                                pointPairDistance.getCoordinate(0).x
                            )

                            val listPoint = mutableListOf(point, footPoint!!)
                            niMapController.lineHandler.showLine(
                                listPoint
                            )

                            if (lastRouteIndex != routeIndex) {
                                createTempPath()
                            }
                            matchingItem()
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
    private fun matchingItem() {
        if (routeIndex > -1 && tempRoutList.isNotEmpty() && tempGeometry != null) {
            //道路前方一定距离范围内的要素信息
            val bindingItemList = mutableListOf<NaviRouteItem>()
            //临时局部路径的游标对应整条路径的游标
            val tempFootIndex = footIndex + tempRoutList[0].startIndexInPath
            //定位点到要素的路径距离
            var distance = 0.0
            //计算要素路径距离的点集合
            val disPoints = mutableListOf(footPoint!!)
            //下一个要素的起点游标
            var tempIndex = footIndex + 1
            for(route in tempRoutList) {
                if( route.indexInPath < routeIndex)
                    continue
                if (route.itemList != null && route.itemList!!.isNotEmpty()) {
                    for (naviItem in route.itemList!!) {
                        if (naviItem.index > tempFootIndex) {
                            val rightI = naviItem.index - tempRoutList[0].startIndexInPath + 1
                            for (i in tempIndex until rightI) {
                                val geo = tempGeometry!!.coordinates[i]
                                disPoints.add(GeoPoint(geo.y, geo.x))
                            }
                            tempIndex = rightI
                            distance = GeometryTools.getDistance(disPoints)
                            if (distance < FARTHEST_DISPLAY_DISTANCE && distance > -1) {
                                naviItem.distance = distance.toInt()
                                bindingItemList.add(naviItem)
                            } else {
                                break
                            }
                        }
                    }
                    if(distance >= FARTHEST_DISPLAY_DISTANCE){
                        break
                    }
                }
            }
            callback.bindingResults(bindingItemList)
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
            while (j < routeList.size && distance < NEXT_ROUTE_DISTANCE) {
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
        if (errorCount >= DEVIATION_COUNT) {
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
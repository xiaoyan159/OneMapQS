package com.navinfo.omqs.util

import android.util.Log
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.utils.FootAndDistance
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.RoadNameBean
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.ui.activity.map.LaneInfoItem
import com.navinfo.omqs.ui.fragment.signMoreInfo.LaneBoundaryItem
import com.navinfo.omqs.ui.fragment.signMoreInfo.TwoItemAdapter
import com.navinfo.omqs.ui.fragment.signMoreInfo.TwoItemAdapterItem
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.oscim.core.GeoPoint
import java.lang.reflect.Field

class SignUtil {
    companion object {

        /**
         * 获取面板上的文字
         */
        fun getSignIconText(data: RenderEntity): String {
            return when (data.code) {
                //道路功能等级
                DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> {
                    "FC${data.properties["functionClass"]}"
                }
                //道路种别
                DataCodeEnum.OMDB_RD_LINK_KIND.code -> {
                    "${data.properties["kind"]}"
                }
                //道路方向
                DataCodeEnum.OMDB_LINK_DIRECT.code -> {
                    when (data.properties["direct"]) {
                        "0" -> return "不应用"
                        "1" -> return "双"
                        "2" -> return "顺"
                        "3" -> return "逆"
                        else -> ""
                    }
                }
                //常规线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> {
                    "${data.properties["maxSpeed"]}"
                }
                //条件线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT_COND.code -> {
                    "${data.properties["maxSpeed"]}"
                }
                //全封闭
                DataCodeEnum.OMDB_CON_ACCESS.code -> {
                    if (data.properties["conAccess"] === "1") "全封闭" else ""
                }
                //匝道
                DataCodeEnum.OMDB_RAMP.code -> {
                    when (data.properties["formOfWay"]) {
                        "93" -> "普通匝"
                        "98" -> "高入匝"
                        "99" -> "高出匝"
                        "100" -> "高连匝"
                        "102" -> "直入匝"
                        "103" -> "直出匝"
                        "104" -> "出入匝"
                        else -> ""
                    }
                }
                //车道数
                DataCodeEnum.OMDB_LANE_NUM.code -> {
                    "${data.properties["laneNum"]}|${data.properties["laneS2e"]}|${data.properties["laneE2s"]}"
                }
                //常规点限速,条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code, DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> getSpeedLimitMaxText(
                    data
                )
                //上下线分离
                DataCodeEnum.OMDB_MULTI_DIGITIZED.code -> {
                    if (data.properties["multiDigitized"] == "1") "上下线" else " "
                }
                //桥
                DataCodeEnum.OMDB_BRIDGE.code -> {
                    when (data.properties["bridgeType"]) {
                        "1" -> return "固定桥"
                        "2" -> return "可移桥"
                        "3" -> return "跨线桥"
                        else -> ""
                    }
                }
                //主辅路出入口
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code -> "出入口"
                //辅路
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code -> "辅路"
                //SA
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code -> "SA"
                //PA
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code -> "PA"
                DataCodeEnum.OMDB_LINK_FORM1_1.code -> "U-T"
                DataCodeEnum.OMDB_LINK_FORM1_2.code -> "提右"
                DataCodeEnum.OMDB_LINK_FORM1_3.code -> "提左"
                DataCodeEnum.OMDB_LINK_FORM2_1.code -> "IC"
                DataCodeEnum.OMDB_LINK_FORM2_2.code -> "JCT"
                DataCodeEnum.OMDB_LINK_FORM2_3.code -> "跨线地"
                DataCodeEnum.OMDB_LINK_FORM2_4.code -> "私道"
                DataCodeEnum.OMDB_LINK_FORM2_5.code -> "步行街"
                DataCodeEnum.OMDB_LINK_FORM2_6.code -> "公交道"
                DataCodeEnum.OMDB_LINK_FORM2_7.code -> "POI"
                DataCodeEnum.OMDB_LINK_FORM2_8.code -> "区域内"
                DataCodeEnum.OMDB_LINK_FORM2_9.code -> "P出入"
                DataCodeEnum.OMDB_LINK_FORM2_10.code -> "P虚拟"
                DataCodeEnum.OMDB_LINK_FORM2_11.code -> "风景路"
                DataCodeEnum.OMDB_LINK_FORM2_12.code -> "测试路"
                DataCodeEnum.OMDB_LINK_FORM2_13.code -> "驾考路"
                else -> ""

            }
        }

        /**
         * 获取要素名称
         */
        fun getSignNameText(data: RenderEntity): String {
            return when (data.code) {
                //道路功能等级
                DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> "功能等级"
                //道路种别
                DataCodeEnum.OMDB_RD_LINK_KIND.code -> "种别"
                //道路方向
                DataCodeEnum.OMDB_LINK_DIRECT.code -> "方向"
                //常规线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> "线限速"
                //条件线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT_COND.code -> "条件限速"

                DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code, DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code, DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code, DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code -> "道路属性"

                DataCodeEnum.OMDB_LINK_FORM1_1.code, DataCodeEnum.OMDB_LINK_FORM1_2.code, DataCodeEnum.OMDB_LINK_FORM1_3.code, DataCodeEnum.OMDB_LINK_FORM2_1.code, DataCodeEnum.OMDB_LINK_FORM2_2.code, DataCodeEnum.OMDB_LINK_FORM2_3.code, DataCodeEnum.OMDB_LINK_FORM2_4.code, DataCodeEnum.OMDB_LINK_FORM2_5.code, DataCodeEnum.OMDB_LINK_FORM2_6.code, DataCodeEnum.OMDB_LINK_FORM2_7.code, DataCodeEnum.OMDB_LINK_FORM2_8.code, DataCodeEnum.OMDB_LINK_FORM2_9.code, DataCodeEnum.OMDB_LINK_FORM2_10.code, DataCodeEnum.OMDB_LINK_FORM2_11.code, DataCodeEnum.OMDB_LINK_FORM2_12.code, DataCodeEnum.OMDB_LINK_FORM2_13.code -> "道路形态"

                else -> DataCodeEnum.findTableNameByCode(data.code)
            }
        }


        /**
         * 获取更多信息
         */
        fun getMoreInfoAdapter(data: RenderEntity): TwoItemAdapter {
            val adapter = TwoItemAdapter()
            val list = mutableListOf<TwoItemAdapterItem>()
            when (data.code) {
                //可变线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT_VAR.code -> list.addAll(
                    getChangeLimitSpeedInfo(
                        data
                    )
                )
                //常规点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code -> list.addAll(getSpeedLimitMoreInfoText(data))

                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> list.addAll(
                    getConditionLimitMoreInfoText(
                        data
                    )
                )
                //到路线
                DataCodeEnum.OMDB_RD_LINK.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkPid", text = "${data.properties["linkPid"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "起点号码", text = "${data.properties["snodePid"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "终点号码", text = "${data.properties["enodePid"]}"
                        )
                    )
                }
                //种别
                DataCodeEnum.OMDB_RD_LINK_KIND.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkPid", text = "${data.properties["linkPid"]}"
                        )
                    )
                    try {
                        list.add(
                            TwoItemAdapterItem(
                                title = "种别",
                                text = "${getKindType(data.properties["kind"]!!.toInt())}"
                            )
                        )
                    } catch (e: Throwable) {

                    }

                }
                //道路方向
                DataCodeEnum.OMDB_LINK_DIRECT.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkPid", text = "${data.properties["linkPid"]}"
                        )
                    )
                    try {
                        list.add(
                            TwoItemAdapterItem(
                                title = "通行方向",
                                text = "${getRoadDirectionType(data.properties["direct"]!!.toInt())}"
                            )
                        )
                    } catch (e: Throwable) {

                    }
                }
                //普通交限
                DataCodeEnum.OMDB_RESTRICTION.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkIn", text = "${data.properties["linkIn"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkOut", text = "${data.properties["linkOut"]}"
                        )
                    )
                }
                //道路功能等级
                DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "功能等级", text = "等级${data.properties["functionClass"]}"
                        )
                    )
                }
                //常规线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> {
                    list.addAll(getLinkSpeedLimitMoreInfo(data))
                }
                //车道数
                DataCodeEnum.OMDB_LANE_NUM.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "车道总数", text = "${data.properties["laneNum"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "顺方向车道数", text = "${data.properties["laneS2e"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "逆方向车道数", text = "${data.properties["laneE2s"]}"
                        )
                    )
                    var str = when (data.properties["laneClass"]) {
                        "0" -> "未赋值"
                        "1" -> "一条车道"
                        "2" -> "两或三条"
                        "3" -> "四条及以上"
                        "-99" -> "参考PA"
                        else -> ""
                    }

                    list.add(
                        TwoItemAdapterItem(
                            title = "车道数等级", text = str
                        )
                    )
                }
                //路口
                DataCodeEnum.OMDB_INTERSECTION.code -> {
                    val type = when (data.properties["type"]) {
                        "0" -> "简单路口"
                        "1" -> "复合路口"
                        else -> ""
                    }
                    list.add(
                        TwoItemAdapterItem(
                            title = "路口类型", text = type
                        )
                    )
                }
                //道路施工
                DataCodeEnum.OMDB_LINK_CONSTRUCTION.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkPid", text = "${data.properties["linkPid"]}"
                        )
                    )

                    val limitType = when (data.properties["limitType"]) {
                        "4" -> "施工(全封闭)"
                        "13" -> "施工(非全封闭)"
                        else -> ""
                    }
                    list.add(
                        TwoItemAdapterItem(
                            title = "限制类型", text = limitType
                        )
                    )
                    val validPeriod = data.properties["validPeriod"]
                    if (validPeriod != null) {
                        list.add(
                            TwoItemAdapterItem(
                                title = "施工时间",
                                text = "${TimePeriodUtil.getTimePeriod(validPeriod)}"
                            )
                        )
                    }
                }
                //车道施工
                DataCodeEnum.OMDB_LANE_CONSTRUCTION.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "车道号码", text = "${data.properties["laneLinkPid"]}"
                        )
                    )
                    val startTime = data.properties["startTime"]
                    if (startTime != null) {
                        list.add(
                            TwoItemAdapterItem(
                                title = "施工开始时间",
                                text = "${TimePeriodUtil.getTimePeriod(startTime)}"
                            )
                        )
                    }
                    val endTime = data.properties["endTime"]
                    if (endTime != null) {
                        list.add(
                            TwoItemAdapterItem(
                                title = "施工结束时间", text = "${TimePeriodUtil.getTimePeriod(endTime)}"
                            )
                        )
                    }
                }
                //警示信息
                DataCodeEnum.OMDB_WARNINGSIGN.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkPid", text = "${data.properties["linkPid"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "警示信息号码", text = "${data.properties["warningsignId"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "作用方向", text = when (data.properties["direct"]) {
                                "2" -> "顺方向"
                                "3" -> "逆方向"
                                else -> ""
                            }
                        )
                    )

                    list.add(
                        TwoItemAdapterItem(
                            title = "标牌类型",
                            text = "${data.properties["typeCode"]}",
                            code = data.code
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "有效距离", text = "${data.properties["validDis"]}米"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "预告距离", text = "${data.properties["warnDis"]}米"
                        )
                    )
                    val vehicleType = data.properties["warnDis"]
                    if (vehicleType != null) {
                        list.add(
                            TwoItemAdapterItem(
                                title = "车辆类型",
                                text = getElectronicEyeVehicleType(vehicleType.toInt())
                            )
                        )
                    }
                    list.add(
                        TwoItemAdapterItem(
                            title = "时间段", text = "${data.properties["validPeriod"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "文字说明", text = "${data.properties["descript"]}"
                        )
                    )
                }
                DataCodeEnum.OMDB_OBJECT_STOPLOCATION.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "对象号码", text = "${data.properties["objectPid"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "宽度", text = "${data.properties["width"]}mm"
                        )
                    )

                    list.add(
                        TwoItemAdapterItem(
                            title = "颜色", text = when (data.properties["color"]) {
                                "1" -> "白色"
                                "9" -> "其他"
                                else -> ""
                            }
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "类型", text = when (data.properties["locationType"]) {
                                "1" -> "停止线"
                                "2" -> "停车让行线"
                                "3" -> "减速让行线"
                                "4" -> "虚拟停止线"
                                else -> ""
                            }
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "符合高精", text = when (data.properties["compliant"]) {
                                "0" -> "否"
                                "1" -> "是"
                                else -> ""
                            }
                        )
                    )
                }
                //人行横道
                DataCodeEnum.OMDB_CROSS_WALK.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "对象号码", text = "${data.properties["objectPid"]}"
                        )
                    )

                    list.add(
                        TwoItemAdapterItem(
                            title = "颜色", text = when (data.properties["color"]) {
                                "1" -> "白色"
                                "9" -> "其他"
                                else -> ""
                            }
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "符合高精", text = when (data.properties["compliant"]) {
                                "0" -> "否"
                                "1" -> "是"
                                else -> ""
                            }
                        )
                    )
                }
                DataCodeEnum.OMDB_OBJECT_TEXT.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "对象号码", text = "${data.properties["objectPid"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "长度", text = "${data.properties["length"]}mm"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "宽度", text = "${data.properties["width"]}mm"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "颜色", text = when (data.properties["color"]) {
                                "0" -> "未验证"
                                "1" -> "白色"
                                "2" -> "黄色"
                                "3" -> "红色"
                                "4" -> "彩色"
                                "9" -> "其他"
                                else -> ""
                            }
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "文字内容", text = "${data.properties["textString"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "符合高精", text = when (data.properties["compliant"]) {
                                "0" -> "否"
                                "1" -> "是"
                                else -> ""
                            }
                        )
                    )
                }

            }
            adapter.data = list
            return adapter
        }

        /**
         * 获取路口详细信息
         */
        fun getTollgateInfo(renderEntity: RenderEntity): List<LaneBoundaryItem> {
            val list = mutableListOf<LaneBoundaryItem>()
            list.add(
                LaneBoundaryItem("linkPid", "${renderEntity.properties["linkPid"]}", null)
            )
            list.add(
                LaneBoundaryItem("收费站号码", "${renderEntity.properties["tollgatePid"]}", null)
            )
            list.add(
                LaneBoundaryItem(
                    "作用方向", when (renderEntity.properties["direct"]) {
                        "2" -> "顺方向"
                        "3" -> "逆方向"
                        else -> ""
                    }, null
                )
            )

            list.add(
                LaneBoundaryItem(
                    "类型", when (renderEntity.properties["tollType"]) {
                        "0" -> "未调查"
                        "1" -> "领卡"
                        "2" -> "交卡付费"
                        "3" -> "固定收费(次费)"
                        "4" -> "交卡付费后再领卡"
                        "5" -> "交卡付费并代收固定费用"
                        "6" -> "验票(无票收费)值先保留"
                        "7" -> "领卡并代收固定费用"
                        "8" -> "持卡打标识不收费"
                        "9" -> "验票领卡"
                        "10" -> "交卡不收费"
                        "11" -> "无收费站建筑物结构但收费"
                        "12" -> "废弃或非收费通道"
                        else -> ""
                    }, null
                )
            )
            list.add(
                LaneBoundaryItem("地图代码", "${renderEntity.properties["backimageCode"]}", null)
            )
            list.add(
                LaneBoundaryItem("箭头代码", "${renderEntity.properties["arrowCode"]}", null)
            )
            try {
                val linkList = renderEntity.properties["tollinfoList"]
                if (linkList != null && linkList != "" && linkList != "null") {

                    val jsonArray = JSONArray(linkList)
                    for (i in 0 until jsonArray.length()) {
                        val arrayObject: JSONObject = jsonArray[i] as JSONObject
                        val itemList = mutableListOf<TwoItemAdapterItem>()
                        try {
                            itemList.add(
                                TwoItemAdapterItem("通道号码", "${arrayObject.optString("pid")}")
                            )
                            val stringBuffer = StringBuffer()

                            stringBuffer.setLength(0)
                            val passageType = arrayObject.getInt("passageType")
                            for (i in 1 downTo 0) {
                                val bit = (passageType shr i) and 1
                                if (bit == 1) {
                                    when (i) {
                                        0 -> stringBuffer.append("称重车道 ")
                                        1 -> stringBuffer.append("绿色通道 ")
                                    }
                                }
                            }
                            itemList.add(
                                TwoItemAdapterItem("通道类型", stringBuffer.toString())
                            )


                            stringBuffer.setLength(0)
                            val payMethod = arrayObject.getInt("payMethod")
                            for (i in 8 downTo 0) {
                                val bit = (payMethod shr i) and 1
                                if (bit == 1) {
                                    when (i) {
                                        0 -> stringBuffer.append("ETC ")
                                        1 -> stringBuffer.append("现金 ")
                                        2 -> stringBuffer.append("银行卡(借记卡) ")
                                        3 -> stringBuffer.append("信用卡 ")
                                        4 -> stringBuffer.append("IC卡 ")
                                        5 -> stringBuffer.append("预付卡 ")
                                        6 -> stringBuffer.append("微信 ")
                                        7 -> stringBuffer.append("支付宝 ")
                                        8 -> stringBuffer.append("其他APP ")

                                    }
                                }
                            }
                            itemList.add(
                                TwoItemAdapterItem("收费方式", stringBuffer.toString())
                            )

                            stringBuffer.setLength(0)
                            val cardType = arrayObject.getInt("cardType")
                            for (i in 2 downTo 0) {
                                val bit = (cardType shr i) and 1
                                if (bit == 1) {
                                    when (i) {
                                        0 -> stringBuffer.append("ETC ")
                                        1 -> stringBuffer.append("人工 ")
                                        2 -> stringBuffer.append("自助 ")
                                    }
                                }
                            }
                            itemList.add(
                                TwoItemAdapterItem("领卡方式", stringBuffer.toString())
                            )

                            val seqNum = arrayObject.getInt("seqNum")
                            list.add(
                                LaneBoundaryItem(
                                    "车道$seqNum", null, itemList
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("jingo", "领卡方式 报错 ${e.message}")
                        }
                    }

                }

            } catch (e: Exception) {

            }
            return list
        }


        /**
         * 获取路口详细信息
         */
        fun getIntersectionInfo(renderEntity: RenderEntity): List<LaneBoundaryItem> {
            val list = mutableListOf<LaneBoundaryItem>()
            list.add(
                LaneBoundaryItem(
                    "路口号码", "${renderEntity.properties["intersectionPid"]}", null
                )
            )
            val type = when (renderEntity.properties["type"]) {
                "0" -> "简单路口"
                "1" -> "复合路口"
                else -> ""
            }
            list.add(LaneBoundaryItem("路口类型", type, null))
            try {
                val linkList = renderEntity.properties["linkList"]
                if (linkList != null && linkList != "" && linkList != "null") {
                    val jsonArray = JSONArray(linkList)
                    for (i in 0 until jsonArray.length()) {
                        val itemList = mutableListOf<TwoItemAdapterItem>()
                        val arrayObject: JSONObject = jsonArray[i] as JSONObject
                        val direct = when (arrayObject.getInt("direct")) {
                            2 -> "顺方向"
                            3 -> "逆方向"
                            else -> ""
                        }
                        itemList.add(TwoItemAdapterItem("方向", direct))

                        val featureType = when (arrayObject.getInt("featureType")) {
                            1 -> "LINK"
                            2 -> "LINK PA"
                            else -> ""
                        }
                        itemList.add(TwoItemAdapterItem("要素类型", featureType))

                        list.add(
                            LaneBoundaryItem(
                                "车道标线序号${arrayObject.getInt("markSeqNum")}", null, itemList
                            )
                        )
                    }
                }

            } catch (e: Exception) {

            }
            return list
        }

        /**
         * 获取车道边界类型详细信息
         */
        fun getLaneBoundaryTypeInfo(renderEntity: RenderEntity): List<LaneBoundaryItem> {
            val list = mutableListOf<LaneBoundaryItem>()
            list.add(LaneBoundaryItem("车道边界线ID", "${renderEntity.properties["featurePid"]}", null))
            val type = renderEntity.properties["boundaryType"]
            if (type != null) {
                val typeStr = when (type.toInt()) {
                    0 -> "不应用"
                    1 -> "无标线无可区分边界"
                    2 -> "标线"
                    3 -> "路牙"
                    4 -> "护栏"
                    5 -> "墙"
                    6 -> "铺设路面边缘"
                    7 -> "虚拟三角岛"
                    8 -> "障碍物"
                    9 -> "杆状障碍物"
                    else -> ""
                }
                list.add(LaneBoundaryItem("边界类型", typeStr, null))
            }
            try {
                val shapeList = renderEntity.properties["shapeList"]
                if (shapeList != null && shapeList != "" && shapeList != "null") {
                    val itemList = mutableListOf<TwoItemAdapterItem>()
                    val jsonArray = JSONArray(shapeList)
                    for (i in 0 until jsonArray.length()) {
                        val arrayObject: JSONObject = jsonArray[i] as JSONObject
                        var markType = when (arrayObject.getInt("markType")) {
                            0 -> "其他"
                            1 -> "实线"
                            2 -> "虚线"
                            4 -> "Gore(导流区边线)"
                            5 -> "铺设路面边缘(标线)"
                            6 -> "菱形减速标线"
                            7 -> "可变导向标线"
                            8 -> "短粗虚线"
                            else -> ""
                        }
                        itemList.add(TwoItemAdapterItem("车道标线类型", markType))

                        val markColor = when (arrayObject.getInt("markColor")) {
                            0 -> "不应用"
                            1 -> "白色"
                            2 -> "黄色"
                            6 -> "蓝色"
                            7 -> "绿色"
                            9 -> "其他"
                            else -> ""
                        }
                        itemList.add(TwoItemAdapterItem("车道标线颜色", markColor))
                        itemList.add(
                            TwoItemAdapterItem(
                                "车道标线宽度(mm)", "${arrayObject.getInt("markWidth")}"
                            )
                        )

                        val markMaterial = when (arrayObject.getInt("markMaterial")) {
                            0 -> "不应用"
                            1 -> "油漆"
                            2 -> "突起"
                            3 -> "油漆和突起"
                            else -> ""
                        }
                        itemList.add(TwoItemAdapterItem("车道标线材质", markMaterial))
                        itemList.add(
                            TwoItemAdapterItem(
                                "横向偏移(mm)", "${arrayObject.getInt("lateralOffset")}"
                            )
                        )
                        list.add(
                            LaneBoundaryItem(
                                "车道标线序号${arrayObject.getInt("markSeqNum")}", null, itemList
                            )
                        )
                    }
                }
            } catch (e: Exception) {

            }
            return list
        }

        /**
         * 右下角文字
         */
        fun getSignBottomRightText(data: RenderEntity): String {
            return when (data.code) {

                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> getConditionLimitText(data)
                //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code -> data.properties["name"].toString()
                //收费站
                DataCodeEnum.OMDB_TOLLGATE.code -> {
                    val tollinfoList = data.properties["tollinfoList"]
                    try {
                        val jsonArray = JSONArray(tollinfoList)
                        return "${jsonArray.length()}"
                    } catch (e: Exception) {
                        return ""
                    }
                }
                DataCodeEnum.OMDB_TRAFFIC_SIGN.code -> {
                    var color = data.properties["color"]
                    if (color != null) {
                        when (color) {
                            "0" -> {
                                return "颜色：未验证"
                            }
                            "1" -> {
                                return "颜色：白色"
                            }
                            "2" -> {
                                return "颜色：黄色"
                            }
                            "3" -> {
                                return "颜色：红色"
                            }
                            "5" -> {
                                return "颜色：棕色"
                            }
                            "6" -> {
                                return "颜色：蓝色"
                            }
                            "7" -> {
                                return "颜色：绿色"
                            }
                            "8" -> {
                                return "颜色：黑色"
                            }
                            "9" -> {
                                return "颜色：其他"
                            }
                        }

                    }
                    return "颜色：未验证"
                }
                else -> ""
            }
        }

        /**
         * 条件点限速更多信息
         */
        fun getConditionLimitMoreInfoText(renderEntity: RenderEntity): List<TwoItemAdapterItem> {

            val list = mutableListOf<TwoItemAdapterItem>()
            val maxSpeed = renderEntity.properties["maxSpeed"]
            if (maxSpeed != null) {
                list.add(
                    TwoItemAdapterItem(
                        title = "最高限速值(km/h)", text = maxSpeed
                    )
                )
            }
            list.add(
                TwoItemAdapterItem(
                    title = "限速条件", text = getConditionLimitText(renderEntity)
                )
            )
            val carType = renderEntity.properties["vehicleType"]
            list.add(
                TwoItemAdapterItem(
                    title = "车辆类型", text = getElectronicEyeVehicleType(carType!!.toInt())
                )
            )
            val time = renderEntity.properties["validPeriod"]
            if (time?.isNotEmpty() == true) {
                list.add(
                    TwoItemAdapterItem(
                        title = "时间段", text = time
                    )
                )
            }
            return list
        }


        /**
         * 条件点限速文字
         */
        private fun getConditionLimitText(data: RenderEntity): String {
            val stringBuffer = StringBuffer()
            try {
                val dependent = data.properties["speedDependent"]
                dependent?.let {
                    val dependentInt = it.toInt()
                    for (i in 31 downTo 0) {
                        val bit = (dependentInt shr i) and 1
                        if (bit == 1) {
                            when (i) {
                                0 -> stringBuffer.append("学校 ")
                                1 -> stringBuffer.append("雾 ")
                                2 -> stringBuffer.append("雨 ")
                                3 -> stringBuffer.append("结冰 ")
                                4 -> stringBuffer.append("其他天气 ")
                                5 -> stringBuffer.append("减速带 ")
                                6 -> stringBuffer.append("时间 ")
                                7 -> stringBuffer.append("车辆 ")
                                8 -> stringBuffer.append("建议 ")
                                9 -> stringBuffer.append("雪 ")
                                10 -> stringBuffer.append("其他 ")
                            }
                        }
                    }
                }

            } catch (e: Exception) {

            }
            return stringBuffer.toString()
        }

        private fun isBitSet(number: Int, n: Int): Boolean {
            // 创建一个二进制数，只有第 n 个 bit 位是 1，其他 bit 位是 0
            val mask = 1 shl (n - 1)

            // 将原始二进制数与上面创建的二进制数进行位运算
            val result = number and mask

            // 判断运算结果是否为 0
            return result != 0
        }

        /**
         * 获取上下线分离值
         */
        private fun getMultiDigitized(data: RenderEntity): String {

            val multiDigitized = data.properties["multiDigitized"]
            try {
                if (multiDigitized?.toInt() == 1) return "上下线"
            } catch (e: Throwable) {

            }
            return ""
        }

        /**
         * 获取限速值文字
         */
        private fun getSpeedLimitMaxText(data: RenderEntity): String {
            try {
                //限速标志 0 限速开始 1 限速解除
                val maxSpeed = data.properties["maxSpeed"]
                return maxSpeed.toString()
            } catch (e: Exception) {
                Log.e("jingo", "获取限速面板ICON出错1 $e")
            }
            return ""
        }

        /**
         * 获取限速值文字
         */
        fun getSpeedLimitMinText(data: RenderEntity): String {
            try {
                //限速标志 0 限速开始 1 限速解除
                val minSpeed = data.properties["minSpeed"]
                return minSpeed.toString()
            } catch (e: Exception) {
                Log.e("jingo", "获取限速面板ICON出错1 $e")
            }
            return "0"
        }


        /**
         * 常规点限速更多信息
         */
        fun getSpeedLimitMoreInfoText(renderEntity: RenderEntity): List<TwoItemAdapterItem> {

            val list = mutableListOf<TwoItemAdapterItem>()
            list.add(
                TwoItemAdapterItem(
                    title = "最高限速值(km/h)", text = getSpeedLimitMaxText(renderEntity)
                )
            )
            list.add(
                TwoItemAdapterItem(
                    title = "最低限速值(km/h)", text = getSpeedLimitMinText(renderEntity)
                )
            )
            val direct = renderEntity.properties["direct"]
            var str = ""
            if (direct == "2") {
                str = "顺方向"
            } else if (direct == "3") {
                str = "逆方向"
            }
            if (str != "") {
                list.add(TwoItemAdapterItem(title = "限速方向", text = str))
            }
            val speedFlag = renderEntity.properties["speedFlag"]
            var flag = ""
            if (speedFlag == "0") {
                flag = "限速开始"
            } else if (speedFlag == "1") {
                flag = "限速解除"
            }
            if (flag != "") {
                list.add(TwoItemAdapterItem(title = "限速标志", text = flag))
            }
            return list
        }


        /**
         * 获取看板图标
         */

        fun getSignIcon(data: RenderEntity): Int {
            return when (data.code) {
//                //道路种别
//                2008 -> getKindCodeIcon(data)
//                //道路方向
//                2010 -> getRoadDirection(data)
//                //车道数
//                2041 -> getLaneNumIcon(data)
                //普通点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code -> {
                    //限速标志 0 限速开始 1 限速解除
                    when (data.properties["speedFlag"]) {
                        "1" -> return R.drawable.icon_speed_limit_off
                        else -> return R.drawable.icon_speed_limit
                    }
                }
                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> {
                    //限速标志 0 限速开始 1 限速解除
                    when (data.properties["speedFlag"]) {
                        "1" -> return R.drawable.icon_conditional_speed_limit_off
                        else -> return R.drawable.icon_conditional_speed_limit
                    }
                }
                //可变点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_VAR.code -> R.drawable.icon_change_limit
                //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code -> R.drawable.icon_electronic_eye
                //交通灯
                DataCodeEnum.OMDB_TRAFFICLIGHT.code -> R.drawable.icon_traffic_light
                //警示信息
                DataCodeEnum.OMDB_WARNINGSIGN.code -> {
                    val typeCode = data.properties["typeCode"]
                    if (typeCode != null) return typeCode.toInt()
                    return 0
                }
                //收费站
                DataCodeEnum.OMDB_TOLLGATE.code -> {
                    var backimageCode = data.properties["backimageCode"]
                    if (backimageCode != null) {
                        backimageCode = backimageCode.lowercase()
                        return getResId(backimageCode, R.drawable::class.java)
                    }
                    return 0
                }
                DataCodeEnum.OMDB_TRAFFIC_SIGN.code -> {
                    var trafsignShape = data.properties["trafsignShape"]
                    if (trafsignShape != null) {
                        trafsignShape =
                            "icon_${DataCodeEnum.OMDB_TRAFFIC_SIGN.code}_${trafsignShape.lowercase()}"
                        return getResId(trafsignShape, R.drawable::class.java)
                    }
                    return 0
                }
                else -> 0
            }

        }

        /**
         * 获取种别图标
         */
        private fun getKindCodeIcon(data: RenderEntity): Int {
            try {
                val kind = data.properties["kind"]
                return when (kind!!.toInt()) {
                    1 -> R.drawable.icon_kind_code_k1
                    2 -> R.drawable.icon_kind_code_k2
                    3 -> R.drawable.icon_kind_code_k3
                    4 -> R.drawable.icon_kind_code_k4
                    6 -> R.drawable.icon_kind_code_k6
                    7 -> R.drawable.icon_kind_code_k7
                    8 -> R.drawable.icon_kind_code_k8
                    9 -> R.drawable.icon_kind_code_k9
                    10 -> R.drawable.icon_kind_code_k10
                    11 -> R.drawable.icon_kind_code_k11
                    13 -> R.drawable.icon_kind_code_k13
                    15 -> R.drawable.icon_kind_code_k15
                    else -> R.drawable.icon_kind_code
                }
            } catch (e: Exception) {
                Log.e("jingo", "获取种别面板ICON出错 $e")
            }
            return R.drawable.icon_kind_code
        }

        /**
         * 获取到路线
         */
        private fun getLaneNumIcon(data: RenderEntity): Int {
            try {
                val lineNum = data.properties["laneNum"]
                return when (lineNum!!.toInt()) {
                    1 -> R.drawable.icon_lane_num1
                    2 -> R.drawable.icon_lane_num2
                    3 -> R.drawable.icon_lane_num3
                    4 -> R.drawable.icon_lane_num4
                    5 -> R.drawable.icon_lane_num5
                    6 -> R.drawable.icon_lane_num6
                    7 -> R.drawable.icon_lane_num7
                    8 -> R.drawable.icon_lane_num8
                    9 -> R.drawable.icon_lane_num9
                    10 -> R.drawable.icon_lane_num10
                    11 -> R.drawable.icon_lane_num11
                    12 -> R.drawable.icon_lane_num12
                    else -> R.drawable.icon_lane_num1
                }
            } catch (e: Exception) {
                Log.e("jingo", "获取车道数面板ICON出错 $e")
            }
            return R.drawable.icon_road_direction
        }

        /**
         * 道路方向
         */
        fun getRoadDirectionType(type: Int): String {
            return when (type) {
                0 -> "不应用"
                1 -> "双方向"
                2 -> "顺方向"
                3 -> "逆方向"
                -99 -> "参考PA"
                else -> "未定义"
            }
        }

//        private fun getRoadDirection(data: RenderEntity): Int {
//            try {
//                val direct = data.properties["direct"]
//                return when (direct!!.toInt()) {
//                    0 -> R.drawable.icon_road_direction
//                    1 -> R.drawable.icon_road_direction
//                    2 -> R.drawable.icon_road_direction
//                    3 -> R.drawable.icon_road_direction
//                    -99 -> R.drawable.icon_road_direction
//                    else -> R.drawable.icon_road_direction
//                }
//            } catch (e: Exception) {
//                Log.e("jingo", "获取道路方向面板ICON出错 $e")
//            }
//            return R.drawable.icon_road_direction
//        }

        /**
         * 获取道路播报语音文字
         */
        fun getRoadSpeechText(topSignList: MutableList<SignBean>): String {
            if (topSignList.size == 0) return ""
            val stringBuffer = StringBuffer()
            stringBuffer.append("当前道路")
            for (item in topSignList) {
                when (item.renderEntity.code) {
                    DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> stringBuffer.append(
                        "功能等级${
                            item.iconText.substring(
                                2
                            )
                        }级,"
                    )
                    DataCodeEnum.OMDB_RD_LINK_KIND.code -> stringBuffer.append("种别${item.iconText},")
                    DataCodeEnum.OMDB_LINK_DIRECT.code -> stringBuffer.append("${item.iconText},")
                    DataCodeEnum.OMDB_LANE_NUM.code -> stringBuffer.append(
                        "${
                            item.iconText.substringBefore(
                                "|"
                            )
                        }车道"
                    )
                }
            }
            return stringBuffer.toString()
        }

        /**
         * 获取道路名列表
         */
        fun getRoadNameList(data: RenderEntity): MutableList<RoadNameBean> {
            val list = mutableListOf<RoadNameBean>()
            try {
                val shapeStr = data.properties["shapeList"]
                val array = JSONArray(shapeStr)
                for (i in 0 until array.length()) {
                    val jsonObject = array.getJSONObject(i)
                    val name = jsonObject.optString("name", "")
                    val type = jsonObject.optInt("nameType", 0)
                    val seqNum = jsonObject.optInt("seqNum", 1)
                    val nameClass = jsonObject.optInt("nameClass", 1)
                    val bean = RoadNameBean(
                        name = name, type = type, seqNum = seqNum, nameClass = nameClass
                    )
                    list.add(bean)
                }
                /**
                 * 排序
                 */
                list.sortWith { n1, n2 ->
                    if (n1.nameClass != n2.nameClass) {
                        n1.nameClass.compareTo(n2.nameClass)
                    } else {
                        n1.seqNum.compareTo(n2.seqNum)
                    }
                }
            } catch (e: Exception) {

            }
            return list
        }

        /**
         * 是否要有详细信息需要展示
         */
        fun isMoreInfo(element: RenderEntity): Boolean {
            val isMore = when (element.code) {
                //常规点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code -> getSpeedLimitMinText(element) != "0"
                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code,
                    //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code,
                    //收费站
                DataCodeEnum.OMDB_TOLLGATE.code,
                    //警示信息
                DataCodeEnum.OMDB_WARNINGSIGN.code -> true
                else -> false
            }
            return isMore
        }

        /**
         * 可变点限速详细信息
         */
        fun getChangeLimitSpeedInfo(renderEntity: RenderEntity): List<TwoItemAdapterItem> {
            val list = mutableListOf<TwoItemAdapterItem>()
            val kindCode = renderEntity.properties["location"]
            if (kindCode != null) {
                when (kindCode.toInt()) {
                    1 -> list.add(
                        TwoItemAdapterItem(
                            title = "标牌位置", text = "左"
                        )
                    )
                    2 -> list.add(
                        TwoItemAdapterItem(
                            title = "标牌位置", text = "右"
                        )
                    )
                    3 -> list.add(
                        TwoItemAdapterItem(
                            title = "标牌位置", text = "上"
                        )
                    )
                }
            }
            return list

        }

        /**
         * 获取电子眼详细信息
         */
        fun getElectronicEyeMoreInfo(renderEntity: RenderEntity): List<TwoItemAdapterItem> {
            val list = mutableListOf<TwoItemAdapterItem>()


            val dir = when (renderEntity.properties["direct"]) {
                "2" -> "顺方向"
                "3" -> "逆方向"
                else -> ""
            }
            if (dir != "") {
                list.add(
                    TwoItemAdapterItem(
                        title = "作用方向", text = dir
                    )
                )
            }

//            val kindUp = when (renderEntity.properties["kindUp"]) {
//                "0" -> "未调查"
//                "1" -> "限速电子眼"
//                "4" -> "区间测速电子眼"
//                "5" -> "交通信号灯电子眼"
//                "6" -> "专用车道电子眼"
//                "7" -> "违章电子眼"
//                "11" -> "路况监控电子眼"
//                "19" -> "交通标线电子眼"
//                "20" -> "专用功能电子眼"
//                else -> ""
//            }

//            list.add(TwoItemAdapterItem(title = "电子眼类型大分类", text = kindUp))

            val kindCode = renderEntity.properties["kind"]!!.toInt()
            list.add(
                TwoItemAdapterItem(
                    title = "电子眼类型", text = getElectronicEyeKindType(kindCode)
                )
            )
            when (kindCode) {
                1, 2, 3, 4, 5, 6, 20, 21 -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "限速值(km/h)",
                            text = renderEntity.properties["speedLimit"].toString()
                        )
                    )
                }
            }
            val carType = renderEntity.properties["vehicleType"]
            if (carType != null && carType != "0") {
                list.add(
                    TwoItemAdapterItem(
                        title = "车辆类型", text = getElectronicEyeVehicleType(carType.toInt())
                    )
                )
            }
            val time = renderEntity.properties["validPeriod"]
            if (time?.isNotEmpty() == true) {
                list.add(
                    TwoItemAdapterItem(
                        title = "时间段", text = time
                    )
                )
            }
            if (kindCode == 20 || kindCode == 21) {
                list.add(
                    TwoItemAdapterItem(
                        title = "区间测试配对", text = renderEntity.properties["pairEleceyeId"].toString()
                    )
                )
            }
            list.add(
                TwoItemAdapterItem(
                    title = "照射角度", text = "${renderEntity.properties["angle"]}"
                )
            )
            return list
        }

        /**
         *  获取电子眼车辆类型
         */
        private fun getElectronicEyeVehicleType(type: Int): String {
            var stringBuffer = StringBuffer()
            for (i in 31 downTo 0) {
                val bit = (type shr i) and 1
                if (bit == 1) {
                    when (i) {
                        0 -> stringBuffer.append("其他 ")
                        1 -> stringBuffer.append("小汽车 ")
                        2 -> stringBuffer.append("公交车 ")
                        3 -> stringBuffer.append("多人乘坐车辆 ")
                        4 -> stringBuffer.append("配送车 ")
                        5 -> stringBuffer.append("摩托车 ")
                        6 -> stringBuffer.append("行人 ")
                        7 -> stringBuffer.append("自行车 ")
                        8 -> stringBuffer.append("出租车 ")
                        10 -> stringBuffer.append("紧急车辆 ")
                        11 -> stringBuffer.append("运输卡车 ")
                    }
                }
            }

            return stringBuffer.toString()
        }

        fun getKindType(kind: Int): String {
            return when (kind) {
                1 -> "高速道路"
                2 -> "城市道路"
                3 -> "国道"
                4 -> "省道"
                6 -> "县道"
                7 -> "乡镇村道路"
                8 -> "其他道路"
                9 -> "非引导道路"
                10 -> "步行道路"
                11 -> "人渡"
                13 -> "轮渡"
                15 -> "自行车道路"
                -99 -> "参考PA"
                else -> "未定义"
            }

        }

        /**
         * 获取电子眼类型
         */
        private fun getElectronicEyeKindType(kind: Int): String {
            return when (kind) {
                0 -> "未调查"
                1 -> "超高速"
                2 -> "超低速"
                3 -> "移动式测速"
                4 -> "可变限速"
                5 -> "分车道限速"
                6 -> "分车种限速"
                7 -> "违规用灯"
                8 -> "违规占车道"
                9 -> "违规过路口"
                10 -> "机动车闯红灯"
                11 -> "路况监控"
                12 -> "单行线"
                13 -> "占用非机动车道"
                14 -> "出入口"
                15 -> "占用公交车专用道"
                16 -> "禁止左右转"
                17 -> "禁止掉头"
                18 -> "占用应急车道"
                19 -> "违反禁止标线"
                20 -> "区间测速开始"
                21 -> "区间测速结束"
                22 -> "违章停车"
                23 -> "尾号限行"
                24 -> "环保限行"
                25 -> "不系安全带"
                26 -> "开车打手机"
                27 -> "礼让行人"
                28 -> "违反禁令标志"
                29 -> "禁止鸣笛"
                30 -> "车辆未按规定年检"
                31 -> "车辆尾气超标"
                32 -> "ETC拍照计费电子眼"
                33 -> "专用车道电子眼预留"
                34 -> "交通标线电子眼预留"
                35 -> "违章电子眼预留"
                36 -> "卡车超限电子眼"
                37 -> "限时长停车电子眼"
                else -> "无效类型"
            }
        }

        /**
         * 获取车信图标
         */
        fun getLineInfoIcons(renderEntity: RenderEntity): List<LaneInfoItem> {
            val list = mutableListOf<LaneInfoItem>()
            try {
                var laneinfoGroup = renderEntity.properties["laneinfoGroup"]
                if (laneinfoGroup != null) {
                    laneinfoGroup = laneinfoGroup.substring(1, laneinfoGroup.length - 1)
                    laneinfoGroup = "[$laneinfoGroup]"
                }
                val jsonArray = JSONArray(laneinfoGroup)
                if (jsonArray.length() == 2) {
                    val itemArray = jsonArray[0]
                    val typeArray = jsonArray[1]
                    if ((itemArray is JSONArray) && (typeArray is JSONArray) && itemArray.length() == typeArray.length()) {
                        for (i in 0 until itemArray.length()) {
                            val itemObject = itemArray[i]
                            val type = typeArray[i]
                            var laneInfo = "laneinfo_${itemObject.toString().replace(",", "_")}"
                            list.add(
                                LaneInfoItem(
                                    id = getResId(
                                        laneInfo, R.drawable::class.java
                                    ), type = type!!.toString().toInt()
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("jingo", "json 解析失败")
            }
            return list
        }

        /**
         * 通过字符串名称获取资源id
         */
        private fun getResId(variableName: String, c: Class<*>): Int {
            return try {
                val idField: Field = c.getDeclaredField(variableName)
                idField.getInt(idField)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                R.drawable.laneinfo_0
            }
        }

        /**
         * 道路信息排序用的
         */
        fun getRoadInfoIndex(element: RenderEntity): Int {
            return when (element.code) {
                DataCodeEnum.OMDB_LANE_NUM.code -> 0
                DataCodeEnum.OMDB_RD_LINK_KIND.code -> 1
                DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> 2
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> 3
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT_COND.code -> 4
                DataCodeEnum.OMDB_LINK_DIRECT.code -> 5
                else -> 999
            }
        }

        //常规线限速详细信息
        private fun getLinkSpeedLimitMoreInfo(data: RenderEntity): List<TwoItemAdapterItem> {
            val list = mutableListOf<TwoItemAdapterItem>()
            val dir = when (data.properties["direction"]) {
                "2" -> "顺方向"
                "3" -> "逆方向"
                else -> ""
            }
            if (dir != "") {
                list.add(
                    TwoItemAdapterItem(
                        title = "限速方向", text = dir
                    )
                )
            }
            list.add(
                TwoItemAdapterItem(
                    title = "最高限速值(km/h)", text = "${data.properties["maxSpeed"]}"
                )
            )
            var maxStr = when (data.properties["maxSpeedSource"]) {
                "0" -> {
                    "不应用"
                }
                "1" -> {
                    "现场"
                }
                "2" -> {
                    "理论"
                }
                else -> ""
            }
            if (maxStr != "") {
                list.add(
                    TwoItemAdapterItem(
                        title = "最高限速来源", text = maxStr
                    )
                )
            }
            list.add(
                TwoItemAdapterItem(
                    title = "最低限速值(km/h)", text = "${data.properties["minSpeed"]}"
                )
            )
            var minStr = when (data.properties["minSpeedSource"]) {
                "0" -> {
                    "不应用"
                }
                "1" -> {
                    "现场"
                }
                "2" -> {
                    "理论"
                }
                else -> ""
            }
            if (minStr != "") {
                list.add(
                    TwoItemAdapterItem(
                        title = "最低限速来源", text = minStr
                    )
                )
            }
            var isLaneDependent = when (data.properties["isLaneDependent"]) {
                "0" -> {
                    "否"
                }
                "1" -> {
                    "是"
                }
                else -> ""
            }
            if (isLaneDependent != "") {
                list.add(
                    TwoItemAdapterItem(
                        title = "是否车道依赖", text = isLaneDependent
                    )
                )
            }
            return list
        }

        /**
         * 计算捕捉点到
         */
        fun getDistance(
            footAndDistance: FootAndDistance,
            lineString: Geometry,
            element: RenderEntity
        ): Int {

            val itemGeometry = GeometryTools.createGeometry(element.geometry)
            if (itemGeometry is Point) {
                val itemFoot = GeometryTools.pointToLineDistance(
                    GeoPoint(itemGeometry.y, itemGeometry.x),
                    lineString
                )
                var dis = GeometryTools.getDistance(
                    footAndDistance.getCoordinate(0).getY(),
                    footAndDistance.getCoordinate(0).getX(),
                    itemFoot.getCoordinate(0).getY(),
                    itemFoot.getCoordinate(0).getX(),
                )
                return if (footAndDistance.footIndex < itemFoot.footIndex) {
                    dis.toInt()
                } else {
                    -dis.toInt()
                }
            } else if (itemGeometry is LineString) {
                val itemFoot = GeometryTools.pointToLineDistance(
                    GeoPoint(
                        lineString.coordinates[lineString.coordinates.size - 1].y,
                        lineString.coordinates[lineString.coordinates.size - 1].x
                    ), lineString
                )
                var dis = GeometryTools.getDistance(
                    footAndDistance.getCoordinate(0).getY(),
                    footAndDistance.getCoordinate(0).getX(),
                    itemFoot.getCoordinate(0).getY(),
                    itemFoot.getCoordinate(0).getX(),
                )
                return if (footAndDistance.footIndex < itemFoot.footIndex) {
                    dis.toInt()
                } else {
                    -dis.toInt()
                }
            }
            return 0
        }
    }
}
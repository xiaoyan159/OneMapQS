package com.navinfo.omqs.ui.widget

import android.util.Log
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.RoadNameBean
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.ui.activity.map.LaneInfoItem
import com.navinfo.omqs.ui.fragment.signMoreInfo.LaneBoundaryItem
import com.navinfo.omqs.ui.fragment.signMoreInfo.TwoItemAdapter
import com.navinfo.omqs.ui.fragment.signMoreInfo.TwoItemAdapterItem
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field

class SignUtil {
    companion object {

        /**
         * 获取面板上的文字
         */
        fun getSignIconText(data: RenderEntity): String {
            return when (data.code) {
                //道路功能等级
                DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> getLinkFunctionClassText(data)
                //道路种别
                DataCodeEnum.OMDB_RD_LINK_KIND.code -> getKindText(data)
                //道路方向
                DataCodeEnum.OMDB_LINK_DIRECT.code -> getRoadDirectionText(data)
                //常规线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> getLineSpeedLimitText(data)
                //全封闭
                DataCodeEnum.OMDB_CON_ACCESS.code -> getConAccessText(data)
                //匝道
                DataCodeEnum.OMDB_RAMP.code -> getRampText(data)
                //车道数
                DataCodeEnum.OMDB_LANE_NUM.code -> getLaneNumText(data)
                //常规点限速,条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code, DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> getSpeedLimitMaxText(
                    data
                )
                //上下线分离
                DataCodeEnum.OMDB_MULTI_DIGITIZED.code -> getMultiDigitized(data)
                //桥
                DataCodeEnum.OMDB_BRIDGE.code -> getBridgeType(data)
                //隧道
                DataCodeEnum.OMDB_TUNNEL.code -> "隧道"
                //环岛
                DataCodeEnum.OMDB_ROUNDABOUT.code -> "环岛"
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
                DataCodeEnum.OMDB_VIADUCT.code -> "高架"
                else -> ""
            }
        }

        /**
         * 获取桥的类型值
         */
        private fun getBridgeType(data: RenderEntity): String {
            val bridgeType = data.properties["bridgeType"]
            try {
                when (bridgeType?.toInt()) {
                    1 -> return "固定桥"
                    2 -> return "可移桥"
                    3 -> return "跨线桥"
                }
            } catch (e: Throwable) {

            }
            return ""
        }

        /**
         * 常规线限速值
         */
        private fun getLineSpeedLimitText(data: RenderEntity): String {
            return "${data.properties["maxSpeed"]}"
        }

        /**
         * 获取全封闭值
         */
        private fun getConAccessText(data: RenderEntity): String {
            val conAccess = data.properties["conAccess"]
            try {
                if (conAccess?.toInt() == 1)
                    return "全封闭"
            } catch (e: Throwable) {

            }
            return ""
        }

        /**
         * 获取匝道值
         */
        private fun getRampText(data: RenderEntity): String {
            try {
                val ramp = data.properties["formOfWay"]
                return when (ramp?.toInt()) {
                    93 -> "普通匝"
                    98 -> "高入匝"
                    99 -> "高出匝"
                    100 -> "高连匝"
                    102 -> "直入匝"
                    103 -> "直出匝"
                    104 -> "出入匝"

                    else -> {
                        ""
                    }
                }
            } catch (e: Throwable) {

            }
            return ""
        }

        /**
         *获取道路功能等级文字
         */
        private fun getLinkFunctionClassText(data: RenderEntity): String {
            return "FC${data.properties["functionClass"]}"
        }

        /**
         * 获取道路方向文字
         */
        private fun getRoadDirectionText(data: RenderEntity): String {
            val direct = data.properties["direct"]
            when (direct?.toInt()) {
                0 -> return "不应用"
                1 -> return "双"
                2 -> return "顺"
                3 -> return "逆"
            }
            return ""
        }

        /**
         * 获取车道数展示文字
         */
        private fun getLaneNumText(data: RenderEntity): String {
            return "${data.properties["laneNum"]}|${data.properties["laneS2e"]}|${data.properties["laneE2s"]}"
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
                //车道边界类型
                DataCodeEnum.OMDB_LANE_MARK_BOUNDARYTYPE.code -> "车道边界类型"
                //常规线限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> "线限速"
                //全封闭
                DataCodeEnum.OMDB_CON_ACCESS.code -> "全封闭" //暂时不要标题
                //匝道
                DataCodeEnum.OMDB_RAMP.code -> "匝道"
                //车道数
                DataCodeEnum.OMDB_LANE_NUM.code -> "车道数"
                //常规点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code -> "常规点限速"
                //常点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> "条件点限速"
                //可变点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_VAR.code -> "可变点限速"
                //普通交限
                DataCodeEnum.OMDB_RESTRICTION.code -> "普通交限"
                //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code -> "电子眼"
                //交通灯
                DataCodeEnum.OMDB_TRAFFICLIGHT.code -> "交通灯"
                //车信
                DataCodeEnum.OMDB_LANEINFO.code -> "车信"
                //上下线分离
                DataCodeEnum.OMDB_MULTI_DIGITIZED.code -> "上下线分离"
                //桥
                DataCodeEnum.OMDB_BRIDGE.code -> "桥"
                //隧道
                DataCodeEnum.OMDB_TUNNEL.code -> "隧道"
                //环岛
                DataCodeEnum.OMDB_ROUNDABOUT.code -> "环岛"

                DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code,
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code,
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code,
                DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code -> "道路属性"

                DataCodeEnum.OMDB_LINK_FORM1_1.code,
                DataCodeEnum.OMDB_LINK_FORM1_2.code,
                DataCodeEnum.OMDB_LINK_FORM1_3.code,
                DataCodeEnum.OMDB_LINK_FORM2_1.code,
                DataCodeEnum.OMDB_LINK_FORM2_2.code,
                DataCodeEnum.OMDB_LINK_FORM2_3.code,
                DataCodeEnum.OMDB_LINK_FORM2_4.code,
                DataCodeEnum.OMDB_LINK_FORM2_5.code,
                DataCodeEnum.OMDB_LINK_FORM2_6.code,
                DataCodeEnum.OMDB_LINK_FORM2_7.code,
                DataCodeEnum.OMDB_LINK_FORM2_8.code,
                DataCodeEnum.OMDB_LINK_FORM2_9.code,
                DataCodeEnum.OMDB_LINK_FORM2_10.code,
                DataCodeEnum.OMDB_LINK_FORM2_11.code,
                DataCodeEnum.OMDB_LINK_FORM2_12.code,
                DataCodeEnum.OMDB_LINK_FORM2_13.code -> "道路形态"

                DataCodeEnum.OMDB_VIADUCT.code -> "高架"

                else -> ""
            }
        }

        /**
         * 获取路口详细信息
         */

        fun getIntersectionInfo(renderEntity: RenderEntity): List<LaneBoundaryItem> {
            val list = mutableListOf<LaneBoundaryItem>()
            list.add(
                LaneBoundaryItem(
                    "路口号码",
                    "${renderEntity.properties["intersectionPid"]}",
                    null
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
                    val itemList = mutableListOf<TwoItemAdapterItem>()
                    val jsonArray = JSONArray(linkList)
                    for (i in 0 until jsonArray.length()) {
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
                                "车道标线序号${arrayObject.getInt("markSeqNum")}",
                                null,
                                itemList
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
                                "车道标线宽度(mm)",
                                "${arrayObject.getInt("markWidth")}"
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
                                "横向偏移(mm)",
                                "${arrayObject.getInt("lateralOffset")}"
                            )
                        )
                        list.add(
                            LaneBoundaryItem(
                                "车道标线序号${arrayObject.getInt("markSeqNum")}",
                                null,
                                itemList
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
            if (carType != "0") {
                list.add(
                    TwoItemAdapterItem(
                        title = "车辆类型", text = getElectronicEyeVehicleType(carType!!.toInt())
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
            return list
        }


        /**
         * 条件点限速文字
         */
        private fun getConditionLimitText(data: RenderEntity): String {
            var stringBuffer = StringBuffer()
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
                if (multiDigitized?.toInt() == 1)
                    return "上下线"
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
         * 获取种别名称
         */
        private fun getKindText(data: RenderEntity): String {
            return data.properties["kind"].toString()
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
         * 限速图标
         */
        private fun getSpeedLimitIcon(data: RenderEntity): Int {
            try {
                //限速标志 0 限速开始 1 限速解除
                return when (data.properties["speedFlag"]) {
                    "1" -> return R.drawable.icon_speed_limit_off
                    else -> return R.drawable.icon_speed_limit
                }
            } catch (e: Exception) {
                Log.e("jingo", "获取限速面板ICON出错2 $e")
            }
            return 0
        }

        /**
         * 条件限速图标
         */
        private fun getConditionalSpeedLimitIcon(data: RenderEntity): Int {
            try {
                //限速标志 0 限速开始 1 限速解除
                return when (data.properties["speedFlag"]) {
                    "1" -> return R.drawable.icon_conditional_speed_limit_off
                    else -> return R.drawable.icon_conditional_speed_limit
                }
            } catch (e: Exception) {
                Log.e("jingo", "获取限速面板ICON出错2 $e")
            }
            return 0
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
                DataCodeEnum.OMDB_SPEEDLIMIT.code -> getSpeedLimitIcon(data)
                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> getConditionalSpeedLimitIcon(data)
                //可变点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_VAR.code -> R.drawable.icon_change_limit
                //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code -> R.drawable.icon_electronic_eye
                //交通灯
                DataCodeEnum.OMDB_TRAFFICLIGHT.code -> R.drawable.icon_traffic_light
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
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> true
                //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code -> true
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
                        title = "作用方向",
                        text = dir
                    )
                )
            }

            val kindUp = when (renderEntity.properties["kindUp"]) {
                "0" -> "未调查"
                "1" -> "限速电子眼"
                "4" -> "区间测速电子眼"
                "5" -> "交通信号灯电子眼"
                "6" -> "专用车道电子眼"
                "7" -> "违章电子眼"
                "11" -> "路况监控电子眼"
                "19" -> "交通标线电子眼"
                "20" -> "专用功能电子眼"
                else -> ""
            }

            list.add(TwoItemAdapterItem(title = "电子眼类型大分类", text = kindUp))

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
            list.add( TwoItemAdapterItem(
                title = "照射角度", text = "${renderEntity.properties["angle"]}"
            ))
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
                DataCodeEnum.OMDB_LINK_DIRECT.code -> 4
                else -> 999
            }
        }

        /**
         * 获取更多信息
         */
        fun getMoreInfoAdapter(data: RenderEntity): TwoItemAdapter {
            val adapter = TwoItemAdapter()
            val list = mutableListOf<TwoItemAdapterItem>()
            when (data.code) {
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT_VAR.code ->
                    list.addAll(getChangeLimitSpeedInfo(data))
                //常规点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code ->
                    list.addAll(getSpeedLimitMoreInfoText(data))

                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code ->
                    list.addAll(getConditionLimitMoreInfoText(data))
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
                DataCodeEnum.OMDB_RESTRICTION.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkIn",
                            text = "${data.properties["linkIn"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "linkOut",
                            text = "${data.properties["linkOut"]}"
                        )
                    )
                }
                DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "功能等级",
                            text = "等级${data.properties["functionClass"]}"
                        )
                    )
                }
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code -> {

                    val dir = when (data.properties["direction"]) {
                        "2" -> "顺方向"
                        "3" -> "逆方向"
                        else -> ""
                    }
                    if (dir != "") {
                        list.add(
                            TwoItemAdapterItem(
                                title = "限速方向",
                                text = dir
                            )
                        )
                    }
                    list.add(
                        TwoItemAdapterItem(
                            title = "最高限速值(km/h)",
                            text = "${data.properties["maxSpeed"]}"
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
                                title = "最高限速来源",
                                text = maxStr
                            )
                        )
                    }
                    list.add(
                        TwoItemAdapterItem(
                            title = "最低限速值(km/h)",
                            text = "${data.properties["minSpeed"]}"
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
                                title = "最低限速来源",
                                text = minStr
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
                                title = "是否车道依赖",
                                text = isLaneDependent
                            )
                        )
                    }
                }
                DataCodeEnum.OMDB_LANE_NUM.code -> {
                    list.add(
                        TwoItemAdapterItem(
                            title = "车道总数",
                            text = "${data.properties["laneNum"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "顺方向车道数",
                            text = "${data.properties["laneS2e"]}"
                        )
                    )
                    list.add(
                        TwoItemAdapterItem(
                            title = "逆方向车道数",
                            text = "${data.properties["laneE2s"]}"
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
                            title = "车道数等级",
                            text = str
                        )
                    )
                }
                DataCodeEnum.OMDB_INTERSECTION.code -> {
                    val type = when (data.properties["type"]) {
                        "0" -> "简单路口"
                        "1" -> "复合路口"
                        else -> ""
                    }
                    list.add(
                        TwoItemAdapterItem(
                            title = "路口类型",
                            text = type
                        )
                    )
                }
            }
            adapter.data = list
            return adapter
        }
    }

}
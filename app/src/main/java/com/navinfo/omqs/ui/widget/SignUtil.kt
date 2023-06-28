package com.navinfo.omqs.ui.widget

import android.util.Log
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.RoadNameBean
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.ui.activity.map.LaneInfoItem
import com.navinfo.omqs.ui.fragment.signMoreInfo.ElectronicEyeMoreInfoAdapterItem
import org.json.JSONArray
import java.lang.reflect.Field

class SignUtil {
    companion object {

        /**
         * 获取面板上的文字
         */
        fun getSignIconText(data: RenderEntity): String {
            return when (data.code) {
                //道路功能等级
                2002 -> getLinkFunctionClassText(data)
                //道路种别
                2008 -> getKindText(data)
                //道路方向
                2010 -> getRoadDirectionText(data)
                //车道数
                2041 -> getLaneNumText(data)
                //常规点限速,条件点限速
                4002, 4003 -> getSpeedLimitMaxText(data)
                else -> ""
            }
        }

        /**
         *获取道路功能等级文字
         */
        private fun getLinkFunctionClassText(data: RenderEntity): String {
            return "等级${data.properties["functionClass"]}"
        }

        /**
         * 获取道路方向文字
         */
        private fun getRoadDirectionText(data: RenderEntity): String {
            val direct = data.properties["direct"]
            when (direct?.toInt()) {
                0 -> return "不应用"
                1 -> return "双方向"
                2 -> return "顺方向"
                3 -> return "逆方向"
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
                2002 -> "功能等级"
                //道路种别
                2008 -> "种别"
                //道路方向
                2010 -> "方向"
                //车道数
                2041 -> "车道数"
                //常规点限速
                4002 -> "常规点限速"
                //常点限速
                4003 -> "条件点限速"
                //可变点限速
                4004 -> "可变点限速"
                //普通交限
                4006 -> "普通交限"
                //电子眼
                4010 -> "电子眼"
                //交通灯
                4022 -> "交通灯"
                //交限
                4601 -> "车信"
                else -> ""
            }
        }

        /**
         * 右下角文字
         */
        fun getSignBottomRightText(data: RenderEntity): String {
            return when (data.code) {
                //条件点限速
                4003 -> getConditionLimitText(data)
                else -> ""
            }
        }

        /**
         * 条件点限速更多信息
         */
        fun getConditionLimitMoreInfoText(renderEntity: RenderEntity): List<ElectronicEyeMoreInfoAdapterItem> {

            val list = mutableListOf<ElectronicEyeMoreInfoAdapterItem>()
            val maxSpeed = renderEntity.properties["maxSpeed"]
            if (maxSpeed != null) {
                list.add(
                    ElectronicEyeMoreInfoAdapterItem(
                        title = "最高限速值(km/h)", text = maxSpeed
                    )
                )
            }
            list.add(
                ElectronicEyeMoreInfoAdapterItem(
                    title = "限速条件", text = getConditionLimitText(renderEntity)
                )
            )
            val carType = renderEntity.properties["vehicleType"]
            if (carType != "0") {
                list.add(
                    ElectronicEyeMoreInfoAdapterItem(
                        title = "车辆类型", text = getElectronicEyeVehicleType(carType!!.toInt())
                    )
                )
            }
            val time = renderEntity.properties["validPeriod"]
            if (time?.isNotEmpty() == true) {
                list.add(
                    ElectronicEyeMoreInfoAdapterItem(
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
        fun getSpeedLimitMoreInfoText(renderEntity: RenderEntity): List<ElectronicEyeMoreInfoAdapterItem> {

            val list = mutableListOf<ElectronicEyeMoreInfoAdapterItem>()
            list.add(
                ElectronicEyeMoreInfoAdapterItem(
                    title = "最高限速值(km/h)", text = getSpeedLimitMaxText(renderEntity)
                )
            )
            list.add(
                ElectronicEyeMoreInfoAdapterItem(
                    title = "最低限速值(km/h)", text = getSpeedLimitMinText(renderEntity)
                )
            )
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
                4002 -> getSpeedLimitIcon(data)
                //条件点限速
                4003 -> getConditionalSpeedLimitIcon(data)
                //可变点限速
                4004 -> R.drawable.icon_change_limit
                //电子眼
                4010 -> R.drawable.icon_electronic_eye
                //交通灯
                4022 -> R.drawable.icon_traffic_light
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

        private fun getRoadDirection(data: RenderEntity): Int {
            try {
                val direct = data.properties["direct"]
                return when (direct!!.toInt()) {
                    0 -> R.drawable.icon_road_direction
                    1 -> R.drawable.icon_road_direction
                    2 -> R.drawable.icon_road_direction
                    3 -> R.drawable.icon_road_direction
                    -99 -> R.drawable.icon_road_direction
                    else -> R.drawable.icon_road_direction
                }
            } catch (e: Exception) {
                Log.e("jingo", "获取道路方向面板ICON出错 $e")
            }
            return R.drawable.icon_road_direction
        }

        /**
         * 获取道路播报语音文字
         */
        fun getRoadSpeechText(topSignList: MutableList<SignBean>): String {
            if (topSignList.size == 0) return ""
            val stringBuffer = StringBuffer()
            stringBuffer.append("当前道路")
            for (item in topSignList) {
                when (item.renderEntity.code) {
                    2002 -> stringBuffer.append("功能等级${item.iconText.substring(2)}级,")
                    2008 -> stringBuffer.append("种别${item.iconText},")
                    2010 -> stringBuffer.append("${item.iconText},")
                    2041 -> stringBuffer.append("${item.iconText.substringBefore("|")}车道")
                }
            }
            return stringBuffer.toString()
        }


        fun getRoadNameList(data: RenderEntity): MutableList<RoadNameBean> {
            val list = mutableListOf<RoadNameBean>()
            if (data.code == 2011) {
                try {
                    val shapeStr = data.properties["shapeList"]
                    val array = JSONArray(shapeStr)
                    for (i in 0 until array.length()) {
                        val jsonObject = array.getJSONObject(0)
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
            }
            return list
        }

        /**
         * 是否要有详细信息需要展示
         */
        fun isMoreInfo(element: RenderEntity): Boolean {
            val isMore = when (element.code) {
                //常规点限速
                4002 -> getSpeedLimitMinText(element) != "0"
                //条件点限速
                4003 -> true
                //电子眼
                4010 -> true
                else -> false
            }
            Log.e("jingo", "更多信息：${element.code} $isMore")
            return isMore
        }


        /**
         * 获取电子眼详细信息
         */
        fun getElectronicEyeMoreInfo(renderEntity: RenderEntity): List<ElectronicEyeMoreInfoAdapterItem> {
            val list = mutableListOf<ElectronicEyeMoreInfoAdapterItem>()
            val kindCode = renderEntity.properties["kind"]!!.toInt()
            val kind = ElectronicEyeMoreInfoAdapterItem(
                title = "电子眼类型", text = getElectronicEyeKindType(kindCode)
            )
            list.add(kind)
            when (kindCode) {
                1, 2, 3, 4, 5, 6, 20, 21 -> {
                    list.add(
                        ElectronicEyeMoreInfoAdapterItem(
                            title = "限速值(km/h)",
                            text = renderEntity.properties["speedLimit"].toString()
                        )
                    )
                }
            }
            val carType = renderEntity.properties["vehicleType"]
            if (carType != null && carType != "0") {
                list.add(
                    ElectronicEyeMoreInfoAdapterItem(
                        title = "车辆类型",
                        text = getElectronicEyeVehicleType(carType.toInt())
                    )
                )
            }
            val time = renderEntity.properties["validPeriod"]
            if (time?.isNotEmpty() == true) {
                list.add(
                    ElectronicEyeMoreInfoAdapterItem(
                        title = "时间段", text = time
                    )
                )
            }
            if (kindCode == 20 || kindCode == 21) {
                list.add(
                    ElectronicEyeMoreInfoAdapterItem(
                        title = "区间测试配对", text = renderEntity.properties["pairEleceyeId"].toString()
                    )
                )
            }
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
                            Log.e("jingo", "车信图标 $laneInfo")
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
    }
}
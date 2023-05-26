package com.navinfo.omqs.ui.widget

import android.util.Log
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.omqs.R

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
                4002, 4003 -> getSpeedLimitText(data)
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
                else -> ""
            }
        }

        /**
         * 右下角文字
         */
        fun getSignBottomRightText(data: RenderEntity): String {
            return when (data.code) {
                //常点限速
                4003 -> getConditionLimitText(data)
                else -> ""
            }
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
        private fun getSpeedLimitText(data: RenderEntity): String {
            try {
                //限速标志 0 限速开始 1 限速解除
                val maxSpeed = data.properties["maxSpeed"]
                val minSpeed = data.properties["minSpeed"]
                return if (maxSpeed != "0")
                    maxSpeed.toString()
                else
                    minSpeed.toString()
            } catch (e: Exception) {
                Log.e("jingo", "获取限速面板ICON出错1 $e")
            }
            return ""
        }

        /**
         * 获取种别名称
         */
        private fun getKindText(data: RenderEntity): String {
            return data.properties["kind"].toString()
        }

        /**
         * 限速图标
         */
        private fun getSpeedLimitIcon(data: RenderEntity): Int {
            try {
                //限速标志 0 限速开始 1 限速解除
                return when (data.properties["speed_flag"]) {
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
                return when (data.properties["speed_flag"]) {
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
    }
}
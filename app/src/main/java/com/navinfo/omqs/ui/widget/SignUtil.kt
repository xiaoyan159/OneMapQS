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
                //常规点限速
                4002 -> getSpeedLimitText(data)
//                //道路种别
//                2008 -> getKindCodeIcon(data)
//                //道路方向
//                2010 -> getRoadDirection(data)
//                //车道数
//                2041 -> getLaneNumIcon(data)
                else -> ""
            }
        }

        fun getSignBottomText(data: RenderEntity): String {
            return when (data.code) {
                //常规点限速
                4002 -> "常规点限速"
                //道路种别
                2008 -> "道路种别"
                //道路方向
                2010 -> "道路方向"
                //车道数
                2041 -> "车道数"
                else -> ""
            }
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
         * 限速图标
         */
        fun getSpeedLimitIcon(data: RenderEntity): Int {
            try {
                //限速标志 0 限速开始 1 限速解除
                return when (data.properties["speed_flag"]) {
                    "1" -> return R.drawable.shape_icon_speed_limit_off
                    else -> return R.drawable.shape_icon_speed_limit
                }
            } catch (e: Exception) {
                Log.e("jingo", "获取限速面板ICON出错2 $e")
            }
            return R.drawable.shape_icon_speed_limit
        }

        /**
         * 获取看板图标
         */

        fun getSignIcon(data: RenderEntity): Int {
            return when (data.code) {
                //道路种别
                2008 -> getKindCodeIcon(data)
                //道路方向
                2010 -> getRoadDirection(data)
                //车道数
                2041 -> getLaneNumIcon(data)
                //限速
                4002 -> getSpeedLimitIcon(data)
                else -> R.drawable.shape_icon_speed_limit
            }

        }


        /**
         * 获取种别图标
         */
        fun getKindCodeIcon(data: RenderEntity): Int {
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
        fun getLaneNumIcon(data: RenderEntity): Int {
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

        fun getRoadDirection(data: RenderEntity): Int {
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
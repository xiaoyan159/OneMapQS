package com.navinfo.omqs

import androidx.core.util.rangeTo
import io.realm.Realm
import java.util.*

class Constant {

    companion object {
        /**
         * sd卡根目录
         */
        lateinit var ROOT_PATH: String

        /**
         * 地图目录
         */
        lateinit var MAP_PATH: String

        /**
         * 数据目录
         */
        lateinit var DATA_PATH: String

        /**
         * 用户id
         */
        lateinit var USER_ID: String

        //数据版本
        lateinit var VERSION_ID: String

        /**
         * 用户数据目录
         */
        lateinit var USER_DATA_PATH: String

        /**
         * 离线地图目录
         */
        lateinit var OFFLINE_MAP_PATH: String

        /**
         * 服务器地址
         */
        const val SERVER_ADDRESS = "http://fastmap.navinfo.com/"

        const val DEBUG = true

        const val message_status_late = "预约，待发送"
        const val message_status_send_over = "已发送"
        const val message_version_right_off = "1" //立即发送

        const val MESSAGE_PAGE_SIZE = 30 //消息列表一页最多数量

        lateinit var realm: Realm

        //选择相机默认或者外设
        const val SELECT_CAMERA_STATE = "select_camera_state"

        //是否连接
        const val CAMERA_CONNECT_STATE = "camera_connect_state"

        //是否可以点击
        const val CAMERA_CLICK_STATE = "camera_click_state"

        //拍照模式
        const val TAKE_CAMERA_MODE = "take_camera_mode"

        const val TAKE_CAMERA_IP = "take_camera_ip"

        const val TAKE_CAMERA_MAC = "take_camera_mac"

        //选择拍照或者录像
        const val SELECT_TAKEPHOTO_OR_RECORD = "select_takephoto_or_record"

    }

}
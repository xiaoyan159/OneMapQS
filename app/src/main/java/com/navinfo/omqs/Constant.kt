package com.navinfo.omqs

import com.navinfo.omqs.bean.ImportConfig

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
         * 当前用户ID
         */
        lateinit var USER_ID: String

        /**
         * 当前用户名称
         */
        lateinit var USER_REAL_NAME: String

        //数据版本
        lateinit var VERSION_ID: String

        /**
         * 用户数据目录
         */
        lateinit var USER_DATA_PATH: String

        /**
         * 用户附件数据目录
         */
        lateinit var USER_DATA_ATTACHEMNT_PATH: String

        /**
         * 离线地图目录
         */
        lateinit var OFFLINE_MAP_PATH: String

        /**
         * 下载目录
         */
        lateinit var DOWNLOAD_PATH: String

        /**
         * 图层管理对应的配置
         * */
        var LAYER_CONFIG_LIST: List<ImportConfig>? = null

        /**
         * 室内整理工具IP
         */
        var INDOOR_IP: String = ""

        const val DEBUG = true

        /**
         * 地图最多缩放级别23
         */
        const val MAX_ZOOM = 23

        /**
         * 是否自动定位
         */
        var AUTO_LOCATION = false

        var IS_VIDEO_SPEED by kotlin.properties.Delegates.notNull<Boolean>()

        const val message_status_late = "预约，待发送"
        const val message_status_send_over = "已发送"
        const val message_version_right_off = "1" //立即发送

        const val MESSAGE_PAGE_SIZE = 30 //消息列表一页最多数量

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

        const val OMDB_CONFIG = "omdb_config.json"
        const val OTHER_CONFIG = "other_config.json"

        val OMDB_LAYER_VISIBLE_LIST: MutableList<String> = mutableListOf() // 记录OMDB数据显示的图层名称列表

        const val EVENT_LAYER_MANAGER_CHANGE = "EVENT_LAYER_MANAGER_CHANGE" // 图层管理中的配置修改

        const val SELECT_TASK_ID = "select_task_id" //选中的任务ID

        const val SHARED_SYNC_TASK_LINK_ID = "shared_sync_task_link_id"//利用shared通知任务页面更新
    }


}
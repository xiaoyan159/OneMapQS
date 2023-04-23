package com.navinfo.omqs

import io.realm.Realm

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
    }

}
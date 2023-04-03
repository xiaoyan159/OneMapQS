package com.navinfo.omqs

class Constant {
    companion object {
        /**
         * sd卡根目录
         */
        lateinit var ROOT_PATH: String
        lateinit var MAP_PATH: String
        lateinit var OFFLINE_MAP_PATH: String
        /**
         * 服务器地址
         */
        const val SERVER_ADDRESS = "http://fastmap.navinfo.com/drdc/"

        const val DEBUG = true

        const val message_status_late = "预约，待发送"
        const val message_status_send_over = "已发送"
        const val message_version_right_off = "1" //立即发送

        const val MESSAGE_PAGE_SIZE = 30 //消息列表一页最多数量

    }

}
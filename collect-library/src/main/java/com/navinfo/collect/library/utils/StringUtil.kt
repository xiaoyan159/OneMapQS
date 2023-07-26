package com.navinfo.collect.library.utils

import java.text.SimpleDateFormat
import java.util.*

class StringUtil {
    companion object {
        /**
         * 创建uuid
         */
        fun createUUID(): String? {
            return UUID.randomUUID().toString().replace("-".toRegex(), "")
        }

        fun getYYYYMMDDHHMMSSS(): String {
            val currentTime = Date()
            val formatter =
                SimpleDateFormat("yyyyMMddHHmmsss")
            return  formatter.format(currentTime)
        }
    }
}
package com.navinfo.omqs.bean

data class OfflineMapCityBean(
    val id: String,
    val fileName: String,
    val name: String,
    val url: String,
    val version: Long,
    val fileSize: Long,
    var currentSize:Long = 0,
    var status:Int = NONE
) {
    companion object Status{
        const val NONE = 0 //无状态
        const val WAITING = 1 //等待中
        const val LOADING = 2 //下载中
        const val PAUSE = 3 //暂停
        const val ERROR = 4 //错误
        const val DONE = 5 //完成
        const val UPDATE = 6 //有新版本要更新
    }
    fun getFileSizeText(): String {
        return if (fileSize < 1024.0)
            "$fileSize B"
        else if (fileSize < 1048576.0)
            "%.2f K".format(fileSize / 1024.0)
        else if (fileSize < 1073741824.0)
            "%.2f M".format(fileSize / 1048576.0)
        else
            "%.2f M".format(fileSize / 1073741824.0)
    }
}
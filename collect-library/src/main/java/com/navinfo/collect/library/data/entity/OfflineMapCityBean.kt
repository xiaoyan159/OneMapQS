package com.navinfo.collect.library.data.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

//enum class StatusEnum(val status: Int) {
//    NONE(0), WAITING(1), LOADING(2), PAUSE(3),
//    ERROR(4), DONE(5), UPDATE(6)
//}

open class OfflineMapCityBean @JvmOverloads constructor(
    @PrimaryKey var id: String = "",
    var fileName: String = "",
    var name: String = "",
    var url: String = "",
    var version: Long = 0L,
    var fileSize: Long = 0L,
    var currentSize: Long = 0L,
    var status: Int = NONE
) : RealmObject() {

    companion object Status{
        const val NONE = 0 //无状态
        const val WAITING = 1 //等待中
        const val LOADING = 2 //下载中
        const val PAUSE = 3 //暂停
        const val ERROR = 4 //错误
        const val DONE = 5 //完成
        const val UPDATE = 6 //有新版本要更新
    }

//    // status的转换对象
//    var statusEnum: StatusEnum
//        get() {
//            return try {
//                StatusEnum.values().find { it.status == status }!!
//            } catch (e: IllegalArgumentException) {
//                StatusEnum.NONE
//            }
//        }
//        set(value) {
//            status = value.status
//        }

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
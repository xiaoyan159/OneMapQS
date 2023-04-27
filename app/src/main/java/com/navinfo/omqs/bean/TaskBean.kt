package com.navinfo.omqs.bean

import com.google.gson.annotations.SerializedName
import com.navinfo.omqs.Constant
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

open class TaskBean @JvmOverloads constructor(
    /**
     * 测评任务id
     */
    @PrimaryKey
    var id: Int = 0,
    /**
     * 测评任务名称
     */
    var evaluationTaskName: String = "",
    /**
     * 市编码
     */
    var cityCode: String = "",
    /**
     *市名称
     */
    var cityName: String = "",
    /**
     * omdb标准版
     */
    var dataVersion: String = "",
    /**
     * 测评人名称
     */
    var evaluatorName: String = "",
    /**
     * 项目标签
     */
    var project: String = "",
    /**
     * 图幅号
     */
    @SerializedName("hadLinkDvo")
    var hadLinkDvoList: RealmList<HadLinkDvoBean> = RealmList<HadLinkDvoBean>(),
    /**
     * 文件大小
     */
    var fileSize: Long = 0L,
    /**
     * 当前下载进度
     */
    var currentSize: Long = 0L,
    /**
     * 当前下载状态
     */
    var status: Int = FileDownloadStatus.NONE,

    /**
     * 上传状态
     */
    var syncStatus: Int = FileManager.Companion.FileUploadStatus.NONE,

    @Ignore
    var message: String = ""
) : RealmObject() {
    fun getDownLoadUrl(): String {
        return "${Constant.SERVER_ADDRESS}devcp/download?fileStr=26"
    }
}
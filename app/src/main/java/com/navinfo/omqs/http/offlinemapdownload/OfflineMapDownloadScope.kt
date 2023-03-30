package com.navinfo.omqs.http.offlinemapdownload

import androidx.lifecycle.MutableLiveData
import com.navinfo.omqs.bean.OfflineMapCityBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 代表一个下载任务
 * [OfflineMapCityBean.id]将做为下载任务的唯一标识
 * 不要直接在外部直接创建此对象,那样就可能无法统一管理下载任务,请通过[OfflineMapDownloadManager.download]获取此对象
 * 这是一个协程作用域，
 * EmptyCoroutineContext 表示一个不包含任何元素的协程上下文，它通常用于创建新的协程上下文，或者作为协程上下文的基础。
 */
class OfflineMapDownloadScope(cityBean: OfflineMapCityBean) : CoroutineScope by CoroutineScope(EmptyCoroutineContext) {
    /**
     *
     */
    private var downloadJob: Job? = null

    /**
     *
     */
    private val downloadData = MutableLiveData<OfflineMapCityBean>()
}
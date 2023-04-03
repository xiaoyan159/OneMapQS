package com.navinfo.omqs.http.offlinemapdownload

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * 管理离线地图下载
 */
class OfflineMapDownloadManager @Inject constructor(
    private val netApi: RetrofitNetworkServiceAPI
) {
    /**
     * 最多同时下载数量
     */
    private val MAX_SCOPE = 3

    /**
     * 存储有哪些城市需要下载的队列
     */
    private val scopeMap: ConcurrentHashMap<String, OfflineMapDownloadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<String, OfflineMapDownloadScope>()
    }

    /**
     * 存储正在下载的城市队列
     */
    private val taskScopeMap: ConcurrentHashMap<String, OfflineMapDownloadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<String, OfflineMapDownloadScope>()
    }


    /**
     * 启动下载任务
     * 请不要直接使用此方法启动下载任务,它是交由[OfflineMapDownloadScope]进行调用
     */
    fun launchScope(scope: OfflineMapDownloadScope) {
        if (taskScopeMap.size >= MAX_SCOPE) {
            return
        }
        if (taskScopeMap.contains(scope.cityBean.id)) {
            return
        }
        taskScopeMap[scope.cityBean.id] = scope
        scope.launch()
    }

    /**
     * 启动下一个任务,如果有正在等待中的任务的话
     * 请不要直接使用此方法启动下载任务,它是交由[OfflineMapDownloadScope]进行调用
     * @param previousUrl 上一个下载任务的下载连接
     */
    fun launchNext(previousUrl: String) {
        taskScopeMap.remove(previousUrl)
        for (entrySet in scopeMap) {
            val downloadScope = entrySet.value
            if (downloadScope.isWaiting()) {
                launchScope(downloadScope)
                break
            }
        }
    }

    /**
     * 暂停任务
     * 只有等待中的任务和正在下载中的任务才可以进行暂停操作
     */
    fun pause(id: String) {
        if (taskScopeMap.containsKey(id)) {
            val downloadScope = taskScopeMap[id]
            downloadScope?.let {
                downloadScope.pause()
            }
            launchNext(id)
        }

    }

    /**
     * 将下载任务加入到协程作用域的下载队列里
     * 请求一个下载任务[OfflineMapDownloadScope]
     * 这是创建[OfflineMapDownloadScope]的唯一途径,请不要通过其他方式创建[OfflineMapDownloadScope]
     */
    fun start(id: String) {
        scopeMap[id]?.start()
    }

    fun cancel(id: String) {
        taskScopeMap.remove(id)
        scopeMap[id]?.cancelTask()
    }

    fun addTask(cityBean: OfflineMapCityBean) {
        if (scopeMap.containsKey(cityBean.id)) {
            return
        } else {
            scopeMap[cityBean.id] = OfflineMapDownloadScope(this, netApi, cityBean)
        }
    }


    fun observer(
        id: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<OfflineMapCityBean>
    ) {
        if (scopeMap.containsKey(id)) {
            val downloadScope = scopeMap[id]
            downloadScope?.let {
                downloadScope.observer(lifecycleOwner, observer)
            }
        }
    }



}
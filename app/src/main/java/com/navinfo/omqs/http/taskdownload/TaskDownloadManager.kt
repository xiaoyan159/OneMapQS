package com.navinfo.omqs.http.taskdownload

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.hilt.ImportOMDBHiltFactory
import com.navinfo.omqs.hilt.OMDBDataBaseHiltFactory
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import dagger.hilt.android.qualifiers.ActivityContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


/**
 * 管理任务数据下载
 */

class TaskDownloadManager constructor(
    val importFactory: ImportOMDBHiltFactory,
    val netApi: RetrofitNetworkServiceAPI,
    val mapController:NIMapController
) {

    lateinit var context: Context

    /**
     * 最多同时下载数量
     */
    private val MAX_SCOPE = 1

    /**
     * 存储有哪些城市需要下载的队列
     */
    private val scopeMap: ConcurrentHashMap<Int, TaskDownloadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<Int, TaskDownloadScope>()
    }

    /**
     * 存储正在下载的城市队列
     */
    private val taskScopeMap: ConcurrentHashMap<Int, TaskDownloadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<Int, TaskDownloadScope>()
    }

    fun init(context: Context) {
        this.context = context
    }

    /**
     * 启动下载任务
     * 请不要直接使用此方法启动下载任务,它是交由[OfflineMapDownloadScope]进行调用
     */
    fun launchScope(scope: TaskDownloadScope) {
        if (taskScopeMap.size >= MAX_SCOPE) {
            return
        }
        if (taskScopeMap.contains(scope.taskBean.id)) {
            return
        }
        taskScopeMap[scope.taskBean.id] = scope
        scope.launch()
    }

    /**
     * 启动下一个任务,如果有正在等待中的任务的话
     * 请不要直接使用此方法启动下载任务,它是交由[OfflineMapDownloadScope]进行调用
     * @param previousUrl 上一个下载任务的下载连接
     */
    fun launchNext(id: Int) {
        taskScopeMap.remove(id)
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
    fun pause(id: Int) {
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
    fun start(id: Int) {
        scopeMap[id]?.start()
    }


    fun addTask(taskBean: TaskBean) {
        if (!scopeMap.containsKey(taskBean.id)) {
            scopeMap[taskBean.id] = TaskDownloadScope( this, taskBean)
        }
    }


    fun observer(
        id: Int, lifecycleOwner: LifecycleOwner, observer: Observer<TaskBean>
    ) {
        if (scopeMap.containsKey(id)) {
            scopeMap[id]!!.observer(lifecycleOwner, observer)
        }
    }

    fun removeObserver(id: Int) {
        if (scopeMap.containsKey(id)) {
            scopeMap[id]!!.removeObserver()
        }
    }
}
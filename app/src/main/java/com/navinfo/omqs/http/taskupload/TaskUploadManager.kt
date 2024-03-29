package com.navinfo.omqs.http.taskupload

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理任务数据上传
 */

class TaskUploadManager constructor(
    val netApi: RetrofitNetworkServiceAPI,
    val realmOperateHelper: RealmOperateHelper,
) {

    lateinit var context: Context

    /**
     * 最多同时下载数量
     */
    private val MAX_SCOPE = 1

    /**
     * 存储有哪些城市需要下载的队列
     */
    private val scopeMap: ConcurrentHashMap<Int, TaskUploadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<Int, TaskUploadScope>()
    }

    /**
     * 存储正在下载的城市队列
     */
    private val taskScopeMap: ConcurrentHashMap<Int, TaskUploadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<Int, TaskUploadScope>()
    }

    fun init(context: Context) {
        this.context = context
    }

    /**
     * 启动下载任务
     * 请不要直接使用此方法启动下载任务,它是交由[OfflineMapDownloadScope]进行调用
     */
    fun launchScope(scope: TaskUploadScope) {
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
            val uploadScope = entrySet.value
            if (uploadScope.isWaiting()) {
                launchScope(uploadScope)
                break
            }
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
            scopeMap[taskBean.id] = TaskUploadScope(this, realmOperateHelper, taskBean)
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
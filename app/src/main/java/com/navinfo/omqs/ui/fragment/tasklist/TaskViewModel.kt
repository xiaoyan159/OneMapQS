package com.navinfo.omqs.ui.fragment.tasklist

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.Constant
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.*
import javax.inject.Inject


@HiltViewModel
class TaskViewModel @Inject constructor(
    private val networkService: NetworkService, private val mapController: NIMapController
) : ViewModel() {

    /**
     * 用来更新任务列表
     */
    val liveDataTaskList = MutableLiveData<List<TaskBean>>()

    /**
     * 用来更新当前任务
     */
    val liveDataTaskLinks = MutableLiveData<List<HadLinkDvoBean>>()

    /**
     * 用来更新数据是否可以上传
     */
    val liveDataTaskUpload = MutableLiveData<Map<TaskBean, Boolean>>()

    /**
     * 用来确定是否关闭
     */
    val liveDataCloseTask = MutableLiveData<Boolean>()

    private val colors =
        arrayOf(Color.RED, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN)

    /**
     * 当前选中的任务
     */
    private var currentSelectTaskBean: TaskBean? = null

    /**
     * 任务列表查询协程
     */
    private var filterTaskListJob: Job? = null

    private var filterTaskJob: Job? = null

    /**
     * 下载任务列表
     */
    fun getTaskList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            var taskList: List<TaskBean> = mutableListOf()
            when (val result = networkService.getTaskList(Constant.USER_ID)) {
                is NetResult.Success -> {
                    if (result.data != null) {
                        val realm = Realm.getDefaultInstance()
                        realm.executeTransaction {
                            result.data.obj?.let { list ->
                                for (index in list.indices) {
                                    val task = list[index]
                                    val item = realm.where(TaskBean::class.java).equalTo(
                                        "id", task.id
                                    ).findFirst()
                                    if (item != null) {
                                        task.fileSize = item.fileSize
                                        task.status = item.status
                                        task.currentSize = item.currentSize
                                        //已上传后不在更新操作时间
                                        if (task.syncStatus != FileManager.Companion.FileUploadStatus.DONE) {
                                            //赋值时间，用于查询过滤
                                            task.operationTime = DateTimeUtil.getNowDate().time
                                        }
                                    } else {
                                        //赋值时间，用于查询过滤
                                        task.operationTime = DateTimeUtil.getNowDate().time
                                    }
                                    realm.copyToRealmOrUpdate(task)
                                }
                            }

                        }
                    }
                }

                is NetResult.Error<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                is NetResult.Failure<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                is NetResult.Loading -> {}
            }
            val realm = Realm.getDefaultInstance()
            //过滤掉已上传的超过90天的数据
            var nowTime: Long = DateTimeUtil.getNowDate().time
            var beginNowTime: Long = nowTime - 90 * 3600 * 24 * 1000L
            var syncUpload: Int = FileManager.Companion.FileUploadStatus.DONE
            val objects =
                realm.where(TaskBean::class.java).notEqualTo("syncStatus", syncUpload).or()
                    .between("operationTime", beginNowTime, nowTime)
                    .equalTo("syncStatus", syncUpload).findAll()
            taskList = realm.copyFromRealm(objects)
            for (item in taskList) {
                FileManager.checkOMDBFileInfo(item)
            }
            liveDataTaskList.postValue(taskList)
        }
    }

    /**
     * 设置当前选择的任务，并高亮当前任务的所有link
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setSelectTaskBean(taskBean: TaskBean) {
        currentSelectTaskBean = taskBean

        liveDataTaskLinks.value = taskBean.hadLinkDvoList

        mapController.lineHandler.omdbTaskLinkLayer.removeAll()
        if (taskBean.hadLinkDvoList.isNotEmpty()) {
            mapController.lineHandler.omdbTaskLinkLayer.addLineList(taskBean.hadLinkDvoList)
            var maxX = 0.0
            var maxY = 0.0
            var minX = 0.0
            var minY = 0.0
            for (item in taskBean.hadLinkDvoList) {
                val geometry = GeometryTools.createGeometry(item.geometry)
                if (geometry != null) {
                    val envelope = geometry.envelopeInternal
                    if (envelope.maxX > maxX) {
                        maxX = envelope.maxX
                    }
                    if (envelope.maxY > maxY) {
                        maxY = envelope.maxY
                    }
                    if (envelope.minX < minX || minX == 0.0) {
                        minX = envelope.minX
                    }
                    if (envelope.minY < minY || minY == 0.0) {
                        minY = envelope.minY
                    }
                }
            }
            //增加异常数据判断
            if (maxX != 0.0 && maxY != 0.0 && minX != 0.0 && minY != 0.0) {
                mapController.animationHandler.animateToBox(
                    maxX = maxX, maxY = maxY, minX = minX, minY = minY
                )
            }
        }
    }

    /**
     * 高亮当前选中的link
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun showCurrentLink(link: HadLinkDvoBean) {
        mapController.lineHandler.omdbTaskLinkLayer.showSelectLine(link)
        val geometry = GeometryTools.createGeometry(link.geometry)
        if (geometry != null) {
            val envelope = geometry.envelopeInternal
            mapController.animationHandler.animateToBox(
                maxX = envelope.maxX,
                maxY = envelope.maxY,
                minX = envelope.minX,
                minY = envelope.minY
            )
        }

    }

    override fun onCleared() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mapController.lineHandler.omdbTaskLinkLayer.clearSelectLine()
        }
        super.onCleared()
    }


    suspend fun saveLinkReason(bean: HadLinkDvoBean, text: String) {
        withContext(Dispatchers.IO) {
            currentSelectTaskBean?.let {
                for (item in it.hadLinkDvoList) {
                    if (item.linkPid == bean.linkPid) {
                        item.reason = text
                    }
                }
            }
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                realm.copyToRealmOrUpdate(currentSelectTaskBean)
            }
        }
    }

    /**
     * 筛选任务列表
     */
    fun filterTaskList(key: String) {
        if (filterTaskListJob != null)
            filterTaskListJob!!.cancel()
        filterTaskListJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            val realm = Realm.getDefaultInstance()
            val list = realm.where(TaskBean::class.java)
                .contains("evaluationTaskName", key)
                .or()
                .contains("dataVersion", key)
                .or()
                .contains("cityName", key)
                .findAll()
            liveDataTaskList.postValue(realm.copyFromRealm(list))
        }
    }

    fun filterTask(pidKey: String) {
        if (currentSelectTaskBean == null)
            return

        if (filterTaskJob != null)
            filterTaskJob!!.cancel()
        filterTaskJob = viewModelScope.launch(Dispatchers.Default) {
            delay(500)
            val list = mutableListOf<HadLinkDvoBean>()
            for (item in currentSelectTaskBean!!.hadLinkDvoList) {
                if (item.linkPid.contains(pidKey))
                    list.add(item)
            }
            liveDataTaskLinks.postValue(list)
        }
    }

    fun removeTask(context: Context, taskBean: TaskBean) {
        if (taskBean != null) {
            val mDialog = FirstDialog(context)
            mDialog.setTitle("提示？")
            mDialog.setMessage("是否关闭，请确认！")
            mDialog.setPositiveButton("确定", object : FirstDialog.OnClickListener {
                override fun onClick(dialog: Dialog?, which: Int) {
                    mDialog.dismiss()
                    viewModelScope.launch(Dispatchers.IO) {
                        val realm = Realm.getDefaultInstance()
                        realm.executeTransaction {
                            val objects = it.where(TaskBean::class.java)
                                .equalTo("id", taskBean.id).findFirst()
                            objects?.deleteFromRealm()
                        }
                        //遍历删除对应的数据
                        taskBean.hadLinkDvoList.forEach { hadLinkDvoBean ->
                            val qsRecordList = realm.where(QsRecordBean::class.java)
                                .equalTo("linkId", hadLinkDvoBean.linkPid).findAll()
                            if (qsRecordList != null && qsRecordList.size > 0) {
                                val copyList = realm.copyFromRealm(qsRecordList)
                                copyList.forEach {
                                    it.deleteFromRealm()
                                    mapController.markerHandle.removeQsRecordMark(it)
                                    mapController.mMapView.vtmMap.updateMap(true)
                                }
                            }
                        }
                        //过滤掉已上传的超过90天的数据
                        var nowTime: Long = DateTimeUtil.getNowDate().time
                        var beginNowTime: Long = nowTime - 90 * 3600 * 24 * 1000L
                        var syncUpload: Int = FileManager.Companion.FileUploadStatus.DONE
                        val objects = realm.where(TaskBean::class.java)
                            .notEqualTo("syncStatus", syncUpload).or()
                            .between("operationTime", beginNowTime, nowTime)
                            .equalTo("syncStatus", syncUpload).findAll()
                        val taskList = realm.copyFromRealm(objects)
                        for (item in taskList) {
                            FileManager.checkOMDBFileInfo(item)
                        }
                        liveDataTaskList.postValue(taskList)
                        liveDataCloseTask.postValue(true)
                    }
                }
            })
            mDialog.setNegativeButton("取消", object : FirstDialog.OnClickListener {
                override fun onClick(dialog: Dialog?, which: Int) {
                    liveDataCloseTask.postValue(false)
                    mDialog.dismiss()
                }
            })
            mDialog.show()
        }
    }

    fun checkUploadTask(context: Context, taskBean: TaskBean) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            taskBean.hadLinkDvoList.forEach { hadLinkDvoBean ->
                val objects = realm.where(QsRecordBean::class.java)
                    .equalTo("linkId", hadLinkDvoBean.linkPid).findAll()
                val map: MutableMap<TaskBean, Boolean> = HashMap<TaskBean, Boolean>()
                if (objects.isEmpty() && hadLinkDvoBean.reason.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        liveDataTaskUpload.postValue(map)
                        val mDialog = FirstDialog(context)
                        mDialog.setTitle("提示？")
                        mDialog.setMessage("此任务中存在未测评link，请确认！")
                        mDialog.setPositiveButton(
                            "确定",
                            object : FirstDialog.OnClickListener {
                                override fun onClick(dialog: Dialog?, which: Int) {
                                    mDialog.dismiss()
                                    map[taskBean] = true
                                    liveDataTaskUpload.postValue(map)
                                }
                            })
                        mDialog.setNegativeButton("取消", object : FirstDialog.OnClickListener {
                            override fun onClick(dialog: Dialog?, which: Int) {
                                mDialog.dismiss()
                            }
                        })
                        mDialog.show()
                    }
                    return@launch
                }
                map[taskBean] = true
                liveDataTaskUpload.postValue(map)
            }
        }
    }
}

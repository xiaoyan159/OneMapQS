package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.OnGeoPointClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.MapParamUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.*
import org.oscim.core.GeoPoint
import java.io.File
import javax.inject.Inject

enum class TaskDelStatus {
    /**
     * 删除开始
     */
    TASK_DEL_STATUS_BEGIN,

    /**
     * 删除中
     */
    TASK_DEL_STATUS_LOADING,

    /**
     * 删除成功
     */
    TASK_DEL_STATUS_SUCCESS,

    /**
     * 删除失败
     */
    TASK_DEL_STATUS_FAILED,

    /**
     * 取消删除
     */
    TASK_DEL_STATUS_CANCEL,
}

enum class TaskLoadStatus {
    /**
     * 加载开始
     */
    TASK_LOAD_STATUS_BEGIN,

    /**
     * 加载结束
     */
    TASK_LOAD_STATUS_FISISH,
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val mapController: NIMapController,
    private val sharedPreferences: SharedPreferences,
    private val realmOperateHelper: RealmOperateHelper,
) : ViewModel(), OnSharedPreferenceChangeListener {
    private val TAG = "TaskViewModel"

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

//    private val colors =
//        arrayOf(Color.RED, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN)
    /**
     * 用来确定是否关闭
     */
    val liveDataCloseTask = MutableLiveData<TaskDelStatus>()

    /**
     * 用来确定是否加载
     */
    val liveDataLoadTask = MutableLiveData<TaskLoadStatus>()

    /**
     * 用来更新任务
     */
    val liveDataUpdateTask = MutableLiveData<TaskBean>()

    /**
     * 提示信息
     */
    val liveDataToastMessage = MutableLiveData<String>()

    /**
     * 点击地图选中的link
     */
    val liveDataSelectLink = MutableLiveData<HadLinkDvoBean>()

    /**
     * 当前选中的任务
     */
//    var currentSelectTaskBean: TaskBean? = null

    /**
     * 任务列表查询协程
     */
    private var filterTaskListJob: Job? = null

    private var filterTaskJob: Job? = null

    /**
     * 是否正在选择导航起点
     */
    private var isSelectNaviStartPoint = false

    /**
     * 是否正在选择导航终点
     */
    private var isSelectNaviEndPoint = false


    /**
     * 是否开启了道路选择
     */
    val liveDataSelectNewLink = MutableLiveData(false)

    /**
     * 选中link
     */
    val liveDataAddLinkDialog = MutableLiveData<RenderEntity>()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        mapController.mMapView.addOnNIMapClickListener(TAG, object : OnGeoPointClickListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onMapClick(tag: String, point: GeoPoint, other: String) {
                if (tag == TAG) {
                    if ((isSelectNaviStartPoint || isSelectNaviEndPoint) && other.isNotEmpty()) {
                        updateTaskNavInfo(other)
                    } else if (liveDataSelectNewLink.value == true) {
                        viewModelScope.launch(Dispatchers.Default) {
                            val realm = realmOperateHelper.getSelectTaskRealmInstance()
                            if (liveDataUpdateTask.value == null) {
                                liveDataToastMessage.postValue("还没有开启任何任务")
                            } else {
                                val links = realmOperateHelper.queryLink(
                                    realm,
                                    point = point,
                                )
                                if (links.isNotEmpty()) {
                                    val l = links[0]
                                    for (link in liveDataUpdateTask.value!!.hadLinkDvoList) {
                                        if (link.linkPid == l.linkPid) {
                                            return@launch
                                        }
                                    }
                                    liveDataAddLinkDialog.postValue(l)
                                }
                            }
                            realm.close()
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.IO) {
                            val realm = realmOperateHelper.getSelectTaskRealmInstance()
                            val links = realmOperateHelper.queryLink(
                                realm,
                                point = point,
                            )
                            if (links.isNotEmpty()) {
                                val l = links[0]
                                liveDataUpdateTask.value?.let { value->
                                    for (link in value.hadLinkDvoList) {
                                        if (link.linkPid == l.linkPid) {

                                            liveDataSelectLink.postValue(link)
                                            mapController.lineHandler.showLine(link.geometry)
                                            break
                                        }
                                    }
                                }

                            }
                            realm.close()
                        }
                    }
                }
            }
        })

    }

    /**
     * 下载任务列表
     */
    fun getTaskList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            getLocalTaskList()
        }
    }


    /**
     * 获取任务列表
     */
    fun loadNetTaskList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = networkService.getTaskList(Constant.USER_ID)) {
                is NetResult.Success -> {
                    if (result.data != null) {
                        val realm = realmOperateHelper.getRealmDefaultInstance()
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
                                        //增加mesh==null兼容性处理
                                        for (hadLink in item.hadLinkDvoList) {
                                            if (hadLink.mesh == null) {
                                                hadLink.mesh = ""
                                                Log.e("qj", "${task.id}==null")
                                            }
                                        }
                                        task.hadLinkDvoList = item.hadLinkDvoList
                                        task.syncStatus = item.syncStatus
                                        //已上传后不在更新操作时间
                                        if (task.syncStatus != FileManager.Companion.FileUploadStatus.DONE) {
                                            //赋值时间，用于查询过滤
                                            task.operationTime = DateTimeUtil.getNowDate().time
                                        } else {//已上传数据不做更新
                                            continue
                                        }
                                    } else {
                                        for (hadLink in task.hadLinkDvoList) {
                                            hadLink.taskId = task.id
                                            if (hadLink.mesh == null) {
                                                hadLink.mesh = ""
                                                Log.e("qj", "${task.id}==新增==null")
                                            }
                                        }
                                        //赋值时间，用于查询过滤
                                        task.operationTime = DateTimeUtil.getNowDate().time
                                    }
                                    Log.e("qj", "${task.id}")
                                    realm.copyToRealmOrUpdate(task)
                                }
                            }

                        }
                        realm.close()
                    }
                    getLocalTaskList()
                }

                is NetResult.Error<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                    getLocalTaskList()
                }

                is NetResult.Failure<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                            .show()
                    }
                    getLocalTaskList()
                }

                is NetResult.Loading -> {}
                else -> {}
            }
        }
    }


    /**
     * 获取任务列表
     */
    private suspend fun getLocalTaskList() {
        val realm = realmOperateHelper.getRealmDefaultInstance()
        //过滤掉已上传的超过90天的数据
        val nowTime: Long = DateTimeUtil.getNowDate().time
        val beginNowTime: Long = nowTime - 90 * 3600 * 24 * 1000L
        val syncUpload: Int = FileManager.Companion.FileUploadStatus.DONE
        val objects = realm.where(TaskBean::class.java).notEqualTo("syncStatus", syncUpload).or()
            .between("operationTime", beginNowTime, nowTime).equalTo("syncStatus", syncUpload)
            .findAll().sort("id")
        val taskList = realm.copyFromRealm(objects)
        for (item in taskList) {
            FileManager.checkOMDBFileInfo(item)
        }
        liveDataTaskList.postValue(taskList)
        realm.close()
        val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
        if (id > -1) {
            for (item in taskList) {
                if (item.id == id) {
                    liveDataUpdateTask.postValue(item)
                    liveDataTaskLinks.postValue(item.hadLinkDvoList)
                    withContext(Dispatchers.Main) {
                        showTaskLinks(liveDataUpdateTask.value!!)
                    }
                    break
                }
            }
        }
    }

    /**
     * 设置当前选择的任务，并高亮当前任务的所有link
     */

    fun setSelectTaskBean(taskBean: TaskBean) {

        sharedPreferences.edit().putInt(Constant.SELECT_TASK_ID, taskBean.id).apply()

        liveDataUpdateTask.value = taskBean

        liveDataTaskLinks.value = taskBean.hadLinkDvoList

        liveDataLoadTask.postValue(TaskLoadStatus.TASK_LOAD_STATUS_BEGIN)

        showTaskLinks(taskBean)
        mapController.lineHandler.removeLine()
        //重新加载轨迹
        viewModelScope.launch(Dispatchers.IO) {
            Constant.TRACE_COUNT = 0
            val list: List<NiLocation>? = TraceDataBase.getDatabase(
                mapController.mMapView.context, Constant.USER_DATA_PATH
            ).niLocationDao.findToTaskIdAll(taskBean.id.toString())
            list!!.forEach {

                Constant.TRACE_COUNT++

                if (Constant.TRACE_COUNT % Constant.TRACE_COUNT_MORE_TIME == 0) {
                    mapController.markerHandle.addNiLocationMarkerItemRough(it)
                }

                if (Constant.TRACE_COUNT % Constant.TRACE_COUNT_TIME == 0) {
                    mapController.markerHandle.addNiLocationMarkerItemSimple(it)
                }

                mapController.markerHandle.addNiLocationMarkerItem(it)

            }
            liveDataLoadTask.postValue(TaskLoadStatus.TASK_LOAD_STATUS_FISISH)
            withContext(Dispatchers.Main) {
                MapParamUtils.setTaskId(taskBean.id)
                Constant.currentSelectTaskFolder = File(Constant.USER_DATA_PATH + "/${taskBean.id}")
                Constant.currentSelectTaskConfig =
                    RealmConfiguration.Builder().directory(Constant.currentSelectTaskFolder)
                        .name("OMQS.realm").encryptionKey(Constant.PASSWORD).allowQueriesOnUiThread(true)
                        .schemaVersion(2).build()
                MapParamUtils.setTaskConfig(Constant.currentSelectTaskConfig)
                mapController.layerManagerHandler.updateOMDBVectorTileLayer()
                mapController.mMapView.updateMap(true)
            }
        }
    }


    private fun showTaskLinks(taskBean: TaskBean) {

        mapController.lineHandler.removeAllTaskLine()
        mapController.markerHandle.clearNiLocationLayer()
        if (taskBean.hadLinkDvoList.isNotEmpty()) {
            mapController.lineHandler.showTaskLines(taskBean.hadLinkDvoList)
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

        //重新加载轨迹
        viewModelScope.launch(Dispatchers.IO) {
            val list: List<NiLocation>? = TraceDataBase.getDatabase(
                mapController.mMapView.context, Constant.USER_DATA_PATH
            ).niLocationDao.findToTaskIdAll(taskBean.id.toString())
            list!!.forEach {
                mapController.markerHandle.addNiLocationMarkerItem(it)
            }
        }
    }

    /**
     * 高亮当前选中的link
     */
    fun showCurrentLink(link: HadLinkDvoBean) {
        isSelectNaviStartPoint = false
        isSelectNaviEndPoint = false
        liveDataSelectLink.value = link
        mapController.markerHandle.removeNaviMarkerLayer()
        mapController.lineHandler.showLine(link.geometry)
//        mapController.lineHandler.omdbTaskLinkLayer.showSelectLine(link)
        val geometry = GeometryTools.createGeometry(link.geometry)
        if (geometry != null) {
            val envelope = geometry.envelopeInternal
            mapController.animationHandler.animateToBox(
                maxX = envelope.maxX + 0.0005,
                maxY = envelope.maxY + 0.0005,
                minX = envelope.minX - 0.0005,
                minY = envelope.minY - 0.0005
            )
        }
    }

    /**
     *
     */
    fun setSkipLink(bean: HadLinkDvoBean) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = realmOperateHelper.getRealmDefaultInstance()
            realm.executeTransaction{
                realm.copyToRealmOrUpdate(bean)
            }
            realm.close()
        }
    }

    /**
     * 设置导航路径起始link
     */
    fun setNaviStartOrEnd(bean: HadLinkDvoBean, bStart: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = realmOperateHelper.getSelectTaskRealmInstance()

            val resDir = realm.where(RenderEntity::class.java)
                .equalTo("linkPid", bean.linkPid)
                .equalTo("table", DataCodeEnum.OMDB_LINK_DIRECT.name).findFirst()
            if (resDir == null) {
                realm.close()
                liveDataToastMessage.postValue("link属性数据缺失,请先下载数据")
                return@launch
            }
            val direct = resDir.properties["direct"]
            val resRd = realm.where(RenderEntity::class.java)
                .equalTo("linkPid", bean.linkPid)
                .equalTo("table", DataCodeEnum.OMDB_RD_LINK.name).findFirst()

            var sNodeId: String? = null
            var eNodeId: String? = null
            resRd?.let { rd ->
                sNodeId = rd.linkRelation!!.sNodeId
                eNodeId = rd.linkRelation!!.eNodeId
            }
            realm.close()
            if (sNodeId == null || eNodeId == null) {
                liveDataToastMessage.postValue("link属性数据缺失,请先下载数据")
                return@launch
            }
            when (direct) {
                "2" -> {
                    liveDataUpdateTask.value?.let { taskBean ->
                        if (taskBean.navInfo == null)
                            taskBean.navInfo = NavInfo(taskBean.id)
                        taskBean.navInfo?.let { navInfo ->
                            if (bStart) {
                                navInfo.naviStartLinkId = bean.linkPid
                                navInfo.naviStartNode = sNodeId!!
                                if (navInfo.naviEndLinkId == bean.linkPid) {
                                    navInfo.naviEndLinkId = ""
                                    navInfo.naviEndNode = ""
                                }
                            } else {
                                navInfo.naviEndLinkId = bean.linkPid
                                navInfo.naviEndNode = eNodeId!!
                                if (navInfo.naviStartLinkId == bean.linkPid) {
                                    navInfo.naviStartLinkId = ""
                                    navInfo.naviStartNode = ""
                                }
                            }
                            val realm = realmOperateHelper.getRealmDefaultInstance()
                            realm.executeTransaction {
                                it.copyToRealmOrUpdate(taskBean)
                            }
                            realm.close()
                            liveDataUpdateTask.postValue(taskBean)
                        }
                    }
                }
                "3" -> {
                    liveDataUpdateTask.value?.let { taskBean ->
                        if (taskBean.navInfo == null)
                            taskBean.navInfo = NavInfo(taskBean.id)
                        taskBean.navInfo?.let { navInfo ->
                            if (bStart) {
                                navInfo.naviStartLinkId = bean.linkPid
                                navInfo.naviStartNode = eNodeId!!
                                if (navInfo.naviEndLinkId == bean.linkPid) {
                                    navInfo.naviEndLinkId = ""
                                    navInfo.naviEndNode = ""
                                }
                            } else {
                                navInfo.naviEndLinkId = bean.linkPid
                                navInfo.naviEndNode = sNodeId!!
                                if (navInfo.naviStartLinkId == bean.linkPid) {
                                    navInfo.naviStartLinkId = ""
                                    navInfo.naviStartNode = ""
                                }
                            }
                            val realm = realmOperateHelper.getRealmDefaultInstance()
                            realm.executeTransaction {
                                it.copyToRealmOrUpdate(taskBean)
                            }
                            realm.close()
                            liveDataUpdateTask.postValue(taskBean)
                        }
                    }
                }
                else -> {
                    liveDataToastMessage.postValue("当前link为双方向道路，不能自动确定起终点，请选择")
                    val list = GeometryTools.getGeoPoints(bean.geometry)
                    withContext(Dispatchers.Main) {
                        if (bStart) {
                            isSelectNaviStartPoint = true
                            isSelectNaviEndPoint = false
                        } else {
                            isSelectNaviStartPoint = false
                            isSelectNaviEndPoint = true
                        }
                        mapController.markerHandle.showNaviStartOrEndLayer(list.first(), list.last(), sNodeId!!, eNodeId!!, bStart)
                    }
                }
            }
        }
    }


    /**
     *更新导航配置信息
     */
    fun updateTaskNavInfo(nodeId: String) {
        liveDataSelectLink.value?.let { linkBean ->
            liveDataUpdateTask.value?.let { taskBean ->
                viewModelScope.launch(Dispatchers.IO) {
                    if (taskBean.navInfo == null)
                        taskBean.navInfo = NavInfo(taskBean.id)
                    taskBean.navInfo?.let { navInfo ->
                        if (isSelectNaviStartPoint) {
                            navInfo.naviStartLinkId = linkBean.linkPid
                            navInfo.naviStartNode = nodeId
                            if (navInfo.naviEndLinkId == linkBean.linkPid) {
                                navInfo.naviEndLinkId = ""
                                navInfo.naviEndNode = ""
                            }
                        } else if (isSelectNaviEndPoint) {
                            navInfo.naviEndLinkId = linkBean.linkPid
                            navInfo.naviEndNode = nodeId
                            if (navInfo.naviStartLinkId == linkBean.linkPid) {
                                navInfo.naviStartLinkId = ""
                                navInfo.naviStartNode = ""
                            }
                        }
                        val realm = realmOperateHelper.getRealmDefaultInstance()
                        realm.executeTransaction {
                            it.copyToRealmOrUpdate(taskBean)
                        }
                        realm.close()
                        liveDataUpdateTask.postValue(taskBean)
                    }

                    withContext(Dispatchers.Main) {
                        mapController.markerHandle.removeNaviMarkerLayer()
                    }
                    isSelectNaviEndPoint = false
                    isSelectNaviStartPoint = false
                }
            }
        }
    }

    override fun onCleared() {
        mapController.mMapView.removeOnNIMapClickListener(TAG)
        mapController.lineHandler.removeAllLine()
        mapController.markerHandle.removeNaviMarkerLayer()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    /**
     * 保存link补作业原因
     */
    suspend fun saveLinkReason(bean: HadLinkDvoBean, text: String) {
        withContext(Dispatchers.IO) {
            liveDataUpdateTask.value?.let {
                for (item in it.hadLinkDvoList) {
                    if (item.linkPid == bean.linkPid) {
                        item.reason = text
                    }
                }
                val realm = realmOperateHelper.getRealmDefaultInstance()
                realm.executeTransaction { r ->
                    r.copyToRealmOrUpdate(it)
                }
                realm.close()
            }

        }
    }

    /**
     * 筛选任务列表
     */
    fun filterTaskList(key: String) {
        if (filterTaskListJob != null) filterTaskListJob!!.cancel()
        filterTaskListJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            val realm = realmOperateHelper.getRealmDefaultInstance()
            val list = realm.where(TaskBean::class.java).contains("evaluationTaskName", key).or()
                .contains("dataVersion", key).or().contains("cityName", key).findAll()
            liveDataTaskList.postValue(realm.copyFromRealm(list))
            realm.close()
        }
    }

    /**
     * 筛选link
     */
    fun filterTask(pidKey: String) {
        if (liveDataUpdateTask.value == null) return

        if (filterTaskJob != null) filterTaskJob!!.cancel()
        filterTaskJob = viewModelScope.launch(Dispatchers.Default) {
            delay(500)
            val list = mutableListOf<HadLinkDvoBean>()
            for (item in liveDataUpdateTask.value!!.hadLinkDvoList) {
                if (item.linkPid.contains(pidKey)) list.add(item)
            }
            liveDataTaskLinks.postValue(list)
        }
    }

    /**
     * 重新下载数据任务
     */
    fun resetDownload(context: Context, taskBean: TaskBean) {
        val mDialog = FirstDialog(context)
        mDialog.setTitle("提示？")
        mDialog.setMessage("是否重置下载状态，请确认！")
        mDialog.setPositiveButton(
            "确定"
        ) { dialog, _ ->
            dialog.dismiss()
            liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_BEGIN)
            viewModelScope.launch(Dispatchers.IO) {
                //删除已下载的数据
                val fileTemp =
                    File("${Constant.DOWNLOAD_PATH}${taskBean.evaluationTaskName}_${taskBean.dataVersion}.zip")
                if (fileTemp.exists()) {
                    fileTemp.delete()
                }
                val taskFileTemp = File(Constant.USER_DATA_PATH + "/${taskBean.id}")
                //重命名
                if (taskFileTemp.exists()) {
                    /*                    var currentSelectTaskFolder = File(Constant.USER_DATA_PATH + "/${taskBean.id}")
                                        var currentSelectTaskConfig =
                                            RealmConfiguration.Builder().directory(currentSelectTaskFolder)
                                                .name("OMQS.realm").encryptionKey(Constant.PASSWORD)
                                                .allowQueriesOnUiThread(true)
                                                .schemaVersion(2).build()
                                        Realm.getInstance(currentSelectTaskConfig).executeTransaction { r ->
                                            //删除已有所有数据
                                            r.delete(RenderEntity::class.java)
                                            r.delete(ReferenceEntity::class.java)
                                        }
                                        Realm.getInstance(currentSelectTaskConfig).close()*/
                }
                //将下载状态修改已下载
                val realm = realmOperateHelper.getRealmDefaultInstance()
                taskBean.syncStatus = FileManager.Companion.FileUploadStatus.NONE
                taskBean.status = FileManager.Companion.FileDownloadStatus.NONE
                realm.beginTransaction()
                realm.copyToRealmOrUpdate(taskBean)
                realm.commitTransaction()
                realm.close()
                liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_SUCCESS)
                withContext(Dispatchers.Main) {
                    if (taskBean.id == liveDataUpdateTask.value?.id ?: 0) {
                        mapController.layerManagerHandler.updateOMDBVectorTileLayer()
                    } else {
                        setSelectTaskBean(taskBean)
                    }
                    realmOperateHelper.getRealmDefaultInstance().refresh()
                    //重新加载数据
                    getLocalTaskList()
                }
            }
        }
        mDialog.setNegativeButton(
            "取消"
        ) { _, _ ->
            liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_CANCEL)
            mDialog.dismiss()
        }
        mDialog.show()
    }

    /**
     * 关闭任务
     */
    fun removeTask(context: Context, taskBean: TaskBean) {
        val mDialog = FirstDialog(context)
        mDialog.setTitle("提示？")
        mDialog.setMessage("是否关闭，请确认！")
        mDialog.setPositiveButton(
            "确定"
        ) { dialog, _ ->
            dialog.dismiss()
            liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_BEGIN)
            viewModelScope.launch(Dispatchers.IO) {
                liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_LOADING)
                val realm = realmOperateHelper.getRealmDefaultInstance()
                realm.executeTransaction {
                    val objects =
                        it.where(TaskBean::class.java).equalTo("id", taskBean.id).findFirst()
                    objects?.deleteFromRealm()
                }
                //遍历删除对应的数据
                taskBean.hadLinkDvoList.forEach { hadLinkDvoBean ->
                    val qsRecordList = realm.where(QsRecordBean::class.java)
                        .equalTo("linkId", hadLinkDvoBean.linkPid).and()
                        .equalTo("taskId", hadLinkDvoBean.taskId).findAll()
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
                val nowTime: Long = DateTimeUtil.getNowDate().time
                val beginNowTime: Long = nowTime - 90 * 3600 * 24 * 1000L
                val syncUpload: Int = FileManager.Companion.FileUploadStatus.DONE
                val objects =
                    realm.where(TaskBean::class.java).notEqualTo("syncStatus", syncUpload).or()
                        .between("operationTime", beginNowTime, nowTime)
                        .equalTo("syncStatus", syncUpload).findAll()
                val taskList = realm.copyFromRealm(objects)
                for (item in taskList) {
                    FileManager.checkOMDBFileInfo(item)
                }
                liveDataTaskList.postValue(taskList)
                liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_SUCCESS)
                realm.close()
            }
        }
        mDialog.setNegativeButton(
            "取消"
        ) { _, _ ->
            liveDataCloseTask.postValue(TaskDelStatus.TASK_DEL_STATUS_CANCEL)
            mDialog.dismiss()
        }
        mDialog.show()
    }

    fun checkUploadTask(context: Context, taskBean: TaskBean) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = realmOperateHelper.getRealmDefaultInstance()
            var result = 0
            val map: MutableMap<TaskBean, Boolean> = HashMap<TaskBean, Boolean>()
            taskBean.hadLinkDvoList.forEach { hadLinkDvoBean ->
                val objects =
                    realm.where(QsRecordBean::class.java).equalTo("linkId", hadLinkDvoBean.linkPid)
                        .and().equalTo("taskId", hadLinkDvoBean.taskId).findAll()
                if (objects.isEmpty() && hadLinkDvoBean.reason.isEmpty()) {
                    if (hadLinkDvoBean.linkStatus == 3) {
                        result = 1
                        realm.close()
                        return@forEach
                    } else {
                        result = 2
                    }
                }
            }
            realm.close()
            if (result == 1) {
                liveDataTaskUpload.postValue(map)
                withContext(Dispatchers.Main) {
                    val mDialog = FirstDialog(context)
                    mDialog.setTitle("提示？")
                    mDialog.setMessage("此任务中存在新增Link无问题记录，请添加至少一条记录！")
                    mDialog.setPositiveButton(
                        "确定"
                    ) { _, _ ->
                        mDialog.dismiss()
                    }
                    mDialog.setCancelVisibility(View.GONE)
                    mDialog.show()
                }
            } else if (result == 2) {
                liveDataTaskUpload.postValue(map)
                withContext(Dispatchers.Main) {
                    val mDialog = FirstDialog(context)
                    mDialog.setTitle("提示？")
                    mDialog.setMessage("此任务中存在未测评link，请确认！")
                    mDialog.setPositiveButton(
                        "确定"
                    ) { _, _ ->
                        mDialog.dismiss()
                        map[taskBean] = true
                        liveDataTaskUpload.postValue(map)
                    }
                    mDialog.setNegativeButton(
                        "取消"
                    ) { _, _ -> mDialog.dismiss() }
                    mDialog.show()
                }
            } else {
                map[taskBean] = true
                liveDataTaskUpload.postValue(map)
            }
        }
    }

    /**
     * 监听新增的评测link
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == Constant.SHARED_SYNC_TASK_LINK_ID) {
            viewModelScope.launch(Dispatchers.IO) {
                getLocalTaskList()
            }
        }
    }

    /**
     * 设置是否开启选择link
     */
    fun setSelectLink(selected: Boolean) {
        liveDataSelectNewLink.value = selected
    }

    /**
     * 添加link
     */
    fun addTaskLink(context: Context, data: RenderEntity) {
        val mDialog = FirstDialog(context)
        mDialog.setTitle("提示")
        mDialog.setMessage("是否添加当前link")
        mDialog.setPositiveButton(
            "确定"
        ) { dialog, _ ->
            dialog.dismiss()
            viewModelScope.launch(Dispatchers.IO) {
                val hadLinkDvoBean = HadLinkDvoBean(
                    taskId = liveDataUpdateTask.value!!.id,
                    linkPid = data.linkPid,
                    geometry = data.geometry,
                    linkStatus = 2
                )
                liveDataUpdateTask.value!!.hadLinkDvoList.add(
                    hadLinkDvoBean
                )
                val realm = realmOperateHelper.getRealmDefaultInstance()
                realm.executeTransaction { r ->
                    r.copyToRealmOrUpdate(hadLinkDvoBean)
                    r.copyToRealmOrUpdate(liveDataUpdateTask.value!!)
                }
                //根据Link数据查询对应数据上要素，对要素进行显示重置
                data.linkPid.let {
                    realmOperateHelper.queryLinkToMutableRenderEntityList(realm, it)
                        ?.forEach { renderEntity ->
                            if (renderEntity.enable != 1) {
                                renderEntity.enable = 1
                                realm.executeTransaction { r ->
                                    r.copyToRealmOrUpdate(renderEntity)
                                }
                            }
                        }
                }
                liveDataTaskLinks.postValue(liveDataUpdateTask.value!!.hadLinkDvoList)
                mapController.lineHandler.addTaskLink(hadLinkDvoBean)
                mapController.layerManagerHandler.updateOMDBVectorTileLayer()
                mapController.mMapView.vtmMap.updateMap(true)
                realm.close()
            }
        }
        mDialog.setNegativeButton(
            "取消"
        ) { _, _ ->
            mDialog.dismiss()
        }
        mDialog.show()
    }


    /**
     * 删除评测link
     */
    fun deleteTaskLink(context: Context, hadLinkDvoBean: HadLinkDvoBean) {
        if (hadLinkDvoBean.linkStatus == 1) {
            val mDialog = FirstDialog(context)
            mDialog.setTitle("提示")
            mDialog.setMessage("当前要评测的link是任务原始规划的，不能删除，如果不进行作业请标记原因")
            mDialog.setCancelVisibility(View.GONE)
            mDialog.setPositiveButton(
                "确定"
            ) { _, _ ->
                mDialog.dismiss()
            }
            mDialog.show()
        } else {
            val mDialog = FirstDialog(context)
            mDialog.setTitle("提示")
            mDialog.setMessage("是否删除当前link，与之相关联的评测任务会一起删除！！")
            mDialog.setPositiveButton(
                "确定"
            ) { dialog, _ ->
                dialog.dismiss()
                viewModelScope.launch(Dispatchers.IO) {

                    val realm = realmOperateHelper.getRealmDefaultInstance()

                    //重置数据为隐藏
                    if (hadLinkDvoBean.linkStatus == 2) {
                        realmOperateHelper.queryLinkToMutableRenderEntityList(
                            realm,
                            hadLinkDvoBean.linkPid
                        )
                            ?.forEach { renderEntity ->
                                if (renderEntity.enable == 1) {
                                    renderEntity.enable = 0
                                    realm.executeTransaction { r ->
                                        r.copyToRealmOrUpdate(renderEntity)
                                    }
                                }
                            }
                        mapController.layerManagerHandler.updateOMDBVectorTileLayer()
                        mapController.mMapView.vtmMap.updateMap(true)
                    }

                    realm.executeTransaction {
                        for (link in liveDataUpdateTask.value!!.hadLinkDvoList) {
                            if (link.linkPid == hadLinkDvoBean.linkPid) {
                                liveDataUpdateTask.value!!.hadLinkDvoList.remove(link)
                                break
                            }
                        }
                        realm.where(HadLinkDvoBean::class.java)
                            .equalTo("linkPid", hadLinkDvoBean.linkPid).findFirst()
                            ?.deleteFromRealm()
                        val markers = realm.where(QsRecordBean::class.java)
                            .equalTo("linkId", hadLinkDvoBean.linkPid).and()
                            .equalTo("taskId", hadLinkDvoBean.taskId).findAll()
                        if (markers != null) {
                            for (marker in markers) {
                                mapController.markerHandle.removeQsRecordMark(marker)
                            }
                            markers.deleteAllFromRealm()
                        }

                        realm.copyToRealmOrUpdate(liveDataUpdateTask.value)
                        mapController.lineHandler.removeTaskLink(hadLinkDvoBean.linkPid)
                        liveDataTaskLinks.postValue(liveDataUpdateTask.value!!.hadLinkDvoList)
                    }
                    realm.close()
                }
            }
            mDialog.setNegativeButton(
                "取消"
            ) { _, _ ->
                mDialog.dismiss()
            }
            mDialog.show()
        }
    }


}

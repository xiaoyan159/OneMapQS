package com.navinfo.omqs.ui.fragment.tasklink

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.LinkInfoBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.MeasureLayerHandler
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.Constant
import com.navinfo.omqs.ui.dialog.FirstDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskLinkViewModel @Inject constructor(
    private val mapController: NIMapController,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * 种别
     */
    private val kindList = listOf(
        TaskLinkInfoAdapterItem("高速道路", 1),
        TaskLinkInfoAdapterItem("城市高速", 2),
        TaskLinkInfoAdapterItem("国道", 3),
        TaskLinkInfoAdapterItem("省道", 4),
        TaskLinkInfoAdapterItem("县道", 6),
        TaskLinkInfoAdapterItem("乡镇村道路", 7),
        TaskLinkInfoAdapterItem("其他道路", 8),
        TaskLinkInfoAdapterItem("非引导道路", 9),
        TaskLinkInfoAdapterItem("步行道路", 10),
        TaskLinkInfoAdapterItem("人渡", 11),
        TaskLinkInfoAdapterItem("轮渡", 13),
        TaskLinkInfoAdapterItem("自行车道路", 15),
    )

    /**
     * FunctionGrade 功能等级
     */
    private val functionLevelList = listOf(
        TaskLinkInfoAdapterItem("等级1", 1),
        TaskLinkInfoAdapterItem("等级2", 2),
        TaskLinkInfoAdapterItem("等级3", 3),
        TaskLinkInfoAdapterItem("等级4", 4),
        TaskLinkInfoAdapterItem("等级5", 5),
    )

    /**
     * 数据级别
     */
    private val dataLevelList = listOf(
        TaskLinkInfoAdapterItem("Pro lane model(有高精车道模型覆盖的高速和城高link)", 1),
        TaskLinkInfoAdapterItem("Lite lane model(有高精车道模型覆盖的普通路link)", 2),
        TaskLinkInfoAdapterItem("Standard road model(其他link)", 3),
    )

    /**
     * 处理结束关闭fragment`
     */
    val liveDataFinish = MutableLiveData(false)

    /**
     * 左侧面板展示内容
     */
    val liveDataLeftAdapterList = MutableLiveData<List<TaskLinkInfoAdapterItem>>()

    /**
     * 选择的种别
     */
    val liveDataSelectKind = MutableLiveData<TaskLinkInfoAdapterItem?>()

    /**
     * 选择的功能等级
     */
    val liveDataSelectFunctionLevel = MutableLiveData<TaskLinkInfoAdapterItem?>()

    /**
     * 选择的数据等级
     */
    val liveDataSelectDataLevel = MutableLiveData<TaskLinkInfoAdapterItem?>()

    /**
     * 要提示的错误信息
     */
    val liveDataToastMessage = MutableLiveData<String>()

    /**
     * 当前选中的任务
     */
    val liveDataTaskBean = MutableLiveData<TaskBean?>()

    /**
     * 当前正在编辑的线
     */
    private var hadLinkDvoBean: HadLinkDvoBean? = null

    /**
     * 当前正在选择哪个数据 1：种别 2：功能等级 3：数据等级
     */
    private var selectType = 0

    init {
        getTaskBean()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }


    private fun getTaskBean() {
        viewModelScope.launch(Dispatchers.IO) {
            val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
            val realm = Realm.getDefaultInstance()
            val res = realm.where(TaskBean::class.java).equalTo("id", id).findFirst()
            liveDataTaskBean.postValue(res?.let { realm.copyFromRealm(it) })
            realm.close()
        }
    }

    /**
     * 编辑点
     */
    fun addPoint() {
        mapController.measureLayerHandler.addPoint(MeasureLayerHandler.MEASURE_TYPE.DISTANCE)
    }

    /**
     * 设置左侧面板要显示的内容
     */
    fun setAdapterList(type: Int) {
        selectType = type
        when (type) {
            1 -> liveDataLeftAdapterList.value = kindList
            2 -> liveDataLeftAdapterList.value = functionLevelList
            3 -> liveDataLeftAdapterList.value = dataLevelList
        }
    }

    /**
     * 返回左侧面板选择的内容
     */
    fun setAdapterSelectValve(item: TaskLinkInfoAdapterItem) {
        when (selectType) {
            1 -> liveDataSelectKind.value = item
            2 -> liveDataSelectFunctionLevel.value = item
            3 -> liveDataSelectDataLevel.value = item
        }
    }

    override fun onCleared() {
        mapController.measureLayerHandler.clear()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    /**
     * 保存数据
     */
    fun saveData() {
        viewModelScope.launch(Dispatchers.Default) {
            if (liveDataTaskBean.value == null) {
                liveDataToastMessage.postValue("还没有选择任何一条任务！")
                return@launch
            }

            if (mapController.measureLayerHandler.mPathLayer.points.size < 2) {
                liveDataToastMessage.postValue("道路点少于2个！")
                return@launch
            }
            if (liveDataSelectKind.value == null) {
                liveDataToastMessage.postValue("请选择种别！")
                return@launch
            }
            if (liveDataSelectFunctionLevel.value == null) {
                liveDataToastMessage.postValue("请选择功能等级！")
                return@launch
            }
            if (liveDataSelectDataLevel.value == null) {
                liveDataToastMessage.postValue("请选择数据等级！")
                return@launch
            }
            val task: TaskBean = liveDataTaskBean.value!!
            if (hadLinkDvoBean != null) {
                hadLinkDvoBean!!.taskId = liveDataTaskBean.value!!.id
                hadLinkDvoBean!!.length =
                    mapController.measureLayerHandler.measureValueLiveData.value!!.value
                hadLinkDvoBean!!.geometry =
                    GeometryTools.getLineString(mapController.measureLayerHandler.mPathLayer.points)
                hadLinkDvoBean!!.linkInfo = LinkInfoBean(
                    kind = liveDataSelectKind.value!!.type,
                    functionLevel = liveDataSelectFunctionLevel.value!!.type,
                    dataLevel = liveDataSelectDataLevel.value!!.type,
                )
                for (l in task.hadLinkDvoList) {
                    if (l.linkPid == hadLinkDvoBean!!.linkPid) {
                        task.hadLinkDvoList.remove(l)
                        task.hadLinkDvoList.add(hadLinkDvoBean)
                        break
                    }
                }
            } else {
                hadLinkDvoBean = HadLinkDvoBean(
                    taskId = liveDataTaskBean.value!!.id,
                    linkPid = UUID.randomUUID().toString(),
                    linkStatus = 3,
                    length = mapController.measureLayerHandler.measureValueLiveData.value!!.value,
                    geometry = GeometryTools.getLineString(mapController.measureLayerHandler.mPathLayer.points),
                    linkInfo = LinkInfoBean(
                        kind = liveDataSelectKind.value!!.type,
                        functionLevel = liveDataSelectFunctionLevel.value!!.type,
                        dataLevel = liveDataSelectDataLevel.value!!.type,
                    )
                )
                task.hadLinkDvoList.add(hadLinkDvoBean)
            }


            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                it.copyToRealmOrUpdate(hadLinkDvoBean)
                it.copyToRealmOrUpdate(task)
            }
            mapController.lineHandler.addTaskLink(hadLinkDvoBean!!)
            sharedPreferences.edit()
                .putString(Constant.SHARED_SYNC_TASK_LINK_ID, hadLinkDvoBean!!.linkPid)
                .apply()
            liveDataFinish.postValue(true)
            realm.close()
        }
    }

    /**
     * 监听shared变化
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == Constant.SELECT_TASK_ID) {
            getTaskBean()
        }
    }

    /**
     * 绘制线的时候回退点
     */
    fun removeLinkLastPoint() {
        mapController.measureLayerHandler.backspacePoint()
    }

    /**
     * 清除重绘
     */
    fun clearLink() {
        mapController.measureLayerHandler.clear()
    }

    /**
     * 初始化数据
     */
    fun initData(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val objects =
                realm.where(HadLinkDvoBean::class.java).equalTo("linkPid", id)
                    .findFirst()
            objects?.linkInfo?.let {
                for (kind in kindList) {
                    if (kind.type == it.kind) {
                        liveDataSelectKind.postValue(kind)
                        break
                    }
                }
                for (function in functionLevelList) {
                    if (function.type == it.functionLevel) {
                        liveDataSelectFunctionLevel.postValue(function)
                        break
                    }
                }
                for (data in dataLevelList) {
                    if (data.type == it.dataLevel) {
                        liveDataSelectDataLevel.postValue(data)
                        break
                    }
                }
            }
            val task =
                realm.where(TaskBean::class.java).equalTo("id", objects?.taskId)
                    .findFirst()

            if (task != null) {
                liveDataTaskBean.postValue(realm.copyFromRealm(task))
            }
            if(objects != null) {
                hadLinkDvoBean = realm.copyFromRealm(objects)
                withContext(Dispatchers.Main) {
                    mapController.measureLayerHandler.initPathLine(hadLinkDvoBean?.geometry!!)
                }
            }
            realm.close()
        }
    }

    /**
     * 删除数据
     */
    fun deleteData(context: Context) {
        if (hadLinkDvoBean == null) {
            liveDataFinish.value = true
            return
        }
        val mDialog = FirstDialog(context)
        mDialog.setTitle("提示？")
        mDialog.setMessage("是否删除Mark，请确认！")
        mDialog.setPositiveButton(
            "确定"
        ) { _, _ ->
            mDialog.dismiss()
            viewModelScope.launch(Dispatchers.IO) {
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    //先找到对应的任务
                    val task = it.where(TaskBean::class.java).equalTo("id", hadLinkDvoBean!!.taskId)
                        .findFirst()
                    //维护任务删除当前link
                    if (task != null) {
                        for (h in task.hadLinkDvoList) {
                            if (h.linkPid == hadLinkDvoBean!!.linkPid)
                                task.hadLinkDvoList.remove(h)
                            break
                        }
                        realm.copyToRealmOrUpdate(task)
                    }

                    //删除link
                    val objects = it.where(HadLinkDvoBean::class.java)
                        .equalTo("linkPid", hadLinkDvoBean!!.linkPid).findFirst()
                    objects?.deleteFromRealm()
                    //删除相关联的评测任务
                    val qsRecordBeans = it.where(QsRecordBean::class.java)
                        .equalTo("linkId", hadLinkDvoBean!!.linkPid).and()
                        .equalTo("taskId", hadLinkDvoBean!!.taskId).findAll()
                    if (qsRecordBeans != null) {
                        for (b in qsRecordBeans) {
                            mapController.markerHandle.removeQsRecordMark(b)
                        }
                        qsRecordBeans.deleteAllFromRealm()
                    }
                }
                mapController.lineHandler.removeTaskLink(hadLinkDvoBean!!.linkPid)
                mapController.mMapView.vtmMap.updateMap(true)
                liveDataFinish.postValue(true)
                realm.close()
            }
        }
        mDialog.setNegativeButton("取消", null)
        mDialog.show()
    }
}
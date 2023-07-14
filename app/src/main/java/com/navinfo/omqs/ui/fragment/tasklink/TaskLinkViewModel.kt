package com.navinfo.omqs.ui.fragment.tasklink

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.LinkInfoBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.Constant
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.bson.codecs.UuidCodec
import org.bson.internal.UuidHelper
import org.oscim.core.GeoPoint
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
        }
    }

    /**
     * 编辑点
     */
    fun addPoint() {
        mapController.measureLayerHandler.drawLineOrPolygon(false)
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
            val linkBean = HadLinkDvoBean(
//                taskId = liveDataTaskBean.value!!.id,
                linkPid = UUID.randomUUID().toString(),
                linkStatus = 3,
                geometry = GeometryTools.getLineString(mapController.measureLayerHandler.mPathLayer.points),
                linkInfo = LinkInfoBean(
                    kind = liveDataSelectKind.value!!.type,
                    functionLevel = liveDataSelectFunctionLevel.value!!.type,
                    dataLevel = liveDataSelectDataLevel.value!!.type,
                    length = mapController.measureLayerHandler.lineLengthLiveData.value!!,
                )
            )
            val task: TaskBean = liveDataTaskBean.value!!
            task.hadLinkDvoList.add(linkBean)
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                it.copyToRealmOrUpdate(task)
            }
            mapController.lineHandler.addTaskLink(linkBean)
            sharedPreferences.edit().putString(Constant.SHARED_SYNC_TASK_LINK_ID, linkBean.linkPid)
                .apply()
            liveDataFinish.postValue(true)
        }
    }

    /**
     * 监听shared变化
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constant.SELECT_TASK_ID) {
            getTaskBean()
        }
    }

    /**
     * 绘制线的时候回退点
     */
    fun removeLinkLastPoint() {
        mapController.measureLayerHandler.drawLineBackspace()
    }

    /**
     * 清除重绘
     */
    fun clearLink() {
        mapController.measureLayerHandler.clear()
    }
}
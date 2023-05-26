package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.Constant
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject


@HiltViewModel
class TaskViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val mapController: NIMapController
) : ViewModel() {

    /**
     * 用来更新任务列表
     */
    val liveDataTaskList = MutableLiveData<List<TaskBean>>()

    /**
     * 用来更新当前任务
     */
    val liveDataTaskLinks = MutableLiveData<List<HadLinkDvoBean>>()
    private val colors =
        arrayOf(Color.RED, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN)

    /**
     * 当前选中的任务
     */
    private var currentSelectTaskBean: TaskBean? = null


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
                                        task.color = item.color
                                    } else {
                                        if (index < 6)
                                            task.color = colors[index]
                                        else {
                                            val random = Random()
                                            task.color = Color.argb(
                                                255,
                                                random.nextInt(256),
                                                random.nextInt(256),
                                                random.nextInt(256)
                                            )
                                        }
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
            val objects = realm.where(TaskBean::class.java).findAll()
            taskList = realm.copyFromRealm(objects)
            for (item in taskList) {
                FileManager.checkOMDBFileInfo(item)
            }
//            niMapController.lineHandler.omdbTaskLinkLayer.setLineColor(
//                Color.rgb(0, 255, 0).toColor()
//            )
//            taskList.forEach {
//                niMapController.lineHandler.omdbTaskLinkLayer.addLineList(it.hadLinkDvoList)
//            }
//            niMapController.lineHandler.omdbTaskLinkLayer.update()
            liveDataTaskList.postValue(taskList)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                mapController.lineHandler.omdbTaskLinkLayer.removeAll()
//                if(taskList.isNotEmpty()){
//                    mapController.lineHandler.omdbTaskLinkLayer.addLineList(item.hadLinkDvoList)
//                }
//                for (item in taskList) {
//                    mapController.lineHandler.omdbTaskLinkLayer.setLineColor(Color.valueOf(item.color))
//
//                }
//            }
        }
    }

    /**
     * 设置当前选择的任务，并高亮当前任务的所有link
     */
    fun setSelectTaskBean(taskBean: TaskBean) {
        currentSelectTaskBean = taskBean
        liveDataTaskLinks.value = taskBean.hadLinkDvoList
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mapController.lineHandler.omdbTaskLinkLayer.removeAll()
            mapController.lineHandler.omdbTaskLinkLayer.addLineList(taskBean.hadLinkDvoList)
            var maxX = 0.0
            var maxY = 0.0
            var minX = 0.0
            var minY = 0.0
            for (item in taskBean.hadLinkDvoList) {
                val geometry = GeometryTools.createGeometry(item.geometry)
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
            mapController.animationHandler.animateToBox(
                maxX = maxX,
                maxY = maxY,
                minX = minX,
                minY = minY
            )
        }
    }

    /**
     * 高亮当前选中的link
     */
    fun showCurrentLink(link: HadLinkDvoBean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mapController.lineHandler.omdbTaskLinkLayer.showSelectLine(link)
            val geometry = GeometryTools.createGeometry(link.geometry)
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
}

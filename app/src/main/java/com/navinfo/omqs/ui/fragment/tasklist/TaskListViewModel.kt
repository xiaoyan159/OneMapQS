package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.TaskBean
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
class TaskListViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val mapController: NIMapController
) : ViewModel() {

    val liveDataTaskList = MutableLiveData<List<TaskBean>>()

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
                                for (task in list) {
                                    val item = realm.where(TaskBean::class.java).equalTo(
                                        "id", task.id
                                    ).findFirst()
                                    if (item != null) {
                                        task.fileSize = item.fileSize
                                        Log.e("jingo", "当前文件大小 ${task.fileSize}")
                                        task.status = item.status
                                        task.currentSize = item.currentSize
                                        task.color = item.color
                                    } else {
                                        val random = Random()
                                        task.color = Color.argb(
                                            255,
                                            random.nextInt(256),
                                            random.nextInt(256),
                                            random.nextInt(256)
                                        )
                                        Log.e("jingo", "任务颜色 ${task.color}")
                                    }
                                    realm.copyToRealmOrUpdate(task)
                                }
                            }
                            val objects = realm.where(TaskBean::class.java).findAll()
                            taskList = realm.copyFromRealm(objects)

                        }
                    }
                }
                is NetResult.Error -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                is NetResult.Failure -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                is NetResult.Loading -> {}
            }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mapController.lineHandler.omdbTaskLinkLayer.removeAll()
                for (item in taskList) {
                    mapController.lineHandler.omdbTaskLinkLayer.setLineColor(Color.valueOf(item.color))
                    mapController.lineHandler.omdbTaskLinkLayer.addLineList(item.hadLinkDvoList)
                }
            }

        }

    }

}

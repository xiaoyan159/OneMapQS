package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val niMapController: NIMapController
) : ViewModel() {

    val liveDataTaskList = MutableLiveData<List<TaskBean>>()

    fun getTaskList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            var taskList: List<TaskBean> = mutableListOf()
            when (val result = networkService.getTaskList("02911")) {
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
//            niMapController.lineHandler.omdbTaskLinkLayer.setLineColor(Color.rgb(0, 255, 0).toColor())
//            taskList.forEach {
//                niMapController.lineHandler.omdbTaskLinkLayer.addLineList(it.hadLinkDvoList)
//            }
//            niMapController.lineHandler.omdbTaskLinkLayer.update()
            liveDataTaskList.postValue(taskList)
        }

    }

}

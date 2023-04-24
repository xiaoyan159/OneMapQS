package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val networkService: NetworkService
) : ViewModel() {

    val liveDataTaskList = MutableLiveData<List<TaskBean>>()

    fun getTaskList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            Log.e("jingo","realm hashCOde ${realm.hashCode()}")
            when (val result = networkService.getTaskList("02911")) {
                is NetResult.Success -> {
                    if (result.data != null) {
                        realm.executeTransaction {
                            realm.copyToRealmOrUpdate(result.data.obj)
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
                else -> {}
            }
            val objects = realm.where(TaskBean::class.java).findAll()
            val taskList = realm.copyFromRealm(objects)
            for(item in taskList){
                FileManager.checkOMDBFileInfo(item)
            }
            liveDataTaskList.postValue(taskList)
        }

    }

}

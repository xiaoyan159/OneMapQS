package com.navinfo.omqs.ui.fragment.evaluationresult

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.omqs.db.RoomAppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import javax.inject.Inject

@HiltViewModel
class EvaluationResultViewModel @Inject constructor(
    private val roomAppDatabase: RoomAppDatabase,
) : ViewModel() {
    init {
        Log.e("jingo", "EvaluationResultViewModel 创建了 ${hashCode()}")
    }

    override fun onCleared() {
        super.onCleared()
        Log.e("jingo", "EvaluationResultViewModel 销毁了 ${hashCode()}")
    }

    /**
     *  问题分类 liveData
     */

    val classTypeListLiveData = MutableLiveData<List<String>?>()

    /**
     * 问题类型 liveData
     */

    val problemTypeListLiveData = MutableLiveData<List<String>>()

    var currentClassType: String = ""

    /**
     * 查询数据库，获取问题分类
     */
    fun getClassTypeList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = roomAppDatabase.getScProblemTypeDao().findClassTypeList()
            classTypeListLiveData.postValue(list)
        }
    }

    /**
     * 查询问题类型
     */
    fun getProblemTypeList(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            classTypeListLiveData.value?.let {
                if (index < it.size) {
                    currentClassType = it[index]
                    val list =
                        roomAppDatabase.getScProblemTypeDao().findProblemTypeList(currentClassType)
                    problemTypeListLiveData.postValue(list)
                }
            }
        }
    }

    fun getPhenomenonList() {
        viewModelScope.launch (Dispatchers.IO){

        }
    }
}
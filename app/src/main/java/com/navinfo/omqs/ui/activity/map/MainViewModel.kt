package com.navinfo.omqs.ui.activity.map

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.OnQsRecordItemClickListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 创建Activity全局viewmode
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
) : ViewModel() {

    val liveDataQsRecordIdList = MutableLiveData<List<String>>()

    init {
        mapController.layerManagerHandler.setOnQsRecordItemClickListener(object :
            OnQsRecordItemClickListener {
            override fun onQsRecordList(list: MutableList<String>) {
                liveDataQsRecordIdList.value = list
            }
        })
    }

    /**
     * 点击我的位置，回到我的位置
     */
    fun onClickLocationButton() {
        mapController.locationLayerHandler.animateToCurrentPosition()
    }

    override fun onCleared() {
        super.onCleared()
    }
}
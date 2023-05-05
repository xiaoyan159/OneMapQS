package com.navinfo.omqs.ui.fragment.layermanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.tools.LayerConfigUtils
import com.navinfo.omqs.util.FlowEventBus
import kotlinx.coroutines.launch
import java.io.File

class LayerManagerViewModel(): ViewModel() {
    private val gson = Gson()

    fun getLayerConfigList(): List<ImportConfig> {
        // 首先读取Shared文件，如果存在则直接返回，否则读取config文件
        return LayerConfigUtils.getLayerConfigList()
    }

    fun saveLayerConfigList(listData: List<ImportConfig>) {
        SPStaticUtils.put(Constant.EVENT_LAYER_MANAGER_CHANGE, gson.toJson(listData))
        // 发送新的配置数据
        viewModelScope.launch {
            FlowEventBus.post(Constant.EVENT_LAYER_MANAGER_CHANGE, listData)
        }
    }

}
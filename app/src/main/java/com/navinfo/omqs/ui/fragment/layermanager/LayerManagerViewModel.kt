package com.navinfo.omqs.ui.fragment.layermanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.util.FlowEventBus
import kotlinx.coroutines.launch
import java.io.File

class LayerManagerViewModel(): ViewModel() {
    private val omdbConfigFile = File("${Constant.USER_DATA_PATH}", Constant.OMDB_CONFIG)
    private val otherConfigFile = File("${Constant.USER_DATA_PATH}", Constant.OTHER_CONFIG)
    private val gson = Gson()

    fun getLayerConfigList(): List<ImportConfig> {
        // 首先读取Shared文件，如果存在则直接返回，否则读取config文件
        val importConfigList: List<ImportConfig>? = SPStaticUtils.getString(Constant.LAYER_MANAGER_CONFIG, null).run {
            if (this!=null) {
                gson.fromJson(this, object : TypeToken<List<ImportConfig>>(){}.type)
            } else {
                null
            }
        }
        if (importConfigList==null) {
            return getLayerConfigListFromAssetsFile()
        } else {
            return importConfigList as List<ImportConfig>
        }
    }

    fun saveLayerConfigList(listData: List<ImportConfig>) {
        SPStaticUtils.put(Constant.EVENT_LAYER_MANAGER_CHANGE, gson.toJson(listData))
        // 发送新的配置数据
        viewModelScope.launch {
            FlowEventBus.post(Constant.EVENT_LAYER_MANAGER_CHANGE, listData)
        }
    }

    private fun getLayerConfigListFromAssetsFile(): List<ImportConfig> {
        val resultList = mutableListOf<ImportConfig>()
        if (omdbConfigFile.exists()) {
            val omdbConfiStr = FileIOUtils.readFile2String(omdbConfigFile)
            val omdbConfig =  gson.fromJson<ImportConfig>(omdbConfiStr, ImportConfig::class.java)
            resultList.add(omdbConfig)
        }
        if (otherConfigFile.exists()) {
            val otherConfiStr = FileIOUtils.readFile2String(otherConfigFile)
            val otherConfig =  gson.fromJson<ImportConfig>(otherConfiStr, ImportConfig::class.java)
            resultList.add(otherConfig)
        }
        return resultList
    }
}
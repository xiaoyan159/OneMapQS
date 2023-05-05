package com.navinfo.omqs.tools

import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.ImportConfig
import java.io.File

class LayerConfigUtils {
    companion object {
        private val omdbConfigFile = File("${Constant.USER_DATA_PATH}", Constant.OMDB_CONFIG)
        private val otherConfigFile = File("${Constant.USER_DATA_PATH}", Constant.OTHER_CONFIG)
        private val gson = Gson()

        fun getLayerConfigList(): List<ImportConfig> {
            // 首先读取Shared文件，如果存在则直接返回，否则读取config文件
            return SPStaticUtils.getString(Constant.LAYER_MANAGER_CONFIG, null).let {
                if (this!=null) {
                    val result: List<ImportConfig> = gson.fromJson(it, object : TypeToken<List<ImportConfig>>(){}.type)
                    result
                } else {
                    LayerConfigUtils.getLayerConfigListFromAssetsFile()
                }
            }
        }

        fun getLayerConfigListFromAssetsFile(): List<ImportConfig> {
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
}
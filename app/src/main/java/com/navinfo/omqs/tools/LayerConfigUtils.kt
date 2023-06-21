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
            // 首先读取全局变量的数据，如果存在则直接返回，否则读取config文件
            if (Constant.LAYER_CONFIG_LIST == null) {
                Constant.LAYER_CONFIG_LIST = getLayerConfigListFromAssetsFile()
            }
            return Constant.LAYER_CONFIG_LIST!!
//            return SPStaticUtils.getString(Constant.EVENT_LAYER_MANAGER_CHANGE, null).let {
//                if (it != null) {
//                    val result: List<ImportConfig> =
//                        gson.fromJson(it, object : TypeToken<List<ImportConfig>>() {}.type)
//                    result
//                } else {
//                    LayerConfigUtils.getLayerConfigListFromAssetsFile()
//                }
//            }
        }

        private fun getLayerConfigListFromAssetsFile(): List<ImportConfig> {
            val resultList = mutableListOf<ImportConfig>()
            if (omdbConfigFile.exists()) {
                val omdbConfiStr = FileIOUtils.readFile2String(omdbConfigFile)
                val omdbConfig = gson.fromJson<ImportConfig>(omdbConfiStr, ImportConfig::class.java)
                resultList.add(omdbConfig)
            }
            if (otherConfigFile.exists()) {
                val otherConfiStr = FileIOUtils.readFile2String(otherConfigFile)
                val otherConfig =
                    gson.fromJson<ImportConfig>(otherConfiStr, ImportConfig::class.java)
                resultList.add(otherConfig)
            }
            return resultList
        }
    }
}
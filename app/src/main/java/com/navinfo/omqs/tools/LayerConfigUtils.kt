package com.navinfo.omqs.tools

import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.tools.LayerConfigUtils.Companion.gson
import java.io.File

class LayerConfigUtils {
    companion object {
        private val omdbConfigFile = File("${Constant.USER_DATA_PATH}", Constant.OMDB_CONFIG)
        private val gson = Gson()

        fun getLayerConfigList(): List<ImportConfig> {
            // 首先读取全局变量的数据，如果存在则直接返回，否则读取config文件
            if (Constant.LAYER_CONFIG_LIST == null) {
                Constant.LAYER_CONFIG_LIST = getLayerConfigListFromAssetsFile()
            }
            return Constant.LAYER_CONFIG_LIST!!
        }

        private fun getLayerConfigListFromAssetsFile(): List<ImportConfig> {
            val resultList = mutableListOf<ImportConfig>()
            if (omdbConfigFile.exists()) {
                val omdbConfiStr = FileIOUtils.readFile2String(omdbConfigFile)
                val type = object : TypeToken<List<ImportConfig>>() {}.type
                return try {
                    val result = gson.fromJson<List<ImportConfig>>(omdbConfiStr, type)
                    result ?: resultList
                } catch (e: Exception) {
                    resultList
                }
            }
            return resultList
        }
    }
}
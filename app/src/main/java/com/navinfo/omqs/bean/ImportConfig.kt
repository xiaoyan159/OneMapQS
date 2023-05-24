package com.navinfo.omqs.bean

import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.omqs.db.ImportPreProcess
import kotlin.reflect.full.declaredMemberFunctions


class ImportConfig {
    var tableMap: MutableMap<String, TableInfo> = mutableMapOf()
    val tableGroupName: String = "OMDB数据"
    var checked : Boolean = true
    val preProcess: ImportPreProcess = ImportPreProcess()

    fun transformProperties(renderEntity: RenderEntity): RenderEntity {
        val transformList = tableMap[renderEntity.code.toString()]?.transformer
        if (transformList.isNullOrEmpty()) {
            return renderEntity
        }
        for (transform in transformList) {
            // 开始执行转换
            val key:String = transform.k
            val value = transform.v
            val keylib = transform.klib
            val valuelib = transform.vlib
            // 如果key是以_开头的，则预处理时忽略
            if (key.startsWith("_")) {
                continue
            }
            // 如果key和value都为空，说明当前数据需要增加一个新字段
            if (key.isNullOrEmpty()&&value.isNullOrEmpty()&&!renderEntity.properties.containsKey(keylib)) {
                renderEntity.properties[keylib] = valuelib
            }
            // 开始解析key和value，并对数据进行匹配
            m@ for (k in processKeyOrValue(key)) {
                if (renderEntity.properties.containsKey(k)) { // json配置的key可以匹配到数据
                    for (v in processKeyOrValue(value)) {
                        if ("~" == v ) { // ~符可以匹配任意元素
                            if (valuelib.endsWith("()")) { // 以()结尾，说明该value配置是一个function，需要通过反射调用指定方法
                                val method = preProcess::class.declaredMemberFunctions.first { it.name == valuelib.replace("()", "") }
                                method.call(preProcess, renderEntity)
                            } else {
                                renderEntity.properties[keylib] = valuelib
                            }
                            break@m
                        } else if (renderEntity.properties[k] == v) { // 完全匹配
                            if (valuelib.endsWith("()")) { // 以()结尾，说明该value配置是一个function，需要通过反射调用指定方法
                                val method = preProcess::class.declaredMemberFunctions.first { it.name == valuelib.replace("()", "") }
                                method.call(preProcess, renderEntity)
                            } else {
                                renderEntity.properties[keylib] = valuelib
                            }
                            break@m
                        }
                    }
                }
            }
        }
        return renderEntity
    }

    /**
     * 处理配置的key
     */
    private fun processKeyOrValue(key: String): List<String> {
        val keys = key.split("|").filter { it.isNotBlank() }
        return keys
    }
}

class TableInfo {
    val table: String = ""
    val code: Int = 0
    val name: String = ""
    var checked : Boolean = true
    var transformer: MutableList<Transform> = mutableListOf()
}

class Transform {
    var k: String = ""
    var v: String = ""
    var klib: String = ""
    var vlib: String = ""
}
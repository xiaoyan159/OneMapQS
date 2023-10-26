package com.navinfo.omqs.bean

import android.util.Log
import com.google.gson.annotations.Expose
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.omqs.db.ImportPreProcess
import io.realm.Realm
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberFunctions


class ImportConfig {
    @Expose
    var tableMap: MutableMap<String, TableInfo> = mutableMapOf()

    @Expose
    val tableGroupName: String = "OMDB数据"

    @Expose
    var checked: Boolean = true
    val preProcess: ImportPreProcess = ImportPreProcess()
    fun transformProperties(renderEntity: RenderEntity, realm: Realm): RenderEntity? {
        preProcess.realm = realm
        val transformList = tableMap[renderEntity.code.toString()]?.transformer
        if (transformList.isNullOrEmpty()) {
            Log.e("qj", "子表转换为空===${renderEntity.code}")
            return renderEntity
        }
        Log.e("qj", "子表转换不为空===${renderEntity.code}")
        for (transform in transformList) {
            // 开始执行转换
            val key: String = transform.k
            val value = transform.v
            val keylib = transform.klib
            val valuelib = transform.vlib
            // 如果key是以_开头的，则预处理时忽略
            if (key.startsWith("_")) {
                continue
            }
            // 如果key和value都为空，说明当前数据需要增加一个新字段
            if (key.isNullOrEmpty() && value.isNullOrEmpty() && !renderEntity.properties.containsKey(
                    keylib
                )
            ) {
                renderEntity.properties[keylib] = valuelib
                continue
            }
            // 开始解析key和value，并对数据进行匹配
            m@ for (k in processKeyOrValue(key)) {
                if (renderEntity.properties.containsKey(k)) { // json配置的key可以匹配到数据
                    for (v in processKeyOrValue(value)) {
                        if ("~" == v) { // ~符可以匹配任意元素
                            if (valuelib.endsWith(")")) { // 以()结尾，说明该value配置是一个function，需要通过反射调用指定方法
                                // 获取方法名
                                val methodName = valuelib.substringBefore("(")
                                // 获取参数
                                val params: List<String> =
                                    valuelib.substringAfter("(").substringBefore(")").split(",")
                                        .filter { it.isNotEmpty() }.map { it.trim() }
                                val method =
                                    preProcess::class.members.filter { it.name == methodName }
                                        .first() as KFunction<*>

                                val methodParams = method.parameters
                                val callByParams = mutableMapOf<KParameter, Any>(
                                    methodParams[0] to preProcess,
                                    methodParams[1] to renderEntity,
                                )
                                for ((index, value) in params.withIndex()) {
                                    // 前2个参数确定为对象本身和RenderEntity，因此自定义参数从index+2开始设置
                                    if (methodParams.size > index + 2) {
                                        callByParams[methodParams[index + 2]] =
                                            value.replace("'", "")
                                    }
                                }
                                when (val result =
                                    method.callBy(callByParams)) { // 如果方法返回的数据类型是boolean，且返回为false，则该数据不处理
                                    is Boolean ->
                                        if (!result) {
                                            return null
                                        }
                                }
                            } else {
                                renderEntity.properties[keylib] = valuelib
                            }
                            break@m
                        } else if (renderEntity.properties[k] == v) { // 完全匹配
                            if (valuelib.endsWith(")")) { // 以()结尾，说明该value配置是一个function，需要通过反射调用指定方法
                                // 获取方法名
                                val methodName = valuelib.substringBefore("(")
                                // 获取参数
                                val params: List<String> =
                                    valuelib.substringAfter("(").substringBefore(")").split(",")
                                        .filter { it.isNotEmpty() }.map { it.trim() }
                                val method =
                                    preProcess::class.members.filter { it.name == methodName }
                                        .first() as KFunction<*>

                                val methodParams = method.parameters
                                val callByParams = mutableMapOf<KParameter, Any>(
                                    methodParams[0] to preProcess,
                                    methodParams[1] to renderEntity
                                )
                                for ((index, value) in params.withIndex()) {
                                    // 前2个参数确定为对象本身和RenderEntity，因此自定义参数从index+2开始设置
                                    if (methodParams.size > index + 2) {
                                        callByParams[methodParams[index + 2]] = value
                                    }
                                }
                                when (val result = method.callBy(callByParams)) {
                                    is Boolean ->
                                        if (!result) {
                                            return null
                                        }
                                }
                            } else {
                                renderEntity.properties[keylib] = valuelib
                            }
                            break@m
                        }
                    }
                }
            }
        }
        preProcess.realm = null
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
    val zoomMin: Int = 16
    val zoomMax: Int = 21
    val checkLinkId: Boolean = true//是否需要校验linkid
    val filterData: Boolean = false//是否需要过滤数据
    val existSubCode: Boolean = false//是否存在子编码

    val catch: Boolean =
        false//是否需要捕捉 // 需要根据丹丹提供的捕捉原则进行设置，参考文档W行设置条件，https://navinfo.feishu.cn/sheets/shtcnfsxKZhekU26ezBcHgl7aWh?sheet=BZd6yM
    val name: String = ""
    var checked: Boolean = true
    var transformer: MutableList<Transform> = mutableListOf()
    var is3D: Boolean = false // 是否支持3D，默认情况下都不支持3D，在数据导入阶段会自动抹去Z轴高程信息
}

class Transform {
    var k: String = ""
    var v: String = ""
    var klib: String = ""
    var vlib: String = ""
}
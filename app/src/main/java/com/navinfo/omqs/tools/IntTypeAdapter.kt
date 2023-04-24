package com.navinfo.omqs.tools

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class IntTypeAdapter : TypeAdapter<Any>() {
    private val delegate: TypeAdapter<Any> = Gson().getAdapter(Any::class.java)

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Any? {
        val token = `in`.peek()
        when (token) {
            JsonToken.BEGIN_ARRAY -> {
                val list: MutableList<Any?> = ArrayList()
                `in`.beginArray()
                while (`in`.hasNext()) {
                    list.add(read(`in`))
                }
                `in`.endArray()
                return list
            }
            JsonToken.BEGIN_OBJECT -> {
                val map: MutableMap<String, Any?> = LinkedTreeMap()
                `in`.beginObject()
                while (`in`.hasNext()) {
                    map[`in`.nextName()] = read(`in`)
                }
                `in`.endObject()
                return map
            }
            JsonToken.STRING -> return `in`.nextString()
            JsonToken.NUMBER -> {
                // 改写数字的处理逻辑，将数字值分为整型与浮点型。
                val dbNum = `in`.nextDouble()
                // 数字超过long的最大值，返回浮点类型
                if (dbNum > Long.MAX_VALUE) {
                    return dbNum
                }
                // 判断数字是否为整数值
                val lngNum = dbNum.toLong()
                return if (dbNum == lngNum.toDouble()) {
                    try {
                        lngNum.toInt()
                    } catch (e: Exception) {
                        lngNum
                    }
                } else {
                    dbNum
                }
            }
            JsonToken.BOOLEAN -> return `in`.nextBoolean()
            JsonToken.NULL -> {
                `in`.nextNull()
                return null
            }
            else -> throw IllegalStateException()
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Any?) {
        delegate.write(out, value)
    }
}
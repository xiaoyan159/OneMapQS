package com.navinfo.collect.library.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CheckManager")
class CheckManager(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    //检查项类型
    val type: Int,
    //检查项名称
    val tag: String,
    //检查项正则内容
    val regexStr: String

) {
    fun toJson(): String {
        return "{\"id\":$id,\"type\":$type,\"tag\":\"$tag\",\"regexStr\":\"$regexStr\"}"
    }

    fun toMap(): Map<String, *> {
        return return mapOf("id" to id, "type" to type, "tag" to tag, "regexStr" to regexStr)
    }
}
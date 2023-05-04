package com.navinfo.omqs.bean

import java.io.Serializable

class ChatMsgEntity : Serializable, Cloneable {
    var voiceUri //声音存储地址
            : String? = null
    var voiceTimeLong //声音时间长度
            : String? = null
    var name //声音名字
            : String? = null
    var isDelete //是否被删除
            = false

    @kotlin.Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    companion object {
        private val TAG: String = ChatMsgEntity::class.java.getSimpleName()
    }
}

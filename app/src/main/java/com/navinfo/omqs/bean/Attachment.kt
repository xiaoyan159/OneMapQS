package com.navinfo.omqs.bean

import java.io.Serializable
import java.util.*

class Attachment(filename: String, type: Int) : Serializable,
    Cloneable {
    //内容
    var filename: String = ""

    //标识 默认照片0    录音1
    var type: Int

    override fun toString(): String {
        return "TipsAttachment{" +
                "filename='" + filename + '\'' +
                ", type=" + type +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as Attachment
        return type == that.type &&
                filename == that.filename
    }

    override fun hashCode(): Int {
        return Objects.hash(filename, type)
    }

    @kotlin.Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    init {
        this.filename = filename
        this.type = type
    }
}


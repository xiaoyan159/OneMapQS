package com.navinfo.omqs.http

class DefaultTaskResponse<T> {
    var success: Boolean = false
    var msg: String = ""
    var obj: T? = null
}
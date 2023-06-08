package com.navinfo.omqs.http

class DefaultResponse<T> {
    var success: Boolean = false
    var msg: String = ""
    var obj: T? = null
}
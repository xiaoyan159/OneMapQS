package com.navinfo.omqs.http

class DefaultUserResponse<T> {
    var success: Boolean = false
    var msg: String = ""
    var obj: T? = null
}
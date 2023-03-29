package com.navinfo.omqs.ui.activity

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.navinfo.omqs.model.LoginUser

class LoginViewModel : ViewModel() {
    val loginUser: MutableLiveData<LoginUser> = MutableLiveData()

    init {
        loginUser.value = LoginUser(username = "admin", password = "123456")
    }

    /**
     * 处理注册按钮
     */
    fun onClick(view: View) {
        loginUser.value!!.username = "admin2"
        loginUser.postValue(loginUser.value)

    }
}
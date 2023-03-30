package com.navinfo.omqs.ui.activity.login

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.navinfo.omqs.bean.LoginUserBean
import com.navinfo.omqs.bean.OfflineMapCityBean
import io.realm.kotlin.RealmConfiguration

class LoginViewModel : ViewModel() {
    val loginUser: MutableLiveData<LoginUserBean> = MutableLiveData()

    init {
        loginUser.value = LoginUserBean(username = "admin", password = "123456")
    }

    fun initRealm() {
        val config = RealmConfiguration.Builder(schema = setOf(OfflineMapCityBean::class)).directory(

        )
    }

    /**
     * 处理注册按钮
     */
    fun onClick(view: View) {
        loginUser.value!!.username = "admin2"
        loginUser.postValue(loginUser.value)
    }
}
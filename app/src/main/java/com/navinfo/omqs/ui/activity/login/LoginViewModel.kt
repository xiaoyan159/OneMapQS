package com.navinfo.omqs.ui.activity.login

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.LoginUserBean
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.*
import okio.IOException
import java.io.File
import java.math.BigInteger

enum class LoginStatus {
    /**
     * 访问服务器登陆中
     */
    LOGIN_STATUS_NET_LOADING,

    /**
     * 初始化文件夹
     */
    LOGIN_STATUS_FOLDER_INIT,

    /**
     * 创建文件夹失败
     */
    LOGIN_STATUS_FOLDER_FAILURE,

    /**
     * 网络访问失败
     */
    LOGIN_STATUS_NET_FAILURE,

    /**
     * 成功
     */
    LOGIN_STATUS_SUCCESS,

    /**
     * 取消
     */
    LOGIN_STATUS_CANCEL,
}

class LoginViewModel(
) : ViewModel() {
    //用户信息
    val loginUser: MutableLiveData<LoginUserBean> = MutableLiveData()

    //是不是登录成功
    val loginStatus: MutableLiveData<LoginStatus> = MutableLiveData()

    var jobLogin: Job? = null;

    init {
        loginUser.value = LoginUserBean(username = "admin", password = "123456")
    }

    private fun initRealm() {
        val password = "password".encodeToByteArray().copyInto(ByteArray(64))
        // 1110000011000010111001101110011011101110110111101110010011001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        Log.d("", "密码是： ${BigInteger(1, password).toString(2).padStart(64, '0')}")
        val config = RealmConfiguration.Builder()
            .directory(File(Constant.DATA_PATH))
            .name("HDData")
//            .encryptionKey(password)
            .build()
        Constant.realm = Realm.getInstance(config)
    }

    /**
     * 处理注册按钮
     */
    fun onClick(view: View) {
        loginUser.value!!.username = "admin2"
        loginUser.value = loginUser.value
    }

    /**
     * 点击
     */
    fun onClickLoginBtn(context: Context, userName: String, password: String) {
        if (userName.isEmpty()) {
            Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
        }
        //不指定IO，会在主线程里运行
        jobLogin = viewModelScope.launch(Dispatchers.IO) {
            loginCheck(context, userName, password)
        }
    }

    /**
     * 如果不用挂起函数的方式，直接把下面这段代码替换到上面，在delay之后，线程和delay之前不是同一个，有啥影响未知。。。
     */

    private suspend fun loginCheck(context: Context, userName: String, password: String) {
        //上面调用了线程切换，这里不用调用，即使调用了还是在同一个线程中，除非自定义协程域？（待验证）
//        withContext(Dispatchers.IO) {
        //网络访问
        loginStatus.postValue(LoginStatus.LOGIN_STATUS_NET_LOADING)
        //假装网络访问，等待2秒
        delay(1000)
        //文件夹初始化
        try {
            loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_INIT)
            createRootFolder(context)
            // 初始化Realm
            initRealm()
        } catch (e: IOException) {
            loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_FAILURE)
        }
        //假装解压文件等
        delay(1000)
        loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)

//        }
    }


    @Throws(IOException::class)
    private fun createRootFolder(context: Context) {
        // 在SD卡创建项目目录
        val sdCardPath = context.getExternalFilesDir(null)
        sdCardPath?.let {
            Constant.ROOT_PATH = sdCardPath.absolutePath
            Constant.MAP_PATH = Constant.ROOT_PATH + "/map/"
            Constant.OFFLINE_MAP_PATH = Constant.MAP_PATH + "offline/"
            val file = File(Constant.MAP_PATH)
            if (!file.exists()) {
                file.mkdirs()
            Constant.DATA_PATH = Constant.ROOT_PATH + "/data/"
            with(File(Constant.MAP_PATH)) {
                if(!this.exists()) this.mkdirs()
            }
            with(File(Constant.DATA_PATH)) {
                if(!this.exists()) this.mkdirs()
            }
        }
    }

    /**
     * 取消登录
     */
    fun cancelLogin() {
        Log.e("jingo", "取消了？${Thread.currentThread().name}")
        jobLogin?.let {
            it.cancel()
            loginStatus.value = LoginStatus.LOGIN_STATUS_CANCEL
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelLogin()
    }


}
package com.navinfo.omqs.ui.activity.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ResourceUtils
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.LoginUserBean
import com.navinfo.omqs.bean.SysUserBean
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.http.DefaultResponse
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.NetUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import javax.inject.Inject

enum class LoginStatus {
    /**
     * 访问服务器登陆中
     */
    LOGIN_STATUS_NET_LOADING,

    /**
     * 访问离线地图列表
     */
    LOGIN_STATUS_NET_OFFLINE_MAP,

    /**
     * 访问任务列表
     */
    LOGIN_STATUS_NET_GET_TASK_LIST,

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

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val roomAppDatabase: RoomAppDatabase
) : ViewModel() {
    //用户信息
    val loginUser: MutableLiveData<LoginUserBean> = MutableLiveData()

    //是不是登录成功
    val loginStatus: MutableLiveData<LoginStatus> = MutableLiveData()

    var jobLogin: Job? = null

    var sharedPreferences: SharedPreferences? = null

    init {
        loginUser.value = LoginUserBean(userCode = "haofuyue00213", passWord = "123456")
    }


    /**
     * 处理注册按钮
     */
    fun onClick(view: View) {
        loginUser.value!!.userCode = "admin2"
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
        sharedPreferences =
            context.getSharedPreferences("USER_SHAREDPREFERENCES", Context.MODE_PRIVATE)
        val userNameCache = sharedPreferences?.getString("userName", null)
        val passwordCache = sharedPreferences?.getString("passWord", null)
        val userCodeCache = sharedPreferences?.getString("userCode", null)
        val userRealName = sharedPreferences?.getString("userRealName", null)
        //增加缓存记录，不用每次连接网络登录
        if (userNameCache != null && passwordCache != null && userCodeCache != null && userRealName != null) {
            if (userNameCache == userName && passwordCache == password) {
                viewModelScope.launch(Dispatchers.IO) {
                    createUserFolder(context, userCodeCache, userRealName)
                    getOfflineCityList(context)
//                    loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
                }
                return
            }
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
        var userCode = "99999";
        var userRealName = "";
        //登录访问
        when (val result = networkService.loginUser(LoginUserBean(userName, password))) {
            is NetResult.Success<*> -> {
                if (result.data != null) {
                    try {
                        val defaultUserResponse = result.data as DefaultResponse<SysUserBean>
                        if (defaultUserResponse.success) {
                            if (defaultUserResponse.obj == null || defaultUserResponse.obj!!.userCode == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "服务返回用户Code信息错误",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                                loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                                return
                            } else {
                                userCode = defaultUserResponse.obj?.userCode.toString()
                                userRealName = defaultUserResponse.obj?.userName.toString()
                                folderInit(
                                    context = context,
                                    userName = userName,
                                    password = password,
                                    userCode = userCode,
                                    userRealName = userRealName
                                )
                                getOfflineCityList(context)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${defaultUserResponse.msg}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                            loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                            return
                        }

                    } catch (e: IOException) {
                        loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_FAILURE)
                    }
                }
            }

            is NetResult.Error<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                return
            }

            is NetResult.Failure<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                return
            }

            else -> {}
        }

    }

    /**
     * 获取离线地图
     */
    private suspend fun getOfflineCityList(context: Context) {

        loginStatus.postValue(LoginStatus.LOGIN_STATUS_NET_OFFLINE_MAP)
        when (val result = networkService.getOfflineMapCityList()) {
            is NetResult.Success -> {

                if (result.data != null) {
                    for (cityBean in result.data) {
                        FileManager.checkOfflineMapFileInfo(cityBean)
                    }
                    roomAppDatabase.getOfflineMapDao().insertOrUpdate(result.data)
                }
                getTaskList(context)
            }

            is NetResult.Error<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                getTaskList(context)
            }

            is NetResult.Failure<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                getTaskList(context)
            }

            is NetResult.Loading -> {}
        }

    }

    /**
     * 获取任务列表
     */
    private suspend fun getTaskList(context: Context) {
        loginStatus.postValue(LoginStatus.LOGIN_STATUS_NET_GET_TASK_LIST)
        when (val result = networkService.getTaskList(Constant.USER_ID)) {
            is NetResult.Success -> {
                if (result.data != null) {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        result.data.obj?.let { list ->
                            for (index in list.indices) {
                                val task = list[index]
                                val item = realm.where(TaskBean::class.java).equalTo(
                                    "id", task.id
                                ).findFirst()
                                if (item != null) {
                                    task.fileSize = item.fileSize
                                    task.status = item.status
                                    task.currentSize = item.currentSize
                                    task.hadLinkDvoList = item.hadLinkDvoList
                                    //已上传后不在更新操作时间
                                    if (task.syncStatus != FileManager.Companion.FileUploadStatus.DONE) {
                                        //赋值时间，用于查询过滤
                                        task.operationTime = DateTimeUtil.getNowDate().time
                                    }
                                } else {
                                    for (hadLink in task.hadLinkDvoList) {
                                        hadLink.taskId = task.id
                                    }
                                    //赋值时间，用于查询过滤
                                    task.operationTime = DateTimeUtil.getNowDate().time
                                }
                                realm.copyToRealmOrUpdate(task)
                            }
                        }

                    }
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
            }

            is NetResult.Error<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
            }

            is NetResult.Failure<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
            }

            is NetResult.Loading -> {}
        }
    }

    /**
     * 初始化文件夹
     */
    private fun folderInit(
        context: Context,
        userName: String,
        password: String,
        userCode: String,
        userRealName: String
    ) {
        //文件夹初始化
        try {
            loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_INIT)
            sharedPreferences?.edit()?.putString("userName", userName)?.commit()
            sharedPreferences?.edit()?.putString("passWord", password)?.commit()
            sharedPreferences?.edit()?.putString("userCode", userCode)?.commit()
            sharedPreferences?.edit()?.putString("userRealName", userRealName)?.commit()

            createUserFolder(context, userCode, userRealName)
        } catch (e: IOException) {
            loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_FAILURE)
        }
    }

    /**
     * 创建用户目录
     */
    private fun createUserFolder(context: Context, userId: String, userRealName: String) {
        Constant.IS_VIDEO_SPEED = false
        Constant.USER_ID = userId
        Constant.USER_REAL_NAME = userRealName
        Constant.VERSION_ID = userId
        Constant.USER_DATA_PATH = Constant.DATA_PATH + Constant.USER_ID + "/" + Constant.VERSION_ID
        Constant.USER_DATA_ATTACHEMNT_PATH = Constant.USER_DATA_PATH + "/attachment/"
        // 在SD卡创建用户目录，解压资源等
        val userFolder = File(Constant.USER_DATA_PATH)
        if (!userFolder.exists()) userFolder.mkdirs()
        //创建附件目录
        val userAttachmentFolder = File(Constant.USER_DATA_ATTACHEMNT_PATH)
        if (!userAttachmentFolder.exists()) userAttachmentFolder.mkdirs()
        // 初始化Realm
        Realm.init(context.applicationContext)
        val password = "encryp".encodeToByteArray().copyInto(ByteArray(64))
        // 656e6372797000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        Log.d("OMQSApplication", "密码是： ${byteArrayToHexString(password)}")
        val config = RealmConfiguration.Builder()
            .directory(userFolder)
            .name("OMQS.realm")
            .encryptionKey(password)
            .allowQueriesOnUiThread(true)
//            .modules(Realm.getDefaultModule(), MyRealmModule())
            .schemaVersion(2)
            .build()
        Realm.setDefaultConfiguration(config)
        // 拷贝配置文件到用户目录下
        val omdbConfigFile = File(userFolder.absolutePath, Constant.OMDB_CONFIG);
//        if (!omdbConfigFile.exists()) {
        ResourceUtils.copyFileFromAssets(Constant.OMDB_CONFIG, omdbConfigFile.absolutePath)
//        }
    }

    /**
     * 取消登录
     */
    fun cancelLogin() {
        jobLogin?.let {
            it.cancel()
            loginStatus.value = LoginStatus.LOGIN_STATUS_CANCEL
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelLogin()
    }

    private fun byteArrayToHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") { "%02x".format(it) }
    }
}
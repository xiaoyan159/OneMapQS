package com.navinfo.omqs.ui.activity.scan

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.omqs.bean.QRCodeBean
import com.navinfo.omqs.bean.SysUserBean
import com.navinfo.omqs.http.DefaultResponse
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

enum class QRCodeStatus {
    /**
     * 访问服务器登陆中
     */
    LOGIN_STATUS_NET_LOADING,

    /**
     * 访问离线地图列表
     */
    LOGIN_STATUS_NET_OFFLINE_MAP,

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
class QRCodeViewModel @Inject constructor(
    private val networkService: NetworkService
) : ViewModel() {
    //用户信息
    val qrCodeBean: MutableLiveData<QRCodeBean> = MutableLiveData()

    //是不是连接成功
    val qrCodeStatus: MutableLiveData<QRCodeStatus> = MutableLiveData()

    var jobQRCodeStatus: Job? = null;

    init {
        qrCodeBean.value = QRCodeBean()
    }


    /**
     * 扫一扫按钮
     */
    fun connect(context: Context, ips: String) {

        if (TextUtils.isEmpty(ips)) {
            Toast.makeText(context, "获取ip失败！", Toast.LENGTH_LONG).show()
            return
        }

        val ipArray = ips.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        //测试代码
        //final String[] ipArray = new String[]{"172.21.2.137"};
        if (ipArray.isEmpty()) {
            Toast.makeText(context, "获取ip失败！", Toast.LENGTH_SHORT).show()
            return
        }

        ipArray.forEach { ip ->
            if (!TextUtils.isEmpty(ip)) {
                viewModelScope.launch(Dispatchers.Default) {
                    val ipTemp: String = ip
                    val url = "http://$ipTemp:8080/sensor/service/keepalive"
                    when (val result = networkService.connectIndoorTools(url)) {
                        is NetResult.Success<*> -> {
                            if (result.data != null) {
                                try {
                                    val defaultUserResponse =
                                        result.data as DefaultResponse<SysUserBean>
                                    if (defaultUserResponse.success) {

                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "${defaultUserResponse.msg}",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }

                                } catch (e: IOException) {
                                }
                            }
                        }

                        is NetResult.Error<*> -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${result.exception.message}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }

                        is NetResult.Failure<*> -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${result.code}:${result.msg}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
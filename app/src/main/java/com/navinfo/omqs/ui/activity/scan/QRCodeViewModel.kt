package com.navinfo.omqs.ui.activity.scan

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.IndoorConnectionInfoBean
import com.navinfo.omqs.bean.QRCodeBean
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

enum class QrCodeStatus {
    /**
     * 网络访问失败
     */
    QR_CODE_STATUS_NET_FAILURE,

    /**
     * 成功
     */
    QR_CODE_STATUS_SUCCESS,

    /**
     * 信息更新成功
     */
    QR_CODE_STATUS_SERVER_INFO_SUCCESS,

}

@HiltViewModel
class QrCodeViewModel @Inject constructor(
    private val networkService: NetworkService
) : ViewModel() {
    //用户信息
    val qrCodeBean: MutableLiveData<QRCodeBean> = MutableLiveData()

    //是不是连接成功
    val qrCodeStatus: MutableLiveData<QrCodeStatus> = MutableLiveData()


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

                                    val defaultUserResponse = result.data as QRCodeBean

                                    if (defaultUserResponse.errcode == 0) {

                                        Constant.INDOOR_IP = ipTemp

                                        qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_SUCCESS)

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "连接室内整理工具成功。",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        updateServerInfo(context)

                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "${defaultUserResponse.errmsg}",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }

                                } catch (e: IOException) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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
                            qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_NET_FAILURE)
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
                            qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_NET_FAILURE)
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * 扫一扫按钮
     */
    fun updateServerInfo(context: Context) {

        if (TextUtils.isEmpty(Constant.INDOOR_IP)) {
            Toast.makeText(context, "获取ip失败！", Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val url = "http://${Constant.INDOOR_IP}:8080/sensor/service/connection"


            val indoorConnectionInfoBean = IndoorConnectionInfoBean(
                Constant.USER_ID,
                Constant.USER_ID,
                Constant.USER_ID,
                "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2ODk2MjI5MjQsInVzZXJJZCI6IjEwNCIsImlhdCI6MTY4OTU3MjUyNCwidXNlcm5hbWUiOiJ3ZWl3ZWlsaW4wMDEwNCJ9.9WUqOhme8Yi_2xRBKMMe0ihb_yR1uwTqWTdZfZ7dMtE",
                "http://fastmap.navinfo.com/onemap",
                Constant.USER_ID,
                "Android"
            )
            when (val result = networkService.updateServerInfo(
                url = url,
                indoorConnectionInfoBean = indoorConnectionInfoBean
            )) {
                is NetResult.Success<*> -> {

                    if (result.data != null) {
                        try {

                            val defaultUserResponse = result.data as QRCodeBean

                            if (defaultUserResponse.errcode == 0) {

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "信息更新成功。",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_SERVER_INFO_SUCCESS)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "${defaultUserResponse.errmsg}",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }

                        } catch (e: IOException) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                    qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_NET_FAILURE)
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
                    qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_NET_FAILURE)
                }

                else -> {}
            }

        }

    }


    override fun onCleared() {
        super.onCleared()
    }

}
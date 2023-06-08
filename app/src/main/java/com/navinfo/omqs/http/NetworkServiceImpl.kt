package com.navinfo.omqs.http

import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.bean.LoginUserBean
import com.navinfo.omqs.bean.SysUserBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

/**
 * 网络访问业务接口的具体实现
 */
class NetworkServiceImpl @Inject constructor(
    private val netApi: RetrofitNetworkServiceAPI,
) : NetworkService {
    /**
     * 获取离线地图城市列表
     */
    override suspend fun getOfflineMapCityList(): NetResult<List<OfflineMapCityBean>> =
        //在IO线程中运行
        withContext(Dispatchers.IO) {
            return@withContext try {
                val result = netApi.retrofitGetOfflineMapCityList()
                if (result.isSuccessful) {
                    if (result.code() == 200) {
                        NetResult.Success(result.body())
                    } else {
                        NetResult.Failure<Any>(result.code(), result.message())
                    }
                } else {
                    NetResult.Failure<Any>(result.code(), result.message())
                }
            } catch (e: Exception) {
                NetResult.Error<Any>(e)
            }
        }

    override suspend fun getTaskList(evaluatorNo: String): NetResult<DefaultResponse<List<TaskBean>>> =
        //在IO线程中运行
        withContext(Dispatchers.IO) {
            return@withContext try {
                val result = netApi.retrofitGetTaskList(evaluatorNo)
                if (result.isSuccessful) {
                    if (result.code() == 200) {
                        NetResult.Success(result.body())
                    } else {
                        NetResult.Failure<Any>(result.code(), result.message())
                    }
                } else {
                    NetResult.Failure<Any>(result.code(), result.message())
                }
            } catch (e: Exception) {
                NetResult.Error<Any>(e)
            }
        }

    override suspend fun loginUser(loginUserBean: LoginUserBean): NetResult<DefaultResponse<SysUserBean>> =
        //在IO线程中运行
        withContext(Dispatchers.IO) {
            return@withContext try {
                val result = netApi.retrofitLoginUser(loginUserBean)
                if (result.isSuccessful) {
                    if (result.code() == 200) {
                        NetResult.Success(result.body())
                    } else {
                        NetResult.Failure<Any>(result.code(), result.message())
                    }
                } else {
                    NetResult.Failure<Any>(result.code(), result.message())
                }
            } catch (e: Exception) {
                NetResult.Error<Any>(e)
            }
        }
}
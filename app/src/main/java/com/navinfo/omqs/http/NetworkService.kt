package com.navinfo.omqs.http

import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.bean.IndoorConnectionInfoBean
import com.navinfo.omqs.bean.LoginUserBean
import com.navinfo.omqs.bean.QRCodeBean
import com.navinfo.omqs.bean.SysUserBean
import com.navinfo.omqs.bean.TraceVideoBean


/**
 * 网络访问 业务接口
 */
interface NetworkService {
    /**
     * 获取离线地图城市列表
     */
    suspend fun getOfflineMapCityList(): NetResult<List<OfflineMapCityBean>>

    /**
     * 获取任务列表
     */
    suspend fun getTaskList(evaluatorNo: String): NetResult<DefaultResponse<List<TaskBean>>>

    /**
     * 登录接口
     */
    suspend fun loginUser(loginUserBean: LoginUserBean): NetResult<DefaultResponse<SysUserBean>>

    /**
     * 连接室内整理工具
     */
    suspend fun connectIndoorTools(url: String): NetResult<QRCodeBean>

    /**
     * 更新用户信息
     */
    suspend fun updateServerInfo(url: String,indoorConnectionInfoBean: IndoorConnectionInfoBean): NetResult<QRCodeBean>

    /**
     * 设置轨迹对应的视频
     */
    suspend fun sendServerCommand(url: String,traceVideoBean: TraceVideoBean): NetResult<QRCodeBean>
}
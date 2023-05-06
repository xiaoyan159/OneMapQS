package com.navinfo.omqs.http

import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.collect.library.data.entity.TaskBean


/**
 * 网络访问 业务接口
 */
interface NetworkService {
    /**
     * 获取离线地图城市列表
     */
    suspend fun getOfflineMapCityList():NetResult<List<OfflineMapCityBean>>
    /**
     * 获取任务列表
     */
    suspend fun getTaskList(evaluatorNo:String): NetResult<DefaultTaskResponse<List<TaskBean>>>
}
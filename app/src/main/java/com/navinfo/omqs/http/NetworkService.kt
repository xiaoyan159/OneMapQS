package com.navinfo.omqs.http

import com.navinfo.omqs.bean.OfflineMapCityBean


/**
 * 网络访问 业务接口
 */
interface NetworkService {
    /**
     * 获取离线地图城市列表
     */
    suspend fun getOfflineMapCityList():NetResult<List<OfflineMapCityBean>>
}
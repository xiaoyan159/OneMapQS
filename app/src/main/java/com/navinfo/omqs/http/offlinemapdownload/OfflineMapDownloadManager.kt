package com.navinfo.omqs.http.offlinemapdownload

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.OfflineMapCityBean
import dagger.hilt.android.qualifiers.ActivityContext
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * 管理离线地图下载
 */
class OfflineMapDownloadManager @Inject constructor(@ActivityContext context: Context) {
    /**
     * 最多同时下载数量
     */
    private val MAX_SCOPE = 5

    /**
     * 存储有哪些城市需要下载
     */
    private val scopeMap: ConcurrentHashMap<String, OfflineMapDownloadScope> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<String, OfflineMapDownloadScope>()
    }

    val downloadFolder: String? by lazy {
        Constant.MAP_PATH + "/offline/"
    }


    /**
     * 请求一个下载任务[OfflineMapDownloadScope]
     * 这是创建[OfflineMapDownloadScope]的唯一途径,请不要通过其他方式创建[OfflineMapDownloadScope]
     */
    fun request(cityBean: OfflineMapCityBean): OfflineMapDownloadScope? {
        //没有下载连接的不能下载
        if (TextUtils.isEmpty(cityBean.url)) return null
//        if(scopeMap.containsKey())
        var downloadScope = scopeMap[cityBean.id]
        if (downloadScope == null) {
            scopeMap[cityBean.id] = OfflineMapDownloadScope(cityBean)
        }
        return downloadScope
    }
}
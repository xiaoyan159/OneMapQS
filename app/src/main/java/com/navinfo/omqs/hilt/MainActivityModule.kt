package com.navinfo.omqs.hilt

import android.content.Context
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityRetainedScoped

@InstallIn(ActivityRetainedComponent::class)
@Module
class MainActivityModule {

    /**
     * 注入地图控制器，在activity范围内使用，单例
     */
    @ActivityRetainedScoped
    @Provides
    fun providesMapController(): NIMapController = NIMapController()

    /**
     * 注入离线地图下载管理，在activity范围内使用，单例
     */
    @ActivityRetainedScoped
    @Provides
    fun providesOfflineMapDownloadManager(
        networkServiceAPI: RetrofitNetworkServiceAPI
    ): OfflineMapDownloadManager =
        OfflineMapDownloadManager( networkServiceAPI)

    /**
     * 实验失败，这样创建，viewmodel不会在activity销毁的时候同时销毁
     */
//    @ActivityRetainedScoped
//    @Provides
//    fun providesMainViewModel(mapController: NIMapController): MainViewModel {
//        return MainViewModel(mapController)
//    }

}
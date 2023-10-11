package com.navinfo.omqs.hilt

import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.http.taskupload.TaskUploadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@InstallIn(ActivityRetainedComponent::class)
@Module
class ActivityModule {

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
        networkServiceAPI: RetrofitNetworkServiceAPI,
        roomAppDatabase: RoomAppDatabase,
        mapController: NIMapController
    ): OfflineMapDownloadManager =
        OfflineMapDownloadManager(networkServiceAPI, roomAppDatabase, mapController)

    /**
     * 注入任务下载
     */
    @ActivityRetainedScoped
    @Provides
    fun providesTaskListDownloadManager(
        networkServiceAPI: RetrofitNetworkServiceAPI,
        importFactory: ImportOMDBHiltFactory,
        mapController: NIMapController,
        realmOperateHelper: RealmOperateHelper,
    ): TaskDownloadManager =
        TaskDownloadManager(importFactory, networkServiceAPI, mapController, realmOperateHelper)

    /**
     * 注入任务下载
     */
    @ActivityRetainedScoped
    @Provides
    fun providesTaskListUploadManager(
        networkServiceAPI: RetrofitNetworkServiceAPI,
        realmOperateHelper: RealmOperateHelper,
    ): TaskUploadManager =
        TaskUploadManager(networkServiceAPI, realmOperateHelper)

    /**
     * 实验失败，这样创建，viewmodel不会在activity销毁的时候同时销毁
     * 4-14:因为没有传入activity的 owner,无法检测生命周期，
     */
//    @ActivityRetainedScoped
//    @Provides
//    fun providesMainViewModel(mapController: NIMapController): MainViewModel {
//        return MainViewModel(mapController)
//    }

    @ActivityRetainedScoped
    @Provides
    fun providesRealmOperateHelper(): RealmOperateHelper {
        return RealmOperateHelper()
    }
}
package com.navinfo.omqs.hilt

import android.util.Log
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.ui.activity.map.MainViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@InstallIn(ActivityRetainedComponent::class)
@Module
class MainActivityModule {

    @ActivityRetainedScoped
    @Provides
    fun providesMapController(): NIMapController = NIMapController()

    /**
     * 实验失败，这样创建，viewmodel不会在activity销毁的时候同时销毁
     */
//    @ActivityRetainedScoped
//    @Provides
//    fun providesMainViewModel(mapController: NIMapController): MainViewModel {
//        Log.e("jingo", "MainViewModel 被创建")
//        return MainViewModel(mapController)
//    }

}
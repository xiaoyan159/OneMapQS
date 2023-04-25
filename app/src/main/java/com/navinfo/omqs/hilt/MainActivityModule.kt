package com.navinfo.omqs.hilt

import android.content.Context
import com.navinfo.collect.library.map.NIMapController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
class MainActivityModule {
    /**
     * 注入地图控制器，在activity范围内使用，单例
     */
    @ActivityScoped
    @Provides
    fun providesContext(@ActivityContext context: Context): Context = context
}
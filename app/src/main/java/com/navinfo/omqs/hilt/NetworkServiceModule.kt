package com.navinfo.omqs.hilt

import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.http.NetworkServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class NetworkServiceModule {

    @Binds
    abstract fun bindNetworkService(networkServiceImpl: NetworkServiceImpl): NetworkService
}
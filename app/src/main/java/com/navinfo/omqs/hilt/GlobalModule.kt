package com.navinfo.omqs.hilt

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import com.navinfo.omqs.Constant
import com.navinfo.omqs.OMQSApplication
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import com.navinfo.omqs.tools.RealmCoroutineScope
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 全局单例 注入对象
 */
@Module
@InstallIn(SingletonComponent::class)
class GlobalModule {

    @Singleton
    @Provides
    fun provideApplication(application: Application): OMQSApplication {
        return application as OMQSApplication
    }

    /**
     * 注入 网络OKHttp 对象
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(interceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder().addInterceptor(interceptor)
            .connectTimeout(60, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES).build()
    }

    /**
     * 注入 OHHttp log
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor {
            if (Constant.DEBUG) {
                Log.e("jingo", it)
            }
        }.apply {
            level = if (Constant.DEBUG) {
                //坑 ！！！！ 下载文件时打印log 内存不足
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }


    @Provides
    @Singleton
    fun provideRetrofit(
        client: Lazy<OkHttpClient>,
        converterFactory: GsonConverterFactory,
    ): Retrofit {
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(Constant.SERVER_ADDRESS)
            .client(client.get())
            .addConverterFactory(converterFactory)

        return retrofitBuilder.build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory {
        return GsonConverterFactory.create(gson)
    }

    @Provides
    @Singleton
    fun provideNetworkService(retrofit: Retrofit): RetrofitNetworkServiceAPI {
        return retrofit.create(RetrofitNetworkServiceAPI::class.java)
    }

    /**
     * realm 注册
     */
    @Provides
    @Singleton
    fun provideRealmService(context: Application): RealmCoroutineScope {
        return RealmCoroutineScope(context)
    }
}
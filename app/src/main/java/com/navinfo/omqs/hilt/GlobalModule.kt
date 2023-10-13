package com.navinfo.omqs.hilt

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.system.Constant.SERVER_ADDRESS
import com.navinfo.omqs.Constant
import com.navinfo.omqs.OMQSApplication
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import com.navinfo.omqs.tools.IntTypeAdapter
import com.tencent.wcdb.database.SQLiteCipherSpec
import com.tencent.wcdb.room.db.WCDBOpenHelperFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.realm.Realm
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
    fun provideApplication(@ApplicationContext application: Application): OMQSApplication {
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
                HttpLoggingInterceptor.Level.BASIC
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
            .baseUrl(SERVER_ADDRESS)
            .client(client.get())
            .addConverterFactory(converterFactory)

        return retrofitBuilder.build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        // 解决解析Json时将int类型自动转换为Double的问题
        .registerTypeAdapter(object : TypeToken<Map<String, Any?>>() {}.type, IntTypeAdapter())
        .registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, IntTypeAdapter())
        .registerTypeAdapter(object : TypeToken<Map<Any, Any>>() {}.type, IntTypeAdapter())
//        .registerTypeAdapter(Call::class.java, object : TypeToken<Call<*>>(){
//
//        })
        .create()

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

    @Singleton
    @Provides
    fun provideDatabase(context: Application): RoomAppDatabase {
        val DB_PASSWORD = "123456";
        val cipherSpec = SQLiteCipherSpec()
            .setPageSize(1024)
            .setSQLCipherVersion(3)
        val factory = WCDBOpenHelperFactory()
//            .passphrase(DB_PASSWORD.toByteArray())  // passphrase to the database, remove this line for plain-text
            .cipherSpec(cipherSpec)               // cipher to use, remove for default settings
            .writeAheadLoggingEnabled(true)       // enable WAL mode, remove if not needed
            .asyncCheckpointEnabled(true);            // enable asynchronous checkpoint, remove if not needed

        return Room.databaseBuilder(
            context,
            RoomAppDatabase::class.java,
            "${Constant.DATA_PATH}/omqs.db"
        )

            // [WCDB] Specify open helper to use WCDB database implementation instead
            // of the Android framework.
            .openHelperFactory(factory)

            // Wipes and rebuilds instead of migrating if no Migration object.
            // Migration is not part of this codelab.
//            .fallbackToDestructiveMigration().addCallback(sRoomDatabaseCallback)
            .build();
    }

    @Singleton
    @Provides
    fun provideTraceDatabase(context: Application): TraceDataBase {
        return TraceDataBase.getDatabase(
            context,
            Constant.USER_DATA_PATH + "/trace.sqlite"
        )
    }

//    /**
//     * realm 注册
//     */
//    @Provides
//    @Singleton
//    fun provideRealmService(context: Application): RealmCoroutineScope {
//        return RealmCoroutineScope(context)
//    }

//    @Singleton
//    @Provides
//    fun provideRealmDefaultInstance(): Realm {
//        return Realm.getDefaultInstance()
//    }

    @Singleton
    @Provides
    fun provideSharedPreferences(context: Application): SharedPreferences {
        return context.getSharedPreferences("Shared" + Constant.USER_ID, Context.MODE_PRIVATE)
    }
}
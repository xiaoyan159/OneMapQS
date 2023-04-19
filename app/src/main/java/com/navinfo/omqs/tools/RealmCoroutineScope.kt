//package com.navinfo.omqs.tools
//
//import android.app.Application
//import com.navinfo.collect.library.data.entity.OfflineMapCityBean
//import com.navinfo.omqs.Constant
//import io.realm.Realm
//import io.realm.RealmConfiguration
//import io.realm.RealmModel
//import io.realm.Sort
//import io.realm.kotlin.where
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.newSingleThreadContext
//import java.io.File
//
//class RealmCoroutineScope(context: Application) :
//    CoroutineScope by CoroutineScope(newSingleThreadContext("RealmThread")) {
//    lateinit var realm: Realm
//
//    init {
//        launch {
//            realm = Realm.getDefaultInstance()
//        }
//    }
//
//    suspend fun getOfflineCityList(): List<OfflineMapCityBean> {
//        var list: List<OfflineMapCityBean> = mutableListOf()
//        realm.executeTransaction {
//            val objects = realm.where<OfflineMapCityBean>().findAll().sort("id", Sort.ASCENDING)
//            list = realm.copyFromRealm(objects)
//        }
//        return list
//    }
//
//    suspend fun insertOrUpdate(objects: Collection<RealmModel?>?) {
//        realm.executeTransaction {
//            realm.insertOrUpdate(objects)
//        }
//    }
//
//    suspend fun insertOrUpdate(realmModel: RealmModel?) {
//        realm.executeTransaction {
//            realm.insertOrUpdate(realmModel)
//        }
//    }
//
//}
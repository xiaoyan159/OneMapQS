package com.navinfo.omqs.tools

import android.app.Application
import com.navinfo.collect.library.data.entity.OfflineMapCityBean
import com.navinfo.omqs.Constant
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmModel
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.File

class RealmCoroutineScope(context: Application) :
    CoroutineScope by CoroutineScope(newSingleThreadContext("RealmThread")) {
    lateinit var realm: Realm

    init {
        launch {
            Realm.init(context)
            val password = "password".encodeToByteArray().copyInto(ByteArray(64))
            // 1110000011000010111001101110011011101110110111101110010011001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
//        Log.d("", "密码是： ${BigInteger(1, password).toString(2).padStart(64, '0')}")
            val config = RealmConfiguration.Builder()
                .directory(File(Constant.DATA_PATH))
                .name("HDData")
//            .encryptionKey(password)
                .build()
            Realm.setDefaultConfiguration(config)
            realm = Realm.getDefaultInstance()
        }
    }

    suspend fun getOfflineCityList(): List<OfflineMapCityBean> {
        var list: List<OfflineMapCityBean> = mutableListOf()
        realm.executeTransaction {
            val objects = realm.where<OfflineMapCityBean>().findAll().sort("id", Sort.ASCENDING)
            list = realm.copyFromRealm(objects)
        }
        return list
    }

    suspend fun insertOrUpdate(objects: Collection<RealmModel?>?) {
        realm.executeTransaction {
            realm.insertOrUpdate(objects)
        }
    }

    suspend fun insertOrUpdate(realmModel: RealmModel?) {
        realm.executeTransaction {
            realm.insertOrUpdate(realmModel)
        }
    }

}
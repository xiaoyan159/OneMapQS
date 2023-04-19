package com.navinfo.omqs

import android.app.Application
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.util.NetUtils
import dagger.hilt.android.HiltAndroidApp
import org.videolan.vlc.Util
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@HiltAndroidApp
class OMQSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FileManager.initRootDir(this)
        Util.getInstance().init(applicationContext)
        NetUtils.getInstance().init(this)
        TakePhotoManager.getInstance().init(this, 1)
        FileManager.initRootDir(this)
        Realm.init(this)
        val password = "password".encodeToByteArray().copyInto(ByteArray(64))
        // 1110000011000010111001101110011011101110110111101110010011001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
//        Log.d("", "密码是： ${BigInteger(1, password).toString(2).padStart(64, '0')}")
        val config = RealmConfiguration.Builder()
            .directory(File(Constant.DATA_PATH))
            .name("HDData")
            .modules(Realm.getDefaultModule(), MyRealmModule())
            .schemaVersion(1)
//            .encryptionKey(password)
            .build()
        Realm.setDefaultConfiguration(config)
    }
}
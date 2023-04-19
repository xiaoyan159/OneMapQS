package com.navinfo.omqs

import android.app.Application
import android.util.Log
import com.navinfo.omqs.db.MyRealmModule
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.util.NetUtils
import dagger.hilt.android.HiltAndroidApp
import org.videolan.vlc.Util
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

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
        val password = "encryp".encodeToByteArray().copyInto(ByteArray(64))
        // 70617373776f72640000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        Log.d("OMQSApplication", "密码是： ${byteArrayToHexString(password)}")
        // 1110000011000010111001101110011011101110110111101110010011001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        val config = RealmConfiguration.Builder()
            .directory(File(Constant.DATA_PATH))
            .name("OMQS.realm")
            .encryptionKey(password)
            .modules(Realm.getDefaultModule(), MyRealmModule())
            .schemaVersion(1)
            .build()
        Realm.setDefaultConfiguration(config)
    }

    private fun getKey(inputString: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(inputString.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) };
    }

    fun byteArrayToHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") { "%02x".format(it) }
    }
}
package com.navinfo.omqs

import android.app.Application
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.util.NetUtils
import dagger.hilt.android.HiltAndroidApp
import org.videolan.vlc.Util
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
    }

    private fun getKey(inputString: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(inputString.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) };
    }

}
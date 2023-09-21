package com.navinfo.omqs

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Surface
import android.view.WindowManager
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.util.NetUtils
import com.umeng.commonsdk.UMConfigure
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
        // 初始化友盟统计
        UMConfigure.preInit(this,"650bece7b2f6fa00ba573c7a","native")
    }

    private fun getKey(inputString: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(inputString.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) };
    }
}
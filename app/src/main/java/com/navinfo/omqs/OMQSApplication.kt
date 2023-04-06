package com.navinfo.omqs

import android.app.Application
import android.util.Log
import com.navinfo.omqs.tools.FileManager
import dagger.hilt.android.HiltAndroidApp
import io.realm.Realm
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@HiltAndroidApp
class OMQSApplication : Application() {
    override fun onCreate() {
        FileManager.initRootDir(this)
        super.onCreate()
    }
}
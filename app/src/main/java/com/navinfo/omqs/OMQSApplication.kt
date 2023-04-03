package com.navinfo.omqs

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.realm.Realm

@HiltAndroidApp
class OMQSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}
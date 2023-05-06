package com.navinfo.omqs.db

import com.navinfo.collect.library.data.entity.TaskBean
import io.realm.annotations.RealmModule

@RealmModule(classes = [TaskBean::class])
class MyRealmModule {
}
package com.navinfo.omqs.db

import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.omqs.bean.TaskBean
import io.realm.annotations.RealmModule

@RealmModule(classes = [TaskBean::class])
class MyRealmModule {
}
package com.navinfo.omqs.db

import com.navinfo.omqs.bean.HadLinkDvoBean
import com.navinfo.omqs.bean.TaskBean
import io.realm.annotations.RealmModule

@RealmModule(classes = [TaskBean::class, HadLinkDvoBean::class])
class MyRealmModule {
}
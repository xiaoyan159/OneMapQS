package com.navinfo.omqs.db

import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.omqs.bean.HadLinkDvoBean
import com.navinfo.omqs.bean.TaskBean

@io.realm.annotations.RealmModule(classes = [TaskBean::class, HadLinkDvoBean::class])
class MyRealmModule {
}
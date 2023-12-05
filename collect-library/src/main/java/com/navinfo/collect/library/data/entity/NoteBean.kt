package com.navinfo.collect.library.data.entity

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class NoteBean @JvmOverloads constructor(
    @PrimaryKey
    var id: String = "",
    var guideGeometry: String = "",
    var description: String = "",
    var taskId :Int = 0,
    var list: RealmList<SketchAttachContent> = RealmList<SketchAttachContent>(),
    var attachmentBeanList: RealmList<AttachmentBean> = RealmList<AttachmentBean>(),
) : RealmObject(){
    fun copy(): NoteBean {
        return NoteBean(
            taskId = taskId,
            id = id,
            guideGeometry = guideGeometry,
            description = description,
            list = list,
            attachmentBeanList = attachmentBeanList
        )
    }
}
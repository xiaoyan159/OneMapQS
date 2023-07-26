package com.navinfo.collect.library.data.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * @author zhjch
 * @version V1.0
 * @ClassName: SketchAttachContent
 * @Date 2016/5/19
 * @Description: ${TODO}(草图内容 )
 */
open class SketchAttachContent @JvmOverloads constructor(
    @PrimaryKey
    var id: String = "",
    /**
     * 获取geo
     *
     * @return geo
     */
    /**
     * 设置geo
     *
     * @param geo geo
     */
    //几何
    var geometry: String = "",
    /**
     * 获取style
     *
     * @return style
     */
    /**
     * 设置style
     *
     * @param style style
     */
    //样式
    var style: String = ""
) : RealmObject()
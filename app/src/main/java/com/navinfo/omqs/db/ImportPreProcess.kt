package com.navinfo.omqs.db

import com.navinfo.collect.library.data.entity.RenderEntity

class ImportPreProcess {
    /**
     * 预处理所需要的函数
     * */
    fun foo(renderEntity: RenderEntity): RenderEntity {
        println("foo")
        renderEntity.properties["foo"] = "bar"
        return renderEntity
    }
}
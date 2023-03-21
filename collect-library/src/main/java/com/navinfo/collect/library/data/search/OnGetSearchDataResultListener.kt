package com.navinfo.collect.library.data.search

import com.navinfo.collect.library.data.entity.Element
import com.navinfo.collect.library.data.entity.LayerManager
import com.navinfo.collect.library.data.entity.Project

interface OnGetSearchDataResultListener {
    fun onGetElementResult(elementList: List<Element>)
    fun onGetLayerResult(layer: LayerManager)
    fun onGetProjectResult(project: Project)
    fun onError(msg:String,keyword:String)
}
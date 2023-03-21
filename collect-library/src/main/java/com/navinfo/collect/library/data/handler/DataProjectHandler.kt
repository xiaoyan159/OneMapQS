package com.navinfo.collect.library.data.handler

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.Project
import kotlin.concurrent.thread

/**
 * 数据库操作
 */


open class DataProjectHandler(context: Context, dataBase: MapLifeDataBase) :
    BaseDataHandler(context, dataBase) {


    fun saveProject(
        project: Project,
        callback: (res: Boolean, errorString: String) -> Unit
    ) {
        thread(start = true) {
            try {
                mDataBase.projectManagerDao.insert(project)
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(true, "")
                }
            } catch (e: Throwable) {
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "${e.message}")
                }
            }

        }
    }


    /**
     * 查询获取图层列表
     */
    fun getProjectList(callback: (list: List<Project>) -> Unit) {
        thread(start = true) {
            val list = mDataBase.projectManagerDao.findList();
            Handler(Looper.getMainLooper()).post {
                callback.invoke(list)
            }
        }
    }
}
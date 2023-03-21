package com.navinfo.collect.library.importExport.handler

import android.content.Context
import android.os.Looper
import android.util.Log
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.CheckManager
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

open class ImportFileHandler(context: Context, dataBase: MapLifeDataBase) :
    ImportExportBaseHandler(context, dataBase) {

    init {
    }

    fun importCheckFileInfo(path: String, callback: (bSuccess: Boolean, message: String) -> Unit) {
        thread(start = true) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val inputStream = file.inputStream()
                    inputStream.bufferedReader().useLines { lines ->
                        try {
                            val iterator = lines.iterator()
                            while (iterator.hasNext()) {
                                var json = iterator.next();
                                val jsonObject = JSONObject(json)
                                val type = jsonObject.optInt("type", 0)
                                val tag = jsonObject.optString("tag", "未命名")
                                var regex = jsonObject.optString("regexStr")
                                if (regex.startsWith("/") && regex.endsWith("/")) {
                                    regex = regex.substring(1, regex.length - 1)
                                }
                                val check = CheckManager(type = type, tag = tag, regexStr = regex)
                                mDataBase.checkManagerDao.insert(check)
                            }

                        } catch (e: Exception) {
                            e.message?.let { Log.e("jingo", it) }
                        }
                    }
//                    inputStream.close()
                }

            } catch (e: Exception) {
                e.message?.let { Log.e("jingo", it) }
            }
            android.os.Handler(Looper.getMainLooper()).post {
                callback.invoke(true, "")
            }
        }
    }

}
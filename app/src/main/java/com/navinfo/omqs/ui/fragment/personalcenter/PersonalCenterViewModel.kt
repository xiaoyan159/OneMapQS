package com.navinfo.omqs.ui.fragment.personalcenter

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.UriUtils
import com.navinfo.collect.library.data.entity.OMDBEntity
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.bean.ScRootCauseAnalysisBean
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.db.RoomAppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PersonalCenterViewModel @Inject constructor(
    private val roomAppDatabase: RoomAppDatabase
) : ViewModel() {
    /**
     * 导入OMDB数据
     * */
    suspend fun importOMDBData(importOMDBHelper: ImportOMDBHelper) {
        Log.d("OMQSApplication", "开始导入数据")
//        Realm.getDefaultInstance().beginTransaction()
        for (table in importOMDBHelper.openConfigFile().tables/*listOf<String>("HAD_LINK")*/) {
            importOMDBHelper.getOMDBTableData(table).collect {
                for (map in it) {
                    val properties = RealmDictionary<String?>()
                    for (entry in map.entries) {
                        properties.putIfAbsent(entry.key, entry.value.toString())
                    }
                    // 将读取到的sqlite数据插入到Realm中
                    Realm.getDefaultInstance().insert(OMDBEntity(table, properties))
                    // 将读取到的数据写入到json中

                }
            }
        }
//        Realm.getDefaultInstance().commitTransaction()

        // 数据导入结束后，开始生成渲染表所需的json文件，并生成压缩包


        Log.d("OMQSApplication", "导入数据完成")
    }

    fun importScProblemData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = UriUtils.uri2File(uri)
                val inputStream: InputStream =
                    FileInputStream(file) //getAssets().open("sample.xlsx")
                val workbook = WorkbookFactory.create(inputStream)
                //获取所有sheet
                val sheet1 = workbook.getSheet("SC_PROBLEM_TYPE")
                sheet1?.let {
                    val rowCount: Int = it.physicalNumberOfRows // 获取行数
                    val list = mutableListOf<ScProblemTypeBean>()
                    for (i in 1 until rowCount) {
                        val row: Row = it.getRow(i) // 获取行
                        val cellCount: Int = row.physicalNumberOfCells // 获取列数
                        if (cellCount == 3) {
                            val bean = ScProblemTypeBean()
                            bean.classType = row.getCell(0).stringCellValue
                            bean.problemType = row.getCell(1).stringCellValue
                            bean.phenomenon = row.getCell(2).stringCellValue
                            list.add(bean)
                            Log.e("jingo", bean.toString())
                        }
                    }
                    roomAppDatabase.getScProblemTypeDao().insertOrUpdateList(list)
                }
                val sheet2 = workbook.getSheet("SC_ROOT_CAUSE_ANALYSIS")
                sheet2?.let {
                    val rowCount: Int = it.physicalNumberOfRows // 获取行数
                    val list = mutableListOf<ScRootCauseAnalysisBean>()
                    for (i in 1 until rowCount) {
                        val row: Row = it.getRow(i) // 获取行
                        val cellCount: Int = row.physicalNumberOfCells // 获取列数
                        if (cellCount == 2) {
                            val bean = ScRootCauseAnalysisBean()
                            bean.problemLink = row.getCell(0).stringCellValue
                            bean.problemCause = row.getCell(1).stringCellValue
                            list.add(bean)
                            Log.e("jingo", bean.toString())
                        }
                    }
                    roomAppDatabase.getScRootCauseAnalysisDao().insertOrUpdateList(list)
                }
                workbook.close()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("jingo", e.toString())
            }
        }

    }
}
package com.navinfo.omqs.ui.fragment.personalcenter

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.ZipUtils
import com.google.gson.Gson
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.bean.ScRootCauseAnalysisBean
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.db.RoomAppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import io.realm.Realm
import io.realm.RealmDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.locationtech.jts.geom.Point
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
    @Inject
    lateinit var realmOperateHelper: RealmOperateHelper
    /**
     * 导入OMDB数据
     * */
    suspend fun obtainOMDBZipData(importOMDBHelper: ImportOMDBHelper) {
        Log.d("OMQSApplication", "开始生成数据")
//        Realm.getDefaultInstance().beginTransaction()
        val gson = Gson()
        val hadLinkFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_LINK.txt")
        val hadLinkKindFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_LINK_KIND.txt")
        val hadLinkDirectFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_LINK_DIRECT.txt")
        val hadSpeedLimitFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_SPEEDLIMIT.txt")
        val hadSpeedLimitCondFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_SPEEDLIMIT_COND.txt")
        val hadSpeedLimitVarFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_SPEEDLIMIT_VAR.txt")

        for (tableName in listOf<String>("HAD_LINK", "HAD_SPEEDLIMIT", "HAD_SPEEDLIMIT_COND", "HAD_SPEEDLIMIT_VAR")/*listOf<String>("HAD_LINK")*/) {
            importOMDBHelper.getOMDBTableData(tableName).collect {
                   for (map in it) {
                    if ("HAD_LINK" == tableName) {
                        // 根据HAD_Link生成json文件
                        val hadLink = HAD_LINK()
                        hadLink.LINK_PID = map["LINK_PID"].toString()
                        hadLink.MESH = map["MESH"].toString()
                        hadLink.S_NODE_PID = map["S_NODE_PID"].toString()
                        hadLink.E_NODE_PID = map["E_NODE_PID"].toString()
                        hadLink.GEOMETRY = map["GEOMETRY"].toString()
                        // 将该数据写入到对应的txt文件
                        FileIOUtils.writeFileFromString(hadLinkFile, gson.toJson(hadLink)+"\r", true)

                        val  hadLinkDirect = HAD_LINK_DIRECT()
                        hadLinkDirect.LINK_PID = map["LINK_PID"].toString()
                        hadLinkDirect.MESH = map["MESH"].toString()
                        hadLinkDirect.DIRECT = map["DIRECT"].toString().toInt()
                        hadLinkDirect.GEOMETRY = map["GEOMETRY"].toString()
                        // 将该数据写入到对应的txt文件
                        FileIOUtils.writeFileFromString(hadLinkDirectFile, gson.toJson(hadLinkDirect)+"\r", true)

                        val  hadLinkKind = HAD_LINK_KIND()
                        hadLinkKind.LINK_PID = map["LINK_PID"].toString()
                        hadLinkKind.MESH = map["MESH"].toString()
                        hadLinkKind.KIND = map["KIND"].toString().toInt()
                        hadLinkKind.GEOMETRY = map["GEOMETRY"].toString()
                        // 将该数据写入到对应的txt文件
                        FileIOUtils.writeFileFromString(hadLinkKindFile, gson.toJson(hadLinkKind)+"\r", true)
                    } else if ("HAD_SPEEDLIMIT" == tableName) {
                        val  hadSpeedlimit = HAD_SPEEDLIMIT()
                        hadSpeedlimit.SPEED_ID = map["SPEED_ID"].toString()
                        hadSpeedlimit.MESH = map["MESH"].toString()
                        hadSpeedlimit.LINK_PID = map["LINK_PID"].toString()
                        hadSpeedlimit.GEOMETRY = map["GEOMETRY"].toString()
                        hadSpeedlimit.DIRECT = map["DIRECT"].toString().toInt()
                        hadSpeedlimit.SPEED_FLAG = map["SPEED_FLAG"].toString().toInt()
                        hadSpeedlimit.MAX_SPEED = map["MAX_SPEED"].toString().toInt()
                        hadSpeedlimit.MIN_SPEED = map["MIN_SPEED"].toString().toInt()
                        // 将该数据写入到对应的txt文件
                        FileIOUtils.writeFileFromString(hadSpeedLimitFile, gson.toJson(hadSpeedlimit)+"\r", true)
                    } else if ("HAD_SPEEDLIMIT_COND" == tableName) {
                        val  hadSpeedlimitCond = HAD_SPEEDLIMIT_COND()
                        hadSpeedlimitCond.SPEED_COND_ID = map["SPEED_COND_ID"].toString()
                        hadSpeedlimitCond.MESH = map["MESH"].toString()
                        hadSpeedlimitCond.LINK_PID = map["LINK_PID"].toString()
                        hadSpeedlimitCond.GEOMETRY = map["GEOMETRY"].toString()
                        hadSpeedlimitCond.DIRECT = map["DIRECT"].toString().toInt()
                        hadSpeedlimitCond.SPEED_FLAG = map["SPEED_FLAG"].toString().toInt()
                        hadSpeedlimitCond.MAX_SPEED = map["MAX_SPEED"].toString().toInt()
                        hadSpeedlimitCond.SPEED_DEPENDENT = map["SPEED_DEPENDENT"].toString().toInt()
                        hadSpeedlimitCond.VEHICLE_TYPE = map["VEHICLE_TYPE"].toString().toInt()
                        hadSpeedlimitCond.VALID_PERIOD = map["VALID_PERIOD"].toString()
                        // 将该数据写入到对应的txt文件
                        FileIOUtils.writeFileFromString(hadSpeedLimitCondFile, gson.toJson(hadSpeedlimitCond)+"\r", true)
                    } else if ("HAD_SPEEDLIMIT_VAR" == tableName) {
                        val  hadSpeedlimitVar = HAD_SPEEDLIMIT_VAR()
                        hadSpeedlimitVar.SPEED_VAR_ID = map["SPEED_ID"].toString()
                        hadSpeedlimitVar.MESH = map["MESH"].toString()
                        hadSpeedlimitVar.LINK_PID = map["LINK_PID"].toString()
                        hadSpeedlimitVar.GEOMETRY = map["GEOMETRY"].toString()
                        hadSpeedlimitVar.DIRECT = map["DIRECT"].toString().toInt()
                        hadSpeedlimitVar.LOCATION = map["LOCATION"].toString()
                        // 将该数据写入到对应的txt文件
                        FileIOUtils.writeFileFromString(hadSpeedLimitVarFile, gson.toJson(hadSpeedlimitVar)+"\r", true)
                    }
//                    val properties = RealmDictionary<String?>()
//                    for (entry in map.entries) {
//                        properties.putIfAbsent(entry.key, entry.value.toString())
//                    }

//                    // 将读取到的sqlite数据插入到Realm中
//                    Realm.getDefaultInstance().insert(OMDBEntity(tableName, properties))
                }
            }
        }
        ZipUtils.zipFiles(mutableListOf(hadLinkFile, hadLinkKindFile, hadLinkDirectFile, hadSpeedLimitFile, hadSpeedLimitCondFile, hadSpeedLimitVarFile), File(importOMDBHelper.omdbFile.parentFile, "output.zip"))

        Log.d("OMQSApplication", "生成数据完成")
    }
    /**
     * 导入OMDB数据
     * */
    suspend fun importOMDBData(importOMDBHelper: ImportOMDBHelper) {
        Log.d("OMQSApplication", "开始导入数据")
//        Realm.getDefaultInstance().beginTransaction()
        importOMDBHelper.importOmdbZipFile(importOMDBHelper.omdbFile).collect {
            Realm.getDefaultInstance().beginTransaction()
            for (map in it) { // 每一个map就是Realm的一条数据
                val renderEntity = RenderEntity()
                renderEntity.code = map["QIcode"].toString().toInt()
                renderEntity.name = map["QIname"].toString()
                renderEntity.table = map["QItable"].toString()
                // 其他数据插入到Properties中
                renderEntity.geometry = map["GEOMETRY"].toString()
                for (entry in map) {
                    renderEntity.properties[entry.key] = entry.value.toString()
                }
                Realm.getDefaultInstance().insert(renderEntity)
            }
            Realm.getDefaultInstance().commitTransaction()
        }
//        Realm.getDefaultInstance().commitTransaction()

//        val gson = Gson()
//        // 数据导入结束后，开始生成渲染表所需的json文件，并生成压缩包
//        for (table in importOMDBHelper.openConfigFile().tables/*listOf<String>("HAD_LINK")*/) {
//            val omdbList = Realm.getDefaultInstance().where(OMDBEntity::class.java).equalTo("table", table.table).findAll()
//            val outputFile = File(importOMDBHelper.omdbFile, "${table.table}.txt")
//            // 将读取到的数据转换为json数据文件
//            for (omdb in omdbList) {
//                FileIOUtils.writeFileFromString(outputFile, gson.toJson(omdb))
//            }
//        }

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
//                        val cellCount: Int = row.physicalNumberOfCells // 获取列数
                        val bean = ScProblemTypeBean(
                            elementType = row.getCell(0).stringCellValue,
                            elementCode = row.getCell(1).numericCellValue.toString(),
                            classType = row.getCell(2).stringCellValue,
                            problemType = row.getCell(3).stringCellValue,
                            phenomenon = row.getCell(4).stringCellValue
                        )
                        list.add(bean)
                        Log.e("jingo", bean.toString())
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

    fun readRealmData() {
        viewModelScope.launch(Dispatchers.IO) {
//            val result = realmOperateHelper.queryLink(GeometryTools.createPoint(115.685817,28.62759))
            val result = realmOperateHelper.queryLinkByLinkPid("84206617008217069")
            Log.d("xiaoyan", result.toString())
        }
    }
}
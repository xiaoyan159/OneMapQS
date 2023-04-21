package com.navinfo.omqs.db

import android.content.Context
import android.database.Cursor.*
import androidx.core.database.getBlobOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.ZipUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.hilt.ImportOMDBHiltFactory
import com.navinfo.omqs.hilt.OMDBDataBaseHiltFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.spatialite.database.SQLiteDatabase
import java.io.File
import javax.inject.Inject
import kotlin.streams.toList

/**
 * 导入omdb数据的帮助类
 * */
class ImportOMDBHelper @AssistedInject constructor(@Assisted("context") val context: Context,@Assisted("omdbFile") val omdbFile: File,@Assisted("configFile") val configFile: File) {
    @Inject
    lateinit var omdbHiltFactory: OMDBDataBaseHiltFactory
    @Inject
    lateinit var gson: Gson
    private val database by lazy { omdbHiltFactory.obtainOmdbDataBaseHelper(context, omdbFile.absolutePath, 1).writableDatabase }


    /**
     * 读取config的配置文件
     * */
    fun openConfigFile(): ImportConfig {
        val configStr = configFile.readText()
        return gson.fromJson(configStr, ImportConfig::class.java)
    }

    /**
     * 读取指定数据表的数据集
     * */
    suspend fun getOMDBTableData(table: String): Flow<List<Map<String, Any>>> =
        withContext(Dispatchers.IO) {
            val listResult: MutableList<Map<String, Any>> = mutableListOf()
            flow<List<Map<String, Any>>> {
                if (database.isOpen) {
                    val comumns = mutableListOf<String>()
                    // 获取要读取的列名
                    val columns = getColumns(database, table)
                    // 处理列名，如果列名是GEOMETRY，则使用spatialite函数ST_AsText读取blob数据
                    val finalColumns = columns.stream().map {
                        val column = it.replace("\"", "", true)
                        if ("GEOMETRY".equals(column, ignoreCase = true)) {
                            "ST_AsText($column)"
                        } else {
                            column
                        }
                    }.toList()

                    val cursor = database.query(table, finalColumns.toTypedArray(), "1=1",
                        mutableListOf<String>().toTypedArray(), null, null, null, null)
                    with(cursor) {
                        if (moveToFirst()) {
                            while (moveToNext()) {
                                val rowMap = mutableMapOf<String, Any>()
                                for (columnIndex in 0 until columnCount) {
                                    var columnName = getColumnName(columnIndex)
                                    if (columnName.startsWith("ST_AsText(")) {
                                        columnName = columnName.replace("ST_AsText(", "").substringBeforeLast(")")
                                    }
                                    when(getType(columnIndex)) {
                                        FIELD_TYPE_NULL -> rowMap[columnName] = ""
                                        FIELD_TYPE_INTEGER -> rowMap[columnName] = getInt(columnIndex)
                                        FIELD_TYPE_FLOAT -> rowMap[columnName] = getFloat(columnIndex)
                                        FIELD_TYPE_BLOB -> rowMap[columnName] = String(getBlob(columnIndex), Charsets.UTF_8)
                                        else -> rowMap[columnName] = getString(columnIndex)
                                    }
                                }
                                listResult.add(rowMap)
                            }
                        }
                    }
                    emit(listResult)
                    cursor.close()
                }
            }
        }

    /**
     * 从zip文件中导入数据到Realm中
     * @param omdbZipFile omdb数据抽取生成的Zip文件
     * @param configFile 对应的配置文件
     * */
    suspend fun importOmdbZipFile(omdbZipFile: File): Flow<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        val importConfig = openConfigFile()
        val unZipFolder = File(omdbZipFile.parentFile, "result")
        flow<List<Map<String, Any>>> {
            if (unZipFolder.exists()) {
                unZipFolder.deleteRecursively()
            }
            unZipFolder.mkdirs()
            // 开始解压zip文件
            val unZipFiles = ZipUtils.unzipFile(omdbZipFile, unZipFolder)
            // 遍历解压后的文件，读取该数据返回
            for (txtFile in unZipFiles) {
                val listResult: MutableList<Map<String, Any>> = mutableListOf()
                // 根据文件名称获取对应的配置
                val currentConfig=importConfig.tables.find {
                    txtFile.name.substring(0, txtFile.name.lastIndexOf("."))==it.table
                }
                val list = FileIOUtils.readFile2List(txtFile, "UTF-8")
                // 将list数据转换为map
                for (line in list) {
                    val map = gson.fromJson<Map<String, Any>>(line, object : TypeToken<MutableMap<String, Any>>() {}.type)
                        .toMutableMap()
                    currentConfig?.let {
                        map["QItable"] = currentConfig.table
                        map["QIname"] = currentConfig.name
                        map["QIcode"] = currentConfig.code
                        listResult.add(map)
                    }
                }
                // 1个文件发送一次flow流
                emit(listResult)
            }
        }
    }

    // 获取指定数据表的列名
    fun getColumns(db: SQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()

        // 查询 sqlite_master 表获取指定数据表的元数据信息
        val cursor = db.query("sqlite_master", arrayOf("sql"), "type='table' AND name=?", arrayOf(tableName), null, null, null)

        // 从元数据信息中解析出列名
        if (cursor.moveToFirst()) {
            val sql = cursor.getString(0)
            val startIndex = sql.indexOf("(") + 1
            val endIndex = sql.lastIndexOf(")")
            val columnDefs = sql.substring(startIndex, endIndex).split(",")
            for (columnDef in columnDefs) {
                val columnName = columnDef.trim().split(" ")[0]
                if (!columnName.startsWith("rowid", true)) { // 排除 rowid 列
                    columns.add(columnName)
                }
            }
        }
        cursor.close()
        return columns
    }

    /**
     * 预处理渲染要素，某些要素需要对数据做二次处理
     * */
    fun performRenderEntity(renderEntity: RenderEntity) {
    }
}
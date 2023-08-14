package com.navinfo.omqs.db

import android.content.Context
import android.database.Cursor.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.ZipUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.enum.DataCodeEnum
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.db.deep.LinkList
import com.navinfo.omqs.hilt.OMDBDataBaseHiltFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.realm.Realm
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
class ImportOMDBHelper @AssistedInject constructor(
    @Assisted("context") val context: Context,
    @Assisted("omdbFile") val omdbFile: File
) {
    @Inject
    lateinit var omdbHiltFactory: OMDBDataBaseHiltFactory

    @Inject
    lateinit var gson: Gson
    private val database by lazy {
        omdbHiltFactory.obtainOmdbDataBaseHelper(
            context,
            omdbFile.absolutePath,
            1
        ).writableDatabase
    }
    private val configFile: File =
        File("${Constant.USER_DATA_PATH}", Constant.OMDB_CONFIG)

    private val importConfig by lazy {
        openConfigFile()
    }

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
    @RequiresApi(Build.VERSION_CODES.N)
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

                    val cursor = database.query(
                        table, finalColumns.toTypedArray(), "1=1",
                        mutableListOf<String>().toTypedArray(), null, null, null, null
                    )
                    with(cursor) {
                        if (moveToFirst()) {
                            while (moveToNext()) {
                                val rowMap = mutableMapOf<String, Any>()
                                for (columnIndex in 0 until columnCount) {
                                    var columnName = getColumnName(columnIndex)
                                    if (columnName.startsWith("ST_AsText(")) {
                                        columnName = columnName.replace("ST_AsText(", "")
                                            .substringBeforeLast(")")
                                    }
                                    when (getType(columnIndex)) {
                                        FIELD_TYPE_NULL -> rowMap[columnName] = ""
                                        FIELD_TYPE_INTEGER -> rowMap[columnName] =
                                            getInt(columnIndex)
                                        FIELD_TYPE_FLOAT -> rowMap[columnName] =
                                            getFloat(columnIndex)
                                        FIELD_TYPE_BLOB -> rowMap[columnName] =
                                            String(getBlob(columnIndex), Charsets.UTF_8)
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
    suspend fun importOmdbZipFile(omdbZipFile: File, task: TaskBean): Flow<String> = withContext(Dispatchers.IO) {
        val unZipFolder = File(omdbZipFile.parentFile, "result")
        flow {
            if (unZipFolder.exists()) {
                unZipFolder.deleteRecursively()
            }
            unZipFolder.mkdirs()
            // 开始解压zip文件
            val unZipFiles = ZipUtils.unzipFile(omdbZipFile, unZipFolder)
            // 将listResult数据插入到Realm数据库中
            val realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            try {
                // 遍历解压后的文件，读取该数据返回
                for ((index, currentEntry) in importConfig.tableMap.entries.withIndex()) {
                    val currentConfig = currentEntry.value
                    val txtFile = unZipFiles.find {
                        it.name == currentConfig.table
                    }

                    val listResult = mutableListOf<RenderEntity>()
                    currentConfig?.let {
                        val list = FileIOUtils.readFile2List(txtFile, "UTF-8")
                        Log.d("ImportOMDBHelper", "开始解析：${txtFile?.name}")
                        if (list != null) {
                            // 将list数据转换为map
                            for ((index, line) in list.withIndex()) {
                                if (line == null || line.trim() == "") {
                                    continue
                                }
                                Log.d("ImportOMDBHelper", "解析第：${index+1}行")
                                val map = gson.fromJson<Map<String, Any>>(line, object:TypeToken<Map<String, Any>>(){}.getType())
                                    .toMutableMap()
                                map["qi_table"] = currentConfig.table
                                map["qi_name"] = currentConfig.name
                                map["qi_code"] =
                                    if (currentConfig.code == 0) currentConfig.code else currentEntry.key
                                map["qi_code"] = if (currentConfig.code == 0) currentConfig.code else currentEntry.key
                                map["qi_zoomMin"] = currentConfig.zoomMin
                                map["qi_zoomMax"] = currentConfig.zoomMax

                                // 先查询这个mesh下有没有数据，如果有则跳过即可
                                // val meshEntity = Realm.getDefaultInstance().where(RenderEntity::class.java).equalTo("properties['mesh']", map["mesh"].toString()).findFirst()
                                val renderEntity = RenderEntity()
                                renderEntity.code = map["qi_code"].toString()
                                renderEntity.name = map["qi_name"].toString()
                                renderEntity.table = map["qi_table"].toString()
                                renderEntity.taskId = task.id
                                renderEntity.zoomMin = map["qi_zoomMin"].toString().toInt()
                                renderEntity.zoomMax = map["qi_zoomMax"].toString().toInt()

                                // 其他数据插入到Properties中
                                renderEntity.geometry = map["geometry"].toString()
                                for ((key, value) in map) {
                                    when (value) {
                                        is String -> renderEntity.properties.put(key, value)
                                        is Int -> renderEntity.properties.put(
                                            key,
                                            value.toInt().toString()
                                        )
                                        is Double -> renderEntity.properties.put(
                                            key,
                                            value.toDouble().toString()
                                        )
                                        else -> renderEntity.properties.put(key, value.toString())
                                    }
                                }
                                //遍历判断只显示与任务Link相关的任务数据
                                if(currentConfig.checkLinkId){

                                    if(renderEntity.properties.containsKey("linkPid")){

                                        var currentLinkPid = renderEntity.properties["linkPid"]

                                        if(!currentLinkPid.isNullOrEmpty()&&currentLinkPid!="null"){

                                            task.hadLinkDvoList.forEach{
                                                if(it.linkPid==renderEntity.properties["linkPid"]){
                                                    renderEntity.enable = 1
                                                    Log.e("qj","${renderEntity.name}==包括任务link")
                                                    return@forEach
                                                }
                                            }
                                        }

                                    }else if(renderEntity.code == DataCodeEnum.OMDB_RESTRICTION.code && renderEntity.properties.containsKey("linkIn")){

                                        if (renderEntity.properties["linkIn"] != null) {

                                            var currentLinkPid = renderEntity.properties["linkIn"]

                                            if(!currentLinkPid.isNullOrEmpty()&&currentLinkPid!="null"){

                                                task.hadLinkDvoList.forEach{
                                                    if(it.linkPid==currentLinkPid){
                                                        renderEntity.enable = 1
                                                        Log.e("qj","${renderEntity.name}==包括任务link")
                                                        return@forEach
                                                    }
                                                }
                                            }
                                        }
                                    }else if(renderEntity.code == DataCodeEnum.OMDB_INTERSECTION.code && renderEntity.properties.containsKey("linkList")){

                                        if (renderEntity.properties["linkList"] != null) {

                                            Log.e("qj", "linkList==开始${renderEntity.name}==${renderEntity.properties["linkList"]}}")

                                            val linkList = renderEntity.properties["linkList"]

                                            if (!linkList.isNullOrEmpty()&&linkList!="null") {

                                                Log.e("qj", "linkList==${renderEntity.name}==${renderEntity.properties["linkList"]}}")

                                                val list: List<LinkList> = gson.fromJson(linkList, object : TypeToken<List<LinkList>>() {}.type)

                                                if (list != null) {
                                                    m@for (link in list){
                                                        for(hadLink in task.hadLinkDvoList){
                                                            if (hadLink.linkPid == link.linkPid) {
                                                                renderEntity.enable = 1
                                                                Log.e("qj", "${renderEntity.name}==包括任务link==${renderEntity.geometry}")
                                                                break@m
                                                            }
                                                        }
                                                    }
                                                }
                                            }else{
                                                renderEntity.enable = 2
                                                Log.e("qj", "简单路口")
                                            }
                                        }
                                    }else{
                                        renderEntity.enable = 2
                                        Log.e("qj","${renderEntity.name}==不包括任务linkPid")
                                    }
                                }else{
                                    renderEntity.enable = 2
                                    Log.e("qj","${renderEntity.name}==不包括任务linkPid")
                                }

                                //道路属性code编码需要特殊处理 存在多个属性值时，渲染优先级：SA>PA,存在多个属性值时，渲染优先级：FRONTAGE>MAIN_SIDE_A CCESS
                                if(renderEntity.code == DataCodeEnum.OMDB_LINK_ATTRIBUTE.code){

                                    var type = renderEntity.properties["SA"]

                                    if(type!=null&&type=="1"){
                                        renderEntity.code = DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code
                                    }else{
                                        type = renderEntity.properties["PA"]
                                        if(type!=null&&type=="1"){
                                            renderEntity.code = DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code
                                        } else{
                                            type = renderEntity.properties["FRONTAGE"]
                                            if(type!=null&&type=="1"){
                                                renderEntity.code = DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code
                                            }else{
                                                type = renderEntity.properties["MAIN_SIDE_ACCESS"]
                                                if(type!=null&&type=="1"){
                                                    renderEntity.code = DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code
                                                }else{
                                                    renderEntity.enable=0
                                                    Log.e("qj","过滤不显示数据${renderEntity.table}")
                                                    continue
                                                }
                                            }
                                        }
                                    }
                                }else if(renderEntity.code == DataCodeEnum.OMDB_RAMP.code){
                                    /*匝道*/
                                    var formWay = renderEntity.properties["FORM_OF_WAY"]
                                    if(formWay!=null){
                                        when (formWay) {
                                            "93"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_1.code
                                            "98"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_2.code
                                            "99"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_3.code
                                            "100"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_4.code
                                            "102"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_5.code
                                            "103"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_6.code
                                            "104"-> renderEntity.code = DataCodeEnum.OMDB_RAMP_7.code
                                        }
                                    }
                                }else if(renderEntity.code == DataCodeEnum.OMDB_LINK_FORM1.code){
                                    /*道路形态1*/
                                    var formWay = renderEntity.properties["FORM_OF_WAY"]
                                    if(formWay!=null){
                                        when (formWay) {
                                            "35"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM1_1.code
                                            "37"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM1_2.code
                                            "38"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM1_3.code
                                        }
                                    }
                                }else if(renderEntity.code == DataCodeEnum.OMDB_LINK_FORM2.code){
                                    /*道路形态2*/
                                    var formWay = renderEntity.properties["FORM_OF_WAY"]
                                    if(formWay!=null){
                                        when (formWay) {
                                            "10"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_1.code
                                            "11"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_2.code
                                            "17"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_3.code
                                            "18"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_4.code
                                            "20"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_5.code
                                            "22"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_6.code
                                            "36"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_7.code
                                            "52"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_8.code
                                            "53"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_9.code
                                            "54"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_10.code
                                            "60"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_11.code
                                            "84"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_12.code
                                            "85"-> renderEntity.code = DataCodeEnum.OMDB_LINK_FORM2_13.code
                                        }
                                    }
                                }else if(renderEntity.code == DataCodeEnum.OMDB_LANE_MARK_BOUNDARYTYPE.code){
                                    var boundaryType = renderEntity.properties["boundaryType"]
                                    if(boundaryType!=null){
                                        when (boundaryType) {
                                            "0","1","6","8","9"->{
                                                renderEntity.enable=0
                                                Log.e("qj","过滤不显示数据${renderEntity.table}")
                                                continue
                                            }
                                        }
                                    }
                                }else if(renderEntity.code == DataCodeEnum.OMDB_RDBOUND_BOUNDARYTYPE.code){
                                    //过滤不需要渲染的要素
                                    var boundaryType = renderEntity.properties["boundaryType"]
                                    if(boundaryType!=null){
                                        when (boundaryType) {
                                            "0","3","4","5","7","9"->{
                                                renderEntity.enable=0
                                                Log.e("qj","过滤不显示数据${renderEntity.table}")
                                                continue
                                            }
                                        }
                                    }
                                }

                                listResult.add(renderEntity)
                                // 对renderEntity做预处理后再保存
                                val resultEntity = importConfig.transformProperties(renderEntity)
                                if (resultEntity != null) {
                                    realm.insert(renderEntity)
                                }
                            }
                        }
                    }
                    // 1个文件发送一次flow流
                    emit("${index + 1}/${importConfig.tableMap.size}")
                    // 如果当前解析的是OMDB_RD_LINK数据，将其缓存在预处理类中，以便后续处理其他要素时使用
                    if (currentConfig.table == "OMDB_RD_LINK") {
                        importConfig.preProcess.cacheRdLink =
                            listResult.associateBy { it.properties["linkPid"] }
                    }
                }
                realm.commitTransaction()
                realm.close()
            } catch (e: Exception) {
                realm.cancelTransaction()
                throw e
            }
            emit("finish")
        }
    }

    // 获取指定数据表的列名
    fun getColumns(db: SQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()

        // 查询 sqlite_master 表获取指定数据表的元数据信息
        val cursor = db.query(
            "sqlite_master",
            arrayOf("sql"),
            "type='table' AND name=?",
            arrayOf(tableName),
            null,
            null,
            null
        )

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
}
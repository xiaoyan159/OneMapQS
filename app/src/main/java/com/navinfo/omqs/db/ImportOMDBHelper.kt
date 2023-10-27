package com.navinfo.omqs.db

import android.content.Context
import android.database.Cursor.*
import android.os.Build
import android.provider.ContactsContract.Data
import android.util.Log
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.ZipUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StrZipUtil
import com.navinfo.omqs.Constant
import com.navinfo.omqs.Constant.Companion.currentInstallTaskConfig
import com.navinfo.omqs.Constant.Companion.currentInstallTaskFolder
import com.navinfo.omqs.Constant.Companion.installTaskid
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.db.deep.LinkList
import com.navinfo.omqs.hilt.OMDBDataBaseHiltFactory
import com.navinfo.omqs.util.CMLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.spatialite.database.SQLiteDatabase
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.streams.toList

/**
 * 导入omdb数据的帮助类
 * */
class ImportOMDBHelper @AssistedInject constructor(
    @Assisted("context") val context: Context, @Assisted("omdbFile") val omdbFile: File
) {
    @Inject
    lateinit var omdbHiltFactory: OMDBDataBaseHiltFactory

    @Inject
    lateinit var gson: Gson
    private val database by lazy {
        omdbHiltFactory.obtainOmdbDataBaseHelper(
            context, omdbFile.absolutePath, 1
        ).writableDatabase
    }
    private val configFile: File = File("${Constant.USER_DATA_PATH}", Constant.OMDB_CONFIG)

    private val importConfigList by lazy {
        openConfigFile()
    }

    /**
     * 读取config的配置文件
     * */
    fun openConfigFile(): List<ImportConfig> {
        val resultList = mutableListOf<ImportConfig>()
        val configStr = configFile.readText()
        val type = object : TypeToken<List<ImportConfig>>() {}.type
        return try {
            val result = gson.fromJson<List<ImportConfig>>(configStr, type)
            result ?: resultList
        } catch (e: Exception) {
            resultList
        }
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
                        table,
                        finalColumns.toTypedArray(),
                        "1=1",
                        mutableListOf<String>().toTypedArray(),
                        null,
                        null,
                        null,
                        null
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
    suspend fun importOmdbZipFile(omdbZipFile: File, task: TaskBean): Flow<String> =
        withContext(Dispatchers.IO) {
            installTaskid = task.id.toString()
            currentInstallTaskFolder = File(Constant.USER_DATA_PATH + "/$installTaskid")
            if (!currentInstallTaskFolder.exists()) currentInstallTaskFolder.mkdirs()
            currentInstallTaskConfig =
                RealmConfiguration.Builder().directory(currentInstallTaskFolder).name("OMQS.realm")
                    .encryptionKey(Constant.PASSWORD)
//                .allowQueriesOnUiThread(true)
                    .schemaVersion(2).build()
            val unZipFolder = File(omdbZipFile.parentFile, "result")

            flow {
                if (unZipFolder.exists()) {
                    unZipFolder.deleteRecursively()
                }
                unZipFolder.mkdirs()
                // 开始解压zip文件
                val unZipFiles = ZipUtils.unzipFile(omdbZipFile, unZipFolder)
                // 先获取当前配置的所有图层的个数，方便后续计算数据解析进度
                var tableNum = 0
                var processIndex = 0
                //下载数据统计
                var dataIndex = 0
                //数据库插入的统计
                var insertIndex = 0
                //单个表要素统计
                var elementIndex = 0
                //单个表要素时间统计
//                var tableImportTime = System.currentTimeMillis()
                //总表要素统计时间
//                var dataImportTime = System.currentTimeMillis()
//                Realm.compactRealm(currentInstallTaskConfig)
                var realm = Realm.getInstance(currentInstallTaskConfig)

                realm.beginTransaction()
                for (importConfig in importConfigList) {
                    tableNum += importConfig.tableMap.size
                }
                //缓存任务link信息，便于下面与数据进行任务link匹配
                val hashMap: HashMap<Long, HadLinkDvoBean> = HashMap<Long, HadLinkDvoBean>()

//                val lineList = arrayOfNulls<LineString>(task.hadLinkDvoList.size)
//                var index = 0
                task.hadLinkDvoList.forEach {
                    hashMap[it.linkPid.toLong()] = it
//                    lineList[index] = GeometryTools.createGeometry(it.geometry) as LineString
//                    index++
                }

                val resHashMap: HashMap<String, RenderEntity> = HashMap() //define empty hashmap
                val listRenderEntity = mutableListOf<RenderEntity>()
                try {

//                    var multipLine = MultiLineString(lineList, GeometryFactory())


                    // 遍历解压后的文件，读取该数据返回
//                    Log.d("ImportOMDBHelper", "表解析===开始时间$dataImportTime===")
                    CMLog.writeLogtoFile(
                        ImportOMDBHelper::class.java.name,
                        "importOmdbZipFile",
                        "开始"
                    )
                    for (importConfig in importConfigList) {

                        for ((index, currentEntry) in importConfig.tableMap.entries.withIndex()) {
                            processIndex += 1

                            val currentConfig = currentEntry.value

                            CMLog.writeLogtoFile(
                                ImportOMDBHelper::class.java.name,
                                "importOmdbZipFile",
                                "${currentConfig.table}开始"
                            )

                            val txtFile = unZipFiles.find {
                                it.name == currentConfig.table
                            }
                            Log.e("qj", "开始遍历文件==${currentConfig.table}")
                            if (txtFile != null && txtFile.exists()) {
                                try {

                                    Log.e("qj", "开始遍历文件1")
                                    val fileReader = FileReader(txtFile)
                                    val bufferedReader = BufferedReader(fileReader)
                                    var line: String? = bufferedReader.readLine()

                                    while (line != null) {
                                        if (line == null || line.trim() == "") {
                                            line = bufferedReader.readLine()
                                            continue
                                        }
                                        elementIndex += 1
                                        dataIndex += 1
                                        val map = gson.fromJson<Map<String, Any>>(
                                            line, object : TypeToken<Map<String, Any>>() {}.type
                                        ).toMutableMap()
                                        map["qi_table"] = currentConfig.table
                                        map["qi_name"] = currentConfig.name
                                        map["qi_code"] =
                                            if (currentConfig.code == 0) currentConfig.code else currentEntry.key
                                        map["qi_zoomMin"] = currentConfig.zoomMin
                                        map["qi_zoomMax"] = currentConfig.zoomMax

                                        // 先查询这个mesh下有没有数据，如果有则跳过即可
                                        val renderEntity = RenderEntity()
                                        renderEntity.code = map["qi_code"].toString()
                                        renderEntity.name = map["qi_name"].toString()
                                        renderEntity.table = map["qi_table"].toString()
                                        renderEntity.taskId = task.id
                                        renderEntity.zoomMin = map["qi_zoomMin"].toString().toInt()
                                        renderEntity.zoomMax = map["qi_zoomMax"].toString().toInt()

                                        Log.e(
                                            "jingo",
                                            "安装数据 ${renderEntity.table} ${renderEntity.linkPid} $elementIndex $insertIndex"
                                        )

                                        renderEntity.geometry = map["geometry"].toString()
                                        Log.e("jingo", "map解析开始")
                                        for ((key, value) in map) {
                                            when (value) {
                                                is String -> renderEntity.properties[key] = value
                                                is Int -> renderEntity.properties[key] =
                                                    value.toInt().toString()

                                                is Double -> renderEntity.properties[key] =
                                                    value.toDouble().toString()

                                                else -> renderEntity.properties[key] =
                                                    value.toString()
                                            }
                                        }

                                        // 如果properties中不包含name，那么自动将要素名称添加进properties中
                                        if (!renderEntity.properties.containsKey("name")) {
                                            renderEntity.properties["name"] = renderEntity.name;
                                        }

                                        Log.e("jingo", "map解析结束")

                                        if (currentConfig.filterData) {
                                            when (renderEntity.code.toInt()) {

                                                DataCodeEnum.OMDB_POLE.code.toInt() -> {
                                                    //过滤树类型的杆状物，无需导入到数据库中
                                                    val poleType =
                                                        renderEntity.properties["poleType"]
                                                    if (poleType != null && poleType.toInt() == 2) {
                                                        line = bufferedReader.readLine()
                                                        continue
                                                    }
                                                }

                                                DataCodeEnum.OMDB_LANE_MARK_BOUNDARYTYPE.code.toInt() -> {
                                                    val boundaryType =
                                                        renderEntity.properties["boundaryType"]
                                                    if (boundaryType != null) {
                                                        when (boundaryType.toInt()) {
                                                            0, 1, 6, 8, 9 -> {
                                                                renderEntity.enable = 0
                                                                line = bufferedReader.readLine()
                                                                continue
                                                            }
                                                        }
                                                    }
                                                }

                                                DataCodeEnum.OMDB_RDBOUND_BOUNDARYTYPE.code.toInt() -> {
                                                    val boundaryType =
                                                        renderEntity.properties["boundaryType"]
                                                    if (boundaryType != null) {
                                                        when (boundaryType.toInt()) {
                                                            0, 1, 3, 4, 5, 7, 9 -> {
                                                                renderEntity.enable = 0
                                                                line = bufferedReader.readLine()
                                                                continue
                                                            }
                                                        }
                                                    }
                                                }

                                                DataCodeEnum.OMDB_OBJECT_STOPLOCATION.code.toInt() -> {
                                                    val locationType =
                                                        renderEntity.properties["locationType"]
                                                    if (locationType != null) {
                                                        when (locationType.toInt()) {
                                                            3, 4 -> {
                                                                renderEntity.enable = 0
                                                                line = bufferedReader.readLine()
                                                                continue
                                                            }
                                                        }
                                                    }
                                                }

                                                DataCodeEnum.OMDB_RESTRICTION.code.toInt() -> {
                                                    if (renderEntity.properties.containsKey("linkIn") && renderEntity.properties.containsKey(
                                                            "linkOut"
                                                        )
                                                    ) {
                                                        val linkIn =
                                                            renderEntity.properties["linkIn"]
                                                        val linkOut =
                                                            renderEntity.properties["linkOut"]
                                                        if (linkIn != null && linkOut != null) {
                                                            val checkMsg = "$linkIn$linkOut"
                                                            if (resHashMap.containsKey(checkMsg)) {
                                                                line = bufferedReader.readLine()
                                                                continue
                                                            } else {
                                                                resHashMap[checkMsg] = renderEntity
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        //遍历判断只显示与任务Link相关的任务数据
                                        if (currentConfig.checkLinkId) {

                                            if (renderEntity.linkPid.isNotEmpty()) {

                                                val currentLinkPid = renderEntity.linkPid


                                                if (!currentLinkPid.isNullOrEmpty() && currentLinkPid != "null") {

                                                    val list = currentLinkPid.split(",")

                                                    if (list.isNotEmpty()) {

                                                        m@ for (linkPid in list) {
                                                            if (hashMap.containsKey(linkPid.toLong())) {
                                                                renderEntity.enable = 1
                                                                break@m
                                                            }
                                                        }
                                                    }
                                                }

                                            } else if (renderEntity.code.toInt() == DataCodeEnum.OMDB_INTERSECTION.code.toInt() || renderEntity.code.toInt() == DataCodeEnum.OMDB_LANE_CONSTRUCTION.code.toInt() && renderEntity.properties.containsKey(
                                                    "linkList"
                                                )
                                            ) {

                                                if (renderEntity.properties["linkList"] != null) {

                                                    val linkList =
                                                        renderEntity.properties["linkList"]

                                                    if (!linkList.isNullOrEmpty() && linkList != "null") {

                                                        val list: List<LinkList> = gson.fromJson(
                                                            linkList,
                                                            object :
                                                                TypeToken<List<LinkList>>() {}.type
                                                        )

                                                        m@ for (link in list) {
                                                            if (hashMap.containsKey(link.linkPid.toLong())) {
                                                                renderEntity.enable = 1
                                                                break@m
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                //不包括linkPid直接过滤
                                                line = bufferedReader.readLine()
                                                continue
                                            }
                                            //过滤掉非任务路线上的数据
                                            if (renderEntity.enable != 1) {
                                                line = bufferedReader.readLine()
                                                continue
                                            }

                                        } else {
                                            renderEntity.enable = 1
                                        }

                                        if (currentConfig.catch) {
                                            renderEntity.catchEnable = 1
                                        } else {
                                            renderEntity.catchEnable = 0
                                        }

                                        var resultEntity = importConfig.transformProperties(renderEntity, realm)

                                        if (resultEntity != null) {

                                            if (currentConfig.existSubCode) {
                                                when (renderEntity.code.toInt()) {
                                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE.code.toInt() -> {
                                                        var type = renderEntity.properties["sa"]

                                                        if (type != null && type == "1") {
                                                            renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code
                                                        } else {
                                                            type = renderEntity.properties["pa"]
                                                            if (type != null && type == "1") {
                                                                renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code
                                                            } else {
                                                                type =
                                                                    renderEntity.properties["frontage"]
                                                                if (type != null && type == "1") {
                                                                    renderEntity.code =
                                                                        DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code
                                                                    renderEntity.zoomMin = 15
                                                                    renderEntity.zoomMax = 17
                                                                } else {
                                                                    type =
                                                                        renderEntity.properties["mainSideAccess"]
                                                                    if (type != null && type == "1") {
                                                                        renderEntity.code =
                                                                            DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code
                                                                        renderEntity.zoomMin = 15
                                                                        renderEntity.zoomMax = 17
                                                                    } else {
                                                                        renderEntity.enable = 0
                                                                        renderEntity.zoomMin = 15
                                                                        renderEntity.zoomMax = 17
                                                                        line =
                                                                            bufferedReader.readLine()
                                                                        continue
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    //桥
                                                    DataCodeEnum.OMDB_BRIDGE.code.toInt() -> {
                                                        when (renderEntity.properties["bridgeType"]) {
                                                            "1" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_BRIDGE_1.code

                                                            "2" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_BRIDGE_2.code

                                                            else -> DataCodeEnum.OMDB_BRIDGE.code
                                                        }
                                                    }

                                                    DataCodeEnum.OMDB_RAMP.code.toInt() -> {
                                                        //*匝道*//*
                                                        val formWay =
                                                            renderEntity.properties["formOfWay"]
                                                        if (formWay != null) {
                                                            when (formWay.toInt()) {
                                                                93 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_1.code

                                                                98 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_2.code

                                                                99 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_3.code

                                                                100 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_4.code

                                                                102 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_5.code

                                                                103 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_6.code

                                                                104 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_RAMP_7.code
                                                            }
                                                        }
                                                    }

                                                    DataCodeEnum.OMDB_LINK_FORM1.code.toInt() -> {
                                                        //*道路形态1*//*
                                                        val formWay =
                                                            renderEntity.properties["formOfWay"]
                                                        if (formWay != null) {
                                                            when (formWay.toInt()) {
                                                                35 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM1_1.code

                                                                37 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM1_2.code

                                                                38 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM1_3.code
                                                            }
                                                        }
                                                    }

                                                    DataCodeEnum.OMDB_LINK_FORM2.code.toInt() -> {
                                                        //*道路形态2*//*
                                                        val formWay =
                                                            renderEntity.properties["formOfWay"]
                                                        if (formWay != null) {
                                                            when (formWay.toInt()) {
                                                                10 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_1.code

                                                                11 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_2.code

                                                                17 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_3.code

                                                                18 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_4.code

                                                                20 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_5.code

                                                                22 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_6.code

                                                                36 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_7.code

                                                                52 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_8.code

                                                                53 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_9.code

                                                                54 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_10.code

                                                                60 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_11.code

                                                                84 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_12.code

                                                                85 -> renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_FORM2_13.code
                                                            }
                                                        }
                                                    }

                                                    DataCodeEnum.OMDB_LANE_CONSTRUCTION.code.toInt() -> {
                                                        //特殊处理空数据，渲染原则使用
                                                        val startTime =
                                                            renderEntity.properties["startTime"]
                                                        if (startTime == null || startTime == "") {
                                                            renderEntity.properties["startTime"] =
                                                                "null"
                                                        }
                                                    }
                                                }

                                                if (renderEntity.table == DataCodeEnum.OMDB_NODE_FORM.name) {//特殊处理，因为code相同，使用表名判断
                                                    //过滤不需要渲染的要素
                                                    val formOfWay =
                                                        renderEntity.properties["formOfWay"]
                                                    if (formOfWay != null && formOfWay.toInt() == 30) {
                                                        renderEntity.enable = 2
                                                        renderEntity.code =
                                                            DataCodeEnum.OMDB_NODE_FORM.code
                                                    } else {
                                                        line = bufferedReader.readLine()
                                                        continue
                                                    }
                                                } else if (renderEntity.table == DataCodeEnum.OMDB_NODE_PA.name) {//特殊处理，因为code相同，使用表名判断
                                                    //过滤不需要渲染的要素
                                                    val attributeType =
                                                        renderEntity.properties["attributeType"]
                                                    if (attributeType != null && attributeType.toInt() == 30) {
                                                        renderEntity.enable = 2
                                                        renderEntity.code =
                                                            DataCodeEnum.OMDB_NODE_PA.code
                                                    } else {
                                                        line = bufferedReader.readLine()
                                                        continue
                                                    }
                                                }
                                            }

                                            ++insertIndex

                                            //移除该字段，减少数据量
                                            if (renderEntity.properties.containsKey("geometry")) {
                                                renderEntity.properties.remove("geometry")
                                            }

                                            //移除该字段，减少数据量
                                            if (renderEntity.properties.containsKey("linkPid")) {
                                                renderEntity.properties.remove("linkPid")
                                            }

                                            // 如果当前解析的是OMDB_RD_LINK数据，将其缓存在预处理类中，以便后续处理其他要素时使用
                                            if (currentConfig.code == DataCodeEnum.OMDB_RD_LINK.code.toInt()) {
                                                if (renderEntity.linkRelation == null) {
                                                    renderEntity.linkRelation = LinkRelation()
                                                }
                                                renderEntity.linkRelation!!.linkPid =
                                                    renderEntity.linkPid
                                                renderEntity.linkRelation!!.sNodeId =
                                                    renderEntity.properties["snodePid"]
                                                renderEntity.linkRelation!!.eNodeId =
                                                    renderEntity.properties["enodePid"]
                                            }

                                            //去掉暂用控件较大的字段多余属性字段
                                            if (renderEntity.properties.containsKey("shapeList")) {
                                                renderEntity.properties.remove("shapeList")
                                            }

                                            var gsonStr = gson.toJson(renderEntity.properties).toString()
                                            renderEntity.propertiesDb = StrZipUtil.compress(gsonStr)

                                            listRenderEntity.add(renderEntity)
                                        }

                                        if (listRenderEntity.size > 50000) {
                                            Log.e("jingo", "50000刷新")
                                            realm.copyToRealmOrUpdate(listRenderEntity)
                                            realm.commitTransaction()
                                            realm.close()
                                            listRenderEntity.clear()
                                            insertIndex = 0
                                            realm = Realm.getInstance(currentInstallTaskConfig)
                                            realm.beginTransaction()
                                        }
                                        line = bufferedReader.readLine()
                                    }

                                    bufferedReader.close()
                                } catch (e: Exception) {
                                    CMLog.writeLogtoFile(
                                        ImportOMDBHelper::class.java.name,
                                        "importOmdbZipFile",
                                        "${currentConfig.table}安装异常"
                                    )
                                }
                            } else {
                                CMLog.writeLogtoFile(
                                    ImportOMDBHelper::class.java.name,
                                    "importOmdbZipFile",
                                    "文件不存在"
                                )
                            }
                            // 1个文件发送一次flow流
                            emit("${processIndex}/${tableNum}")
                            CMLog.writeLogtoFile(
                                ImportOMDBHelper::class.java.name,
                                "importOmdbZipFile",
                                "${currentConfig.table}结束==$elementIndex"
                            )
                            elementIndex = 0
                        }
                    }
                    CMLog.writeLogtoFile(
                        ImportOMDBHelper::class.java.name,
                        "importOmdbZipFile",
                        "结束===总量$dataIndex"
                    )

                    realm.copyToRealmOrUpdate(listRenderEntity)
                    realm.commitTransaction()
                    realm.close()
                    listRenderEntity.clear()
                    Log.e("qj", "安装结束")
                } catch (e: Exception) {
                    if (realm.isInTransaction) {
                        realm.cancelTransaction()
                        realm.close()
                    }
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
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
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.utils.GeometryTools
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
import org.locationtech.jts.geom.Geometry
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
    suspend fun importOmdbZipFile(omdbZipFile: File, task: TaskBean): Flow<String> =
        withContext(Dispatchers.IO) {
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
                for (importConfig in importConfigList) {
                    tableNum += importConfig.tableMap.size
                }


                //缓存任务link信息，便于下面与数据进行任务link匹配
                val hashMap: HashMap<String, HadLinkDvoBean> =
                    HashMap<String, HadLinkDvoBean>() //define empty hashmap
                task.hadLinkDvoList.forEach {
                    hashMap.put(it.linkPid, it);
                }

                val resHashMap: HashMap<String, RenderEntity> =
                    HashMap<String, RenderEntity>() //define empty hashmap

                // 遍历解压后的文件，读取该数据返回
                for (importConfig in importConfigList) {
                    try {
                        for ((index, currentEntry) in importConfig.tableMap.entries.withIndex()) {
                            val currentConfig = currentEntry.value
                            val txtFile = unZipFiles.find {
                                it.name == currentConfig.table
                            }
                            // 将listResult数据插入到Realm数据库中
                            val realm = Realm.getDefaultInstance()
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
                                        Log.d("ImportOMDBHelper", "解析第：${index + 1}行")
                                        val map = gson.fromJson<Map<String, Any>>(
                                            line,
                                            object : TypeToken<Map<String, Any>>() {}.getType()
                                        )
                                            .toMutableMap()
                                        map["qi_table"] = currentConfig.table
                                        map["qi_name"] = currentConfig.name
                                        map["qi_code"] =
                                            if (currentConfig.code == 0) currentConfig.code else currentEntry.key
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

                                        renderEntity.geometry = map["geometry"].toString()
                                        // 其他数据插入到Properties中
                                        if (!currentConfig.is3D) { // 如果是非3d要素，则自动将Z轴坐标全部置为0
                                            val coordinates =
                                                renderEntity.wkt?.coordinates?.map { coordinate ->
                                                    coordinate.z = 0.0
                                                    coordinate
                                                }?.toTypedArray()
                                            var newGeometry: Geometry? = null
                                            if (renderEntity.wkt?.geometryType == Geometry.TYPENAME_POINT) {
                                                newGeometry = GeometryTools.createPoint(
                                                    coordinates!![0].x,
                                                    coordinates!![0].y
                                                )
                                            } else if (renderEntity.wkt?.geometryType == Geometry.TYPENAME_LINESTRING) {
                                                newGeometry =
                                                    GeometryTools.createLineString(coordinates)
                                            } else if (renderEntity.wkt?.geometryType == Geometry.TYPENAME_POLYGON) {
                                                newGeometry =
                                                    GeometryTools.createLineString(coordinates)
                                            }
                                            if (newGeometry != null) {
                                                renderEntity.geometry = newGeometry.toString()
                                            }
                                        }

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

                                                else -> renderEntity.properties.put(
                                                    key,
                                                    value.toString()
                                                )
                                            }
                                        }

                                        //测试代码
                                        /*                                    if(renderEntity.code == DataCodeEnum.OMDB_RD_LINK_KIND.code) {

                                                                                var currentLinkPid = renderEntity.properties["linkPid"]

                                                                                if(currentLinkPid!="84209046927907835"){
                                                                                    continue
                                                                                }
                                                                            }else if(renderEntity.code == DataCodeEnum.OMDB_RD_LINK.code){
                                                                                continue
                                                                            }else{
                                                                                continue
                                                                            }*/

                                        // 如果properties中不包含name，那么自动将要素名称添加进properties中
                                        if (!renderEntity.properties.containsKey("name")) {
                                            renderEntity.properties["name"] = renderEntity.name;
                                        }

                                        //优先过滤掉不需要的数据
                                        if (renderEntity.code == DataCodeEnum.OMDB_POLE.code) { // 杆状物
                                            //过滤树类型的杆状物，无需导入到数据库中
                                            val poleType = renderEntity.properties["poleType"]
                                            if (poleType != null && poleType.toInt() == 2) {
                                                continue
                                            }
                                        } else if (renderEntity.code == DataCodeEnum.OMDB_LANE_MARK_BOUNDARYTYPE.code) {
                                            var boundaryType =
                                                renderEntity.properties["boundaryType"]
                                            if (boundaryType != null) {
                                                when (boundaryType) {
                                                    "0", "1", "6", "8", "9" -> {
                                                        renderEntity.enable = 0
                                                        Log.e(
                                                            "qj",
                                                            "过滤不显示数据${renderEntity.table}"
                                                        )
                                                        continue
                                                    }
                                                }
                                            }
                                        } else if (renderEntity.code == DataCodeEnum.OMDB_RDBOUND_BOUNDARYTYPE.code) {

                                            //过滤不需要渲染的要素
                                            var boundaryType =
                                                renderEntity.properties["boundaryType"]
                                            if (boundaryType != null) {
                                                when (boundaryType) {
                                                    "0", "3", "4", "5", "7", "9" -> {
                                                        renderEntity.enable = 0
                                                        Log.e(
                                                            "qj",
                                                            "过滤不显示数据${renderEntity.table}"
                                                        )
                                                        continue
                                                    }
                                                }
                                            }
                                        } else if (renderEntity.code == DataCodeEnum.OMDB_OBJECT_STOPLOCATION.code) {
                                            //过滤不需要渲染的要素
                                            var locationType =
                                                renderEntity.properties["locationType"]
                                            if (locationType != null) {
                                                when (locationType) {
                                                    "3", "4" -> {
                                                        renderEntity.enable = 0
                                                        Log.e(
                                                            "qj",
                                                            "过滤不显示数据${renderEntity.table}"
                                                        )
                                                        continue
                                                    }
                                                }
                                            }
                                        }

                                        //交限增加相同LinkIn与LinkOut过滤原则
                                        if (renderEntity.code == DataCodeEnum.OMDB_RESTRICTION.code) {
                                            if (renderEntity.properties.containsKey("linkIn") && renderEntity.properties.containsKey(
                                                    "linkOut"
                                                )
                                            ) {
                                                var linkIn = renderEntity.properties["linkIn"]
                                                var linkOut = renderEntity.properties["linkOut"]
                                                if (linkIn != null && linkOut != null) {
                                                    var checkMsg = "$linkIn$linkOut"
                                                    if (resHashMap.containsKey(checkMsg)) {
                                                        Log.e(
                                                            "qj",
                                                            "${renderEntity.name}==过滤交限linkin与linkout相同且存在多条数据"
                                                        )
                                                        continue
                                                    } else {
                                                        resHashMap.put(checkMsg, renderEntity)
                                                    }
                                                }
                                            }
                                        }

                                        //遍历判断只显示与任务Link相关的任务数据
                                        if (currentConfig.checkLinkId) {

                                            if (renderEntity.properties.containsKey("linkPid")) {

                                                var currentLinkPid =
                                                    renderEntity.properties["linkPid"]

                                                if (!currentLinkPid.isNullOrEmpty() && currentLinkPid != "null") {

                                                    var list = currentLinkPid.split(",")

                                                    if (list != null && list.size > 0) {
                                                        m@ for (linkPid in list) {
                                                            if (hashMap.containsKey(linkPid)) {
                                                                renderEntity.enable = 1
                                                                Log.e(
                                                                    "qj",
                                                                    "${renderEntity.name}==包括任务link"
                                                                )
                                                                break@m
                                                            }
                                                        }
                                                    }
                                                }

                                            } else if (renderEntity.code == DataCodeEnum.OMDB_INTERSECTION.code && renderEntity.properties.containsKey(
                                                    "linkList"
                                                )
                                            ) {

                                                if (renderEntity.properties["linkList"] != null) {

                                                    Log.e(
                                                        "qj",
                                                        "linkList==开始${renderEntity.name}==${renderEntity.properties["linkList"]}}"
                                                    )

                                                    val linkList =
                                                        renderEntity.properties["linkList"]

                                                    if (!linkList.isNullOrEmpty() && linkList != "null") {

                                                        Log.e(
                                                            "qj",
                                                            "linkList==${renderEntity.name}==${renderEntity.properties["linkList"]}}"
                                                        )

                                                        val list: List<LinkList> = gson.fromJson(
                                                            linkList,
                                                            object :
                                                                TypeToken<List<LinkList>>() {}.type
                                                        )

                                                        if (list != null) {
                                                            m@ for (link in list) {
                                                                if (hashMap.containsKey(link.linkPid)) {
                                                                    renderEntity.enable = 1
                                                                    break@m
                                                                    Log.e(
                                                                        "qj",
                                                                        "${renderEntity.name}==包括任务link"
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        renderEntity.enable = 2
                                                        Log.e("qj", "简单路口")
                                                    }
                                                }
                                            } else {
                                                renderEntity.enable = 2
                                                Log.e(
                                                    "qj",
                                                    "${renderEntity.name}==不包括任务linkPid"
                                                )
                                            }
                                        } else {
                                            renderEntity.enable = 2
                                            Log.e("qj", "${renderEntity.name}==不包括任务linkPid")
                                        }


                                        // 对renderEntity做预处理后再保存
                                        try {
                                            val resultEntity =
                                                importConfig.transformProperties(renderEntity)

                                            if (resultEntity != null) {
                                                if (currentConfig.catch) {
                                                    renderEntity.catchEnable = 0
                                                } else {
                                                    renderEntity.catchEnable = 1
                                                }

                                                //对code编码需要特殊处理 存在多个属性值时，渲染优先级：SA>PA,存在多个属性值时，渲染优先级：FRONTAGE>MAIN_SIDE_A CCESS
                                                if (renderEntity.code == DataCodeEnum.OMDB_LINK_ATTRIBUTE.code) {

                                                    Log.e("qj", "道路属性===0")

                                                    var type = renderEntity.properties["sa"]

                                                    if (type != null && type == "1") {
                                                        renderEntity.code =
                                                            DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code
                                                        Log.e("qj", "道路属性===1")
                                                    } else {
                                                        type = renderEntity.properties["pa"]
                                                        if (type != null && type == "1") {
                                                            renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code
                                                            Log.e("qj", "道路属性===2")
                                                        } else {
                                                            type = renderEntity.properties["frontage"]
                                                            if (type != null && type == "1") {
                                                                renderEntity.code =
                                                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code
                                                                Log.e("qj", "道路属性===3")
                                                            } else {
                                                                type =
                                                                    renderEntity.properties["mainSideAccess"]
                                                                if (type != null && type == "1") {
                                                                    renderEntity.code =
                                                                        DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code
                                                                    Log.e("qj", "道路属性===4")
                                                                } else {
                                                                    renderEntity.enable = 0
                                                                    Log.e(
                                                                        "qj",
                                                                        "过滤不显示数据${renderEntity.table}"
                                                                    )
                                                                    Log.e("qj", "道路属性===5")
                                                                    continue
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (renderEntity.code == DataCodeEnum.OMDB_RAMP.code) {
                                                    /*匝道*/
                                                    var formWay = renderEntity.properties["formOfWay"]
                                                    if (formWay != null) {
                                                        when (formWay) {
                                                            "93" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_1.code

                                                            "98" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_2.code

                                                            "99" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_3.code

                                                            "100" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_4.code

                                                            "102" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_5.code

                                                            "103" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_6.code

                                                            "104" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_RAMP_7.code
                                                        }
                                                    }
                                                } else if (renderEntity.code == DataCodeEnum.OMDB_LINK_FORM1.code) {
                                                    /*道路形态1*/
                                                    var formWay = renderEntity.properties["formOfWay"]
                                                    if (formWay != null) {
                                                        when (formWay) {
                                                            "35" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM1_1.code

                                                            "37" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM1_2.code

                                                            "38" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM1_3.code
                                                        }
                                                    }
                                                } else if (renderEntity.code == DataCodeEnum.OMDB_LINK_FORM2.code) {
                                                    Log.e(
                                                        "qj",
                                                        "道路形态2${renderEntity.properties["formOfWay"]}"
                                                    )
                                                    /*道路形态2*/
                                                    var formWay = renderEntity.properties["formOfWay"]
                                                    if (formWay != null) {
                                                        when (formWay) {
                                                            "10" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_1.code

                                                            "11" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_2.code

                                                            "17" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_3.code

                                                            "18" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_4.code

                                                            "20" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_5.code

                                                            "22" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_6.code

                                                            "36" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_7.code

                                                            "52" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_8.code

                                                            "53" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_9.code

                                                            "54" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_10.code

                                                            "60" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_11.code

                                                            "84" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_12.code

                                                            "85" -> renderEntity.code =
                                                                DataCodeEnum.OMDB_LINK_FORM2_13.code
                                                        }
                                                    }
                                                } else if (renderEntity.table == DataCodeEnum.OMDB_NODE_FORM.name) {//特殊处理，因为code相同，使用表名判断
                                                    //过滤不需要渲染的要素
                                                    var formOfWay = renderEntity.properties["formOfWay"]
                                                    if (formOfWay != null && formOfWay == "30") {
                                                        renderEntity.enable = 2
                                                        renderEntity.code =
                                                            DataCodeEnum.OMDB_NODE_FORM.code
                                                    } else {
                                                        Log.e(
                                                            "qj",
                                                            "过滤不显示数据${renderEntity.table}"
                                                        )
                                                        continue
                                                    }
                                                } else if (renderEntity.table == DataCodeEnum.OMDB_NODE_PA.name) {//特殊处理，因为code相同，使用表名判断
                                                    //过滤不需要渲染的要素
                                                    var attributeType =
                                                        renderEntity.properties["attributeType"]
                                                    if (attributeType != null && attributeType == "30") {
                                                        renderEntity.enable = 2
                                                        renderEntity.code =
                                                            DataCodeEnum.OMDB_NODE_PA.code
                                                    } else {
                                                        Log.e(
                                                            "qj",
                                                            "过滤不显示数据${renderEntity.table}"
                                                        )
                                                        continue
                                                    }
                                                } else if (renderEntity.code == DataCodeEnum.OMDB_LANE_CONSTRUCTION.code) {
                                                    //特殊处理空数据，渲染原则使用
                                                    var startTime = renderEntity.properties["startTime"]
                                                    if (startTime == null || startTime == "") {
                                                        renderEntity.properties["startTime"] = "null"
                                                    }
                                                }
                                                listResult.add(renderEntity)
                                            }
                                        }catch ( e:Exception){

                                        }

                                    }
                                }
                            }
                            // 1个文件发送一次flow流
                            emit("${++processIndex}/${tableNum}")
                            realm.beginTransaction()
                            realm.insert(listResult)
                            realm.commitTransaction()
                            realm.close()
                            // 如果当前解析的是OMDB_RD_LINK数据，将其缓存在预处理类中，以便后续处理其他要素时使用
                            if (currentConfig.table == "OMDB_RD_LINK") {
                                importConfig.preProcess.cacheRdLink =
                                    listResult.associateBy { it.properties["linkPid"] }
                            }
                        }
                    } catch (e: Exception) {
                        throw e
                    }
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
package com.navinfo.omqs.ui.fragment.personalcenter

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.UriUtils
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.bean.ScRootCauseAnalysisBean
import com.navinfo.omqs.bean.ScWarningCodeBean
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.db.MultiPathsCallback
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.tools.MetadataUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.io.input.BOMInputStream
import java.io.*
import javax.inject.Inject


@HiltViewModel
class PersonalCenterViewModel @Inject constructor(
    private val roomAppDatabase: RoomAppDatabase
) : ViewModel() {
    @Inject
    lateinit var realmOperateHelper: RealmOperateHelper

    val liveDataMessage = MutableLiveData<String>()

//    /**
//     * 导入OMDB数据
//     * */
//    @RequiresApi(Build.VERSION_CODES.N)
//    suspend fun obtainOMDBZipData(importOMDBHelper: ImportOMDBHelper) {
//        Log.d("OMQSApplication", "开始生成数据")
//        val gson = Gson()
//        val hadLinkFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_LINK.txt")
//        val hadLinkKindFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_LINK_KIND.txt")
//        val hadLinkDirectFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_LINK_DIRECT.txt")
//        val hadSpeedLimitFile = File(importOMDBHelper.omdbFile.parentFile, "HAD_SPEEDLIMIT.txt")
//        val hadSpeedLimitCondFile =
//            File(importOMDBHelper.omdbFile.parentFile, "HAD_SPEEDLIMIT_COND.txt")
//        val hadSpeedLimitVarFile =
//            File(importOMDBHelper.omdbFile.parentFile, "HAD_SPEEDLIMIT_VAR.txt")
//
//        for (tableName in listOf<String>(
//            "HAD_LINK", "HAD_SPEEDLIMIT", "HAD_SPEEDLIMIT_COND", "HAD_SPEEDLIMIT_VAR"
//        )/*listOf<String>("HAD_LINK")*/) {
//            importOMDBHelper.getOMDBTableData(tableName).collect {
//                for (map in it) {
//                    if ("HAD_LINK" == tableName) {
//                        // 根据HAD_Link生成json文件
//                        val hadLink = HAD_LINK()
//                        hadLink.LINK_PID = map["LINK_PID"].toString()
//                        hadLink.MESH = map["MESH"].toString()
//                        hadLink.S_NODE_PID = map["S_NODE_PID"].toString()
//                        hadLink.E_NODE_PID = map["E_NODE_PID"].toString()
//                        hadLink.GEOMETRY = map["GEOMETRY"].toString()
//                        // 将该数据写入到对应的txt文件
//                        FileIOUtils.writeFileFromString(
//                            hadLinkFile, gson.toJson(hadLink) + "\r", true
//                        )
//
//                        val hadLinkDirect = HAD_LINK_DIRECT()
//                        hadLinkDirect.LINK_PID = map["LINK_PID"].toString()
//                        hadLinkDirect.MESH = map["MESH"].toString()
//                        hadLinkDirect.DIRECT = map["DIRECT"].toString().toInt()
//                        hadLinkDirect.GEOMETRY = map["GEOMETRY"].toString()
//                        // 将该数据写入到对应的txt文件
//                        FileIOUtils.writeFileFromString(
//                            hadLinkDirectFile, gson.toJson(hadLinkDirect) + "\r", true
//                        )
//
//                        val hadLinkKind = HAD_LINK_KIND()
//                        hadLinkKind.LINK_PID = map["LINK_PID"].toString()
//                        hadLinkKind.MESH = map["MESH"].toString()
//                        hadLinkKind.KIND = map["KIND"].toString().toInt()
//                        hadLinkKind.GEOMETRY = map["GEOMETRY"].toString()
//                        // 将该数据写入到对应的txt文件
//                        FileIOUtils.writeFileFromString(
//                            hadLinkKindFile, gson.toJson(hadLinkKind) + "\r", true
//                        )
//                    } else if ("HAD_SPEEDLIMIT" == tableName) {
//                        val hadSpeedlimit = HAD_SPEEDLIMIT()
//                        hadSpeedlimit.SPEED_ID = map["SPEED_ID"].toString()
//                        hadSpeedlimit.MESH = map["MESH"].toString()
//                        hadSpeedlimit.LINK_PID = map["LINK_PID"].toString()
//                        hadSpeedlimit.GEOMETRY = map["GEOMETRY"].toString()
//                        hadSpeedlimit.DIRECT = map["DIRECT"].toString().toInt()
//                        hadSpeedlimit.SPEED_FLAG = map["SPEED_FLAG"].toString().toInt()
//                        hadSpeedlimit.MAX_SPEED = map["MAX_SPEED"].toString().toInt()
//                        hadSpeedlimit.MIN_SPEED = map["MIN_SPEED"].toString().toInt()
//                        // 将该数据写入到对应的txt文件
//                        FileIOUtils.writeFileFromString(
//                            hadSpeedLimitFile, gson.toJson(hadSpeedlimit) + "\r", true
//                        )
//                    } else if ("HAD_SPEEDLIMIT_COND" == tableName) {
//                        val hadSpeedlimitCond = HAD_SPEEDLIMIT_COND()
//                        hadSpeedlimitCond.SPEED_COND_ID = map["SPEED_COND_ID"].toString()
//                        hadSpeedlimitCond.MESH = map["MESH"].toString()
//                        hadSpeedlimitCond.LINK_PID = map["LINK_PID"].toString()
//                        hadSpeedlimitCond.GEOMETRY = map["GEOMETRY"].toString()
//                        hadSpeedlimitCond.DIRECT = map["DIRECT"].toString().toInt()
//                        hadSpeedlimitCond.SPEED_FLAG = map["SPEED_FLAG"].toString().toInt()
//                        hadSpeedlimitCond.MAX_SPEED = map["MAX_SPEED"].toString().toInt()
//                        hadSpeedlimitCond.SPEED_DEPENDENT =
//                            map["SPEED_DEPENDENT"].toString().toInt()
//                        hadSpeedlimitCond.VEHICLE_TYPE = map["VEHICLE_TYPE"].toString().toInt()
//                        hadSpeedlimitCond.VALID_PERIOD = map["VALID_PERIOD"].toString()
//                        // 将该数据写入到对应的txt文件
//                        FileIOUtils.writeFileFromString(
//                            hadSpeedLimitCondFile, gson.toJson(hadSpeedlimitCond) + "\r", true
//                        )
//                    } else if ("HAD_SPEEDLIMIT_VAR" == tableName) {
//                        val hadSpeedlimitVar = HAD_SPEEDLIMIT_VAR()
//                        hadSpeedlimitVar.SPEED_VAR_ID = map["SPEED_ID"].toString()
//                        hadSpeedlimitVar.MESH = map["MESH"].toString()
//                        hadSpeedlimitVar.LINK_PID = map["LINK_PID"].toString()
//                        hadSpeedlimitVar.GEOMETRY = map["GEOMETRY"].toString()
//                        hadSpeedlimitVar.DIRECT = map["DIRECT"].toString().toInt()
//                        hadSpeedlimitVar.LOCATION = map["LOCATION"].toString()
//                        // 将该数据写入到对应的txt文件
//                        FileIOUtils.writeFileFromString(
//                            hadSpeedLimitVarFile, gson.toJson(hadSpeedlimitVar) + "\r", true
//                        )
//                    }
//                }
//            }
//        }
//        ZipUtils.zipFiles(
//            mutableListOf(
//                hadLinkFile,
//                hadLinkKindFile,
//                hadLinkDirectFile,
//                hadSpeedLimitFile,
//                hadSpeedLimitCondFile,
//                hadSpeedLimitVarFile
//            ), File(importOMDBHelper.omdbFile.parentFile, "output.zip")
//        )
//
//        Log.d("OMQSApplication", "生成数据完成")
//    }

    /**
     * 导入OMDB数据
     * */
    fun importOMDBData(importOMDBHelper: ImportOMDBHelper, task: TaskBean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("OMQSApplication", "开始导入数据")
            if (task != null) {
                importOMDBHelper.importOmdbZipFile(importOMDBHelper.omdbFile, task, this,object :MultiPathsCallback<String>{
                    override fun onProgress(value: Int) {
                    }

                    override fun onResult(value: String) {
                    }

                    override fun onError(t: Throwable) {
                    }

                    override fun onComplete() {
                    }
                })
            } else {
                val newTask = TaskBean()
                newTask.id = -1
                importOMDBHelper.importOmdbZipFile(importOMDBHelper.omdbFile, newTask, this,object :MultiPathsCallback<String>{
                    override fun onProgress(value: Int) {

                    }

                    override fun onResult(value: String) {
                    }

                    override fun onError(t: Throwable) {
                    }

                    override fun onComplete() {
                    }
                })

            }
            Log.d("OMQSApplication", "导入数据完成")
        }

    }

    fun importScProblemData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = UriUtils.uri2File(uri)
                val inputStream: InputStream =
                    FileInputStream(file) //getAssets().open("sample.xlsx")
                val bomInputStreamReader = BOMInputStream(inputStream)
                val inputStreamReader = InputStreamReader(bomInputStreamReader, "UTF-8")

                val bufferedReader = BufferedReader(inputStreamReader)
                var line: String? = null
                var index = 0
                var elementTypeIndex = -1
                var elementCodeIndex = -1
                var classTypeIndex = -1
                var problemTypeIndex = -1
                var phenomenonIndex = -1
                var problemLinkIndex = -1
                var problemCauseIndex = -1
                var warningCodeIndex = -1
                var warningDescribeIndex = -1
                val list = mutableListOf<ScProblemTypeBean>()
                val list2 = mutableListOf<ScRootCauseAnalysisBean>()
                val list3 = mutableListOf<ScWarningCodeBean>()
                while (bufferedReader.readLine()?.also { line = it } != null) {  // 处理 CSV 文件中的每一行数据
                    val data =
                        line!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (index == 0) {
                        for (i in data.indices) {
                            when (data[i]) {
                                MetadataUtils.Companion.ScProblemTypeTitle.TITLE_ELEMENT_TYPE -> {
                                    elementTypeIndex = i
                                }
                                MetadataUtils.Companion.ScProblemTypeTitle.TITLE_ELEMENT_CODE -> {
                                    elementCodeIndex = i
                                }
                                MetadataUtils.Companion.ScProblemTypeTitle.TITLE_CLASS_TYPE -> {
                                    classTypeIndex = i
                                }
                                MetadataUtils.Companion.ScProblemTypeTitle.TITLE_PROBLEM_TYPE -> {
                                    problemTypeIndex = i
                                }
                                MetadataUtils.Companion.ScProblemTypeTitle.TITLE_PHENOMENON -> {
                                    phenomenonIndex = i
                                }
                                MetadataUtils.Companion.ScRootCauseAnalysisTitle.TITLE_PROBLEM_LINK -> {
                                    problemLinkIndex = i
                                }
                                MetadataUtils.Companion.ScRootCauseAnalysisTitle.TITLE_PROBLEM_CAUSE -> {
                                    problemCauseIndex = i
                                }
                                MetadataUtils.Companion.ScWarningCodeTitle.TITLE_CODE -> {
                                    warningCodeIndex = i
                                }
                                MetadataUtils.Companion.ScWarningCodeTitle.TITLE_DESCRIBE -> {
                                    warningDescribeIndex = i
                                }

                            }
                        }
                    } else {
                        if (elementTypeIndex > -1
                            && elementCodeIndex > -1
                            && classTypeIndex > -1
                            && problemTypeIndex > -1
                            && phenomenonIndex > -1
                        ) {
                            val bean = ScProblemTypeBean(
                                elementType = data[elementTypeIndex],
                                elementCode = data[elementCodeIndex],
                                classType = data[classTypeIndex],
                                problemType = data[problemTypeIndex],
                                phenomenon = data[phenomenonIndex],
                            )
                            list.add(bean)
                        } else if (problemLinkIndex > -1 && problemCauseIndex > -1) {
                            val bean = ScRootCauseAnalysisBean(
                                problemLink = data[problemLinkIndex],
                                problemCause = data[problemCauseIndex],
                            )
                            list2.add(bean)
                        } else if (warningDescribeIndex > -1 && warningCodeIndex > -1) {
                            val bean = ScWarningCodeBean(
                                code = data[warningCodeIndex],
                                describe = data[warningDescribeIndex]
                            )
                            list3.add(bean)
                        } else {
                            liveDataMessage.postValue("元数据表规格不正确，请仔细核对")
                            break
                        }
                    }
                    index++
                }
                if (list.isNotEmpty()) {
                    liveDataMessage.postValue("元数据表导入成功")
                    roomAppDatabase.getScProblemTypeDao().insertOrUpdateList(list)
                }
                if (list2.isNotEmpty()) {
                    liveDataMessage.postValue("元数据表导入成功")
                    roomAppDatabase.getScRootCauseAnalysisDao().insertOrUpdateList(list2)
                }
                if (list3.isNotEmpty()) {
                    liveDataMessage.postValue("标牌对照表导入成功")
                    roomAppDatabase.getScWarningCodeDao().insertList(list3)
                }

                bufferedReader.close()
                inputStreamReader.close()
                inputStream.close()

            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("jingo", e.toString())
                liveDataMessage.postValue("元数据表导入失败！$e")
            }
        }
    }

    fun readRealmData() {
        viewModelScope.launch(Dispatchers.IO) {
//            val result = realmOperateHelper.queryLink(GeometryTools.createPoint(115.685817,28.62759))
            val realm = realmOperateHelper.getSelectTaskRealmInstance()
            val result = realmOperateHelper.queryLinkByLinkPid(realm, "84206617008217069")
            realm.close()
            Log.d("xiaoyan", result.toString())
        }
    }
}
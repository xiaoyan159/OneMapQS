package com.navinfo.omqs.http.taskupload

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.omqs.bean.EvaluationInfo
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.bean.SysUserBean
import com.navinfo.omqs.http.DefaultResponse
import com.navinfo.omqs.tools.FileManager.Companion.FileUploadStatus
import com.navinfo.omqs.util.DateTimeUtil
import io.realm.Realm
import kotlinx.coroutines.*
import java.util.*

class TaskUploadScope(
    private val uploadManager: TaskUploadManager,
    val taskBean: TaskBean,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("OfflineMapUpLoad")) {


    /**
     * 用来取消的
     */
    private var uploadJob: Job? = null

    /**
     * 管理观察者，同时只有一个就行了
     */
    private val observer = Observer<Any> {}
//    private var lifecycleOwner: LifecycleOwner? = null

    /**
     *通知UI更新
     */
    private val uploadData = MutableLiveData<TaskBean>()

    init {
        uploadData.value = taskBean
    }

    //改进的代码
    fun start() {
        change(FileUploadStatus.WAITING)
        uploadManager.launchScope(this@TaskUploadScope)
    }


    /**
     * 启动协程进行下载
     * 请不要尝试在外部调用此方法,那样会脱离[OfflineMapuploadManager]的管理
     */
    fun launch() {
        uploadJob = launch() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                upload()
            }
            uploadManager.launchNext(taskBean.id)
        }
    }

    /**
     * 更新状态
     * @param status [OfflineMapCityBean.Status]
     */
    private fun change(status: Int, message: String = "") {
        if (taskBean.syncStatus != status) {
            taskBean.syncStatus = status
            taskBean.errMsg = message
            //赋值时间，用于查询过滤
            taskBean.operationTime = DateTimeUtil.getNowDate().time
            uploadData.postValue(taskBean)
            //同步中不进行状态记录,只做界面变更显示
            if (status != FileUploadStatus.UPLOADING) {
                launch {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        it.copyToRealmOrUpdate(taskBean)
                    }
                }
            }
        }
    }

    /**
     * 是否未上传
     */
    fun isWaiting(): Boolean {
        return taskBean.syncStatus == FileUploadStatus.WAITING
    }

    /**
     * 添加下载任务观察者
     */
    fun observer(owner: LifecycleOwner, ob: Observer<TaskBean>) {
        removeObserver()
        uploadData.observe(owner, ob)
    }

    /**
     * 上传文件
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun upload() {
        try {
            //如果已上传则返回
            if (taskBean.syncStatus == FileUploadStatus.DONE) {
                return
            }

            val realm = Realm.getDefaultInstance()

            val bodyList: MutableList<EvaluationInfo> = ArrayList()

            if (taskBean.syncStatus == FileUploadStatus.WAITING) {
                change(FileUploadStatus.UPLOADING)
            }

            taskBean.hadLinkDvoList.forEach { hadLinkDvoBean ->

                val linkStatus = 1
                //存在原因标记未测评
                if (hadLinkDvoBean.reason.isNotEmpty()) {
                    //未测评
                    val linkStatus = 0

                    val evaluationInfo = EvaluationInfo(
                        evaluationTaskId = taskBean.id.toString(),
                        linkPid = hadLinkDvoBean.linkPid,//"84207223282277331"
                        linkStatus = linkStatus,
                        markId = hadLinkDvoBean.mesh,//"20065597"
                        trackPhotoNumber = "",
                        markGeometry = "",
                        featureName = "",
                        problemType = 0,
                        problemPhenomenon = "",
                        problemDesc = "",
                        problemLink = "",
                        preliminaryAnalysis = "",
                        evaluatorName = "",
                        evaluationDate = "",
                        evaluationWay = 2,
                        roadClassfcation = 1,
                        roadFunctionGrade = 0,
                        noEvaluationreason = hadLinkDvoBean.reason,
                        linkLength = 0.0,
                        dataLevel = 0,
                        linstringLength = 0.0,
                    )

                    bodyList.add(evaluationInfo)

                } else {

                    val linkStatus = hadLinkDvoBean.linkStatus

                    var s: String = "%.3f".format(hadLinkDvoBean.length)//保留一位小数(且支持四舍五入)

                    val objects = realm.where(QsRecordBean::class.java)
                        .equalTo("linkId", /*"84207223282277331"*/hadLinkDvoBean.linkPid).and()
                        .equalTo("taskId", hadLinkDvoBean.taskId).findAll()

                    if (objects != null && objects.size > 0) {
                        val copyList = realm.copyFromRealm(objects)
                        copyList.forEach {
                            var problemType = 0
                            if (it.problemType == "错误") {
                                problemType = 0
                            } else if (it.problemType == "多余") {
                                problemType = 1
                            } else if (it.problemType == "遗漏") {
                                problemType = 2
                            }

                            var roadClassfcation = 0

                            var roadFunctionGrade = 0

                            var dataLevel = 0

                            if (hadLinkDvoBean.linkInfo != null) {
                                roadClassfcation = hadLinkDvoBean.linkInfo!!.kind
                                roadFunctionGrade = hadLinkDvoBean.linkInfo!!.functionLevel
                                dataLevel = hadLinkDvoBean.linkInfo!!.dataLevel
                            }

                            var evaluationWay = 2
                            val evaluationInfo = EvaluationInfo(
                                evaluationTaskId = taskBean.id.toString(),
                                linkPid = hadLinkDvoBean.linkPid,//"84207223282277331"
                                linkStatus = linkStatus,
                                markId = hadLinkDvoBean.mesh,//"20065597"
                                trackPhotoNumber = "",
                                markGeometry = it.geometry,
                                featureName = it.classCode,
                                problemType = problemType,
                                problemPhenomenon = it.phenomenon,
                                problemDesc = it.description,
                                problemLink = it.problemLink,
                                preliminaryAnalysis = it.cause,
                                evaluatorName = it.checkUserId,
                                evaluationDate = it.checkTime,
                                evaluationWay = evaluationWay,
                                roadClassfcation = roadClassfcation,
                                roadFunctionGrade = roadFunctionGrade,
                                noEvaluationreason = "",
                                linkLength = s.toDouble(),
                                dataLevel = dataLevel,
                                linstringLength = 0.0,
                            )

                            bodyList.add(evaluationInfo)
                        }
                    }
                }

            }

            if (bodyList.size > 0) {
                val result = uploadManager.netApi.postRequest(bodyList)// .enqueue(object :
//                        Callback<ResponseBody> {
                if (result.isSuccessful) {
                    if (result.code() == 200 && result.body() != null) {
                        val defaultUserResponse = result.body() as DefaultResponse<*>
                        if (defaultUserResponse.success) {
                            change(FileUploadStatus.DONE, "上传成功")
                        } else {
                            change(FileUploadStatus.ERROR, "${defaultUserResponse.msg}")
                        }
                    } else {
                        // handle the failure
                        change(FileUploadStatus.ERROR)
                    }
                } else {
                    change(FileUploadStatus.ERROR)
                }
            } else {
                change(FileUploadStatus.NONE)
            }
        } catch (e: Throwable) {
            change(FileUploadStatus.ERROR)
            Log.e("jingo", "数据上传出错 ${e.message}")
        } finally {

        }
    }

    fun removeObserver() {
        uploadData.observeForever(observer)
        uploadData.removeObserver(observer)
    }
}
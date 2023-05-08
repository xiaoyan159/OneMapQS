package com.navinfo.omqs.http.taskupload

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.omqs.bean.EvaluationInfo
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.tools.FileManager.Companion.FileUploadStatus
import io.realm.Realm
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
            uploadData.postValue(taskBean)
            launch {
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    it.copyToRealmOrUpdate(taskBean)
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
            taskBean.hadLinkDvoList.forEach { hadLinkDvoBean ->
                val objects = realm.where(QsRecordBean::class.java)
                    .equalTo("linkId", /*"84207223282277331"*/hadLinkDvoBean.linkPid).findAll()
                if (objects != null) {
                    val copyList = realm.copyFromRealm(objects)
                    copyList.forEach {
                        val evaluationInfo = EvaluationInfo(
                            evaluationTaskId = taskBean.id.toString(),
                            linkPid = hadLinkDvoBean.linkPid,//"84207223282277331"
                            linkStatus = "已测评",
                            markId = hadLinkDvoBean.mesh,//"20065597"
                            trackPhotoNumber = "",
                            markGeometry = it.geometry,
                            featureName = it.classType,
                            problemType = it.problemType,
                            problemPhenomenon = it.phenomenon,
                            problemDesc = it.description,
                            problemLink = it.problemLink,
                            problemReason = it.cause,
                            evaluatorName = it.checkUserId,
                            evaluationDate = it.checkTime,
                            evaluationWay = "现场测评"
                        )

                        bodyList.add(evaluationInfo)
                    }

                }
            }
            if (bodyList.size == 0) {
                if (taskBean.syncStatus == FileUploadStatus.WAITING)
                    change(FileUploadStatus.NONE)
                return
            }
            val result = uploadManager.netApi.postRequest(bodyList)// .enqueue(object :
//                        Callback<ResponseBody> {
            if (result.isSuccessful) {
                if (result.code() == 200) {
                    // handle the response
                    change(FileUploadStatus.DONE)
                } else {
                    // handle the failure
                    change(FileUploadStatus.ERROR)
                }
            } else {
                change(FileUploadStatus.ERROR)
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
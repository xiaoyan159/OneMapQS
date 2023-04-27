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
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
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
        change(FileDownloadStatus.WAITING)
        uploadManager.launchScope(this@TaskUploadScope)
    }


    /**
     * 启动协程进行下载
     * 请不要尝试在外部调用此方法,那样会脱离[OfflineMapuploadManager]的管理
     */
    fun launch() {
        uploadJob = launch() {
            upload()
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
        return taskBean.syncStatus == FileManager.Companion.FileUploadStatus.WAITING
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
            if (taskBean.syncStatus == FileManager.Companion.FileUploadStatus.DONE) {
                return
            }

            val realm = Realm.getDefaultInstance()
            taskBean.hadLinkDvoList.forEach {
                val hadLinkDvoBean = it
                val liveDataQSList = MutableLiveData<List<QsRecordBean>>()
                val objects = realm.where(QsRecordBean::class.java)
                    .equalTo("linkId", /*"84207223282277331"*/hadLinkDvoBean.linkPid).findAll()
                val bodyList: MutableList<EvaluationInfo> = ArrayList()
                if (objects != null) {

                    liveDataQSList.postValue(realm.copyFromRealm(objects))

                    if (liveDataQSList.value!!.isNotEmpty()) {

                        liveDataQSList.value?.forEach {
                            val evaluationInfo = EvaluationInfo(
                                taskBean.id.toString(),
                                hadLinkDvoBean.linkPid,//"84207223282277331"
                                "已测评",
                                hadLinkDvoBean.mesh,//"20065597"
                                "",
                                it.geometry,
                                it.classType,
                                it.problemType,
                                it.phenomenon,
                                it.description,
                                it.problemLink,
                                it.cause,
                                it.checkUserId,
                                it.checkTime
                            )

                            bodyList.add(evaluationInfo)
                        }

                        uploadManager.netApi.postRequest(bodyList).enqueue(object :
                            Callback<ResponseBody> {
                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                if(response.code()==200){
                                    taskBean.syncStatus = FileManager.Companion.FileUploadStatus.DONE
                                    // handle the response
                                    Log.e("qj", "")
                                    change(FileManager.Companion.FileUploadStatus.DONE)
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                // handle the failure
                                Log.e("qj", "")
                                change(FileManager.Companion.FileUploadStatus.ERROR)
                            }
                        })
                    }
                }
            }

        } catch (e: Throwable) {
            change(FileManager.Companion.FileUploadStatus.ERROR)
            Log.e("jingo", "数据上传出错 ${e.message}")
        } finally {

        }
    }

    fun removeObserver() {
        uploadData.observeForever(observer)
        uploadData.removeObserver(observer)
    }
}
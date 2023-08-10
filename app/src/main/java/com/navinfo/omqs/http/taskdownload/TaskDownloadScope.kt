package com.navinfo.omqs.http.taskdownload

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.Constant
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
import com.navinfo.omqs.util.DateTimeUtil
import io.realm.Realm
import kotlinx.coroutines.*
import org.oscim.android.theme.AssetsRenderTheme
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

class TaskDownloadScope(
    private val downloadManager: TaskDownloadManager,
    val taskBean: TaskBean,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("TaskMapDownLoad")) {

    /**
     *下载任务，用来取消的
     */
    private var downloadJob: Job? = null

    /**
     * 管理观察者，同时只有一个就行了
     */
//    private val observer = Observer<Any> {}
    private var lifecycleOwner: LifecycleOwner? = null

    /**
     *通知UI更新
     */
    private val downloadData = MutableLiveData<TaskBean>()

    init {
        downloadData.value = taskBean
    }

    //改进的代码
    fun start() {
        launch {
            change(FileDownloadStatus.WAITING)
        }
        downloadManager.launchScope(this@TaskDownloadScope)
    }

    /**
     * 暂停任务
     * 其实就是取消任务，移除监听
     */
    fun pause() {
        downloadJob?.cancel("pause")
        launch {
            if (taskBean.fileSize == 0L) {
                change(FileDownloadStatus.NONE)
            } else {
                change(FileDownloadStatus.PAUSE)
            }
        }

    }

    /**
     * 启动协程进行下载
     * 请不要尝试在外部调用此方法,那样会脱离[OfflineMapDownloadManager]的管理
     */
    fun launch() {
        downloadJob = launch() {
            FileManager.checkOMDBFileInfo(taskBean)
            if (taskBean.status == FileDownloadStatus.IMPORT) {
                importData(task = taskBean)
            } else {
                download()
            }
            downloadManager.launchNext(taskBean.id)
        }
    }


    /**
     * 是否是等待任务
     */
    fun isWaiting(): Boolean {
        return taskBean.status == FileDownloadStatus.WAITING
    }

    /**
     * 更新任务
     * @param status [OfflineMapCityBean.Status]
     */
    private suspend fun change(status: Int, message: String = "") {

        if (taskBean.status != status || status == FileDownloadStatus.LOADING || status == FileDownloadStatus.IMPORTING) {
            taskBean.status = status
            taskBean.message = message
            //赋值时间，用于查询过滤
            taskBean.operationTime = DateTimeUtil.getNowDate().time
            downloadData.postValue(taskBean)
            if (status != FileDownloadStatus.LOADING && status != FileDownloadStatus.IMPORTING) {
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    it.insertOrUpdate(taskBean)
                }
            }
        }
    }

    /**
     * 添加下载任务观察者
     */
    fun observer(owner: LifecycleOwner, ob: Observer<TaskBean>) {

        removeObserver()
        this.lifecycleOwner = owner
        downloadData.observe(owner, ob)
    }

    /**
     * 导入数据
     */
    private suspend fun importData(file: File? = null, task: TaskBean? = null) {
        try {
            Log.e("jingo", "importData SSS")
            change(FileDownloadStatus.IMPORTING)
            var fileNew = file
                ?: File("${Constant.DOWNLOAD_PATH}${taskBean.evaluationTaskName}_${taskBean.dataVersion}.zip")
            val importOMDBHelper: ImportOMDBHelper =
                downloadManager.importFactory.obtainImportOMDBHelper(
                    downloadManager.context,
                    fileNew
                )
            if (task != null) {
                importOMDBHelper.importOmdbZipFile(importOMDBHelper.omdbFile, task).collect {
                    Log.e("jingo", "数据安装 $it")
                    if (it == "finish") {
                        change(FileDownloadStatus.DONE)
                        withContext(Dispatchers.Main) {
                            downloadManager.mapController.layerManagerHandler.updateOMDBVectorTileLayer()
                        }
                    } else {
                        change(FileDownloadStatus.IMPORTING, it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("jingo", "数据安装失败 ${e.toString()}")
            change(FileDownloadStatus.ERROR)
        } finally {

        }

        Log.e("jingo", "importData EEE")
    }

    /**
     * 下载文件
     */
    private suspend fun download() {
        //如果文件下载安装已经完毕
        if (taskBean.status == FileDownloadStatus.DONE) {
            return
        }
        var inputStream: InputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        try {
            //创建离线地图 下载文件夹，.map文件夹的下一级
            val fileDir = File("${Constant.DOWNLOAD_PATH}")
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }

            val fileTemp =
                File("${Constant.DOWNLOAD_PATH}${taskBean.evaluationTaskName}_${taskBean.dataVersion}.zip")
            var startPosition = taskBean.currentSize
            if (fileTemp.length() > taskBean.fileSize && taskBean.fileSize > 0) {
                fileTemp.delete()
                fileTemp.createNewFile()
                startPosition = 0
            }
            if (fileTemp.length() > 0 && taskBean.fileSize > 0 && fileTemp.length() == taskBean.fileSize) {
                importData(fileTemp, taskBean)
                return
            }

            val response = downloadManager.netApi.retrofitDownLoadFile(
                start = "bytes=$startPosition-",
                url = taskBean.getDownLoadUrl()
            )
            val responseBody = response.body()


            responseBody ?: throw IOException("jingo ResponseBody is null")
            if (startPosition == 0L) {
                taskBean.fileSize = responseBody.contentLength()
            }
            change(FileDownloadStatus.LOADING)
            //写入文件
            randomAccessFile = RandomAccessFile(fileTemp, "rwd")
            randomAccessFile.seek(startPosition)
            taskBean.currentSize = startPosition
            inputStream = responseBody.byteStream()
            val bufferSize = 1024 * 4
            val buffer = ByteArray(bufferSize)

            var readLength = 0
            while (downloadJob?.isActive == true) {
                readLength = inputStream.read(buffer)
                if (readLength != -1) {
                    randomAccessFile.write(buffer, 0, readLength)
                    taskBean.currentSize += readLength
                    change(FileDownloadStatus.LOADING)
                } else {
                    break
                }
            }

            if (taskBean.currentSize == taskBean.fileSize) {
                inputStream?.close()
                randomAccessFile?.close()
                inputStream = null
                randomAccessFile = null
                importData(task = taskBean)
            } else {
                change(FileDownloadStatus.PAUSE)
            }
        } catch (e: Throwable) {
            change(FileDownloadStatus.ERROR)
            Log.e("jingo", "数据下载出错 ${e.message}")
        } finally {
            inputStream?.close()
            randomAccessFile?.close()
        }
    }

    fun removeObserver() {
//        downloadData.observeForever(observer)
////        lifecycleOwner?.let {
//        downloadData.removeObserver(observer)
////            null
////        }
        if (lifecycleOwner != null) {
            downloadData.removeObservers(lifecycleOwner!!)
        }
    }
}
package com.navinfo.omqs.http.taskdownload

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

class TaskDownloadScope(
    private val downloadManager: TaskDownloadManager,
    val taskBean: TaskBean,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("OfflineMapDownLoad")) {
    /**
     *下载任务，用来取消的
     */
    private var downloadJob: Job? = null

    /**
     * 管理观察者，同时只有一个就行了
     */
    private val observer = Observer<Any> {}
//    private var lifecycleOwner: LifecycleOwner? = null

    /**
     *通知UI更新
     */
    private val downloadData = MutableLiveData<TaskBean>()

    init {
        downloadData.value = taskBean
    }

    //改进的代码
    fun start() {
        change(FileDownloadStatus.WAITING)
        downloadManager.launchScope(this@TaskDownloadScope)
    }

    /**
     * 暂停任务
     * 其实就是取消任务，移除监听
     */
    fun pause() {
        downloadJob?.cancel("pause")
        change(FileDownloadStatus.PAUSE)
    }

    /**
     * 启动协程进行下载
     * 请不要尝试在外部调用此方法,那样会脱离[OfflineMapDownloadManager]的管理
     */
    fun launch() {
        downloadJob = launch() {
            download()
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
    private fun change(status: Int) {
        if (taskBean.status != status || status == FileDownloadStatus.LOADING) {
            taskBean.status = status
            downloadData.postValue(taskBean)
            launch(Dispatchers.IO) {
//                downloadManager.roomDatabase.getOfflineMapDao().update(taskBean)
            }

        }
    }

    /**
     * 添加下载任务观察者
     */
    fun observer(owner: LifecycleOwner, ob: Observer<TaskBean>) {
        removeObserver()
//        this.lifecycleOwner = owner
        downloadData.observe(owner, ob)
    }

    /**
     * 下载文件
     */
    private suspend fun download() {
        var inputStream: InputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        try {
            //创建离线地图 下载文件夹，.map文件夹的下一级
            val fileDir = File("${Constant.OFFLINE_MAP_PATH}download")
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }

            val fileTemp =
                File("${Constant.OFFLINE_MAP_PATH}download/${taskBean.id}_${taskBean.dataVersion}")
            val startPosition = taskBean.currentSize
            //验证断点有效性
            if (startPosition < 0) throw IOException("jingo Start position less than zero")
            val response = downloadManager.netApi.retrofitDownLoadFile(
                start = "bytes=$startPosition-",
                url = taskBean.getDownLoadUrl()
            )
            val responseBody = response.body()
            change(FileDownloadStatus.LOADING)
            responseBody ?: throw IOException("jingo ResponseBody is null")
            //写入文件
            randomAccessFile = RandomAccessFile(fileTemp, "rwd")
            randomAccessFile.seek(startPosition)
            taskBean.currentSize = startPosition
            inputStream = responseBody.byteStream()
            val bufferSize = 1024 * 2
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

            Log.e("jingo", "文件下载完成 ${taskBean.currentSize} == ${taskBean.fileSize}")
            if (taskBean.currentSize == taskBean.fileSize) {
                val res =
                    fileTemp.renameTo(File("${Constant.OFFLINE_MAP_PATH}${taskBean.evaluationTaskName}.zip"))
                Log.e("jingo", "文件下载完成 修改文件 $res")
                change(FileDownloadStatus.DONE)
            } else {
                change(FileDownloadStatus.PAUSE)
            }
        } catch (e: Throwable) {
            change(FileDownloadStatus.ERROR)
        } finally {
            inputStream?.close()
            randomAccessFile?.close()
        }
    }

    fun removeObserver() {
        downloadData.observeForever(observer)
//        lifecycleOwner?.let {
        downloadData.removeObserver(observer)
//            null
//        }
    }
}
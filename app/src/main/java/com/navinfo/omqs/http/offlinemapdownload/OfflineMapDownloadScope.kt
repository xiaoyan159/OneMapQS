package com.navinfo.omqs.http.offlinemapdownload

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.OfflineMapCityBean
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * 代表一个下载任务
 * [OfflineMapCityBean.id]将做为下载任务的唯一标识
 * 不要直接在外部直接创建此对象,那样就可能无法统一管理下载任务,请通过[OfflineMapDownloadManager.request]获取此对象
 * 这是一个协程作用域，
 * EmptyCoroutineContext 表示一个不包含任何元素的协程上下文，它通常用于创建新的协程上下文，或者作为协程上下文的基础。
 */
class OfflineMapDownloadScope(
    private val downloadManager: OfflineMapDownloadManager,
    val cityBean: OfflineMapCityBean,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
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
    private val downloadData = MutableLiveData<OfflineMapCityBean>()

    init {
        downloadData.value = cityBean
    }

    //改进的代码
    fun start() {
        change(OfflineMapCityBean.WAITING)
        downloadManager.launchScope(this@OfflineMapDownloadScope)
    }

    /**
     * 暂停任务
     * 其实就是取消任务，移除监听
     */
    fun pause() {
        downloadJob?.cancel("pause")
        change(OfflineMapCityBean.PAUSE)
    }

    /**
     * 启动协程进行下载
     * 请不要尝试在外部调用此方法,那样会脱离[OfflineMapDownloadManager]的管理
     */
    fun launch() {
        downloadJob = launch() {
            Log.e("jingo", "启动下载1")
            download()
            Log.e("jingo", "启动下载2")
            downloadManager.launchNext(cityBean.id)
            Log.e("jingo", "启动下载3")
        }
    }


    /**
     * 是否是等待任务
     */
    fun isWaiting(): Boolean {
        return cityBean.status == OfflineMapCityBean.WAITING
    }

    /**
     * 更新任务
     * @param status [OfflineMapCityBean.Status]
     */
    private fun change(status: Int) {
        if (cityBean.status != status || status == OfflineMapCityBean.LOADING) {
            cityBean.status = status
            downloadData.postValue(cityBean)
            launch(Dispatchers.IO) {
                downloadManager.roomDatabase.getOfflineMapDao().update(cityBean)
            }

        }
    }

    /**
     * 添加下载任务观察者
     */
    fun observer(owner: LifecycleOwner, ob: Observer<OfflineMapCityBean>) {
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
                File("${Constant.OFFLINE_MAP_PATH}download/${cityBean.id}_${cityBean.version}")
            val startPosition = cityBean.currentSize
            //验证断点有效性
            if (startPosition < 0) throw IOException("jingo Start position less than zero")
            val response = downloadManager.netApi.retrofitDownLoadFile(
                start = "bytes=$startPosition-",
                url = cityBean.url
            )
            val responseBody = response.body()
            change(OfflineMapCityBean.LOADING)
            responseBody ?: throw IOException("jingo ResponseBody is null")
            //写入文件
            randomAccessFile = RandomAccessFile(fileTemp, "rwd")
            randomAccessFile.seek(startPosition)
            cityBean.currentSize = startPosition
            inputStream = responseBody.byteStream()
            val bufferSize = 1024 * 2
            val buffer = ByteArray(bufferSize)

            var readLength = 0
            while (downloadJob?.isActive == true) {
                readLength = inputStream.read(buffer)
                if (readLength != -1) {
                    randomAccessFile.write(buffer, 0, readLength)
                    cityBean.currentSize += readLength
                    change(OfflineMapCityBean.LOADING)
                } else {
                    break
                }
            }

            Log.e("jingo", "文件下载完成 ${cityBean.currentSize} == ${cityBean.fileSize}")
            if (cityBean.currentSize == cityBean.fileSize) {
                val res =
                    fileTemp.renameTo(File("${Constant.OFFLINE_MAP_PATH}${cityBean.fileName}"))
                Log.e("jingo", "文件下载完成 修改文件 $res")
                change(OfflineMapCityBean.DONE)
                withContext(Dispatchers.Main) {
                    downloadManager.mapController.layerManagerHandler.loadBaseMap()
                }
            } else {
                change(OfflineMapCityBean.PAUSE)
            }
        } catch (e: Throwable) {
            change(OfflineMapCityBean.ERROR)
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
package com.navinfo.omqs.http.offlinemapdownload

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 代表一个下载任务
 * [OfflineMapCityBean.id]将做为下载任务的唯一标识
 * 不要直接在外部直接创建此对象,那样就可能无法统一管理下载任务,请通过[OfflineMapDownloadManager.request]获取此对象
 * 这是一个协程作用域，
 * EmptyCoroutineContext 表示一个不包含任何元素的协程上下文，它通常用于创建新的协程上下文，或者作为协程上下文的基础。
 */
class OfflineMapDownloadScope(
    private val downloadManager: OfflineMapDownloadManager,
    private val netApi: RetrofitNetworkServiceAPI,
    val cityBean: OfflineMapCityBean

) :
    CoroutineScope by CoroutineScope(EmptyCoroutineContext) {
    /**
     *下载任务，用来取消的
     */
    private var downloadJob: Job? = null

    /**
     * 管理观察者，同时只有一个就行了
     */
    private var observer: Observer<OfflineMapCityBean>? = null

    /**
     *
     */
    private val downloadData = MutableLiveData<OfflineMapCityBean>()

    init {
        downloadData.value = cityBean
    }

    /**
     * 开始任务的下载
     * [OfflineMapCityBean]是在协程中进行创建的,它的创建会优先从数据库和本地文件获取,但这种操作是异步的,详情请看init代码块
     * 我们需要通过观察者观察[OfflineMapCityBean]来得知它是否已经创建完成,只有当他创建完成且不为空(如果创建完成,它一定不为空)
     * 才可以交由[OfflineMapDownloadManager]进行下载任务的启动
     * 任务的开始可能并不是立即的,任务会受到[OfflineMapDownloadManager]的管理
     *
     * 这段原来代码没看懂：要触发 Observer 得观察的对象[OfflineMapCityBean]发生变化才行，原demo里没找到livedata的变化也触发了onChange，这里根本触发不了
     *
     * 找到原因了：是[cityBean]根本没有设置到liveData中，但是还是不用这样了，因为cityBean是一定创建好了的
     */
    //原代码
//    fun start() {
//        var observer: Observer<OfflineMapCityBean>? = null
//        observer = Observer { cityBean ->
//            Log.e("jingo","Observer 创建了，bean 为null吗？$cityBean")
//            cityBean?.let {
//                observer?.let {
//                    Log.e("jingo","Observer 这里为什么要解除观察？")
//                    downloadData.removeObserver(it)
//                }
//                Log.e("jingo","Observer 状态 ${cityBean.status} ")
//                when (cityBean.status) {
//
//                    OfflineMapCityBean.PAUSE, OfflineMapCityBean.ERROR, OfflineMapCityBean.NONE -> {
//                        change(OfflineMapCityBean.WAITING)
//                        downloadManager.launchScope(this@OfflineMapDownloadScope)
//                    }
//                }
//            }
//        }
//        downloadData.observeForever(observer)
//    }
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
    }

    /**
     * 启动协程进行下载
     * 请不要尝试在外部调用此方法,那样会脱离[OfflineMapDownloadManager]的管理
     */
    fun launch() {
        downloadJob = launch {
            try {
                download()
                change(OfflineMapCityBean.DONE)
            } catch (e: Throwable) {
                Log.e("jingo DownloadScope", "error:${e.message}")
                if (e.message == "pause") {
                    change(OfflineMapCityBean.PAUSE)
                } else {
                    change(OfflineMapCityBean.ERROR)
                }
            } finally {
                downloadManager.launchNext(cityBean.id)
            }
        }
    }


    /**
     * 是否是等待任务
     */
    fun isWaiting(): Boolean {
        val downloadInfo = downloadData.value
        downloadInfo ?: return false
        return downloadInfo.status == OfflineMapCityBean.WAITING
    }

    /**
     * 更新任务
     * @param status [OfflineMapCityBean.Status]
     */
    private fun change(status: Int) {
        downloadData.value?.let {
            it.status = status
            downloadData.postValue(it)
        }
    }

    /**
     * 添加下载任务观察者
     */
    fun observer(lifecycleOwner: LifecycleOwner, ob: Observer<OfflineMapCityBean>) {
        if (observer != null) {
            downloadData.removeObserver(observer!!)
        }
        this.observer = ob
        downloadData.observe(lifecycleOwner, observer!!)
    }

    /**
     * 下载文件
     */
    private suspend fun download() = withContext(context = Dispatchers.IO, block = {

        val downloadInfo = downloadData.value ?: throw IOException("jingo Download info is null")
        //创建离线地图 下载文件夹，.map文件夹的下一级
        val fileDir = File("${Constant.OFFLINE_MAP_PATH}download")
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        //遍历文件夹，找到对应的省市.map文件
        val files = fileDir.listFiles()
        for (item in files) {
            //用id找到对应的文件
            if (item.isFile && item.name.startsWith(downloadInfo.id)) {
                //判断文件的版本号是否一致
                if (item.name.contains("_${downloadInfo.version}.map")) {
                    //都一致，说明文件已经下载完成，不用再次下载
                    change(OfflineMapCityBean.DONE)
                    return@withContext
                }else{

                }
                break
            }
        }

        //查看下.map文件夹在不在
        val fileMap = File("${Constant.OFFLINE_MAP_PATH}${downloadInfo.fileName}")
        val fileTemp =
            File("${Constant.OFFLINE_MAP_PATH}download/${downloadInfo.id}_${downloadInfo.version}")


        if (fileTemp.exists()) {

        }

        if (!fileMap.exists()) {
        }

        change(OfflineMapCityBean.LOADING)


        val startPosition = downloadInfo.currentSize
        //验证断点有效性
        if (startPosition < 0) throw IOException("jingo Start position less than zero")
        //下载的文件是否已经被删除
//        if (startPosition > 0 && !TextUtils.isEmpty(downloadInfo.path))
//            if (!File(downloadInfo.path).exists()) throw IOException("File does not exist")
        val response = netApi.retrofitDownLoadFile(
            start = "bytes=$startPosition-",
            url = downloadInfo.url
        )
        val responseBody = response.body()

        responseBody ?: throw IOException("jingo ResponseBody is null")
        //文件长度
        downloadInfo.fileSize = responseBody.contentLength()
        //保存的文件名称
//        if (TextUtils.isEmpty(downloadInfo.fileName))
//            downloadInfo.fileName = UrlUtils.getUrlFileName(downloadInfo.url)

//        //验证下载完成的任务与实际文件的匹配度
//        if (startPosition == downloadInfo.fileSize && startPosition > 0) {
//            if (file.exists() && startPosition == file.length()) {
//                change(OfflineMapCityBean.DONE)
//                return@withContext
//            } else throw IOException("jingo The content length is not the same as the file length")
//        }
        //写入文件
        val randomAccessFile = RandomAccessFile(fileTemp, "rwd")
        randomAccessFile.seek(startPosition)
//        if (downloadInfo.currentSize == 0L) {
//            randomAccessFile.setLength(downloadInfo.fileSize)
//        }
        downloadInfo.currentSize = startPosition
        val inputStream = responseBody.byteStream()
        val bufferSize = 1024 * 2
        val buffer = ByteArray(bufferSize)
        try {
            var readLength = 0
            while (isActive) {
                readLength = inputStream.read(buffer)
                if (readLength != -1) {
                    randomAccessFile.write(buffer, 0, readLength)
                    downloadInfo.currentSize += readLength
                    change(OfflineMapCityBean.LOADING)
                } else {
                    break
                }
            }
        } finally {
            inputStream.close()
            randomAccessFile.close()
        }
    })

    /**
     *
     */
    private fun checkFile(){

    }

}
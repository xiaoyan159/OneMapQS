//package com.navinfo.collect.library.data.handler
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.media.ExifInterface
//import android.util.Log
//import com.baidu.ai.edge.ui.util.ImageUtil
//import com.baidu.ai.edge.ui.view.model.OcrViewResultModel
//import com.navinfo.collect.FlutterBaseActivity
//import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
//import com.navinfo.ocr.OCRManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.*
//import java.util.*
//
//interface OnOCRBatchListener {
//    fun onProgress(total: Int, current: Int)
//    suspend fun onResult(list: List<Map<String, Any>>)
//}
//
//open class DataCameraHandler(
//    context: Context,
//    activity: FlutterBaseActivity,
//    dataBase: MapLifeDataBase
//) :
//    BaseDataHandler(context, dataBase) {
//
//    private val mActivity: FlutterBaseActivity = activity
//
//    init {
//        OCRManager.instance.init(activity)
//    }
//
//    fun openCamera() {
//        OCRManager.instance.openCamera(mActivity)
//    }
//
//    /**
//     * 批量OCR识别
//     */
//    fun ocrBatch(filePath: String, listener: OnOCRBatchListener) {
//        mActivity.launch(Dispatchers.IO) {
//            Log.e("jingo", "OCRManager 线程开始 ${Thread.currentThread().name}")
//            val file = File(filePath)
//            val resultList = mutableListOf<Map<String, Any>>()
//            if (file.isDirectory) {
//                val fileList = file.listFiles()
//                val bitmapList = mutableListOf<String>()
//                for (item in fileList!!) {
//                    if (item.isFile) {
//                        if (checkIsImageFile(item.name)) {
//                            bitmapList.add(item.path)
//                        }
//                    }
//                }
//                val bfw: BufferedWriter
//                val csvFile = File("$filePath/ocr.csv")
//                val out: FileOutputStream
//                val osw: OutputStreamWriter
//                try {
//                    out = FileOutputStream(csvFile)
//                    //用excel打开，中文会乱码，所以用GBK编译
//                    osw = OutputStreamWriter(out, "GBK")
//                    bfw = BufferedWriter(osw)
//                    //第一行表头数据
//                    bfw.write("图片路径和名称,")
//                    bfw.write("识别结果序号,")
//                    bfw.write("识别结果内容,")
//                    bfw.write("置信度,")
//                    bfw.write("识别面积,")
//                    bfw.write("图片大小,")
//                    bfw.write("识别区域,")
//                    //写好表头后换行
//                    bfw.newLine()
//                    for (i in 0 until bitmapList.size) {
//                        val path = bitmapList[i]
//
//                        val bitmap: Bitmap? = readFile(path)
//                        var exif = ExifInterface(path)
//                        val exifRotation = exif.getAttributeInt(
//                            ExifInterface.TAG_ORIENTATION,
//                            ExifInterface.ORIENTATION_NORMAL
//                        )
//                        val rotation = ImageUtil.exifToDegrees(exifRotation)
//                        val rotateBitmap = ImageUtil.createRotateBitmap(bitmap, rotation)
//                        val list = OCRManager.instance.ocr(rotateBitmap)
//
//
////                        val list = ocrBitmap(path)
//                        if (list != null) {
//                            for (o in list) {
//                                bfw.write("$path,")
//                                bfw.write("${o.index},")
//                                bfw.write("${o.name},")
//                                bfw.write("${o.confidence.toString()},")
//                                val pointList = o.bounds
//                                bfw.write("${(pointList[3].y - pointList[0].y) * (pointList[2].x - pointList[0].x)},")
//                                bfw.write("${rotateBitmap.width} ${rotateBitmap.height},")
//                                bfw.write("${o.bounds[0].x} ${o.bounds[0].y};${o.bounds[1].x} ${o.bounds[1].y};${o.bounds[2].x} ${o.bounds[2].y};${o.bounds[3].x} ${o.bounds[3].y},")
//                                bfw.newLine()
//                            }
//                            bfw.newLine()
//                            withContext(Dispatchers.Main) {
//                                listener.onProgress(bitmapList.size, i)
//                            }
//                            val m1 = mutableMapOf<String, Any>();
//                            m1["data"] = list;
//                            m1["width"] = rotateBitmap.width
//                            m1["height"] = rotateBitmap.height
//                            m1["path"] = path
//                            resultList.add(m1)
//                        }
//                        rotateBitmap.recycle()
//                    }
//
//                    //将缓存数据写入文件
//                    bfw.flush()
//                    //释放缓存
//                    bfw.close()
//                    osw.close()
//                    out.close()
//                } catch (e: Throwable) {
//
//                }
//                //将缓存数据写入文件
//
//                withContext(Dispatchers.Main) {
//                    Log.e("jingo", "OCRManager 线程名称2 ${Thread.currentThread().name}")
//                    listener.onResult(resultList)
//                }
//            } else if (file.isFile && checkIsImageFile(file.name)) {
//                val list = ocrBitmap(filePath)
//                if (list != null) {
//                    withContext(Dispatchers.Main) {
//                        Log.e("jingo", "OCRManager 线程名称2 ${Thread.currentThread().name}")
//                        listener.onProgress(1, 1)
//                    }
//                    val m = mutableMapOf<String, List<OcrViewResultModel>>()
//                    m[file.name] = list
//                    resultList.add(m)
//                }
//                withContext(Dispatchers.Main) {
//                    Log.e("jingo", "OCRManager 线程名称2 ${Thread.currentThread().name}")
//                    listener.onResult(resultList)
//                }
//            }
//
//        }
//    }
//
//    private fun ocrBitmap(path: String): List<OcrViewResultModel>? {
//        try {
//            val bitmap: Bitmap? = readFile(path)
//            var exif = ExifInterface(path)
//            val exifRotation = exif.getAttributeInt(
//                ExifInterface.TAG_ORIENTATION,
//                ExifInterface.ORIENTATION_NORMAL
//            )
//            val rotation = ImageUtil.exifToDegrees(exifRotation)
//            val rotateBitmap = ImageUtil.createRotateBitmap(bitmap, rotation)
//            val res = OCRManager.instance.ocr(rotateBitmap)
//            rotateBitmap.recycle()
//            return res
//        } catch (e: IOException) {
//            Log.e("jingo", "图像识别，获取图像信息失败 ${e.printStackTrace()}")
//        }
//        return null
//    }
//
//    /**
//     *  检查是不是bitmap文件
//     */
//    private fun checkIsImageFile(fName: String): Boolean {
//        val isImageFile: Boolean
//        // 获取扩展名
//        val fileEnd = fName.substring(
//            fName.lastIndexOf(".") + 1,
//            fName.length
//        ).lowercase(Locale.getDefault())
//        isImageFile =
//            fileEnd == "jpg" || fileEnd == "png" || fileEnd == "webp" || fileEnd == "jpeg" || fileEnd == "bmp"
//        return isImageFile
//    }
//
//    /**
//     * 读取bitmap文件
//     */
//    private fun readFile(path: String): Bitmap? {
//        var stream: FileInputStream? = null
//        try {
//            stream = FileInputStream(path)
//            return BitmapFactory.decodeStream(stream)
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        } finally {
//            if (stream != null) {
//                try {
//                    stream.close()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//        return null
//    }
//
////    private  fun onOcrBitmap(
////        bitmap, confidence,
////        object: OcrListener {
////        override fun onResult(models: List<BasePolygonResultModel>) {
////            if (models == null) {
////                listener.onResult(null)
////                return
////            }
////            ocrResultModelCache = models
////            listener.onResult(models)
////        }
////    })
//
//}
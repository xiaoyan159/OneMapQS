package com.navinfo.omqs.tools

import android.content.Context
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.OfflineMapCityBean
import java.io.File

class FileManager {
    companion object {
        //初始化数据文件夹
        fun initRootDir(context:Context){
            // 在SD卡创建项目目录
            val sdCardPath = context.getExternalFilesDir(null)
            sdCardPath?.let {
                Constant.ROOT_PATH = sdCardPath.absolutePath
                Constant.MAP_PATH = Constant.ROOT_PATH + "/map/"
                Constant.OFFLINE_MAP_PATH = Constant.MAP_PATH + "offline/"
                val file = File(Constant.MAP_PATH)
                if (!file.exists()) {
                    file.mkdirs()
                    Constant.DATA_PATH = Constant.ROOT_PATH + "/data/"
                    with(File(Constant.MAP_PATH)) {
                        if (!this.exists()) this.mkdirs()
                    }
                    with(File(Constant.DATA_PATH)) {
                        if (!this.exists()) this.mkdirs()
                    }
                }else{
                    Constant.DATA_PATH = Constant.ROOT_PATH + "/data/"
                }
            }
        }

        /**
         * 检查离线地图文件
         */
        suspend fun checkOfflineMapFileInfo(cityBean: OfflineMapCityBean) {
            //访问离线地图文件夹
            val fileDir = File("${Constant.OFFLINE_MAP_PATH}")
            //如果连本地文件夹还没有，就不用修改任何数据了
            if (!fileDir.exists()) {
                return
            }
            //访问离线地图临时下载文件夹
            val fileTempDir = File("${Constant.OFFLINE_MAP_PATH}download/")
            //是否有一份.map文件了
            var mapFile: File? = null
            //文件夹里文件挨个访问
            for (item in fileDir.listFiles()) {
                //先找到对应的省市文件，例如：540000_西藏自治区_20230401195018.map",以id开头
                if (item.isFile && item.name.startsWith(cityBean.id)) {
                    //如果本地文件与从网络获取到版本号一致，表示这个文件已经下载完毕,不用处理了
                    if (item.name.contains("_${cityBean.version}.map")) {
                        cityBean.status = OfflineMapCityBean.DONE
                        return
                    }
                    //文件存在，版本号不对应，留给下面流程处理
                    mapFile = item
                    break
                }
            }
            //临时下载文件夹
            if (fileTempDir.exists()) {
                for (item in fileTempDir.listFiles()) {
                    //先找到对应的省市文件，例如：540000_20230401195018",以id开头
                    if (item.isFile && item.name.startsWith(cityBean.id)) {
                        //如果本地文件与从网络获取到版本号一致，表示这个文件已经在下载列表中
                        if (item.name == "${cityBean.id}_${cityBean.version}") {
                            //如果这个临时文件的大小和下载大小是一致的，说明已经下载完了，但是在下载环节没有更名移动成功，需要重命名和移动文件夹
                            if (item.length() == cityBean.fileSize) {
                                //移动更名文件后删除旧数据，修改状态
                                if (item.renameTo(File("${Constant.OFFLINE_MAP_PATH}${cityBean.fileName}"))) {
                                    //删除旧版本数据
                                    mapFile?.delete()
                                    cityBean.status = OfflineMapCityBean.DONE
                                    return
                                }
                            } else { // 临时文件大小和目标不一致，说明下载了一半
                                cityBean.status = OfflineMapCityBean.PAUSE
                                cityBean.currentSize = item.length()
                                return
                            }
                        } else { //虽然省市id开头一致，但是版本号不一致，说明之前版本下载了一部分，现在要更新了，原来下载的文件直接删除
                            cityBean.status = OfflineMapCityBean.UPDATE
                            item.delete()
                            return
                        }
                        break
                    }
                }
            }
        }
    }
}
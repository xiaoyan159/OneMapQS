package com.navinfo.collect.library.importExport.flutter

class FlutterImportExportProtocolKeys {
    /**
     * SHP文件操作
     */
    object ShpFileProtocol {
        /**
         * 获取SHP文件信息
         */
        const val kGetImportShpFileInfo = "flutter_nimap/ShpFile/getImportShpFileInfo";


        /**
         * 导入SHP数据
         */
        const val kImportShpData = "flutter_nimap/ShpFile/ImportShpData";


    }

    object MainProtocol{
        /**
         * 导入检查项数据
         */
        const val kGetImportCheckFileInfo =
            "flutter_nimap/ShpFile/getImportCheckFileInfo";
    }

}
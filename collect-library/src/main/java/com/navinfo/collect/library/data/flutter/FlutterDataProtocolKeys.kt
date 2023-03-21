package com.navinfo.collect.library.data.flutter

object FlutterDataProtocolKeys {

    /**
     * 数据图层管理
     */
    object DateLayerProtocol {
        /**
         * 获取数据层列表
         */
        const val kDataGetDataLayerList = "flutter_nimap/DataLayer/getDataLayerList";

        /**
         * 获取某个数据层列表
         */
        const val kDataGetDataLayer = "flutter_nimap/DataLayer/getDataLayer";

        /**
         * 创建数据层
         */
        const val kDataCreateDataLayer = "flutter_nimap/DataLayer/createDataLayer";
    }

    /**
     * 数据操作
     */
    object DataElementProtocol {


        //保存数据
        const val kDataSaveElementData = "flutter_nimap/ElementData/saveElementData";

        ///删除数据
        const val kDataDeleteElementData = "flutter_nimap/ElementData/deleteElementData";

        //查询数据
        const val kDataSnapElementDataList = "flutter_nimap/ElementData/snapElementDataList";

        // 导入pbf数据
        const val kImportPbfData = "flutter_nimap/HDData/importPBF";

        //查询数据详细信息
        const val kDataQueryElementDeepInfo = "flutter_nimap/ElementData/queryElementDeepInfo";

        ///按名称搜索数据
        const val kDataSearchData = "flutter_nimap/ElementData/queryElementByName"
    }

    /**
     * 数据操作
     */
    object DataNiLocationProtocol {


        //保存数据
        const val kDataSaveNiLocationData = "flutter_nimap/NiLocationData/saveNiLocationData";

        ///删除数据
        const val kDataDeleteNiLocationData = "flutter_nimap/NiLocationData/deleteNiLocationData";
    }

    object DataProjectProtocol {
        /**
         * 获取项目列表
         */
        const val kDataGetDataProjectList = "flutter_nimap/Project/getDataProjectList";

        /**
         * 保存项目
         */
        const val kDataSaveDataProject = "flutter_nimap/Project/saveDataProject";

    }

    object DataCheckProtocol {
        ///获取检查项列表
        const val kDataGetCheckManagerList = "flutter_nimap/CheckManager/getDataCheckManagerList";

        ///根据id获取检查项列表
        const val kDataGetCheckManagerListByIds =
            "flutter_nimap/CheckManager/getDataCheckManagerListByIds";
    }

    object DataCameraProtocol {
        const val kDataOpenCamera = "flutter_nimap/openCamera";

        const val kDataOCRResults = "flutter_nimap/ocrResults";
        ///批量识别
        const val kDataOCRBatchResults = "flutter_nimap/ocrBatch";
        ///ocr 批量回调进度
        const val kDataOCRBatchProgress = "flutter_nimap/ocrBatchProgress";
    }
}

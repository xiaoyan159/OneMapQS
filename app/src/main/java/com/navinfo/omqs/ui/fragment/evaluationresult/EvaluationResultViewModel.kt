package com.navinfo.omqs.ui.fragment.evaluationresult

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.map.GeoPoint
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.omqs.db.RoomAppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EvaluationResultViewModel @Inject constructor(
    private val roomAppDatabase: RoomAppDatabase, private val mapController: NIMapController
) : ViewModel() {

    private val markerTitle = "点选marker"

    /**
     * 操作结束，销毁页面
     */
    val liveDataFinish = MutableLiveData<Boolean>()

    /**
     *  问题分类 liveData，给[LeftAdapter]展示的数据
     */
    val liveDataLeftTypeList = MutableLiveData<List<String>>()

    /**
     * 问题类型 liveData 给[MiddleAdapter]展示的数据
     */
    val liveDataMiddleTypeList = MutableLiveData<List<String>>()

    /**
     * 问题现象 liveData 给[RightGroupHeaderAdapter]展示的数据
     */
    val liveDataRightTypeList = MutableLiveData<List<RightBean>>()


    var liveDataQsRecordBean = MutableLiveData<QsRecordBean>()

    init {
        liveDataQsRecordBean.value = QsRecordBean(id = UUID.randomUUID().toString(), classType = "89193")
        Log.e("jingo", "EvaluationResultViewModel 创建了 ${hashCode()}")
        mapController.markerHandle.apply {
            setOnMapClickListener {
                liveDataQsRecordBean.value!!.geometry = it.toGeometry()
                addMarker(it, markerTitle)
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        Log.e("jingo", "EvaluationResultViewModel 销毁了 ${hashCode()}")
        mapController.markerHandle.removeMarker(markerTitle)
        mapController.markerHandle.removeOnMapClickListener()
    }


    /**
     * 查询数据库，获取问题分类
     */
    fun loadMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            getClassTypeList()
            getProblemLinkList()
        }
        val geoPoint = mapController.locationLayerHandler.getCurrentGeoPoint()
        geoPoint?.let {
            liveDataQsRecordBean.value!!.geometry = it.toGeometry()
            mapController.markerHandle.addMarker(geoPoint, markerTitle)
        }
    }

    /**
     *  //获取问题分类列表
     */
    fun getClassTypeList() {
        
        viewModelScope.launch(Dispatchers.IO) {
            val list = roomAppDatabase.getScProblemTypeDao().findClassTypeList()
            list?.let {
                if (list.isNotEmpty()) {
                    //通知页面更新
                    liveDataLeftTypeList.postValue(it)
                    val classType = it[0]
                    //如果右侧栏没数据，给个默认值
                    if (liveDataQsRecordBean.value!!.classType.isEmpty()) {
                        liveDataQsRecordBean.value!!.classType = classType
                    }
                    getProblemList(classType)
                }
            }
        }

    }

    /**
     * 获取问题环节列表和初步问题
     */
    fun getProblemLinkList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = roomAppDatabase.getScRootCauseAnalysisDao().findAllData()
            list?.let { tl ->
                if (tl.isNotEmpty()) {
                    val middleList = mutableListOf<String>()
                    val rightList = mutableListOf<RightBean>()
                    for (item in tl) {
                        if (!middleList.contains(item.problemLink)) {
                            middleList.add(item.problemLink)
                        }
                        rightList.add(
                            RightBean(
                                title = item.problemLink, text = item.problemCause, isSelect = false
                            )
                        )
                    }
                    if (liveDataQsRecordBean.value!!.problemLink.isEmpty()) {
                        liveDataQsRecordBean.value!!.problemLink = middleList[0]
                    }
                    if (liveDataQsRecordBean.value!!.cause.isEmpty()) {
                        liveDataQsRecordBean.value!!.cause = rightList[0].text
                    }
                    liveDataMiddleTypeList.postValue(middleList)
                    liveDataRightTypeList.postValue(rightList)
                }
            }
        }
    }

    /**
     * 获取问题类型列表和问题现象
     */
    private suspend fun getProblemList(classType: String) {
        val typeList = roomAppDatabase.getScProblemTypeDao().findProblemTypeList(classType)
        typeList?.let { tl ->
            if (tl.isNotEmpty()) {
                val typeTitleList = mutableListOf<String>()
                val phenomenonRightList = mutableListOf<RightBean>()
                for (item in tl) {
                    if (!typeTitleList.contains(item.problemType)) {
                        typeTitleList.add(item.problemType)
                    }
                    phenomenonRightList.add(
                        RightBean(
                            title = item.problemType, text = item.phenomenon, isSelect = false
                        )
                    )
                }
                liveDataMiddleTypeList.postValue(typeTitleList)
                liveDataRightTypeList.postValue(phenomenonRightList)
            }
        }
    }

    /**
     * 查询问题类型列表
     */
    fun getProblemTypeList(classType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getProblemList(classType)
        }
    }

    /**
     * 监听右侧栏的点击事件，修改数据
     */
    fun setPhenomenonMiddleBean(adapterBean: RightBean) {
        liveDataQsRecordBean.value!!.phenomenon = adapterBean.text
        liveDataQsRecordBean.value!!.problemType = adapterBean.title
    }

    fun setProblemLinkMiddleBean(adapterBean: RightBean) {
        liveDataQsRecordBean.value!!.cause = adapterBean.text
        liveDataQsRecordBean.value!!.problemLink = adapterBean.title

    }

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                it.copyToRealmOrUpdate(liveDataQsRecordBean.value)
            }
            realm.close()
            mapController.layerManagerHandler.addOrUpdateQsRecordMark(liveDataQsRecordBean.value!!)
            liveDataFinish.postValue(true)
        }
    }

    fun deleteData() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val qsRecord = QsRecordBean(
//                id = UUID.randomUUID().toString(),
//                classType = liveDataCurrentClassType.value.toString(),
//                type = liveDataCurrentProblemType.value.toString(),
//                phenomenon = liveDataCurrentPhenomenon.value.toString(),
//                problemLink = liveDataCurrentProblemLink.value.toString(),
//                cause = liveDataCurrentCause.value.toString(),
//            )
//            qsRecord.geometry = currentGeoPoint!!.toGeometry()
//            val realm = Realm.getDefaultInstance()
//            realm.executeTransaction {
//                it.copyToRealmOrUpdate(qsRecord)
//            }
//            realm.close()
//            mapController.layerManagerHandler.addOrUpdateQsRecordMark(qsRecord)
//            liveDataFinish.postValue(true)
//        }
    }

    /**
     * 根据数据id，查询数据
     */
    fun loadData(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val objects = realm.where<QsRecordBean>().equalTo("id", id).findFirst()

            if (objects != null) {
                liveDataQsRecordBean.postValue(realm.copyFromRealm(objects))
            }
        }
    }
}
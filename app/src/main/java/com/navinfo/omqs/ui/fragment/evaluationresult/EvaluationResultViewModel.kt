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
     *  问题分类 liveData，给[PhenomenonLeftAdapter]展示的数据
     */
    val liveDataClassTypeList = MutableLiveData<List<String>>()

    /**
     * 问题类型 liveData 给[PhenomenonMiddleAdapter]展示的数据
     */
    val liveDataProblemTypeList = MutableLiveData<List<String>>()

    /**
     * 问题现象 liveData 给[PhenomenonRightGroupHeaderAdapter]展示的数据
     */
    val liveDataPhenomenonRightList = MutableLiveData<List<PhenomenonMiddleBean>>()


    /**
     * 当前选择问题分类 给[EvaluationResultFragment]中 【问题分类】展示数据
     */
    var liveDataCurrentClassType = MutableLiveData<String>()

    /**
     * 当前选择的问题类型 给[EvaluationResultFragment]中 【问题类型】展示数据
     */
    var liveDataCurrentProblemType = MutableLiveData<String>()

    /**
     * 当前选择的问题现象 给[EvaluationResultFragment]中 【问题现象】展示数据
     */
    var liveDataCurrentPhenomenon = MutableLiveData<String>()


    /**
     * 当前选择的问题环节 给[EvaluationResultFragment]中 【问题环节】展示数据
     */
    var liveDataCurrentProblemLink = MutableLiveData<String>()

    /**
     * 当前选择的问初步原因 给[EvaluationResultFragment]中 【初步原因】展示数据
     */
    var liveDataCurrentCause = MutableLiveData<String>()

    var currentGeoPoint: GeoPoint? = null


    init {
        Log.e("jingo", "EvaluationResultViewModel 创建了 ${hashCode()}")
        mapController.markerHandle.apply {
            setOnMapClickListener {
                currentGeoPoint = it
                addMarker(it, markerTitle)
            }
        }
        val geoPoint = mapController.locationLayerHandler.getCurrentGeoPoint()
        geoPoint?.let {
            currentGeoPoint = it
            mapController.markerHandle.addMarker(geoPoint, markerTitle)
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
    }

    /**
     *  //获取问题分类列表
     */
    fun getClassTypeList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = roomAppDatabase.getScProblemTypeDao().findClassTypeList()
            list?.let {
                //通知页面更新
                liveDataClassTypeList.postValue(it)
                //如果右侧栏没数据，给个默认值
                if (liveDataCurrentClassType.value == null) {
                    liveDataCurrentClassType.postValue(it[0])
                }
                getProblemList(it[0])
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
                    val typeTitleList = mutableListOf<String>()
                    val phenomenonRightList = mutableListOf<PhenomenonMiddleBean>()
                    for (item in tl) {
                        if (!typeTitleList.contains(item.problemLink)) {
                            typeTitleList.add(item.problemLink)
                        }
                        phenomenonRightList.add(
                            PhenomenonMiddleBean(
                                title = item.problemLink, text = item.problemCause, isSelect = false
                            )
                        )
                    }

                    if (liveDataCurrentProblemLink.value == null) {
                        liveDataCurrentProblemLink.postValue(phenomenonRightList[0].text)
                    }
                    if (liveDataCurrentCause.value == null) {
                        liveDataCurrentCause.postValue(typeTitleList[0])
                    }
                    liveDataProblemTypeList.postValue(typeTitleList)
                    liveDataPhenomenonRightList.postValue(phenomenonRightList)
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
                val phenomenonRightList = mutableListOf<PhenomenonMiddleBean>()
                for (item in tl) {
                    if (!typeTitleList.contains(item.problemType)) {
                        typeTitleList.add(item.problemType)
                    }
                    phenomenonRightList.add(
                        PhenomenonMiddleBean(
                            title = item.problemType, text = item.phenomenon, isSelect = false
                        )
                    )
                }
                if (liveDataCurrentPhenomenon.value == null) {
                    liveDataCurrentPhenomenon.postValue(phenomenonRightList[0].text)
                }
                if (liveDataCurrentProblemType.value == null) {
                    liveDataCurrentProblemType.postValue(typeTitleList[0])
                }
                liveDataProblemTypeList.postValue(typeTitleList)
                liveDataPhenomenonRightList.postValue(phenomenonRightList)
            }
        }
    }

    /**
     * 查询问题类型
     */
    fun getProblemTypeList(classType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            liveDataCurrentClassType.postValue(classType)
            getProblemList(classType)
        }
    }

    fun setPhenomenonMiddleBean(bean: PhenomenonMiddleBean) {
        if (liveDataCurrentPhenomenon.value != bean.text) liveDataCurrentPhenomenon.value =
            bean.text
        if (liveDataCurrentProblemType.value != bean.title) liveDataCurrentProblemType.value =
            bean.title

    }

    fun setProblemLinkMiddleBean(bean: PhenomenonMiddleBean) {
        if (liveDataCurrentProblemLink.value != bean.text) liveDataCurrentProblemLink.value =
            bean.text
        if (liveDataCurrentCause.value != bean.title) liveDataCurrentCause.value = bean.title

    }

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            val qsRecord = QsRecordBean(
                id = UUID.randomUUID().toString(),
                classType = liveDataCurrentClassType.value.toString(),
                type = liveDataCurrentProblemType.value.toString(),
                phenomenon = liveDataCurrentPhenomenon.value.toString(),
                problemLink = liveDataCurrentProblemLink.value.toString(),
                cause = liveDataCurrentCause.value.toString(),
            )
            qsRecord.geometry = currentGeoPoint!!.toGeometry()
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                it.copyToRealmOrUpdate(qsRecord)
            }
            realm.close()
            mapController.mMapView.updateMap()
            liveDataFinish.postValue(true)
        }
    }

    fun deleteData() {

    }
}
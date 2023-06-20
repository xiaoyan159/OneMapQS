package com.navinfo.omqs.ui.fragment.evaluationresult

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ToastUtils
import com.navinfo.collect.library.data.entity.AttachmentBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.RenderEntity.Companion.LinkTable
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ChatMsgEntity
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.SoundMeter
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmResults
import io.realm.kotlin.addChangeListener
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.oscim.core.GeoPoint
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EvaluationResultViewModel @Inject constructor(
    private val roomAppDatabase: RoomAppDatabase,
    private val mapController: NIMapController,
    private val realmOperateHelper: RealmOperateHelper,
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
//    val liveDataMiddleTypeList = MutableLiveData<List<String>>()

    /**
     * 问题现象 liveData 给[RightGroupHeaderAdapter]展示的数据
     */
    val liveDataRightTypeList = MutableLiveData<List<RightBean>>()

    var liveDataQsRecordBean = MutableLiveData<QsRecordBean>()

    var listDataChatMsgEntityList = MutableLiveData<MutableList<ChatMsgEntity>>()

    var oldBean: QsRecordBean? = null

    //语音窗体
    private var pop: PopupWindow? = null

    private var mSpeakMode: SpeakMode? = null

    //录音图标
    var volume: ImageView? = null

    var mSoundMeter: SoundMeter? = null

    var classTypeTemp: String = ""

    init {
        liveDataQsRecordBean.value = QsRecordBean(id = UUID.randomUUID().toString())
        viewModelScope.launch {
            mapController.onMapClickFlow.collect {
                liveDataQsRecordBean.value!!.geometry = GeometryTools.createGeometry(it).toText()
                mapController.markerHandle.addMarker(it, markerTitle)
                viewModelScope.launch {
                    captureLink(it.longitude, it.latitude)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mapController.markerHandle.removeMarker(markerTitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mapController.lineHandler.removeLine()
        }
    }


    /**
     * 查询数据库，获取问题分类
     */
    fun initNewData(bean: SignBean?, filePath: String) {
        //获取当前定位点
        val geoPoint = mapController.locationLayerHandler.getCurrentGeoPoint()
        //如果不是从面板进来的
        if (bean == null) {
            geoPoint?.let {
                liveDataQsRecordBean.value!!.geometry = GeometryTools.createGeometry(it).toText()
                mapController.markerHandle.addMarker(geoPoint, markerTitle)
                mapController.animationHandler.animationByLatLon(
                    geoPoint.latitude, geoPoint.longitude
                )
                viewModelScope.launch {
                    captureLink(geoPoint.longitude, geoPoint.latitude)
                }
            }
        } else {
            liveDataQsRecordBean.value?.run {
                elementId = bean.elementId
                linkId = bean.linkId
                if (linkId.isNotEmpty()) {
                    viewModelScope.launch {
                        val link = realmOperateHelper.queryLink(linkId)
                        link?.let { l ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                mapController.lineHandler.showLine(l.geometry)
                            }
                        }
                    }
                }
                val point = GeometryTools.createGeoPoint(bean.geometry)
                this.geometry = GeometryTools.createGeometry(point).toText()
                mapController.animationHandler.animationByLatLon(point.latitude, point.longitude)
                mapController.markerHandle.addMarker(point, markerTitle)
            }
        }
        //查询元数据
        viewModelScope.launch(Dispatchers.IO) {
            getClassTypeList(bean)
            getProblemLinkList()
        }
        addChatMsgEntity(filePath)
    }

    /**
     * 捕捉道路
     */
    private suspend fun captureLink(longitude: Double, latitude: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val linkList = realmOperateHelper.queryLink(
                point = GeoPoint(latitude, longitude),
            )

            liveDataQsRecordBean.value?.let {
                if (linkList.isNotEmpty()) {
                    it.linkId = linkList[0].properties[LinkTable.linkPid] ?: ""
                    mapController.lineHandler.showLine(linkList[0].geometry)
                    Log.e("jingo", "捕捉到的linkId = ${it.linkId}")
                } else {
                    it.linkId = ""
                    mapController.lineHandler.removeLine()
                }
            }
        }
    }

    /**
     *  //获取问题分类列表
     */
    fun getClassTypeList(bean: SignBean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = roomAppDatabase.getScProblemTypeDao().findClassTypeList()
            list?.let {
                if (list.isNotEmpty()) {
                    //通知页面更新
                    var classType = list[0]
                    liveDataLeftTypeList.postValue(it)
                    if (bean != null) {
                        val classType2 = roomAppDatabase.getScProblemTypeDao()
                            .findClassTypeByCode(bean.elementCode)
                        if (classType2 != null) {
                            classType = classType2
                        }
                    }
                    //如果右侧栏没数据，给个默认值
                    if (liveDataQsRecordBean.value!!.classType.isEmpty()) {

                        liveDataQsRecordBean.value!!.classType = classType
                        classTypeTemp = classType
                    } else {
                        classType = liveDataQsRecordBean.value!!.classType
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
                    liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
//                    liveDataMiddleTypeList.postValue(middleList)
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
                if (liveDataQsRecordBean.value!!.problemType.isEmpty()) {
                    liveDataQsRecordBean.value!!.problemType = typeTitleList[0]
                }
//                liveDataMiddleTypeList.postValue(typeTitleList)
                if (liveDataQsRecordBean.value!!.phenomenon.isEmpty()) {
                    liveDataQsRecordBean.value!!.phenomenon = phenomenonRightList[0].text
                }
                liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
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
        classTypeTemp = classType
    }

    /**
     * 监听右侧栏的点击事件，修改数据
     */
    fun setPhenomenonMiddleBean(adapterBean: RightBean) {
        liveDataQsRecordBean.value!!.classType = classTypeTemp
        liveDataQsRecordBean.value!!.phenomenon = adapterBean.text
        liveDataQsRecordBean.value!!.problemType = adapterBean.title
        liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
    }

    fun setProblemLinkMiddleBean(adapterBean: RightBean) {
        liveDataQsRecordBean.value!!.cause = adapterBean.text
        liveDataQsRecordBean.value!!.problemLink = adapterBean.title
        liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
    }

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            liveDataQsRecordBean.value!!.checkTime = DateTimeUtil.getDataTime()
            realm.executeTransaction {
                it.copyToRealmOrUpdate(liveDataQsRecordBean.value)
            }
//            realm.close()
            mapController.markerHandle.addOrUpdateQsRecordMark(liveDataQsRecordBean.value!!)
            liveDataFinish.postValue(true)
        }
    }

    fun deleteData(context: Context) {
        val mDialog = FirstDialog(context)
        mDialog.setTitle("提示？")
        mDialog.setMessage("是否删除Mark，请确认！")
        mDialog.setPositiveButton("确定", object : FirstDialog.OnClickListener {
            override fun onClick(dialog: Dialog?, which: Int) {
                mDialog.dismiss()
                viewModelScope.launch(Dispatchers.IO) {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        val objects = it.where(QsRecordBean::class.java)
                            .equalTo("id", liveDataQsRecordBean.value?.id).findFirst()
                        objects?.deleteFromRealm()
                    }
                    mapController.markerHandle.removeQsRecordMark(liveDataQsRecordBean.value!!)
                    mapController.mMapView.vtmMap.updateMap(true)
                    liveDataFinish.postValue(true)
                }
            }
        })
        mDialog.setNegativeButton("取消", null)
        mDialog.show()
    }

    /**
     * 根据数据id，查询数据
     */

    fun initData(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            viewModelScope.launch(Dispatchers.IO) {

                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransactionAsync { bgRealm ->
                        // find the item
                        val objects = bgRealm.where(QsRecordBean::class.java).equalTo("id", id).findFirst()
                        if (objects != null) {
                            oldBean = bgRealm.copyFromRealm(objects)
                            oldBean?.let {
                                liveDataQsRecordBean.postValue(it.copy())
                                val p = GeometryTools.createGeoPoint(it.geometry)
                                mapController.markerHandle.addMarker(GeoPoint(p.latitude, p.longitude), markerTitle)

                                //获取linkid
                                if (it.linkId.isNotEmpty()) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        val link = realmOperateHelper.queryLink(it.linkId)
                                        link?.let { l ->
                                            mapController.lineHandler.showLine(l.geometry)
                                        }
                                    }
                                }
                                liveDataQsRecordBean.value?.attachmentBeanList = it.attachmentBeanList
                                // 显示语音数据到界面
                                getChatMsgEntityList()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 查询问题类型列表
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun getChatMsgEntityList() {
        val chatMsgEntityList: MutableList<ChatMsgEntity> = ArrayList()
        liveDataQsRecordBean.value?.attachmentBeanList?.forEach {
            //1 录音
            if (it.type == 1) {
                val chatMsgEntity = ChatMsgEntity()
                chatMsgEntity.name = it.name
                chatMsgEntity.voiceUri = Constant.USER_DATA_ATTACHEMNT_PATH
                chatMsgEntityList.add(chatMsgEntity)
            }
        }
        listDataChatMsgEntityList.postValue(chatMsgEntityList)
    }

    fun addChatMsgEntity(filePath: String) {

        if (filePath.isNotEmpty()) {
            var chatMsgEntityList: MutableList<ChatMsgEntity> = ArrayList()
            if (listDataChatMsgEntityList.value?.isEmpty() == false) {
                chatMsgEntityList = listDataChatMsgEntityList.value!!
            }
            val chatMsgEntity = ChatMsgEntity()
            chatMsgEntity.name = filePath.replace(Constant.USER_DATA_ATTACHEMNT_PATH, "").toString()
            chatMsgEntity.voiceUri = Constant.USER_DATA_ATTACHEMNT_PATH
            chatMsgEntityList.add(chatMsgEntity)


            var attachmentList: RealmList<AttachmentBean> = RealmList()

            //赋值处理
            if (liveDataQsRecordBean.value?.attachmentBeanList?.isEmpty() == false) {
                attachmentList = liveDataQsRecordBean.value?.attachmentBeanList!!
            }

            val attachmentBean = AttachmentBean()
            attachmentBean.name = chatMsgEntity.name!!
            attachmentBean.type = 1
            attachmentList.add(attachmentBean)
            liveDataQsRecordBean.value?.attachmentBeanList = attachmentList

            listDataChatMsgEntityList.postValue(chatMsgEntityList)
        }
    }

    fun startSoundMetter(activity: Activity, v: View) {

        if (mSpeakMode == null) {
            mSpeakMode = SpeakMode(activity)
        }

        //语音识别动画
        if (pop == null) {
            pop = PopupWindow()
            pop!!.width = ViewGroup.LayoutParams.MATCH_PARENT
            pop!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
            pop!!.setBackgroundDrawable(BitmapDrawable())
            val view =
                View.inflate(activity as Context, R.layout.cv_card_voice_rcd_hint_window, null)
            pop!!.contentView = view
            volume = view.findViewById(R.id.volume)
        }

        pop!!.update()

        Constant.IS_VIDEO_SPEED = true
        //录音动画
        if (pop != null) {
            pop!!.showAtLocation(v, Gravity.CENTER, 0, 0)
        }
        volume!!.setBackgroundResource(R.drawable.pop_voice_img)
        val animation = volume!!.background as AnimationDrawable
        animation.start()

        val name: String = DateTimeUtil.getTimeSSS().toString() + ".m4a"
        if (mSoundMeter == null) {
            mSoundMeter = SoundMeter()
        }
        mSoundMeter!!.setmListener(object : SoundMeter.OnSoundMeterListener {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onSuccess(filePath: String?) {
                if (!TextUtils.isEmpty(filePath) && File(filePath).exists()) {
                    if (File(filePath) == null || File(filePath).length() < 1600) {
                        ToastUtils.showLong("语音时间太短，无效！")
                        mSpeakMode!!.speakText("语音时间太短，无效")
                        stopSoundMeter()
                        return
                    }
                }

                mSpeakMode!!.speakText("结束录音")

                addChatMsgEntity(filePath!!)
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            override fun onfaild(message: String?) {
                ToastUtils.showLong("录制失败！")
                mSpeakMode!!.speakText("录制失败")
                stopSoundMeter()
            }
        })

        mSoundMeter!!.start(Constant.USER_DATA_ATTACHEMNT_PATH + name)
        ToastUtils.showLong("开始录音")
        mSpeakMode!!.speakText("开始录音")
    }

    //停止语音录制
    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun stopSoundMeter() {
        //先重置标识，防止按钮抬起时触发语音结束
        Constant.IS_VIDEO_SPEED = false
        if (mSoundMeter != null && mSoundMeter!!.isStartSound()) {
            mSoundMeter!!.stop()
        }
        if (pop != null && pop!!.isShowing) pop!!.dismiss()
    }
}
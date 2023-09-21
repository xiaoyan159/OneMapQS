package com.navinfo.omqs.ui.fragment.evaluationresult

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
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
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.RenderEntity.Companion.LinkTable
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.OnGeoPointClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ChatMsgEntity
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.SoundMeter
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.oscim.core.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EvaluationResultViewModel @Inject constructor(
    private val roomAppDatabase: RoomAppDatabase,
    private val mapController: NIMapController,
    private val realmOperateHelper: RealmOperateHelper,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {


    private val TAG = "点选marker"

    /**
     * 操作结束，销毁页面
     */
    val liveDataFinish = MutableLiveData<Boolean>()

    /**
     *  问题分类 liveData，给[LeftAdapter]展示的数据
     */
    val liveDataLeftTypeList = MutableLiveData<List<ScProblemTypeBean>>()


    /**
     * 问题现象 liveData 给[RightGroupHeaderAdapter]展示的数据
     */
    val liveDataRightTypeList = MutableLiveData<List<RightBean>>()

    /**
     * 要保存的评测数据
     */
    val liveDataQsRecordBean = MutableLiveData(QsRecordBean(id = UUID.randomUUID().toString()))

    /**
     * 语音列表
     */
    val listDataChatMsgEntityList = MutableLiveData<MutableList<ChatMsgEntity>>()

    /**
     * 照片列表
     */
    val liveDataPictureList = MutableLiveData<MutableList<String>>()

    /**
     * toast信息
     */
    val liveDataToastMessage = MutableLiveData<String>()

    /**
     * 当前选择的任务
     */
    val liveDataTaskBean = MutableLiveData<TaskBean>()

    /**
     * 编辑数据时用来差分数据
     */
    var oldBean: QsRecordBean? = null

    //语音窗体
    private var pop: PopupWindow? = null

    private var mSpeakMode: SpeakMode? = null

    //录音图标
    var volume: ImageView? = null

    var mSoundMeter: SoundMeter? = null

    var classTypeTemp: String = ""

    var classCodeTemp: String = ""

    init {
        mapController.mMapView.addOnNIMapClickListener(TAG, object : OnGeoPointClickListener {
            override fun onMapClick(tag: String, point: GeoPoint) {
                if (tag == TAG) {
                    liveDataQsRecordBean.value!!.geometry =
                        GeometryTools.createGeometry(point).toText()
                    mapController.markerHandle.addMarker(point, TAG)
                    viewModelScope.launch {
                        captureLink(point)
                    }
                }

            }
        })
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        mapController.mMapView.removeOnNIMapClickListener(TAG)
        mapController.markerHandle.removeMarker()
        mapController.lineHandler.removeLine()
    }


    /**
     * 查询数据库，获取问题分类
     */
    fun initNewData(bean: SignBean?, filePath: String) {
        //查询元数据
        viewModelScope.launch(Dispatchers.IO) {
            /**
             * 获取当前所选的任务
             */
            val taskId = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
            val realm = Realm.getDefaultInstance()
            val objects = realm.where(TaskBean::class.java).equalTo("id", taskId).findFirst()
            if (objects != null) {
                liveDataTaskBean.postValue(realm.copyFromRealm(objects))
            }
            //获取当前定位点
            val geoPoint = mapController.locationLayerHandler.getCurrentGeoPoint()
            //如果不是从面板进来的
            if (bean == null) {
                geoPoint?.let {
                    liveDataQsRecordBean.value!!.geometry =
                        GeometryTools.createGeometry(it).toText()
                    withContext(Dispatchers.Main) {
                        mapController.markerHandle.addMarker(geoPoint, TAG)
                        mapController.animationHandler.animationByLatLon(
                            geoPoint.latitude, geoPoint.longitude
                        )
                    }
                    captureLink(geoPoint)
                }
            } else {
                liveDataQsRecordBean.value?.run {
                    elementId = bean.renderEntity.code
                    linkId = bean.linkId
                    if (linkId.isNotEmpty()) {
                        viewModelScope.launch {
                            val link = realmOperateHelper.queryLink(linkId)
                            if(link != null){
                                mapController.lineHandler.showLine(link.geometry)
                            }
                        }
                    }

                    val point = GeometryTools.createGeoPoint(bean.renderEntity.geometry)
                    this.geometry = GeometryTools.createGeometry(point).toText()
                    withContext(Dispatchers.Main) {
                        mapController.animationHandler.animationByLatLon(
                            point.latitude, point.longitude
                        )
                        mapController.markerHandle.addMarker(point, TAG)
                    }
                }
            }

            getClassTypeList(bean)
            getProblemLinkList()
            realm.close()
        }
        addChatMsgEntity(filePath)
    }

    /**
     * 捕捉道路或新增评测link
     */
    private suspend fun captureLink(point: GeoPoint) {
        if (liveDataTaskBean.value == null) {
            liveDataToastMessage.postValue("请先选择所属任务!")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            liveDataQsRecordBean.value?.let {

                val taskLink =
                    realmOperateHelper.captureTaskLink(point)
                if (taskLink != null) {
                    it.linkId = taskLink.linkPid
                    mapController.lineHandler.showLine(taskLink.geometry)
                    return
                } else {
                    val linkList = realmOperateHelper.queryLink(point = point)
                    if (linkList.isNotEmpty()) {
                        it.linkId = linkList[0].properties[LinkTable.linkPid] ?: ""
                        mapController.lineHandler.showLine(linkList[0].geometry)
                        return
                    }
                }
                it.linkId = ""
                mapController.lineHandler.removeLine()
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
                    var classType = list[0].classType
                    var classCode = list[0].elementCode
                    liveDataLeftTypeList.postValue(it)
                    if (bean != null) {
                        val classType2 = roomAppDatabase.getScProblemTypeDao()
                            .findClassTypeByCode(bean.renderEntity.code)
                        if (classType2 != null) {
                            classType = classType2
                        }
                        classCode = bean.renderEntity.code
                    }
                    //如果右侧栏没数据，给个默认值
                    if (liveDataQsRecordBean.value!!.classType.isEmpty()) {
                        liveDataQsRecordBean.value!!.classType = classType
                        liveDataQsRecordBean.value!!.classCode = classCode
                        classTypeTemp = classType
                        classCodeTemp = classCode
                    } else {
                        classType = liveDataQsRecordBean.value!!.classType
                        classCode = liveDataQsRecordBean.value!!.classCode
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
    fun getProblemTypeList(scProblemTypeBean: ScProblemTypeBean) {
        viewModelScope.launch(Dispatchers.IO) {
            getProblemList(scProblemTypeBean.classType)
        }
        classTypeTemp = scProblemTypeBean.classType
        classCodeTemp = scProblemTypeBean.elementCode
    }

    /**
     * 监听右侧栏的点击事件，修改数据
     */
    fun setPhenomenonMiddleBean(adapterBean: RightBean) {
        liveDataQsRecordBean.value!!.classType = classTypeTemp
        liveDataQsRecordBean.value!!.classCode = classCodeTemp
        liveDataQsRecordBean.value!!.phenomenon = adapterBean.text
        liveDataQsRecordBean.value!!.problemType = adapterBean.title
        liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
    }

    fun setProblemLinkMiddleBean(adapterBean: RightBean) {
        liveDataQsRecordBean.value!!.cause = adapterBean.text
        liveDataQsRecordBean.value!!.problemLink = adapterBean.title
        liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
    }

    /**
     * 保存数据
     */

    fun saveData() {

        viewModelScope.launch(Dispatchers.IO) {
            val taskBean = liveDataQsRecordBean.value!!
            if (liveDataTaskBean.value == null) {
                liveDataToastMessage.postValue("请选择所属任务！")
                return@launch
            } else if (taskBean.classType.isEmpty()) {
                liveDataToastMessage.postValue("请选择要素分类！")
                return@launch
            } else if (taskBean.problemType.isEmpty()) {
                liveDataToastMessage.postValue("请选择问题类型！")
                return@launch
            } else if (taskBean.phenomenon.isEmpty()) {
                liveDataToastMessage.postValue("请选择问题现象！")
                return@launch
            } else if (taskBean.problemLink.isEmpty()) {
                liveDataToastMessage.postValue("请选择问题环节！")
                return@launch
            } else if (taskBean.classType.isEmpty()) {
                liveDataToastMessage.postValue("请选择问题分类！")
                return@launch
            } else if (taskBean.cause.isEmpty()) {
                liveDataToastMessage.postValue("请选择初步分析原因！")
                return@launch
            } else if (taskBean.linkId.isEmpty()) {
                liveDataToastMessage.postValue("没有绑定到任何link，请选择")
                return@launch
            }

            val realm = Realm.getDefaultInstance()
            liveDataQsRecordBean.value!!.taskId = liveDataTaskBean.value!!.id
            liveDataQsRecordBean.value!!.checkTime = DateTimeUtil.getDataTime()
            liveDataQsRecordBean.value!!.checkUserId = Constant.USER_REAL_NAME
            realm.executeTransaction {
                it.copyToRealmOrUpdate(liveDataQsRecordBean.value)
            }
            mapController.markerHandle.addOrUpdateQsRecordMark(liveDataQsRecordBean.value!!)
            liveDataFinish.postValue(true)
            realm.close()
        }
    }

    /**
     * 删除数据
     */
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
                    realm.close()
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
        Log.e("jingo", "捕捉到的要素 id = $id")
        viewModelScope.launch(Dispatchers.Main) {

            val realm = Realm.getDefaultInstance()

            val objects = realm.where(QsRecordBean::class.java).equalTo("id", id).findFirst()
            Log.e("jingo", "查询数据 id= $id")
            if (objects != null) {
                oldBean = realm.copyFromRealm(objects)
                oldBean?.let {
                    /**
                     * 获取当前所选的任务
                     */
                    val objects =
                        realm.where(TaskBean::class.java).equalTo("id", it.taskId).findFirst()
                    if (objects != null) {
                        liveDataTaskBean.postValue(realm.copyFromRealm(objects))
                    }

                    liveDataQsRecordBean.postValue(it.copy())
                    val p = GeometryTools.createGeoPoint(it.geometry)
                    mapController.markerHandle.addMarker(
                        GeoPoint(
                            p.latitude, p.longitude
                        ), TAG, "", null
                    )
                    //定位
                    val mapPosition = mapController.mMapView.vtmMap.mapPosition
                    mapPosition.setPosition(p.latitude, p.longitude)
                    mapController.mMapView.vtmMap.animator().animateTo(300, mapPosition)
                    //获取linkid
                    if (it.linkId.isNotEmpty()) {
                        val link = realmOperateHelper.queryLink(it.linkId)
                        if (link != null) {
                            mapController.lineHandler.showLine(link.geometry)
                        } else {
                            val realmR = realm.where(HadLinkDvoBean::class.java)
                                .equalTo("linkPid", it.linkId).and().equalTo("taskId", it.taskId)
                                .findFirst()
                            if (realmR != null) {
                                mapController.lineHandler.showLine(realmR.geometry)
                            }
                        }
                    }
                    liveDataQsRecordBean.value?.attachmentBeanList = it.attachmentBeanList
                    // 显示语音数据到界面
                    getChatMsgEntityList()
                }
            } else {
                liveDataToastMessage.postValue("数据读取失败")
            }
            realm.close()
        }
    }

    /**
     * 查询问题类型列表
     */
    private suspend fun getChatMsgEntityList() {
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
        if (mSoundMeter != null && mSoundMeter!!.isStartSound) {
            mSoundMeter!!.stop()
        }
        pop?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }


    fun savePhoto(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            // 创建一个名为 "MyApp" 的文件夹
            val myAppDir = File(Constant.USER_DATA_ATTACHEMNT_PATH)
            if (!myAppDir.exists()) myAppDir.mkdirs() // 确保文件夹已创建

            // 创建一个名为 fileName 的文件
            val file = File(myAppDir, "${UUID.randomUUID()}.png")
            file.createNewFile() // 创建文件

            // 将 Bitmap 压缩为 JPEG 格式，并将其写入文件中
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            var picList = mutableListOf<String>()
            if (liveDataPictureList.value == null) {
                picList.add(file.absolutePath)
            } else {
                picList.addAll(liveDataPictureList.value!!)
                picList.add(file.absolutePath)
            }
            liveDataPictureList.postValue(picList)
        }

    }

    /**
     * 监听任务选择变化
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == Constant.SELECT_TASK_ID) {
            if (oldBean == null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val taskId = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
                    val realm = Realm.getDefaultInstance()
                    val objects =
                        realm.where(TaskBean::class.java).equalTo("id", taskId).findFirst()
                    if (objects != null) {
                        liveDataTaskBean.postValue(realm.copyFromRealm(objects))
                    }
                    realm.close()
                }
            } else {
                liveDataFinish.postValue(true)
            }
        }
    }
}
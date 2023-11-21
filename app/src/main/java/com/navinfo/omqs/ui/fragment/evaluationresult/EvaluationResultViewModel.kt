package com.navinfo.omqs.ui.fragment.evaluationresult

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.ContactsContract.Data
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
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.enums.DataCodeEnum
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
import com.navinfo.omqs.ui.activity.map.LaneInfoItem
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.SignUtil
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

    /**
     * 关联的数据
     */
    var renderEntity: RenderEntity? = null

    /**
     * 车信列表
     */
    var laneInfoList: MutableList<LaneInfoItem>? = null

    var liveDataLanInfoChange = MutableLiveData<String>()

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
            override fun onMapClick(tag: String, point: GeoPoint,other:String) {
                if (tag == TAG) {
                    liveDataQsRecordBean.value!!.geometry =
                        GeometryTools.createGeometry(point).toText()
                    mapController.markerHandle.addMarker(point, TAG)
                    viewModelScope.launch(Dispatchers.IO) {
                        val realm = realmOperateHelper.getSelectTaskRealmInstance()
                        captureLink(realm, point)
                        realm.close()
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

        if (bean != null) {
            renderEntity = bean.renderEntity
            if (renderEntity!!.code == DataCodeEnum.OMDB_LANEINFO.code) {
                laneInfoList = SignUtil.getLineInfoIcons(renderEntity!!)
            }
        }

        //查询元数据
        viewModelScope.launch(Dispatchers.IO) {
            /**
             * 获取当前所选的任务
             */
            val taskId = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
            val realm = realmOperateHelper.getRealmDefaultInstance()

            val objects = realm.where(TaskBean::class.java).equalTo("id", taskId).findFirst()
            if (objects != null) {
                liveDataTaskBean.postValue(realm.copyFromRealm(objects))
            }
            realm.close()
            //获取当前定位点
            val geoPoint = mapController.locationLayerHandler.getCurrentGeoPoint()
            //如果不是从面板进来的
            if (bean == null) {
                geoPoint?.let {
                    liveDataQsRecordBean.value!!.geometry =
                        GeometryTools.createGeometry(it).toText()
                    withContext(Dispatchers.Main) {
                        mapController.markerHandle.addMarker(geoPoint, TAG)
//                        mapController.animationHandler.animationByLatLon(
//                            geoPoint.latitude, geoPoint.longitude
//                        )
                    }
                    val realm = realmOperateHelper.getSelectTaskRealmInstance()
                    captureLink(realm, geoPoint)
                    realm.close()
                }
            } else {
                liveDataQsRecordBean.value?.run {
                    elementId = bean.renderEntity.code
                    linkId = bean.linkId
                    if (linkId.isNotEmpty()) {
                        viewModelScope.launch {
                            val link = realmOperateHelper.queryLink(linkId)
                            if (link != null) {
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
        }
        viewModelScope.launch(Dispatchers.IO) {
            getClassTypeList(bean)
            getProblemLinkList()
            addChatMsgEntity(filePath)
        }
    }

    /**
     * 捕捉道路或新增评测link
     */
    private suspend fun captureLink(realm: Realm, point: GeoPoint) {
        Log.e("jingo", "捕捉道路SSS")
        if (liveDataTaskBean.value == null) {
            liveDataToastMessage.postValue("请先选择所属任务!")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            liveDataQsRecordBean.value?.let {
                val taskLink =
                    realmOperateHelper.captureTaskLink(point, taskId = liveDataTaskBean.value!!.id)
                if (taskLink != null) {
                    it.linkId = taskLink.linkPid
                    mapController.lineHandler.showLine(taskLink.geometry)
                    Log.e("jingo", "捕捉道路EEE 1")
                    return
                } else {
                    val linkList = realmOperateHelper.queryLink(realm, point = point)
                    if (linkList.isNotEmpty()) {
                        it.linkId = linkList[0].linkPid
                        mapController.lineHandler.showLine(linkList[0].geometry)
                        Log.e("jingo", "捕捉道路EEE 2")
                        return
                    }
                }
                it.linkId = ""
                mapController.lineHandler.removeLine()
            }
        }
        Log.e("jingo", "捕捉道路EEE 3")
    }

    /**
     *  //获取问题分类列表
     */
    fun getClassTypeList(bean: SignBean? = null) {
        if (bean != null) {
            renderEntity = bean.renderEntity
            if (renderEntity!!.code == DataCodeEnum.OMDB_LANEINFO.code) {
                laneInfoList = SignUtil.getLineInfoIcons(renderEntity!!)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            Log.e("jingo", "获取问题分类列表 SSS")
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
            Log.e("jingo", "获取问题分类列表 EEE")
        }
    }

    /**
     * 获取问题环节列表和初步问题
     */
    fun getProblemLinkList() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.e("jingo", "获取问题环节列表 SSS")
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
                    val problemLinkB = liveDataQsRecordBean.value!!.problemLink.isEmpty()
                    val causeB = liveDataQsRecordBean.value!!.cause.isEmpty()
                    if (liveDataQsRecordBean.value!!.problemLink.isEmpty()) {
                        liveDataQsRecordBean.value!!.problemLink = middleList[0]
                    }
                    if (liveDataQsRecordBean.value!!.cause.isEmpty()) {
                        liveDataQsRecordBean.value!!.cause = rightList[0].text
                    }
                    if (problemLinkB && causeB)
                        liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
//                    liveDataMiddleTypeList.postValue(middleList)
                    liveDataRightTypeList.postValue(rightList)
                }
            }
            Log.e("jingo", "获取问题环节列表 EEE")
        }
    }

    /**
     * 获取问题类型列表和问题现象
     */
    private suspend fun getProblemList(classType: String) {
        Log.e("jingo", "获取问题类型列表和问题现象 SSS")
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
                val problemTypeB = liveDataQsRecordBean.value!!.problemType.isEmpty()
                val phenomenonB = liveDataQsRecordBean.value!!.phenomenon.isEmpty()
                if (problemTypeB) {
                    liveDataQsRecordBean.value!!.problemType = typeTitleList[0]
                }
//                liveDataMiddleTypeList.postValue(typeTitleList)
                if (phenomenonB) {
                    liveDataQsRecordBean.value!!.phenomenon = phenomenonRightList[0].text
                }
                if (problemTypeB && phenomenonB)
                    liveDataQsRecordBean.postValue(liveDataQsRecordBean.value)
                liveDataRightTypeList.postValue(phenomenonRightList)
            }
        }
        Log.e("jingo", "获取问题类型列表和问题现象 EEE")
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

            val realm = realmOperateHelper.getRealmDefaultInstance()
            liveDataQsRecordBean.value!!.taskId = liveDataTaskBean.value!!.id
            liveDataQsRecordBean.value!!.checkTime = DateTimeUtil.getDataTime()
            liveDataQsRecordBean.value!!.checkUserId = Constant.USER_REAL_NAME
            realm.executeTransaction {
                it.copyToRealmOrUpdate(liveDataQsRecordBean.value)
            }
            mapController.markerHandle.addOrUpdateQsRecordMark(liveDataQsRecordBean.value!!)
            liveDataFinish.postValue(true)
            realm.refresh()
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
                    val realm = realmOperateHelper.getRealmDefaultInstance()
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

            val realm = realmOperateHelper.getRealmDefaultInstance()

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
                    liveDataLanInfoChange.value = it.description
                    // 显示语音数据到界面
                    getChatMsgEntityList()
                    realm.close()
                    if (it.elementId == DataCodeEnum.OMDB_LANEINFO.code) {
                        val realm2 = realmOperateHelper.getSelectTaskRealmInstance()
                        val r = realm2.where(RenderEntity::class.java)
                            .equalTo("table", DataCodeEnum.OMDB_LANEINFO.name)
                            .equalTo("linkPid", it.linkId).findFirst()
                        if (r != null) {
                            renderEntity = realm2.copyFromRealm(r)
                            laneInfoList = SignUtil.getLineInfoIcons(renderEntity!!)
                        }
                        realm2.close()
                    }
                }
            } else {
                liveDataToastMessage.postValue("数据读取失败")
                realm.close()
            }


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
                filePath?.let {
                    val file = File(it)
                    if (file.exists() && file.length() < 1600) {
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
            val picList = mutableListOf<String>()
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
                    val realm = realmOperateHelper.getRealmDefaultInstance()
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

    /**
     * 增加车信
     */
    fun updateLaneInfo(index: Int, id: Int, type: Int) {
        laneInfoList?.let {
            val laneInfoItem = it[index]
            if (laneInfoItem.id != id || laneInfoItem.type != type) {
                laneInfoItem.id = id
                laneInfoItem.type = type
                editLaneInfoProblem()
            }
        }
    }

    /**
     * 增加车信
     */
    fun addLaneInfo(id: Int, type: Int): Int {
        laneInfoList?.let {
            it.add(LaneInfoItem(id, type))
            editLaneInfoProblem()
            return it.size
        }
        return 0
    }

    /**
     * 删除车信
     */
    fun backspaceLaneInfo() {
        laneInfoList?.let {
            if (it.isNotEmpty()) {
                it.removeLast()
                editLaneInfoProblem()
            }
        }
    }

    /**
     * 删除车信
     */
    fun removeAllLaneInfo() {
        laneInfoList?.clear()
    }

    /**
     * 组织车信备注文字
     */
    private fun editLaneInfoProblem() {
        laneInfoList?.let {
            liveDataQsRecordBean.value?.let { bean ->
                var strBuffer = StringBuffer()
                if (bean.problemType == "遗漏")
                    strBuffer.append("车信缺失,车道从左到右分别是：")
                else if (bean.problemType == "错误")
                    strBuffer.append("车信错误,车道从左到右分别是：")
                for (item in it) {
                    when (item.id) {
                        R.drawable.laneinfo_1 -> strBuffer.append("[直(1)")
                        R.drawable.laneinfo_2 -> strBuffer.append("[左(2)")
                        R.drawable.laneinfo_3 -> strBuffer.append("[右(3)")
                        R.drawable.laneinfo_5 -> strBuffer.append("[左斜前(5)")
                        R.drawable.laneinfo_6 -> strBuffer.append("[右斜前(6)")
                        R.drawable.laneinfo_4 -> strBuffer.append("[调(4)")
                        R.drawable.laneinfo_7 -> strBuffer.append("[反向调(7)")
                        R.drawable.laneinfo_1_2 -> strBuffer.append("[左直(1,2)")
                        R.drawable.laneinfo_1_5 -> strBuffer.append("[左斜前直(1,5)")
                        R.drawable.laneinfo_2_5 -> strBuffer.append("[左左斜前(2,5)")
                        R.drawable.laneinfo_2_6 -> strBuffer.append("[左右斜前(2,6)")
                        R.drawable.laneinfo_1_3 -> strBuffer.append("[直右(1,3)")
                        R.drawable.laneinfo_1_6 -> strBuffer.append("[右斜前直(1,6)")
                        R.drawable.laneinfo_3_5 -> strBuffer.append("[左斜前右(3,5)")
                        R.drawable.laneinfo_3_6 -> strBuffer.append("[右斜前右(3,6)")
                        R.drawable.laneinfo_2_3 -> strBuffer.append("[左右(2,3)")
                        R.drawable.laneinfo_5_6 -> strBuffer.append("[左斜前右斜前(5,6)")
                        R.drawable.laneinfo_1_4 -> strBuffer.append("[直调(1,4)")
                        R.drawable.laneinfo_4_5 -> strBuffer.append("[调左斜前(4,5)")
                        R.drawable.laneinfo_2_4 -> strBuffer.append("[左调(2,4)")
                        R.drawable.laneinfo_3_4 -> strBuffer.append("[右调(3,4)")
                        R.drawable.laneinfo_4_6 -> strBuffer.append("[调右斜前(4,6)")
                        R.drawable.laneinfo_1_7 -> strBuffer.append("[直反向调(1,7)")
                        R.drawable.laneinfo_1_2_3 -> strBuffer.append("[左直右(1,2,3)")
                        R.drawable.laneinfo_1_2_4 -> strBuffer.append("[调左直(1,2,4)")
                        R.drawable.laneinfo_1_2_5 -> strBuffer.append("[左左斜前直(1,2,5)")
                        R.drawable.laneinfo_1_2_6 -> strBuffer.append("[左直右斜前(1,2,6)")
                        R.drawable.laneinfo_1_3_4 -> strBuffer.append("[调直右(1,3,4)")
                        R.drawable.laneinfo_1_3_5 -> strBuffer.append("[左斜前直右(1,3,5)")
                        R.drawable.laneinfo_1_3_6 -> strBuffer.append("[直右斜前右(1,3,6)")
                        R.drawable.laneinfo_2_3_4 -> strBuffer.append("[调左右(2,3,4)")
                        R.drawable.laneinfo_0 -> strBuffer.append("[不允许存在(0)")
                    }
                    if (item.type == 1) {
                        strBuffer.append("(附加)]")
                    } else if (item.type == 2) {
                        strBuffer.append("(公交)]")
                    } else {
                        strBuffer.append("]")
                    }
                }
                liveDataQsRecordBean.value!!.description = strBuffer.toString()
                liveDataLanInfoChange.value = strBuffer.toString()
            }
        }
    }
}
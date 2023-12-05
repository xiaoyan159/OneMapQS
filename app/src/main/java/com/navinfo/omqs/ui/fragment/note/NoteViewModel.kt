package com.navinfo.omqs.ui.fragment.note

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
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
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.MapParamUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ChatMsgEntity
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.SoundMeter
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val mapController: NIMapController,
    private val realmOperateHelper: RealmOperateHelper
) : ViewModel() {

    lateinit var canvasView: CanvasView


    /**
     * 要保存的评测数据
     */
    val liveDataNoteBean = MutableLiveData(NoteBean(id = UUID.randomUUID().toString()))

    /**
     * 语音列表
     */
    val listDataChatMsgEntityList = MutableLiveData<MutableList<ChatMsgEntity>>()

    /**
     * 照片列表
     */
    val liveDataPictureList = MutableLiveData<MutableList<String>>()

    var isEraser = false

    var noteBeanDescription = ""

    //语音窗体
    private var pop: PopupWindow? = null

    private var mSpeakMode: SpeakMode? = null

    var oldBean: NoteBean? = null

    //录音图标
    var volume: ImageView? = null

    var mSoundMeter: SoundMeter? = null
    /**
     * toast信息
     */
    val liveDataToastMessage = MutableLiveData<String>()
    /**
     * 处理结束关闭fragment
     */
    val liveDataFinish = MutableLiveData(false)

    /**
     * 通知页面画布初始化完成
     */
    val liveDataCanvasViewInitFinished = MutableLiveData(false)

    fun initCanvasView(canvasView: CanvasView) {
        this.canvasView = canvasView
        liveDataCanvasViewInitFinished.value = true
    }


    /**
     * 通知橡皮擦开关
     */
    fun onEraser() {
        isEraser = !isEraser
        canvasView.setEraser(isEraser)
//        liveEraserData.value = !liveEraserData.value!!
    }

    /**
     * 通知清除
     */
    fun onClear() {
        canvasView.removeAllPaint()
//        liveClearData.value = true
    }

    /**
     * 通知回退
     */
    fun onBack() {
        canvasView.back()
//        liveBackData.value = true
    }

    /**
     * 通知撤销回退
     */
    fun onForward() {
        canvasView.forward()
//        liveForward.value = true
    }

    /**
     * 保存数据
     */
    fun onSaveData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (canvasView.paths != null && canvasView.paths!!.isNotEmpty()) {
                var noteBean = CanvasViewHelper.createNoteBean(mapController, canvasView.paths!!)
                liveDataNoteBean.value!!.taskId = MapParamUtils.getTaskId()
                liveDataNoteBean.value!!.list = noteBean.list
                liveDataNoteBean.value!!.description = noteBeanDescription
                liveDataNoteBean.value!!.guideGeometry = noteBean.guideGeometry
                val realm = realmOperateHelper.getRealmDefaultInstance()
                realm.executeTransaction {
                    it.copyToRealmOrUpdate(liveDataNoteBean.value)
                }
                mapController.markerHandle.addOrUpdateNoteMark(liveDataNoteBean.value!!)
                liveDataFinish.postValue(true)
                realm.refresh()
                realm.close()
            }else{
                liveDataToastMessage.postValue("请绘制内容!")
            }
        }
    }

    /**
     * 删除数据
     */
    fun deleteData(context: Context) {
        if (liveDataNoteBean.value == null) {
            liveDataFinish.postValue(true)
            return
        } else {
            val mDialog = FirstDialog(context)
            mDialog.setTitle("提示？")
            mDialog.setMessage("是否删除标签，请确认！")
            mDialog.setPositiveButton(
                "确定"
            ) { dialog, _ ->
                dialog.dismiss()
                viewModelScope.launch(Dispatchers.IO) {
                    val realm = realmOperateHelper.getRealmDefaultInstance()
                    realm.executeTransaction {
                        val objects = it.where(NoteBean::class.java)
                            .equalTo("id", liveDataNoteBean.value!!.id).findFirst()
                        objects?.deleteFromRealm()
                    }
                    mapController.markerHandle.removeNoteMark(liveDataNoteBean.value!!)
                    liveDataFinish.postValue(true)
                    realm.close()
                }
            }
            mDialog.setNegativeButton("取消", null)
            mDialog.show()
        }
    }

    /**
     * 初始化数据
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun initData(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = realmOperateHelper.getRealmDefaultInstance()
            realm.executeTransaction { it ->
                val objects = it.where(NoteBean::class.java)
                    .equalTo("id", id).findFirst()
                if(objects!=null){
                    oldBean = realm.copyFromRealm(objects)
                    oldBean?.let {
                        noteBeanDescription = it.description
                        liveDataNoteBean.postValue(it.copy())
                        val list = CanvasViewHelper.createDrawPaths(mapController, it)
                        canvasView.setDrawPathList(list)
                        liveDataNoteBean.value?.attachmentBeanList = it.attachmentBeanList
                    }
                }
            }
            // 显示语音数据到界面
            getChatMsgEntityList()

            realm.close()

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
                picList.add(file.name)
            } else {
                picList.addAll(liveDataPictureList.value!!)
                picList.add(file.name)
            }

            var attachmentList: RealmList<AttachmentBean> = RealmList()
            //赋值处理
            if (liveDataNoteBean.value?.attachmentBeanList?.isEmpty() == false) {
                attachmentList = liveDataNoteBean.value?.attachmentBeanList!!
            }
            val attachmentBean = AttachmentBean()
            attachmentBean.name = file.name!!
            attachmentBean.type = 2
            attachmentList.add(attachmentBean)
            liveDataNoteBean.value?.attachmentBeanList = attachmentList
            liveDataPictureList.postValue(picList)
        }

    }


    /**
     * 多媒体列表
     */
    private suspend fun getChatMsgEntityList() {
        val chatMsgEntityList: MutableList<ChatMsgEntity> = ArrayList()
        val pictureList: MutableList<String> = ArrayList()
        liveDataNoteBean.value?.attachmentBeanList?.forEach {
            //1 录音
            if (it.type == 1) {
                val chatMsgEntity = ChatMsgEntity()
                chatMsgEntity.name = it.name
                chatMsgEntity.voiceUri = Constant.USER_DATA_ATTACHEMNT_PATH
                chatMsgEntityList.add(chatMsgEntity)
            }else if(it.type==2){
                pictureList.add(it.name)
            }

        }
        listDataChatMsgEntityList.postValue(chatMsgEntityList)
        liveDataPictureList.postValue(pictureList)
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
            if (liveDataNoteBean.value?.attachmentBeanList?.isEmpty() == false) {
                attachmentList = liveDataNoteBean.value?.attachmentBeanList!!
            }

            val attachmentBean = AttachmentBean()
            attachmentBean.name = chatMsgEntity.name!!
            attachmentBean.type = 1
            attachmentList.add(attachmentBean)
            liveDataNoteBean.value?.attachmentBeanList = attachmentList

            listDataChatMsgEntityList.postValue(chatMsgEntityList)
        }
    }

}
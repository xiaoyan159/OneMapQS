package com.navinfo.omqs.ui.fragment.note

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.MapParamUtils
import com.navinfo.omqs.ui.dialog.FirstDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    val mapController: NIMapController
) : ViewModel() {

    lateinit var canvasView: CanvasView


    var mNoteBean: NoteBean? = null


    var isEraser = false

    var noteBeanDescription = ""
//    /**
//     * 橡皮擦开关
//     */
//    val liveEraserData = MutableLiveData(false)
//
//    /**
//     * 清除按钮
//     */
//    val liveClearData = MutableLiveData(false)
//
//    /**
//     * 回退按钮
//     */
//    val liveBackData = MutableLiveData(false)
//
//    /**
//     * 撤销按钮
//     */
//    val liveForward = MutableLiveData(false)

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
                var noteBean =
                    CanvasViewHelper.createNoteBean(mapController, canvasView.paths!!)
                if (mNoteBean != null) {
                    noteBean.id = mNoteBean!!.id
                    noteBean.description = noteBeanDescription
                }
                noteBean.taskId = MapParamUtils.getTaskId()
                mNoteBean = noteBean
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    it.copyToRealmOrUpdate(noteBean)
                }
                mapController.markerHandle.addOrUpdateNoteMark(mNoteBean!!)
                liveDataFinish.postValue(true)
                realm.close()
            }
        }
    }

    /**
     * 删除数据
     */
    fun deleteData(context: Context) {
        if (mNoteBean == null) {
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
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        val objects = it.where(NoteBean::class.java)
                            .equalTo("id", mNoteBean!!.id).findFirst()
                        objects?.deleteFromRealm()
                    }
                    mapController.markerHandle.removeNoteMark(mNoteBean!!)
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
    fun initData(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction { it ->
                val objects = it.where(NoteBean::class.java)
                    .equalTo("id", id).findFirst()
                mNoteBean = realm.copyFromRealm(objects)
                mNoteBean?.let { bean ->
                    noteBeanDescription = bean.description
                    val list = CanvasViewHelper.createDrawPaths(mapController, bean)
                    canvasView.setDrawPathList(list)
                }
            }
            realm.close()
        }
    }
}
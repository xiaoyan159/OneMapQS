package com.navinfo.omqs.ui.activity.map

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import com.blankj.utilcode.util.ToastUtils
import com.navinfo.collect.library.map.NIMapController
import org.videolan.libvlc.LibVlcUtil
import com.navinfo.omqs.R
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 创建Activity全局viewmode
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
) : ViewModel() {

    private var mCameraDialog: CommonDialog? = null

    /**
     * 点击我的位置，回到我的位置
     */
    fun onClickLocationButton() {
        mapController.locationLayerHandler.animateToCurrentPosition()
    }

    override fun onCleared() {
        super.onCleared()
    }

    //点击相机按钮
    fun onClickCameraButton(context: Context){

        Log.e("qj", LibVlcUtil.hasCompatibleCPU(context).toString())

        ToastUtils.showShort("点击了相机")

        if (mCameraDialog == null) {
            mCameraDialog = CommonDialog(context, context.resources.getDimension(R.dimen.head_img_width).toInt() * 3 + context.resources.getDimension(R.dimen.ten).toInt() + context.resources.getDimension(R.dimen.twenty_four).toInt(), context.resources.getDimension(R.dimen.head_img_width).toInt() + 10, 1)
            mCameraDialog!!.setCancelable(true)
        }
        mCameraDialog!!.openCamear(mCameraDialog!!.getmShareUtil().continusTakePhotoState)
        mCameraDialog!!.show()
        mCameraDialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
            mCameraDialog!!.hideLoading()
            mCameraDialog!!.stopVideo()
            try {
                if (!mCameraDialog!!.getmShareUtil().connectstate){
                    mCameraDialog!!.updateCameraResources(1, mCameraDialog!!.getmDeviceNum())
                }
                TakePhotoManager.getInstance().getCameraVedioClent(mCameraDialog!!.getmDeviceNum()).StopSearch()
            } catch (e: Exception) {
            }
        })
        mCameraDialog!!.setOnShowListener(DialogInterface.OnShowListener {
            mCameraDialog!!.initmTakePhotoOrRecord(mCameraDialog!!.getmShareUtil().selectTakePhotoOrRecord)
            if (!mCameraDialog!!.isShowVideo && mCameraDialog!!.getmShareUtil().connectstate && mCameraDialog!!.getmShareUtil().continusTakePhotoState) {
                mCameraDialog!!.playVideo()
            }
        })
    }
}
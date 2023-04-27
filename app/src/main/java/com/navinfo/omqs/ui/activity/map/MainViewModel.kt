package com.navinfo.omqs.ui.activity.map

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.OnQsRecordItemClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmSet
import org.oscim.core.GeoPoint
import org.videolan.libvlc.LibVlcUtil
import javax.inject.Inject

/**
 * 创建Activity全局viewmode
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
) : ViewModel() {

    val liveDataQsRecordIdList = MutableLiveData<List<String>>()
    private var mCameraDialog: CommonDialog? = null

    private var niLocationList: MutableList<NiLocation> = ArrayList<NiLocation>()

    init {
        mapController.markerHandle.setOnQsRecordItemClickListener(object :
            OnQsRecordItemClickListener {
            override fun onQsRecordList(list: MutableList<String>) {
                liveDataQsRecordIdList.value = list
            }
        })
    }

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
    fun onClickCameraButton(context: Context) {

        Log.e("qj", LibVlcUtil.hasCompatibleCPU(context).toString())

        //ToastUtils.showShort("点击了相机")

        if (mCameraDialog == null) {
            mCameraDialog = CommonDialog(
                context,
                context.resources.getDimension(R.dimen.head_img_width)
                    .toInt() * 3 + context.resources.getDimension(R.dimen.ten)
                    .toInt() + context.resources.getDimension(R.dimen.twenty_four).toInt(),
                context.resources.getDimension(R.dimen.head_img_width).toInt() + 10,
                1
            )
            mCameraDialog!!.setCancelable(true)
        }
        mCameraDialog!!.openCamear(mCameraDialog!!.getmShareUtil().continusTakePhotoState)
        mCameraDialog!!.show()
        mCameraDialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
            mCameraDialog!!.hideLoading()
            mCameraDialog!!.stopVideo()
            try {
                if (!mCameraDialog!!.getmShareUtil().connectstate) {
                    mCameraDialog!!.updateCameraResources(1, mCameraDialog!!.getmDeviceNum())
                }
                TakePhotoManager.getInstance().getCameraVedioClent(mCameraDialog!!.getmDeviceNum())
                    .StopSearch()
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

    fun startSaveTraceThread(context: Context) {
        Thread(Runnable {
            try {
                while (true) {

                    if (niLocationList != null && niLocationList.size > 0) {

                        var niLocation = niLocationList[0]
                        val geometry = GeometryTools.createGeometry(GeoPoint(niLocation.latitude,niLocation.longitude))
                        val tileX = RealmSet<Int>()
                        GeometryToolsKt.getTileXByGeometry(geometry.toString(), tileX)
                        val tileY = RealmSet<Int>()
                        GeometryToolsKt.getTileYByGeometry(geometry.toString(), tileY)

                        //遍历存储tile对应的x与y的值
                        tileX.forEach { x ->
                            tileY.forEach { y ->
                                niLocation.tilex = x
                                niLocation.tiley = y
                            }
                        }

                        TraceDataBase.getDatabase(context, Constant.USER_DATA_PATH + "/trace.sqlite").niLocationDao.insert(niLocation)
                        niLocationList.remove(niLocation)

                        Log.e("qj", "saveTrace==${niLocationList.size}")
                    }
                    Thread.sleep(30)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                Log.e("qj", "异常==${e.message}")
            }
        }).start()
    }

    //增加轨迹存储
    fun addSaveTrace(niLocation: NiLocation) {
        if (niLocation != null && niLocationList != null) {
            niLocationList.add(niLocation)
        }
    }

    fun navigation(activity: MainActivity, list: List<String>) {
        //获取右侧fragment容器
        val naviController = activity.findNavController(R.id.main_activity_right_fragment)

        naviController.currentDestination?.let { navDestination ->
//            when (val fragment =
//                activity.supportFragmentManager.findFragmentById(navDestination.id)) {
//                //判断右侧的fragment是不是质检数据
////                is EvaluationResultFragment -> {
////                    val viewModelFragment =
////                        ViewModelProvider(fragment)[EvaluationResultViewModel::class.java]
////                    viewModelFragment.notifyData(list)
////                }
//                is EmptyFragment -> {
//                    if (list.size == 1) {
//                        val bundle = Bundle()
//                        bundle.putString("QsId", list[0])
//                        naviController.navigate(R.id.EvaluationResultFragment, bundle)
//                    }
//                }
//            }
            when (navDestination.id) {
                R.id.EmptyFragment -> {
                    if (list.size == 1) {
                        val bundle = Bundle()
                        bundle.putString("QsId", list[0])
                        naviController.navigate(R.id.EvaluationResultFragment, bundle)
                    }
                }
            }
        }

    }
}
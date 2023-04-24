package com.navinfo.omqs.ui.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.VolleyError;
import com.navinfo.collect.library.garminvirbxe.CameraEventListener;
import com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE;
import com.navinfo.collect.library.garminvirbxe.HostBean;
import com.navinfo.collect.library.garminvirbxe.SensorParams;
import com.navinfo.collect.library.sensor.ISensor.enmConnectionStatus;
import com.navinfo.collect.library.sensor.ISensor.enmSensorModel;
import com.navinfo.collect.library.sensor.ISensor.SensorWorkingMode;
import com.navinfo.collect.library.sensor.ISensor.enmSensorType;
import com.navinfo.collect.library.sensor.ISensor.enmSignalQuality;
import com.navinfo.collect.library.sensor.SensorManager;
import com.navinfo.collect.library.utils.StringUtil;
import com.navinfo.omqs.Constant;
import com.navinfo.omqs.util.ShareUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author qj
 * @version V1.0
 * @Date 2023/4/14.
 * @Description: (外设相机控制类)
 */
public class TakePhotoManager {

    /**
     * 拍照控制类集合
     */
    private List<CameraGarminVirbXE> mSensorInstanceList;

    /**
     * 拍照过程回调监听
     */
    /**
     * 拍照管理类
     */
    private static volatile TakePhotoManager mInstance;
    //外设相机连接状态
    private HashMap<String, enmConnectionStatus> mConnectionStatusHashMap;
    //上下文
    private Context mCon;

    /**
     * 装拍照过程回调数组
     */
    private final List<CameraEventListener> mOnCameraEventListeners = new ArrayList<CameraEventListener>();

    public static TakePhotoManager getInstance() {
        if (mInstance == null) {
            synchronized (TakePhotoManager.class) {
                if (mInstance == null) {
                    mInstance = new TakePhotoManager();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context, int cameraCount) {

        mCon = context;

        if (cameraCount == 0)
            cameraCount = 1;

        mSensorInstanceList = new ArrayList<CameraGarminVirbXE>();

        mConnectionStatusHashMap = new HashMap<String, enmConnectionStatus>();

        CameraEventListener cameraEevent = new CameraEventListener() {
            @Override
            public void OnSnapPictureResponse(HostBean hostBean, Bitmap bitmap, String picName, int tag) {
                Log.i("info", "bitmap:" + bitmap);
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnSnapPictureResponse(hostBean, bitmap, picName, tag);
                        }
                    }
                }
            }

            @Override
            public void requestError(HostBean hostBean, com.android.volley.VolleyError e, CameraGarminVirbXE.enmCommandType commandType, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.requestError(hostBean, e, commandType, tag);
                        }
                    }
                }
            }

            @Override
            public void OnStartRecordResponse(HostBean hostBean, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnStartRecordResponse(hostBean, tag);
                        }
                    }
                }
            }

            @Override
            public void OnStopRecordResponse(HostBean hostBean, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnStopRecordResponse(hostBean, tag);
                        }
                    }
                }
            }

            @Override
            public void OnStatusResponse(HostBean hostBean, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnStatusResponse(hostBean, tag);
                        }
                    }
                }
            }

            @Override
            public void OnSearchResponse(int tag, ArrayList<HostBean> scanIpList) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnSearchResponse(tag, scanIpList);
                        }
                    }
                }
            }

            @Override
            public void OnConnectStatusChanged(HostBean hostBean, enmConnectionStatus connectStatus, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnConnectStatusChanged(hostBean, connectStatus, tag);
                        }
                    }
                }
            }

            @Override
            public void OnGetGpsStatusResponse(HostBean hostBean, boolean status, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnGetGpsStatusResponse(hostBean, status, tag);
                        }
                    }
                }
            }

            @Override
            public void OnConnectionStatusChanged(HostBean hostBean, enmConnectionStatus cs, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnConnectionStatusChanged(hostBean, cs, tag);
                        }
                    }
                }
            }

            @Override
            public void OnContinuousPhototTimeLapseRateResponse(HostBean hostBean, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnContinuousPhototTimeLapseRateResponse(hostBean, tag);
                        }
                    }
                }
            }

            @Override
            public void OnContinuousPhototTimeLapseRateStartResponse(HostBean hostBean, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnContinuousPhototTimeLapseRateStartResponse(hostBean, tag);
                        }
                    }
                }
            }

            @Override
            public void OnContinuousPhototTimeLapseRateStopResponse(HostBean hostBean, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnContinuousPhototTimeLapseRateStopResponse(hostBean, tag);
                        }
                    }
                }
            }

            @Override
            public void OnContinuousPhototSingle(HostBean hostBean, String url, String name, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnContinuousPhototSingle(hostBean, url, name, tag);
                        }
                    }
                }
            }

            @Override
            public void OnGetMediaList(HostBean hostBean, String json, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnGetMediaList(hostBean, json, tag);
                        }
                    }
                }
            }

            @Override
            public void OnSensorEvent() {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnSensorEvent();
                        }
                    }
                }
            }

            @Override
            public void OnConnectionStatusChanged(enmConnectionStatus cs) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnConnectionStatusChanged(cs);
                        }
                    }
                }
            }

            @Override
            public void OnGetDeviceInfo(HostBean hostBean, String devicesId, int tag) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnGetDeviceInfo(hostBean, devicesId, tag);
                        }
                    }
                }
            }

            @Override
            public void OnSignalQualityChanged(enmSignalQuality sq) {
                if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                    for (CameraEventListener weakRef : mOnCameraEventListeners) {
                        if (weakRef != null) {
                            weakRef.OnSignalQualityChanged(sq);
                        }
                    }
                }
            }
        };

        for (int i = 0; i < cameraCount; i++) {

            CameraGarminVirbXE clent = (CameraGarminVirbXE) SensorManager.getInstance().CreateSensor(context, enmSensorType.SENSOR_CAMEAR, enmSensorModel.CAMERA_GARMIN_VIRB_XE);
            clent.setmTag(i + 1);
            clent.RegisterSensorEvent(cameraEevent);
            mSensorInstanceList.add(clent);
        }

    }

    //连接网络
    public boolean connect(int indexClent, HostBean hostBean, SensorParams params) {
        if (hostBean != null) {
            CameraGarminVirbXE cameraGarminVirbXE = getCameraVedioClent(indexClent);
            if (cameraGarminVirbXE != null) {
                cameraGarminVirbXE.Connect(hostBean, params);
                return true;
            }
        }
        return false;
    }

    public enmConnectionStatus getConnectionStatus(HostBean hostBean) {

        if (hostBean != null && mConnectionStatusHashMap != null && mConnectionStatusHashMap.containsKey(hostBean.hardwareAddress)) {
            return mConnectionStatusHashMap.get(hostBean.hardwareAddress);
        }


        return enmConnectionStatus.DISCONNECTTED;
    }

    public enmConnectionStatus getConnectionStatus() {

        if (mSensorInstanceList != null && mConnectionStatusHashMap != null) {
            enmConnectionStatus mEnmConnectionStatus = enmConnectionStatus.DISCONNECTTED;
            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                mEnmConnectionStatus = cameraGarminVirbXE.GetConnectionStatus();
                if (mEnmConnectionStatus == enmConnectionStatus.CONNECTTED)
                    return enmConnectionStatus.CONNECTTED;

            }
        }


        return enmConnectionStatus.DISCONNECTTED;
    }

    //设置相机模式
    public void setCameraMode(int indexClent, SensorWorkingMode mode) {
        if (indexClent > 0 && mSensorInstanceList!=null && indexClent <= mSensorInstanceList.size()) {
            getCameraVedioClent(indexClent).SetMode(mode);
        }
    }

    //获取当前相机模式
    public SensorWorkingMode getCameraMode(int indexClent) {
        if (mSensorInstanceList != null && indexClent > 0 && indexClent <= mSensorInstanceList.size()) {
            return getCameraVedioClent(indexClent).GetMode();
        }

        return null;
    }

    public String mediaList(int indexClent, String path) {
        if (mSensorInstanceList != null && indexClent > 0 && indexClent <= mSensorInstanceList.size()) {
            getCameraVedioClent(indexClent).mediaList(path);
        }

        return null;
    }


    //搜索网络
    public void SearchNet(HostBean hostBean, int indexClent, boolean isReadCache) {
        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0 && indexClent > 0 && indexClent <= mSensorInstanceList.size()) {
            mSensorInstanceList.get(indexClent - 1).Search(isReadCache);
            return;
        }
    }

    //连续拍照
    public void StartRecording(HostBean hostBean, int indexCamera) {
        if (mSensorInstanceList != null && indexCamera > 0 && indexCamera <= mSensorInstanceList.size()) {
            getCameraVedioClent(indexCamera).StartRecording();
        }
    }


    public void getGpsStatus() {
        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0) {
            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                cameraGarminVirbXE.GetGpsStatus();
            }
        }

    }

    //获取GPS状态
    public void getGpsStatus(HostBean hostBean, int index) {
        if (hostBean != null) {
            CameraGarminVirbXE cameraGarminVirbXE = findCameraGarminVirbXE(hostBean, index);
            if (cameraGarminVirbXE != null) {
                cameraGarminVirbXE.GetGpsStatus();
            }
        }
    }

    public void startCameraVedio(HostBean hostBean, int indexClent) {
        if (hostBean != null) {

            StopContinuousTakePhoto(hostBean, indexClent);

            setCameraMode(indexClent, SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE);

            ShareUtil.getCameraMode(mCon).setContinusTakePhotoState(Constant.USER_ID, false);

            StartRecording(hostBean, indexClent);

        }

    }

    //抓拍
    public void SnapShot(HostBean hostBean, int index) {
        if (hostBean != null) {
            CameraGarminVirbXE cameraGarminVirbXE = findCameraGarminVirbXE(hostBean, index);
            if (cameraGarminVirbXE != null) {
                cameraGarminVirbXE.snapPicture(StringUtil.Companion.createUUID());
            }
        }
    }

    public Long getGoogleTime() {
        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0) {
            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                if (cameraGarminVirbXE.getGoogleGpsTime() > 0) {
                    return cameraGarminVirbXE.getGoogleGpsTime();
                }
            }
        }

        return 0l;
    }

    //停止时间线程，减少电量消耗
    public void stopTimeThread() {
        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0) {
            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                cameraGarminVirbXE.stopTimeThread();
            }
        }
    }

    //设置拍照路径
    public void setSavePath(HostBean hostBean, String path, int index) {
        if (path == null || path.equals("")) {
            return;

        }
        if (hostBean != null) {
            CameraGarminVirbXE cameraGarminVirbXE = findCameraGarminVirbXE(hostBean, index);
            if (cameraGarminVirbXE != null) {
                File file = new File(path);
                if (file.exists() && file.isDirectory()) {
                    cameraGarminVirbXE.SetCameraPictureSavaPath(path);
                }
            }
        }
    }

    //停止搜索
    public void StopSearchNet(HostBean hostBean, int indexClent) {
        if (hostBean != null) {
            CameraGarminVirbXE cameraGarminVirbXE = findCameraGarminVirbXE(hostBean, indexClent);
            if (cameraGarminVirbXE != null) {
                cameraGarminVirbXE.StopSearch();
            }
        }
    }

    //停止连拍
    public void StopContinuousTakePhoto(HostBean hostBean, int indexCamera) {
        if (mSensorInstanceList != null && indexCamera > 0 && indexCamera <= mSensorInstanceList.size()) {
            getCameraVedioClent(indexCamera).StopRecording();
        }

    }

    //停止所有相机
    public void StopContinuousTakePhotoAll() {
        if (mSensorInstanceList != null) {
            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                cameraGarminVirbXE.StopRecording();
            }
        }

    }

    //查找对应控制类
    private CameraGarminVirbXE findCameraGarminVirbXE(HostBean hostBean, int index) {

        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0) {

            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                if (cameraGarminVirbXE.getmHostBean() != null && hostBean != null && !TextUtils.isEmpty(cameraGarminVirbXE.getmHostBean().hardwareAddress)
                        && !TextUtils.isEmpty(hostBean.hardwareAddress) && cameraGarminVirbXE.getmHostBean().hardwareAddress.equalsIgnoreCase(hostBean.hardwareAddress) && cameraGarminVirbXE.getmTag() == index) {
                    return cameraGarminVirbXE;
                }
            }

            return mSensorInstanceList.get(0);
        }

        return null;
    }


    public void setOnCameraEventChangeListener(CameraEventListener listener) {
        synchronized (mOnCameraEventListeners) {
            if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                for (CameraEventListener weakRef : mOnCameraEventListeners) {
                    if (weakRef != null
                            && weakRef == listener) {
                        return;
                    }
                }
            }
            mOnCameraEventListeners.add(listener);
        }
    }

    public void removeCameraEventListener(HostBean hostBean, CameraEventListener listener) {
        synchronized (mOnCameraEventListeners) {
            CameraEventListener weakRef;
            if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                for (int idx = 0; idx < mOnCameraEventListeners.size(); idx++) {
                    if ((weakRef = mOnCameraEventListeners.get(idx)) != null) {
                        if (weakRef == listener) {
                            mOnCameraEventListeners.remove(idx);
                            return;
                        }
                    } else {
                        mOnCameraEventListeners.remove(idx);
                        idx--;
                    }
                }
            }
        }
    }

    public void removeCameraEventListenerAll(CameraEventListener listener) {
        synchronized (mOnCameraEventListeners) {
            CameraEventListener weakRef;
            if (mOnCameraEventListeners != null && mOnCameraEventListeners.size() > 0) {
                for (int idx = 0; idx < mOnCameraEventListeners.size(); idx++) {
                    if ((weakRef = mOnCameraEventListeners.get(idx)) != null) {
                        if (weakRef == listener) {
                            mOnCameraEventListeners.remove(idx);
                            return;
                        }
                    } else {
                        mOnCameraEventListeners.remove(idx);
                        idx--;
                    }
                }
            }
        }
    }

    //根据拍照模式获取控制类
    public CameraGarminVirbXE getCameraVedioClent(SensorWorkingMode mode) {

        if (mode == null)
            return null;

        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0) {
            for (CameraGarminVirbXE cameraGarminVirbXE : mSensorInstanceList) {
                if (cameraGarminVirbXE.GetMode() != null && cameraGarminVirbXE.GetMode() == mode) {
                    return cameraGarminVirbXE;
                }
            }
        }

        return null;
    }

    public CameraGarminVirbXE getCameraVedioClent(int indexClent) {

        if (indexClent <= 0)
            return null;

        if (mSensorInstanceList != null && mSensorInstanceList.size() > 0 && indexClent <= mSensorInstanceList.size()) {
            return mSensorInstanceList.get(indexClent - 1);
        }

        return null;
    }
}

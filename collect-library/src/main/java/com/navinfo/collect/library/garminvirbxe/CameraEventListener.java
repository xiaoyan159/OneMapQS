package com.navinfo.collect.library.garminvirbxe;

import android.graphics.Bitmap;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import com.navinfo.collect.library.sensor.SensorEventListener;
import com.navinfo.collect.library.sensor.ISensor.enmConnectionStatus;

/**
 * @author dongpuxiao
 * @version V1.0
 * @ClassName: CameraEventListener.java
 * @Date 2017年8月22日 下午2:24:43
 * @Description: 相机操作接口定义
 */
public interface CameraEventListener extends SensorEventListener{
    /**
     *拍照成功
     *
     * @return
     */
    public void OnSnapPictureResponse(HostBean hostBean, Bitmap bitmap, String picName, int tag);

    /** 错误 */
    public void requestError(HostBean hostBean, VolleyError e, CameraGarminVirbXE.enmCommandType commandType, int tag);

    /**
     * 开始自动拍照回调
     */
    public void OnStartRecordResponse(HostBean hostBean, int tag);

    /**
     * 停止自动拍照回调
     */
    public void OnStopRecordResponse(HostBean hostBean, int tag);

    /**
     * 停止自动拍照回调
     */
    public void OnStatusResponse(HostBean hostBean, int tag);

    /**
     * 局域网搜索相机完成回调
     * @param tag
     * @param scanIpList
     */
    public void OnSearchResponse(int tag,ArrayList<HostBean> scanIpList);

    /**
     * 状态发生改变时候回调
     * @param connectStatus
     */
    public void OnConnectStatusChanged(HostBean hostBean, enmConnectionStatus connectStatus, int tag);


    /**
     * 返回是否有gps信息
     * @param status
     */
    public void OnGetGpsStatusResponse(HostBean hostBean, boolean status, int tag);

    /**
     * 状态发生改变时候回调
     * @param hostBean 相机
     * @param cs
     */
    public void OnConnectionStatusChanged(HostBean hostBean, enmConnectionStatus cs, int tag);


    /**
     * 设置自动拍照为1s
     */
    public void OnContinuousPhototTimeLapseRateResponse(HostBean hostBean, int tag);

    /**
     * 启动自动拍照
     */
    public void OnContinuousPhototTimeLapseRateStartResponse(HostBean hostBean, int tag);

    /**
     * 启动自动拍照
     */
    public void OnContinuousPhototTimeLapseRateStopResponse(HostBean hostBean, int tag);

    /**
     * 单拍照
     */
    public void OnContinuousPhototSingle(HostBean hostBean, String url, String name, int tag);

    /**
     * 获取多媒体信息
     *
     */
    public void OnGetMediaList(HostBean hostBean, String json, int tag);

    /**
     * 返回设备id
     * @param devicesId
     */
    public void OnGetDeviceInfo(HostBean hostBean,String devicesId,int tag);
}


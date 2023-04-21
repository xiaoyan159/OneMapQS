package com.navinfo.collect.library.garminvirbxe;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.navinfo.collect.library.utils.PreferencesUtils;

public class Command {

    private Context mContext;
    private static CameraEventListener cameraEventListener;
    String mPicUrl; // 返回照片的uri
    Bitmap mBitmap;
    private RequestManager requestManager;
    private Handler mHandle;
    public static Boolean createBitmap = false; // 是否生成bitmap

    public Command(Context mContext) {
        this.mContext = mContext;
        requestManager = new RequestManager(mContext);
    }
    
    public void SetHandle(Handler mHandle){
        this.mHandle = mHandle;
        requestManager.SetRequestHandle(mHandle);
    }

    /**
     * 拍照命令 返回照片
     */
    public void snapPicture(HostBean hostBean,Boolean createBitmap,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "snapPicture");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_SNAPPICTURE,hostBean,tag);
        Command.createBitmap = createBitmap;
    }

    /**
     * 单拍照拍照命令 返回照片
     */
    public void snapSinglePicture(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "snapPicture");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_SNAPPICTURE_SINGLE,hostBean,tag);
    }

    /**
     * 获取指定目录多媒体信息
     */
    public void mediaList(HostBean hostBean,int tag,String path) {
        SensorParams params = new SensorParams();
        params.put("command", "mediaList");
        params.put("path",path+"");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_MEDIA_LIST,hostBean,tag);
    }


    /**
     * 获取相机状态
     */
    public void getStatus(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "status");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETSTATUS,hostBean,tag);
    }

    /**
     * (主要用于测试请求)
     */
    public void getWifiTestStatus(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        //params.put("command", "status");解决相机性能下降后获取status慢问题
        params.put("command", "deviceInfo");
        sendConmandWifi(params, true, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETSTATUS,hostBean,tag);
    }
    
    /**
     * (主要用于监控状态请求)
     */
    public void getMonitoringStatus(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "deviceInfo");//解决相机性能下降后获取status慢问题
        //测试代码
        //params.put("command", "mediaList");
        //D:/DCIM/797_VIRB/VIRB0110.jpg
        //..\/DCIM\/797_VIRB\/VIRB0110.jpg
        //D:/DCIM/797_VIRB/VIRB0110
        //params.put("path",RequestApi.getApiMediaUri(tag)+"/media/photo/DCIM/797_VIRB/VIRB0110.jpg");
        //params.put("path","D:/DCIM/797_VIRB/");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_MONITORING,hostBean,tag);
    }

    /**
     * 获取设备信息
     */
    public void getDeviceInfo(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "deviceInfo");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETDEVICESINFO,hostBean,tag);
    }
    
    /**
     * 获取gps是否连接
     */
    public void getGpsStatus(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "status");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETGPSSTATUS,hostBean,tag);
    }
    
    /**
     * 设置缩时模式
     */
    public void setTimeLapse(HostBean hostBean,int tag){
        SensorParams params = new SensorParams();
        params.put("command", "updateFeature");
        params.put("feature", "videoMode");
        params.put("value", "缩时");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_UNKNOW,hostBean,tag);
    }

    /**
     * 停止录像
     */
    public void StopRecording(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "stopRecording");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_STOP_RECORD,hostBean,tag);
    }

    /**
     * 开始录像
     */
    public void StartRecording(HostBean hostBean,int tag) {
        SensorParams params = new SensorParams();
        params.put("command", "startRecording");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_RECORD,hostBean,tag);
    }

    /**
     * 设置连续拍照模式
     */
    public void setContinuousPhoto(HostBean hostBean,int tag){
        SensorParams params = new SensorParams();
        params.put("command", "updateFeature");
        params.put("feature", "photoMode");
        params.put("value", "Timelapse");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_MODE,hostBean,tag);
    }

    /**
     * 设置单拍照模式
     */
    public void setContinuousPhotoSingle(HostBean hostBean,int tag){
        SensorParams params = new SensorParams();
        params.put("command", "updateFeature");
        params.put("feature", "photoMode");
        params.put("value", "Single");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_MODE,hostBean,tag);
    }


    public void setContinuousPhototTimeLapseRate(HostBean hostBean,int tag){
        SensorParams params = new SensorParams();
        params.put("command", "updateFeature");
        params.put("feature", "photoTimeLapseRate");
        params.put("value", "1s");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_TIME_LASPE_RATE,hostBean,tag);
    }

    /**
     * 停止连拍
     */
    public void stopContinuousPhototTimeLapseRate(HostBean hostBean,int tag){
        SensorParams params = new SensorParams();
        params.put("command", "stopStillRecording");
        sendConmand(params, false, com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_TIME_LASPE_RATE_STOP,hostBean,tag);
    }

    public int RegisterSensorEvent(CameraEventListener cameraEventListener) {
        Command.cameraEventListener = cameraEventListener;
        return 0;
    }

    /**
     * 发送命令
     * 
     * @param params
     *            isDebug 参数用来测试，当测试连接时候，需要将超时时间该短，普通拍照请求超时时间设长
     * * @param tag
     * @return
     */
    public void sendConmand(SensorParams params, Boolean isDebug,
            com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType type,HostBean hostBean,int tag) {
        IRequest.post(mContext, RequestApi.getApiUri(tag), params, isDebug,
                cameraEventListener, type,hostBean,tag);
    }

    /**
     * 发送命令
     *
     * @param params
     *            isDebug 参数用来测试，当测试连接时候，需要将超时时间该短，普通拍照请求超时时间设长
     * * @param tag
     * @return
     */
    public void sendConmandWifi(SensorParams params, Boolean isDebug,
                            com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType type,HostBean hostBean,int tag) {
        IRequest.post(mContext, RequestApi.getApiWifiIp(), params, isDebug,
                cameraEventListener, type,hostBean,tag);
    }

    /**
     * 
     * @param hostBean
     * * @param tag
     */
    public void getBitmapByUrl(HostBean hostBean,int tag) {
        String path = PreferencesUtils.getSpText(mContext, "pictureUri");
        IRequest.getbitmap(mContext, hostBean,path, false, cameraEventListener,tag);
    }

}

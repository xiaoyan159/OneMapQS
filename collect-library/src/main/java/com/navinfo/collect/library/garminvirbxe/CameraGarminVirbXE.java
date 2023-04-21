package com.navinfo.collect.library.garminvirbxe;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import com.navinfo.collect.library.sensor.Camera;
import com.navinfo.collect.library.sensor.SensorEventListener;
import com.navinfo.collect.library.utils.SensorUtils;

public class CameraGarminVirbXE extends Camera /*implements LocationCallBack*/ {

    private Command conmand;
    private boolean bSnapPicProgress = false; // 每秒自动拍照标示
    private boolean bMonitoring = false;  //
    private final Context mContext;
    SensorWorkingMode mSensorWorkingMode;
    private CameraEventListener cameraEventListener;
    private WifiDiscovery mDiscoveryTask;
    private static String mSavePath; // 保存照片路径
    private enmConnectionStatus emuConnectionStatus;
    private SimpleDateFormat formatter;
    private long gpstime = 0;
    private long ntptime = 0;
    //private MyLocationManager myLocation;
    public static String mPicUuid;
    private Calendar calendar = Calendar.getInstance();
    private Calendar gpscalendar = Calendar.getInstance();
    private HostBean mHostBean;
    private int mTag;
    private boolean mContinuousPhototTimeLapseRate;
    private boolean mNTPTimeFlag;
    private boolean mGoogleTimeFlag;

    public enum enmCommandType {
        COMMAND_CAMERA_SNAPPICTURE, COMMAND_CAMERA_RECORD, COMMAND_CAMERA_STOP_RECORD,COMMAND_CAMERA_MONITORING,COMMAND_CAMERA_GETSTATUS, COMMAND_CAMERA_GETDEVICESINFO, COMMAND_CAMERA_GETGPSSTATUS,COMMAND_CAMERA_LIVEPREVIEW,
        COMMAND_UNKNOW,COMMAND_PHOTO_MODE,COMMAND_PHOTO_TIME_LASPE_RATE,COMMAND_PHOTO_TIME_LASPE_RATE_STOP,COMMAND_CAMERA_SNAPPICTURE_SINGLE/*单独拍照*/,COMMAND_CAMERA_MEDIA_LIST/*多媒体信息集合*/
    }

    public CameraGarminVirbXE(Context mcontext) {
        super(mcontext);
        this.mContext = mcontext;
        conmand = new Command(mContext);
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
/*        // 初始化google gps 定位
        MyLocationManager.init(mContext, CameraGarminVirbXE.this);
        myLocation = MyLocationManager.getInstance();*/
        mNTPTimeFlag = true;
        new Thread(mGetNtpTimeProgress).start();
    }

    @Override
    public enmSensorType GetSensorType() {
        return enmSensorType.SENSOR_CAMEAR;
    }

    @Override
    public enmConnectionStatus GetConnectionStatus() {
        return emuConnectionStatus;
    }

    public enmConnectionStatus Connect(HostBean hostBean, SensorParams params) {
        try {
            setmHostBean(hostBean);
            conmand.SetHandle(mCameraHandler);
            RequestApi.setApiIp(getmTag(),params.getParams().get("ip").toString());
            emuConnectionStatus = enmConnectionStatus.CONNECTTING;
            cameraEventListener.OnConnectionStatusChanged(hostBean,emuConnectionStatus,mTag);
            conmand.getStatus(hostBean,getmTag());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public enmConnectionStatus DisConnect() {
        emuConnectionStatus = enmConnectionStatus.DISCONNECTTED;
        return null;
    }

    @Override
    public enmSignalQuality GetSignalQuality() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void GetGpsStatus() {
        conmand.getGpsStatus(getmHostBean(),getmTag());
    }

    @Override
    public void snapPicture(String picuuid) {
        CameraGarminVirbXE.mPicUuid = picuuid;
        if (mSensorWorkingMode == SensorWorkingMode.CAMEAR_VEDIO_720P
                || mSensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_1080P
                || mSensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE) {
            if(getGoogleGpsTime() != 0) {
                cameraEventListener.OnSnapPictureResponse(getmHostBean(),null, formatter.format(new Date(getGoogleGpsTime())),getmTag());
            }
            else
                cameraEventListener.OnSnapPictureResponse(getmHostBean(),null,
                        formatter.format(new Date(System.currentTimeMillis())),getmTag());
        }else if(mSensorWorkingMode==SensorWorkingMode.CAMERA_PHOTO_SINGLE){//单拍流程，时间校验使用
            conmand.snapSinglePicture(getmHostBean(),getmTag());
        } else {
            if (bSnapPicProgress) { // 代表一秒拍照线程开启
                Log.e("CamerGarminVirb", "直接取路径");
                conmand.getBitmapByUrl(getmHostBean(),getmTag());
            } else {
                Log.e("CamerGarminVirb", "拍照进行取路径");
                conmand.snapPicture(getmHostBean(),true,getmTag());
            }
        }
    }

    @Override
    public void StartRecording() {
        if (mSensorWorkingMode == SensorWorkingMode.CAMEAR_PHOTO_12MP
                || mSensorWorkingMode == SensorWorkingMode.CAMEAR_PHOTO_7MP) {
            if (!bSnapPicProgress) {
                mSnapPicProgress.run();
                bSnapPicProgress = true;
                if(cameraEventListener != null)
                    cameraEventListener.OnStartRecordResponse(getmHostBean(),getmTag());
            }
        } else if (mSensorWorkingMode == SensorWorkingMode.CAMEAR_VEDIO_720P
                || mSensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_1080P
                || mSensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE) {
            conmand.StartRecording(getmHostBean(),getmTag());
            bMonitoring = true;
            mCameraHandler.postDelayed(mGetStatusProgress, 5000/*调大值域，对应录像命令处理结束后再执行*/);
        } else if(mSensorWorkingMode == SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO){
            // 其他模式下干其他事情
            //conmand.snapPicture(getmHostBean(),false,getmTag());
            //优先设置缩时时间为1s
            if(!mContinuousPhototTimeLapseRate){
                conmand.setContinuousPhototTimeLapseRate(getmHostBean(),getmTag());
                mContinuousPhototTimeLapseRate = true;
            }
            else {
                conmand.snapPicture(getmHostBean(),false,getmTag());
                bMonitoring = true;
                mCameraHandler.postDelayed(mGetStatusProgress, 5000/*调大值域，对应录像命令处理结束后再执行*/);
            }
        }
    }

    @Override
    public void StopRecording() {
        if (mSensorWorkingMode == SensorWorkingMode.CAMEAR_PHOTO_12MP
                || mSensorWorkingMode == SensorWorkingMode.CAMEAR_PHOTO_7MP) {
            mCameraHandler.removeCallbacks(mSnapPicProgress);
            if(cameraEventListener != null)
                cameraEventListener.OnStopRecordResponse(getmHostBean(),getmTag());
            bSnapPicProgress = false;
        } else if (mSensorWorkingMode == SensorWorkingMode.CAMEAR_VEDIO_720P
                || mSensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_1080P
                || mSensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE) {
            conmand.StopRecording(getmHostBean(),getmTag());
            mCameraHandler.removeCallbacks(mGetStatusProgress);
            bMonitoring = false;
        } else if(mSensorWorkingMode == SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO){
            mContinuousPhototTimeLapseRate = false;
            conmand.stopContinuousPhototTimeLapseRate(getmHostBean(),getmTag());
            mCameraHandler.removeCallbacks(mGetStatusProgress);
            bMonitoring = false;
        }
    }


    @Override
    public void Search(boolean isReadCache) {

        //先停止当前搜索
        StopSearch();

        mDiscoveryTask = new WifiDiscovery(mContext, cameraEventListener,isReadCache);
        mDiscoveryTask.setTag(getmTag());
        mDiscoveryTask.execute();

    }

    @Override
    public void StopSearch() {
        if(mDiscoveryTask!=null){

            if(!mDiscoveryTask.isCancelled()){

                boolean result = mDiscoveryTask.cancel(true);

                Log.e("StopSearch",result+"===");
            }

            mDiscoveryTask = null;
        }
    }

    @Override
    public int RegisterSensorEvent(SensorEventListener sensorEventLister) {
        cameraEventListener = (CameraEventListener) sensorEventLister;
        return conmand.RegisterSensorEvent(cameraEventListener);
    }

    @Override
    public void SetMode(SensorWorkingMode sensorWorkingMode) {
        mSensorWorkingMode = sensorWorkingMode;
        if (sensorWorkingMode == SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE){

            /*外业需求变更，不在强制设置缩时模式，有作业员自己设置相机*///conmand.setTimeLapse(getmHostBean(),getmTag());
        }
        else if(sensorWorkingMode==SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO){
            conmand.setContinuousPhoto(getmHostBean(),getmTag());
        }else if(sensorWorkingMode==SensorWorkingMode.CAMERA_PHOTO_SINGLE){
            conmand.setContinuousPhotoSingle(getmHostBean(),getmTag());
        }
    }

    //获取指定目录多媒体信息
    public void mediaList(String path){
        conmand.mediaList(getmHostBean(),getmTag(),path);
    }


    //获取相机模式
    public SensorWorkingMode GetMode() {
        return mSensorWorkingMode;
    }

    // 每隔30s进行一次获取连接
    private Runnable mGetStatusProgress = new Runnable() {

        @Override
        public void run() {
            /*if(1==1)
                return;*/
            conmand.getMonitoringStatus(getmHostBean(),getmTag());
            if(bMonitoring)
                mCameraHandler.postDelayed(mGetStatusProgress, 1000*30l/*降低频率，解决机器性能下降导致命令无效*/);
        }
    };

    // 每隔1000s进行一次拍照
    private Runnable mSnapPicProgress = new Runnable() {

        @Override
        public void run() {
            conmand.snapPicture(getmHostBean(),false,getmTag());
        }
    };

    //停止时间线程
    public void stopTimeThread(){
        mGoogleTimeFlag = false;
        mNTPTimeFlag = false;
        ntptime = 0;
        gpstime = 0;
    }

    private Runnable mGetNtpTimeProgress = new Runnable() {

        @Override
        public void run() {
            while (mNTPTimeFlag) {
                if (SensorUtils.isNetworkAvailable(mContext)) {
                    Date dateNTP = SensorUtils.syncNow();
                    if (dateNTP != null) {
                        ntptime = dateNTP.getTime();
                        calendar.setTime(dateNTP);
                        //Log.e("AAA", "ntp time :" + DateUtils.formatTime(dateNTP));
                        mNTPTimeFlag = false;
                        mGoogleTimeFlag = true;
                        //获取NTP时间后开始启动计时线程
                        new Thread(mNtpClockTimeProgress).start();
                        new Thread(mGpsClockTimeProgress).start();
                    }
                }
                //30秒执行一次，降低频率，节省电量
                try{
                    Thread.sleep(30000);
                }catch (Exception e){

                }

            }
        }
    };

    /**
     * 计时器
     */
    private Runnable mNtpClockTimeProgress = new  Runnable() {

        @Override
        public void run() {
            while (mGoogleTimeFlag) {
                if(ntptime != 0) {
                    int second = calendar.get(Calendar.SECOND);
                    second += 1;
                    calendar.set(Calendar.SECOND, second);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    try{
                        Thread.sleep(1);
                    }catch (Exception e){

                    }
                }
            }
        }
    };

    /**
     * 计时器
     */
    private Runnable mGpsClockTimeProgress = new  Runnable() {

        @Override
        public void run() {
            while (mGoogleTimeFlag) {
                if(gpstime != 0) {
                    int second = gpscalendar.get(Calendar.SECOND);
                    second += 1;
                    gpscalendar.set(Calendar.SECOND, second);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    try{
                        Thread.sleep(1);
                    }catch (Exception e){

                    }
                }
            }
        }
    };

    private Handler mCameraHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SensorUtils.HADNLE_CONNECT_OK:
                    emuConnectionStatus = enmConnectionStatus.CONNECTTED;
                    cameraEventListener
                            .OnConnectionStatusChanged(getmHostBean(),emuConnectionStatus,getmTag());
                    break;
                case SensorUtils.HADNLE_CONNECT_FAIL:
                    Log.e("AAA", "断开连接~~~~~~~~~~");
                    emuConnectionStatus = enmConnectionStatus.DISCONNECTTED;
                    cameraEventListener
                            .OnConnectionStatusChanged(getmHostBean(),emuConnectionStatus,getmTag());
                    break;
                case SensorUtils.HADNLE_SNAPPICTURE:// 定时拍照返回
                    if (bSnapPicProgress)
                        mCameraHandler.post(mSnapPicProgress);
                case SensorUtils.HADNLE_MONITORING:
                    Log.e("AAA", "状态监测中....");

                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 设置照片保存路径
     *
     * @param savePath
     */
    public void SetCameraPictureSavaPath(String savePath) {
        mSavePath = savePath;
    }

    //获取google定位时间
    public Long getGoogleGpsTime() {
        long time = 0;
        if (ntptime != 0) {
            time = calendar.getTime().getTime();
            return time;
        } else {
            if(gpstime != 0 )
                time = gpscalendar.getTime().getTime();
            return time;
        }
    }

    /**
     * 获取照片保存路径
     *
     * @return
     */
    public static String getCameraPcitureSavePath() {
        if ("".equals(mSavePath) || null == mSavePath)
            return SensorUtils.STR_CAMERA_PICTURE_SAVEPATH;
        else
            return mSavePath;
    }

/*    @Override
    public void onCurrentLocation(Location location) {
        if(location != null) {
            Log.e("AAA", "onCurrentLocation");
            gpstime = location.getTime();
            gpscalendar.setTime(new Date(gpstime));
        }
        else
            gpstime = 0;

        if (myLocation != null)
            myLocation.destoryLocationManager();
    }*/

    public HostBean getmHostBean() {
        return mHostBean;
    }

    public void setmHostBean(HostBean mHostBean) {
        this.mHostBean = mHostBean;
    }

    public int getmTag() {
        return mTag;
    }

    public void setmTag(int mTag) {
        this.mTag = mTag;
    }
}

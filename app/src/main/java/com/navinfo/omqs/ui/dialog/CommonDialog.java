package com.navinfo.omqs.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE;
import com.navinfo.collect.library.garminvirbxe.CameraEventListener;
import com.navinfo.collect.library.sensor.ISensor.enmSignalQuality;
import com.navinfo.collect.library.sensor.ISensor.enmConnectionStatus;
import com.navinfo.collect.library.sensor.ISensor.SensorWorkingMode;
import com.navinfo.collect.library.garminvirbxe.SensorParams;
import com.navinfo.collect.library.garminvirbxe.HostBean;
import com.navinfo.omqs.Constant;
import com.navinfo.omqs.R;
import com.navinfo.omqs.ui.activity.map.MainActivity;
import com.navinfo.omqs.ui.manager.TakePhotoManager;
import com.navinfo.omqs.ui.other.BaseToast;
import com.navinfo.omqs.util.DateTimeUtil;
import com.navinfo.omqs.util.FileUtils;
import com.navinfo.omqs.util.NetUtils;
import com.navinfo.omqs.util.ShareUtil;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.vlc.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 * 公共相机对话框
 *
 * @author chentao
 */
public class CommonDialog extends Dialog implements SurfaceHolder.Callback, IVideoPlayer {
    /**
     * 上下文环境
     */
    private Context context;
    /**
     * 拍照控制类
     */
    private TakePhotoManager takephotoManager;
    /**
     * 相机事件回调
     */
    private CameraEventListener listener;


    /**
     * 对话框取消按钮
     */
    private ImageView mCancelDialog;
    /**
     * 开始/结束拍照按钮
     */
    private CheckBox mStartOrEndTakePicture;
    /**
     * 一键连接按钮
     */
    private TextView mOneBtConnect;
    /**
     * GPS连接提示
     */
    private TextView mGpsTv;

    /**
     * 选择使用默认相机还是外接相机
     */
    private CheckBox mSelectCamera;
    /**
     * 选择录像还是拍照
     */
    private CheckBox mTakePhotoOrRecord;
    /**
     * 选择相机保持的状态
     */
    private boolean bl;
    /**
     * 连续拍照的状态
     */
    private boolean conbl;

    /**
     * 录像还是拍照bl
     */
    private boolean recordOrTaKebl;

    //相机模式
    private int mMode;

    //true连接成功，false未连接成功
    public boolean connectstate = false;

    //true 可点击，false不可点击
    private boolean click_state = true;

    private boolean isShowVideo;
    private SurfaceView mSurfaceView;//展示视频的surfaceView
    private LinearLayout mSurfaceBgLayout;
    private ImageView mImgView;
    private LibVLC mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
    private View mLoadingView;
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;
    //存储状态信息
    private ShareUtil mShareUtil;

    private LoadingDialog videoLoadingProDlg = null;

    private int mX, mY;
    //相机对象
    private HostBean mHostBean;

    //设备号
    private int mDeviceNum;

    //是否点击取消
    private boolean isCancel;

    //开始校验时间
    private long mStartCheckTime = 0;

    //单拍模式照片名称
    private String mSinglePhotoName;

    //是否校验了时间
    private boolean isCheckTime;

    private static int COMMAND_CAMERA_SNAPPICTURE_SINGLE_COUNT = 0;

    private static int COMMAND_CAMERA_MEDIAPLAY_ERROR_COUNT = 0;

    private long mPreTime;
    boolean isStartThread;
    boolean isStartThread2;

    public CommonDialog(Context context, int x, int y, int deviceNum) {
        super(context, R.style.MyCustomDialog);
        // TODO Auto-generated constructor stub
        mX = x;
        mY = y;
        setmDeviceNum(deviceNum);
        mShareUtil = new ShareUtil(context, deviceNum);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.connect_out_camera);

        mSurfaceView = (SurfaceView) findViewById(R.id.video);

        mImgView = (ImageView) findViewById(R.id.video_defalut);
        try {
            mMediaPlayer = Util.getInstance().getLibVlcInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
        mSurfaceHolder.addCallback(this);

        EventHandler em = EventHandler.getInstance();
        em.addHandler(mVlcHandler);

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mSurfaceView.setKeepScreenOn(true);

        WindowManager.LayoutParams params = this.getWindow().getAttributes();
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        params.width = (int) (display.getWidth() * 0.3);
        params.height = (int) (display.getHeight() * 0.8);
        params.x = mX;
        params.y = mY;
        this.getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
        this.getWindow().setAttributes(params);

        takephotoManager = TakePhotoManager.getInstance();

        listener = new CameraEventListener() {

            @Override
            public void OnSignalQualityChanged(enmSignalQuality sq) {
                // TODO Auto-generated method stub

            }

            @Override
            public void OnSensorEvent() {
                // TODO Auto-generated method stub

            }

            @Override
            public void OnConnectionStatusChanged(enmConnectionStatus cs) {

            }

            @Override
            public void OnConnectionStatusChanged(HostBean hostBean, enmConnectionStatus cs, int tag) {

                if (tag != getmDeviceNum())
                    return;

                ImageView iv = (ImageView) ((MainActivity) context).findViewById(getResId());

                iv.setBackgroundDrawable(null);

                if (cs == enmConnectionStatus.CONNECTTED) {

                    Log.e("AAA", "连接成功");

                    updateCameraResources(0, getmDeviceNum());

                    //当前是否开始录像或拍照
                    if (mStartOrEndTakePicture != null) {

                        conbl = mShareUtil.getContinusTakePhotoState();

                        if (mStartOrEndTakePicture.isChecked() != conbl){
                            mStartOrEndTakePicture.setChecked(conbl);
                        }

                    }

                    if (conbl && isShowing()) {
                        startGPSStatus();
                    }else{
                        //恢复按钮可用状态
                        if(mStartOrEndTakePicture!=null)
                            mStartOrEndTakePicture.setEnabled(true);
                    }


                } else if (cs == enmConnectionStatus.DISCONNECTTED) {

                    connectionFailed("相机丢失连接，请检查网络是否畅通！");
/*                    if (ShowMode.getInstance().getImageWorkMode().isStartWork()) {
                        BaseToast.makeText(getContext(), "相机拍摄中断，图像作业已结束！", BaseToast.LENGTH_SHORT).show();
                        ShowMode.getInstance().getImageWorkMode().endWork();
                    }*/
                } else if (cs == enmConnectionStatus.CONNECTTING) {

                    Log.e("AAA", "连接中");
                    connectstate = false;
                    mShareUtil.setConnectstate(Constant.USER_ID, connectstate);
                    mOneBtConnect.setText("连接中");
                    updateCameraResources(2, getmDeviceNum());

                } else {
                    connectstate = false;
                    mShareUtil.setConnectstate(Constant.USER_ID, connectstate);
                    click_state = true;

                    mOneBtConnect.setEnabled(click_state);
                    mOneBtConnect.setPressed(false);
                    mOneBtConnect.setText("一键连接");
                    mStartOrEndTakePicture.setEnabled(false);
                    mOneBtConnect.setBackgroundResource(R.drawable.shape_btn_connect_bg_enabled);
                    updateCameraResources(1, getmDeviceNum());

                    Log.e("AAA", "未知返回");

                    stopVideo();
                }

            }

            @Override
            public void OnContinuousPhototTimeLapseRateResponse(HostBean hostBean, int tag) {
                if (tag != getmDeviceNum())
                    return;
                //再次启动命令
                takephotoManager.StartRecording(mHostBean, getmDeviceNum());
            }

            @Override
            public void OnContinuousPhototTimeLapseRateStartResponse(HostBean hostBean, int tag) {
                if (tag != getmDeviceNum())
                    return;

                Message msg = new Message();
                msg.what = 0x55;
                mHandler.sendMessage(msg);
            }

            @Override
            public void OnContinuousPhototTimeLapseRateStopResponse(HostBean hostBean, int tag) {
                if (tag != getmDeviceNum())
                    return;

                //启动监控
                if (connectstate && isShowing()) {
                    playVideo();
                    updateCameraResources(0, getmDeviceNum());
                }
            }

            @Override  //单拍模式
            public void OnContinuousPhototSingle(HostBean hostBean, final String url, final String name, int tag) {

                if (tag != getmDeviceNum())
                    return;

                Log.i("OnGetGpsStatusResponse", "OnContinuousPhototSingle");


                if (!TextUtils.isEmpty(url)) {

                    Message msg = new Message();
                    msg.what = HANDLER_BUFFER_START;
                    mHandler.sendMessage(msg);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {


                            Message msg = new Message();

                            String picPath = Constant.DATA_PATH + "/" + Constant.USER_ID + "/pic.jpg";

                            //创建目录
                            if(!new File(Constant.DATA_PATH + "/" + Constant.USER_ID).exists()){
                                new File(Constant.DATA_PATH + "/" + Constant.USER_ID).mkdirs();
                            }

                            //多次获取照片信息，解决概率事件无法获取有效照片问题
                            for (int i = 0; i < 5; i++) {
                                if (new File(picPath).exists()) {
                                    new File(picPath).delete();
                                }

                                FileUtils.saveImageToDisk(url, picPath);

                                String time = FileUtils.getExifTime(picPath);

                                Log.e("AAA", "获取单张拍照时间" + time+"次数"+i);
                                if (!TextUtils.isEmpty(time)) {
                                    long picTime = DateTimeUtil.getPicTime(time);

                                    long disTime = mStartCheckTime - picTime / 1000;

                                    Log.i("qj", "OnGetMediaList" + disTime);

                                    if (disTime < 30 && disTime > -30) {

                                        mMode = mShareUtil.getTakeCameraMode();

                                        //恢复模式
                                        if (mMode == 0) {
                                            if (takephotoManager.getCameraMode(getmDeviceNum()) != SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE)
                                                takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE);
                                        } else {
                                            if (takephotoManager.getCameraMode(getmDeviceNum()) != SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO)
                                                takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO);
                                        }

                                        try {
                                            //延时1秒发送预览命令
                                            Thread.sleep(1000);
                                        } catch (Exception e) {

                                        }
                                        msg.what = 0x66;
                                        mHandler.sendMessage(msg);
                                        return;

                                    }
                                }
                            }
                            msg.what = 0x77;
                            mHandler.sendMessage(msg);
                        }
                    }).start();

                } else {
                    BaseToast.makeText(context, "校验时间信息失败，请注意核对时间信息!", Toast.LENGTH_SHORT).show();
                    mGpsTv.setVisibility(View.VISIBLE);
                    mGpsTv.setText("请在设置中确保PAD/时区与相机/时区保持一致。");
                }
            }

            @Override
            public void OnGetMediaList(HostBean hostBean, String json, int tag) {
                if (tag != getmDeviceNum())
                    return;

            }

            @Override
            public void requestError(HostBean hostBean, VolleyError e, CameraGarminVirbXE.enmCommandType commandType, int tag) {

                if (tag != getmDeviceNum())
                    return;

                //如果是单拍命令异常时重新再次进行获取
                if(commandType!=null&&commandType== CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_SNAPPICTURE_SINGLE&&!isCheckTime&&mOneBtConnect!=null&&mOneBtConnect.getText().toString().contains("断开连接")){

                    COMMAND_CAMERA_SNAPPICTURE_SINGLE_COUNT ++;

                    //连续尝试多次进行获取，如果多次失败则提示
                    if(COMMAND_CAMERA_SNAPPICTURE_SINGLE_COUNT == 3){
                        Message msg = new Message();
                        msg.what = 0x77;
                        mHandler.sendMessage(msg);
                        COMMAND_CAMERA_SNAPPICTURE_SINGLE_COUNT = 0;
                    }else{
                        BaseToast.makeText(context,"重新获取时间信息。",Toast.LENGTH_SHORT).show();
                        showLoading();
                        //重新获取拍照时间
                        takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMERA_PHOTO_SINGLE);
                        //启动单拍模式开始校验时间
                        takephotoManager.SnapShot(getmHostBean(),getmDeviceNum());
                        //只要秒时间
                        mStartCheckTime = new Date().getTime() / 1000;
                    }
                }

                // BaseToast.makeText(context,"命令异常，请确认网络是否正常。",Toast.LENGTH_SHORT).show();

            }

            @Override
            public void OnStopRecordResponse(HostBean hostBean, int tag) {
                if (tag != getmDeviceNum())
                    return;

                //启动监控
                if (connectstate && isShowing()) {
                    playVideo();
                    updateCameraResources(0, getmDeviceNum());
                }


            }

            @Override
            public void OnStatusResponse(HostBean hostBean, int tag) {

                if (tag != getmDeviceNum())
                    return;

                updateCameraResources(0, getmDeviceNum());
                connectstate = true;
                mShareUtil.setConnectstate(Constant.USER_ID, connectstate);
                stopVideo();
                mOneBtConnect.setPressed(true);
                mOneBtConnect.setBackgroundResource(R.drawable.shape_btn_red_disconnect_bg);
                mOneBtConnect.setText("断开连接！");
                //停止获取GPS状态
                if (getmDeviceNum() == 1)
                    isStartThread = false;
                else
                    isStartThread2 = false;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.i("chentao", "连接成功");
                Log.e("AAA", "OnStatusResponse ok ");

            }

            @Override
            public void OnStartRecordResponse(HostBean hostBean, int tag) {

                if (tag != getmDeviceNum())
                    return;

                Message msg = new Message();
                msg.what = 0x55;
                mHandler.sendMessage(msg);

            }

            @Override
            public void OnSnapPictureResponse(HostBean hostBean, Bitmap bitmap, String picName, int tag) {

                if (tag != getmDeviceNum())
                    return;

                if (hostBean != null && mHostBean != null && hostBean.hardwareAddress.equals(mHostBean.hardwareAddress)) {

                }
            }

            @Override
            public void OnSearchResponse(int tag, ArrayList<HostBean> listIp) {

                //判断标识
                if (tag != getmDeviceNum())
                    return;

                Log.e("AAA", "搜索到完成");

                if (listIp.size() > 0) {
                    //已连接相机地址
                    String connectstateMac = ShareUtil.getConnectstateMac(context);
                    //存在连接设备
                    if (!TextUtils.isEmpty(connectstateMac)) {
                        boolean isConn = false;
                        b:
                        for (HostBean hostBean : listIp) {
                            if (!hostBean.hardwareAddress.equalsIgnoreCase(connectstateMac)) {
                                isConn = true;
                                connection(hostBean);
                                break b;
                            }
                        }
                        if (!isConn) {
                            connectionFailed("无可用设备");
                        }
                    } else {
                        connection(listIp.get(0));
                    }
                } else {
                    connectionFailed("相机丢失连接，请检查网络是否畅通！");
                }
            }

            @Override
            public void OnConnectStatusChanged(HostBean hostBean, enmConnectionStatus connectStatus, int tag) {
                if (hostBean != null && mHostBean != null && hostBean.hardwareAddress.equals(mHostBean.hardwareAddress)) {

                }
            }

            @Override
            public void OnGetDeviceInfo(HostBean hostBean, String devicesId, int tag) {
                // TODO Auto-generated method stub
                if (hostBean != null && mHostBean != null && hostBean.hardwareAddress.equals(mHostBean.hardwareAddress)) {
                    Log.e("AAA", "devicesId" + devicesId);
                }
            }

            @Override
            public void OnGetGpsStatusResponse(HostBean hostBean, boolean status, int tag) {

                if (tag != getmDeviceNum())
                    return;

                try {

                    if (mShareUtil.getContinusTakePhotoState() && !mStartOrEndTakePicture.isEnabled()) {

                        if (status) {

                            BaseToast.makeText(context, "成功获取相机GPS信号!正在校验时间信息。。。", Toast.LENGTH_SHORT).show();

                            //获取GPS信号停止获取线程
                            if (getmDeviceNum() == 1) {
                                isStartThread = false;
                            } else {
                                isStartThread2 = false;
                            }

                            //相机GPS信号差，请稍等片刻，或将相机移到开发地带。
                            if (!isCheckTime) {
                                mGpsTv.setText("正在校验时区正确性，请耐心等待！");
                                showLoading();
                                mGpsTv.setVisibility(View.VISIBLE);
                                //初始化
                                COMMAND_CAMERA_SNAPPICTURE_SINGLE_COUNT = 0;
                                takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMERA_PHOTO_SINGLE);
                                //启动单拍模式开始校验时间
                                takephotoManager.SnapShot(getmHostBean(),getmDeviceNum());
                                //只要秒时间
                                mStartCheckTime = new Date().getTime() / 1000;
                            }
                            Log.i("qj", "OnGetGpsStatusResponse启动时间校验");
                        } else {
                            mGpsTv.setText("相机GPS信号差，请稍等片刻，或将相机移到开发地带。");
                            mGpsTv.setVisibility(View.VISIBLE);
                            mStartOrEndTakePicture.setEnabled(status);
                            mShareUtil.setContinusTakePhotoState(Constant.USER_ID, true);
                        }

                    }

                } catch (Exception e) {
                }
            }
        };

        takephotoManager.setOnCameraEventChangeListener(listener);

        bl = mShareUtil.getSelectCameraKind();

        recordOrTaKebl = mShareUtil.getSelectTakePhotoOrRecord();

        conbl = mShareUtil.getContinusTakePhotoState();

        //初始化
        init();

        //添加监听器
        addListeners();
    }

    //初始化
    private void init() {

        //拍照或录像
        mTakePhotoOrRecord = (CheckBox) findViewById(R.id.takephoto_or_record);
        mCancelDialog = (ImageView) findViewById(R.id.cancel_dialog);
        mStartOrEndTakePicture = (CheckBox) findViewById(R.id.startorendtakepicture);
        mOneBtConnect = (TextView) findViewById(R.id.one_bt_connect);
        mGpsTv = (TextView) findViewById(R.id.gps_status_hint);
        //是否为外设相机拍照
        mSelectCamera = (CheckBox) findViewById(R.id.select_camera);

        mMode = mShareUtil.getTakeCameraMode();

        if (mMode == 0) {
            recordOrTaKebl = true;
            mTakePhotoOrRecord.setText("录像");
            setmImgViewImageDrawable(0);
            if (takephotoManager.getCameraMode(getmDeviceNum()) != SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE)
                takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE);
        } else {
            recordOrTaKebl = false;
            if (takephotoManager.getCameraMode(getmDeviceNum()) != SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO)
                takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO);
            mTakePhotoOrRecord.setText("拍照");
            setmImgViewImageDrawable(1);
        }

        mSelectCamera.setChecked(bl);

        mTakePhotoOrRecord.setChecked(recordOrTaKebl);

        mOneBtConnect.setEnabled(click_state);

        connectstate = mShareUtil.getConnectstate();

        //当前为连接时启动已有的状态
        if (connectstate) {

            mOneBtConnect.setPressed(true);

            mOneBtConnect.setBackgroundResource(R.drawable.shape_btn_red_disconnect_bg);

            mOneBtConnect.setText("断开连接！");

            //启动或关闭拍照模式
            mStartOrEndTakePicture.setChecked(conbl);

            if (conbl) {

                mStartOrEndTakePicture.setEnabled(false);
                mHostBean = new HostBean();
                mHostBean.hardwareAddress = mShareUtil.getTakeCameraMac();
                mHostBean.ipAddress = mShareUtil.getTakeCameraIP();
                takephotoManager.getCameraVedioClent(getmDeviceNum()).setmHostBean(mHostBean);

                playVideo();

                startGPSStatus();

            } else {
                mStartOrEndTakePicture.setEnabled(true);
            }

        }

    }

    /**
     * 更新相机状态资源
     */
    public void updateCameraResources(int statusType, int indexClentCamera) {
        int resId = R.id.main_activity_camera;

        if (indexClentCamera == 2)
            resId = R.id.main_activity_camera2;

        Drawable drawable = context.getResources().getDrawable(R.drawable.icon_page_video_a0);

        ShareUtil shareUtil = new ShareUtil(context, indexClentCamera);
        //0为录像模式
        if (shareUtil.getTakeCameraMode() == 0) {
            switch (statusType) {
                case 0:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_video_a0);
                    break;
                case 1:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_video_a1);
                    break;
                case 2:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_video_a2);
                    break;
                case 3:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_video_a3);
                    break;
            }
        } else {
            switch (statusType) {
                case 0:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_take_photo_a0);
                    break;
                case 1:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_take_photo_a1);
                    break;
                case 2:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_take_photo_a2);
                    break;
                case 3:
                    drawable = context.getResources().getDrawable(R.drawable.icon_page_take_photo_a3);
                    break;
            }
        }


        ImageView ivStatus = (ImageView) findViewById(resId);
        if (ivStatus != null) {
            try {
                AnimationDrawable animation = (AnimationDrawable) ivStatus.getBackground();
                if (animation.isRunning()) {
                    animation.stop();
                    ivStatus.setBackground(null);
                }

            } catch (Exception e) {

            }
            ivStatus.setImageDrawable(drawable);
        }

    }

    public void updateCameraBackgroundResources(int drawable, int indexClentCamera) {
        int resId = R.id.main_activity_camera;

        if (indexClentCamera == 2)
            resId = R.id.main_activity_camera2;

        ImageView ivStatus = (ImageView) findViewById(resId);

        if (ivStatus != null) {
            String time = DateTimeUtil.getDateSimpleTime(DateTimeUtil.getTimeInfo(DateTimeUtil.getTime()) - 0/*MainActivity.disGoogleTime*/);

            ivStatus.setImageDrawable(null);

            ivStatus.setBackgroundResource(drawable);

            AnimationDrawable animation = (AnimationDrawable) ivStatus.getBackground();

            animation.start();
        }
    }

    //添加监听器
    private void addListeners() {

        //取消对话框按钮
        mCancelDialog.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                dismiss();

            }
        });
        //打开/结束连拍
        mStartOrEndTakePicture.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                if (connectstate) {

                    Log.i("chentao", "打开/结束连拍:" + arg1);
                    mShareUtil.setContinusTakePhotoState(Constant.USER_ID, arg1);

                    if (!arg1) {

                        stopVideoOrPicture();

                    } else {

                        takephotoManager.StopContinuousTakePhoto(mHostBean, getmDeviceNum());

                        //重新運行
                        startGPSStatus();

                    }

                } else {

                    mStartOrEndTakePicture.setChecked(true);

                    mShareUtil.setContinusTakePhotoState(Constant.USER_ID, true);

                    BaseToast.makeText(context, "外接相机没有连接成功！不能进行连续拍照！", Toast.LENGTH_SHORT).show();
                }

            }
        });
        //一键连接按钮
        mOneBtConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (connectstate) {

                    connectstate = false;

                    mShareUtil.setContinusTakePhotoState(Constant.USER_ID, true);

                    mShareUtil.setConnectstate(Constant.USER_ID, connectstate);

                    mStartOrEndTakePicture.setChecked(true);

                    //修改为禁用状态
                    mStartOrEndTakePicture.setEnabled(false);

                    //停止当前活动
                    takephotoManager.StopContinuousTakePhoto(mHostBean, getmDeviceNum());

                    mOneBtConnect.setBackgroundResource(R.drawable.shape_btn_connect_bg_enabled);

                    mOneBtConnect.setText("一键连接！");

                    mOneBtConnect.setPressed(false);

                    mGpsTv.setVisibility(View.GONE);

                    mGpsTv.setText("相机GPS信号差，请稍等片刻，或将相机移到开发地带。");

                    updateCameraResources(1, getmDeviceNum());

                    stopVideo();

                } else {
                    if (mOneBtConnect.getText().toString().equals("连接中")) {
                        mOneBtConnect.setText("一键连接！");
                        mStartOrEndTakePicture.setEnabled(false);
                        connectstate = false;
                        takephotoManager.StopSearchNet(mHostBean,getmDeviceNum());
                        mGpsTv.setVisibility(View.GONE);
                        mGpsTv.setText("相机GPS信号差，请稍等片刻，或将相机移到开发地带。");
                        updateCameraResources(1, getmDeviceNum());
                    } else {

                        if (NetUtils.getInstance().isExistWifi(false)) {
                            //搜索相机
                            takephotoManager.SearchNet(null, getmDeviceNum(), false);

                            mOneBtConnect.setText("连接中");

                            updateCameraResources(2, getmDeviceNum());

                        } else {
                            connectionFailed("相机丢失连接，请检查网络是否畅通！");
                        }

                    }

                }

            }
        });
        //选择相机
        mSelectCamera.setOnCheckedChangeListener(new OnCheckedChangeListener() {


            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                if (connectstate) {
                    Log.i("chentao", "选择相机:" + arg1);
                    mShareUtil.setSelectCameraKind(Constant.USER_ID, arg1);
                } else {
                    mSelectCamera.setChecked(false);
                    mShareUtil.setSelectCameraKind(Constant.USER_ID, false);
                    BaseToast.makeText(context, "外接相机没有连接成功！只能使用系统相机，谢谢！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        /**
         * 选择拍照或者录像
         */
        mTakePhotoOrRecord.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, final boolean isCheck) {

                //if (mStartOrEndTakePicture.isChecked()) {
                //点击取消重置按钮时不进行窗体提示
                if (isCancel) {
                    isCancel = false;
                    return;
                }

                FirstDialog builder = new FirstDialog(context);
                builder.setTitle("影响状态切换提示");

                String status = null;

                if (isCheck) {
                    status = "录像";
                    setmImgViewImageDrawable(0);
                } else {
                    status = "连拍";
                    setmImgViewImageDrawable(1);
                }

                mTakePhotoOrRecord.setText(status);

                builder.setMessage("切换影像状态为" + status);

                builder.setPositiveButton("确认", new FirstDialog.OnClickListener() {

                    @Override
                    public void onClick(Dialog dialog, int arg1) {
                        dialog.dismiss();

                        //连接状态时先停止录像或者拍照状态
                        if (connectstate) {
                            //如果当前为工作状态，先停止
                            if (!mShareUtil.getContinusTakePhotoState()) {
                                stopVideoOrPicture();
                            }

                            //重置拍照按钮
                            mShareUtil.setContinusTakePhotoState(Constant.USER_ID, true);
                        }

                        //设置另外一台相机状态
                        ShareUtil shareUtilOther = new ShareUtil(context, getmDeviceNum() == 1 ? 2 : 1);

                        //如果另外一个台处于连接并且开始录像或者拍照时需要停止运行
                        if (shareUtilOther.getConnectstate() && !shareUtilOther.getContinusTakePhotoState()) {
                            try {
                                CameraGarminVirbXE cameraGarminVirbXE = takephotoManager.getCameraVedioClent(getmDeviceNum());
                                cameraGarminVirbXE.StopRecording();

                            } catch (Exception e) {

                            }

                        }

                        shareUtilOther.setSelectTakePhotoOrRecord(Constant.USER_ID, !isCheck);

                        shareUtilOther.setContinusTakePhotoState(Constant.USER_ID, true);

                        mShareUtil.setSelectTakePhotoOrRecord(Constant.USER_ID, isCheck);

                        if (isCheck) {

                            mTakePhotoOrRecord.setText("录像");
                            setmImgViewImageDrawable(0);

                            takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMERA_VEDIO_TIMELAPSE);

                            mShareUtil.setTakeCameraMode(Constant.USER_ID, 0);

                            shareUtilOther.setTakeCameraMode(Constant.USER_ID, 1);

                        } else {

                            mTakePhotoOrRecord.setText("拍照");
                            setmImgViewImageDrawable(1);

                            mShareUtil.setTakeCameraMode(Constant.USER_ID, 1);

                            shareUtilOther.setTakeCameraMode(Constant.USER_ID, 0);

                            takephotoManager.setCameraMode(getmDeviceNum(), SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO);

                        }
                        updateCameraStatusImg();
                    }

                });
                builder.setNegativeButton("取消", new FirstDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog, int which) {
                        dialog.dismiss();
                        isCancel = true;
                        if (!isCheck) {
                            mTakePhotoOrRecord.setText("录像");
                            setmImgViewImageDrawable(0);
                        } else {
                            mTakePhotoOrRecord.setText("连拍");
                            setmImgViewImageDrawable(1);
                        }
                        updateCameraStatusImg();
                        mTakePhotoOrRecord.setChecked(!isCheck);
                    }
                });

                builder.show();


                //}
                /*else {

                    mTakePhotoOrRecord.setChecked(!isCheck);

                    BaseToast.makeText(context, "请先停止当前活动！", Toast.LENGTH_SHORT).show();
                }*/

            }

        });
    }

    //更新资源图标
    private void updateCameraStatusImg() {
        if (mShareUtil.getConnectstate()) {
            updateCameraResources(0, getmDeviceNum());
        } else {
            updateCameraResources(1, getmDeviceNum());
        }
        ShareUtil shareUtilOther = new ShareUtil(context, getmDeviceNum() == 1 ? 2 : 1);
        if (shareUtilOther.getConnectstate()) {
            updateCameraResources(0, getmDeviceNum() == 1 ? 2 : 1);
        } else {
            updateCameraResources(1, getmDeviceNum() == 1 ? 2 : 1);
        }
    }

    /**
     * @param :
     * @return :
     * @method : connectionFailed
     * @Author : xiaoxiao
     * @Describe : 外接相机连接失败的处理
     * @Date : 2018/5/14
     */
    private void connectionFailed(String msg) {
        Log.e("AAA", "连接失败");
        BaseToast.makeText(context, TextUtils.isEmpty(msg) ? "相机丢失连接，请检查网络是否畅通！" : msg, Toast.LENGTH_SHORT).show();

        connectstate = false;
        mShareUtil.setConnectstate(Constant.USER_ID, connectstate);
        mOneBtConnect.setPressed(false);
        mOneBtConnect.setBackgroundResource(R.drawable.shape_btn_connect_bg_enabled);
        mOneBtConnect.setText("一键连接");
        //增加按钮状态控制
        mStartOrEndTakePicture.setEnabled(false);
        updateCameraResources(1, getmDeviceNum());

        hideLoading();

        //齐济，连接中断了需要停止当前的拍照流程
        mStartOrEndTakePicture.setChecked(false);
    }

    /**
     * @param :
     * @return :
     * @method : isShowVideo
     * @Author : xiaoxiao
     * @Describe : 获取当前是否显示视频
     * @Date : 2018/5/14
     */
    public boolean isShowVideo() {
        return isShowVideo;
    }

    /**
     * @param :
     * @return :
     * @method : playVideo
     * @Author : xiaoxiao
     * @Describe : 播放视频
     * @Date : 2018/5/14
     */
    public void playVideo() {
        try {

            if (isShowVideo)
                return;

            if (NetUtils.getInstance().isExistWifi(false)) {
                showLoading();

                final String ip = mShareUtil.getTakeCameraIP();

                if (TextUtils.isEmpty(ip))
                    return;
                final String httpIp = "rtsp://" + ip + "/livePreviewStream";
                if (mMediaPlayer != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(mMediaPlayer.isPlaying())
                                mMediaPlayer.stop();
                            int result = -1;
                            try {
                                result = mMediaPlayer.readMedia(httpIp);
                            } catch (Exception e) {
                                e.printStackTrace();
                                result = -1;
                            }
                            Message msg = new Message();
                            msg.what = 0x11;
                            msg.arg1 = result;
                            Log.e(CommonDialog.class.getName(), "result==" + result);
                            mHandler.sendMessage(msg);
                        }
                    }).start();
                }
                isShowVideo = true;
                mSurfaceView.setVisibility(View.VISIBLE);
            } else {
                BaseToast.makeText(context, "网络异常，请检查网络是否畅通！", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            BaseToast.makeText(context, "网络异常，请检查网络是否畅通！", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * @param :
     * @return :
     * @method : stopVideo
     * @Author : xiaoxiao
     * @Describe : 停止播放视频
     * @Date : 2018/5/14
     */
    public void stopVideo() {
        isShowVideo = false;
        if (mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mSurfaceView.setKeepScreenOn(false);
            mSurfaceView.setBackgroundColor(getContext().getResources().getColor(R.color.bg_gray2));
            mSurfaceView.setVisibility(View.GONE);
            // mSurfaceBgLayout.setBackgroundColor(getContext().getResources().getColor(R.color.bg_gray2));
        }
        if (mImgView != null)
            mImgView.setVisibility(View.VISIBLE);

        //启动录像
        if (connectstate && !mShareUtil.getContinusTakePhotoState()) {

            takephotoManager.StartRecording(mHostBean, getmDeviceNum());
        }
    }

    /**
     * @param : arg1-打开true，关闭false
     * @return :
     * @method : openCamear
     * @Author : xiaoxiao
     * @Describe : 打开或关闭相机
     * @Date : 2018/5/14
     */
    public void openCamear(boolean arg1) {
        if (mStartOrEndTakePicture != null)
            mStartOrEndTakePicture.setChecked(arg1);

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
        if (mMediaPlayer != null) {
            mMediaPlayer.attachSurface(holder.getSurface(), this, width, height);
        }
        if (width > 0) {
            mVideoHeight = height;
            mVideoWidth = width;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mMediaPlayer != null) {
            mMediaPlayer.detachSurface();
        }
    }

    @Override
    public void setSurfaceSize(int width, int height, int visible_width, int visible_height, int sar_num, int sar_den) {
        mVideoHeight = height;
        mVideoWidth = width;
        mVideoVisibleHeight = visible_height;
        mVideoVisibleWidth = visible_width;
        mSarNum = sar_num;
        mSarDen = sar_den;
        mHandler.removeMessages(HANDLER_SURFACE_SIZE);
        mHandler.sendEmptyMessage(HANDLER_SURFACE_SIZE);
    }

    private static final int HANDLER_BUFFER_START = 1;
    private static final int HANDLER_BUFFER_END = 2;
    private static final int HANDLER_SURFACE_SIZE = 3;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;

    private Handler mVlcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg == null || msg.getData() == null)
                return;
            int key = msg.getData().getInt("event");
            Log.e("AAA", "key=="+key);
            switch (key) {
                case EventHandler.MediaPlayerPositionChanged:
                    Log.e("AAA", "MediaPlayerPositionChanged");
                    break;
                case EventHandler.MediaPlayerPlaying:
                    mHandler.removeMessages(HANDLER_BUFFER_END);
                    mHandler.sendEmptyMessage(HANDLER_BUFFER_END);
                    Log.e("AAA", "MediaPlayerPlaying");
/*                    if(mShareUtil!=null&&mShareUtil.getConnectstate())
                        updateCameraResources(((MainActivity) context).getResources().getDrawable(R.drawable.icon_page_video_a0), getResId());*/
                    break;
                case EventHandler.MediaPlayerEndReached:
                    Log.e("AAA", "MediaPlayerEndReached");
                    //播放完成
                    break;
                case EventHandler.MediaPlayerStopped:
                    Log.e("AAA", "MediaPlayerStopped");
                    break;
                case EventHandler.MediaPlayerEncounteredError:
                    Log.e("AAA", "MediaPlayerEncounteredError"+COMMAND_CAMERA_MEDIAPLAY_ERROR_COUNT);
                    COMMAND_CAMERA_MEDIAPLAY_ERROR_COUNT ++;
                    //预览失败
                    if(COMMAND_CAMERA_MEDIAPLAY_ERROR_COUNT==3){
                        Message msg1 = new Message();
                        msg1.what = 0x11;
                        msg1.arg1 = -1;
                        mHandler.sendMessage(msg1);
                        COMMAND_CAMERA_MEDIAPLAY_ERROR_COUNT = 0;
                    }else{
                        //重新尝试连接
                        isShowVideo = false;
                        //增加预览失败处理逻辑
                        playVideo();
                    }
                    break;
            }

        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_BUFFER_START:
                    showLoading();
                    break;
                case HANDLER_BUFFER_END:
                    mSurfaceView.setBackgroundColor(getContext().getResources().getColor(com.navinfo.collect.library.R.color.transp));
                    mImgView.setVisibility(View.GONE);
                    //重置异常计数器
                    COMMAND_CAMERA_MEDIAPLAY_ERROR_COUNT = 0;
                    hideLoading();
                    break;
                case HANDLER_SURFACE_SIZE:
                    changeSurfaceSize();
                    break;
                case 0x55:
                    int drawable = R.drawable.pop_camera_img;
                    try {
                        if (TakePhotoManager.getInstance().getCameraVedioClent(getmDeviceNum()).GetMode() == SensorWorkingMode.CAMEAR_PHOTO_CONTINUOUS_PHOTO)
                            drawable = R.drawable.pop_camera_take_photo_img;
                    } catch (Exception e) {

                    }
                    updateCameraBackgroundResources(drawable, getmDeviceNum());
                    break;
                case 0x11:
                    if (msg.arg1 >= 0) {
                        //Toast.makeText(getContext(),"预览成功",Toast.LENGTH_SHORT).show();
                    } else {
                        hideLoading();
                        Toast.makeText(getContext(), "预览失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 0x66:
                    if(videoLoadingProDlg!=null&&videoLoadingProDlg.isShowing())
                        videoLoadingProDlg.setText("成功获取时间，正在加载预留画面");
                    //相机GPS信号差，请稍等片刻，或将相机移到开发地带。
                    mGpsTv.setText("相机GPS信号差，请稍等片刻，或将相机移到开发地带。");
                    mGpsTv.setVisibility(View.GONE);
                    isCheckTime = true;
                    //没有开始录像或者拍照时启动GPS获取，判断连接是否正常
                    if (getmDeviceNum() == 1) {
                        isStartThread = true;
                    } else {
                        isStartThread2 = true;
                    }
                    mStartOrEndTakePicture.setEnabled(true);
                    //开启预览模式
                    playVideo();
                    Log.e("checkTime", "校验成功");
                    break;
                case 0x77:
                    hideLoading();
                    Log.e("checkTime", "校验失败");
                    mGpsTv.setVisibility(View.VISIBLE);
                    mGpsTv.setText("请在设置中确保PAD/时区与相机/时区保持一致或重新连接相机。");
                    BaseToast.makeText(context, "请在设置中确保PAD/时区与相机/时区保持一致。", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

    /**
     * @param :
     * @return :
     * @method : showLoading
     * @Author : xiaoxiao
     * @Describe : 显示等待对话框
     * @Date : 2018/5/14
     */
    private void showLoading() {
        try {

            if (videoLoadingProDlg == null)
                videoLoadingProDlg = new LoadingDialog(getContext());


            if (!videoLoadingProDlg.isShowing()) {
                videoLoadingProDlg.setText("正在获取中...");
                videoLoadingProDlg.setOnCancelListener(null);
                videoLoadingProDlg.show();
            }

        } catch (Exception e) {

        }
    }

    /**
     * @param :
     * @return :
     * @method : hideLoading
     * @Author : xiaoxiao
     * @Describe : 隐藏等待对话框
     * @Date : 2018/5/14
     */
    public void hideLoading() {
        try {
            if (videoLoadingProDlg != null) {
                videoLoadingProDlg.dismiss();
                videoLoadingProDlg = null;
            }
        } catch (Exception e) {

        }
    }

    /**
     * @param :
     * @return :
     * @method : startGPSStatus
     * @Author : xiaoxiao
     * @Describe :启动gps
     * @Date : 2018/5/14
     */
    public void startGPSStatus() {

        //重新校验时间
        isCheckTime = false;

        if (getmDeviceNum() == 1) {

            if (!isStartThread) {
                isStartThread = true;
                mUIUpdateRecoverProgress.run();
            }
        } else if (getmDeviceNum() == 2) {

            if (!isStartThread2) {
                isStartThread2 = true;
                //((MainActivity) context).mUIUpdateRecoverProgress.run();
            }
        }
    }

    public Runnable mUIUpdateRecoverProgress = new Runnable() {

        @Override
        public void run() {
            //都获信号取后不在监听
            if (!isStartThread)
                return;

            if (isStartThread && mShareUtil.getConnectstate() && NetUtils.getInstance().isExistWifi(false) && !TextUtils.isEmpty(mShareUtil.getTakeCameraIP()) && mShareUtil.getContinusTakePhotoState()) {

                long subTime = System.currentTimeMillis() - mPreTime;

                //修改为绝对值，防止时间错误
                if (Math.abs(subTime) > 3000 && TakePhotoManager.getInstance() != null) {
                    mPreTime = System.currentTimeMillis();
                    TakePhotoManager.getInstance().getCameraVedioClent(1).setmTag(1);
                    TakePhotoManager.getInstance().getCameraVedioClent(1).GetGpsStatus();
                }
            }

            mHandler.postDelayed(mUIUpdateRecoverProgress, 1000);

        }
    };

    /**
     * @param :
     * @return :
     * @method : changeSurfaceSize
     * @Author : xiaoxiao
     * @Describe : 改变视频屏幕显示尺寸
     * @Date : 2018/5/14
     */
    private void changeSurfaceSize() {

        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int dw = display.getWidth();
        int dh = display.getHeight();

        // calculate aspect ratio
        double ar = (double) mVideoWidth / (double) mVideoHeight;
        // calculate display aspect ratio
        double dar = (double) dw / (double) dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_FIT_HORIZONTAL:
                dh = (int) (dw / ar);
                break;
            case SURFACE_FIT_VERTICAL:
                dw = (int) (dh * ar);
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoHeight;
                dw = mVideoWidth;
                break;
        }

        mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

        Log.e("qj", mVideoWidth + "===");

        mSurfaceView.invalidate();
    }

    public HostBean getmHostBean() {
        return mHostBean;
    }

    //连接
    public void connection(HostBean hostBean) {
        if (hostBean != null) {
            SensorParams params = new SensorParams();

            params.put("ip", hostBean.ipAddress.toString());

            Log.e("AAA", "ip" + hostBean.ipAddress);

            mHostBean = hostBean;

            mShareUtil.setTakeCameraIP(Constant.USER_ID, hostBean.ipAddress.toString());

            mShareUtil.setTakeCameraMac(Constant.USER_ID, hostBean.hardwareAddress.toString());

            takephotoManager.connect(getmDeviceNum(), hostBean, params);
        }
    }

    private int getResId() {

        if (getmDeviceNum() == 2)
            return R.id.main_activity_camera2;

        return R.id.main_activity_camera;
    }

    public ShareUtil getmShareUtil() {
        return mShareUtil;
    }

    //停止录像或拍照
    private void stopVideoOrPicture() {
        updateCameraResources(0, getmDeviceNum());

        if (isShowVideo()) {

            stopVideo();

        } else {

            takephotoManager.StartRecording(mHostBean, getmDeviceNum());

            isStartThread = true;

        }
    }

    //获取编号
    public int getmDeviceNum() {
        return mDeviceNum;
    }

    //设置编号
    public void setmDeviceNum(int mDeviceNum) {
        this.mDeviceNum = mDeviceNum;
    }

    //启动或关闭拍照模式
    public void initmTakePhotoOrRecord(boolean isCheck) {
        if (mTakePhotoOrRecord != null && mTakePhotoOrRecord.isChecked() != isCheck) {
            mTakePhotoOrRecord.setChecked(isCheck);
            mTakePhotoOrRecord.setText(isCheck ? "录像" : "拍照");
            isCancel = true;

            if (!isCheck)
                setmImgViewImageDrawable(1);
            else
                setmImgViewImageDrawable(0);
        }
    }

    private void setmImgViewImageDrawable(int type) {
        Drawable drawable = getContext().getResources().getDrawable(R.drawable.icon_camera_img);
        if (type == 1)
            drawable = getContext().getResources().getDrawable(R.drawable.icon_camera_take_photo_img);

        if (mImgView != null)
            mImgView.setImageDrawable(drawable);
    }
}

package com.navinfo.omqs.util;

import android.media.MediaRecorder;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;

/**
 * 录音接口
 */
public class SoundMeter {
    static final private double EMA_FILTER = 0.6;

    private static final String TAG = "SoundMeter";
    private String mFilePath;
    private MediaRecorder mRecorder = null;
    private double mEMA = 0.0;
    //监听
    private OnSoundMeterListener mListener;
    //是否开启了语音录制
    private boolean isStartSound;
    /**
     * 开始录音
     *
     * @param name 录音文件保存路径
     */
    public void start(final String name) {
        mFilePath = name;
        isStartSound = false;
        //执行录音操作
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED) || TextUtils.isEmpty(name)) {
            if(mListener!=null)
                mListener.onfaild("权限失败或者文件名称错误");
            return;
        }

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setOutputFile(name);
            Log.w(TAG, "录音" + name);

            try {
                mRecorder.prepare();
                mRecorder.start();
                mEMA = 0.0;
                isStartSound = true;
            } catch (IllegalStateException e) {
                if(mListener!=null)
                    mListener.onfaild(e.getMessage());
                if (mRecorder != null)
                    mRecorder.release();
                //启动异常释放资源
                isStartSound = false;
                mRecorder = null;
                System.out.print(e.getMessage());
            } catch (IOException e) {
                System.out.print(e.getMessage());
                if(mListener!=null)
                    mListener.onfaild(e.getMessage());
                //启动异常释放资源
                isStartSound = false;
                if (mRecorder != null)
                    mRecorder.release();
                mRecorder = null;
            }finally {

            }
        }
    }

    /**
     * 结束录音接，释放录音对象
     */
    public void stop() {
        isStartSound = false;
        try {
            if (mRecorder != null) {
                mRecorder.stop();
            }
            if(new File(mFilePath).exists()){
                if(mListener!=null)
                    mListener.onSuccess(mFilePath);
            }
        } catch (Exception e) {
            if(mListener!=null)
                mListener.onfaild(e.getMessage());
        } finally {
            if (mRecorder != null)
                mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * 停止录音
     */
    public void pause() {
        if (mRecorder != null) {
            mRecorder.stop();
        }
    }

    /**
     * 开始录音
     */
    public void start() {
        if (mRecorder != null) {
            mRecorder.start();
        }
    }

    /**
     * 获取录音基准值
     *
     * @return
     */
    public double getAmplitude() {
        if (mRecorder != null)
            return (mRecorder.getMaxAmplitude() / 2700.0);
        else
            return 0;

    }

    /**
     * 获取EMA基准值
     *
     * @return
     */
    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

    public OnSoundMeterListener getmListener() {
        return mListener;
    }

    public void setmListener(OnSoundMeterListener mListener) {
        this.mListener = mListener;
    }

    //是否开启了语音录制
    public boolean isStartSound(){
        return isStartSound;
    }

    //录音监听
    public interface OnSoundMeterListener{
        public void onSuccess(String filePath);
        public void onfaild(String message);
    }

}

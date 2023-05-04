package com.navinfo.omqs.util;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.blankj.utilcode.util.ToastUtils;
import com.navinfo.omqs.Constant;
import com.navinfo.omqs.R;
import java.io.File;

public class SoundRecordeUtils {
    private static SoundRecordeUtils instance;
    private SoundMeter mSensor; // 系统录音组件
    private PopupWindow pop;
    private ImageView volume;
    private SpeakMode mSpeakMode;
    private Activity mActivity;

    public static SoundRecordeUtils getInstance(Activity context) {
        if (instance == null) {
            instance = new SoundRecordeUtils(context);
        }
        return instance;
    }

    public SoundRecordeUtils(Activity mContext) {
        this.mActivity = mContext;
        mSpeakMode = new SpeakMode(mContext);
        initVoicePop();
    }

    private void initVoicePop() {
        //语音识别动画
        pop = new PopupWindow();
        pop.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        pop.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        pop.setBackgroundDrawable(new BitmapDrawable());
        View view = View.inflate(mActivity, R.layout.cv_card_voice_rcd_hint_window, null);
        pop.setContentView(view);

        pop.update();
        volume = (ImageView) view.findViewById(R.id.volume);
    }

    //启动录音
    public String startSoundMeter(View v, SoundRecordeCallback soundRecordeCallback, boolean isRemind/*是否需要默认语音提醒开始和结束录音*/) {
        //录音动画
        if (pop != null)
            pop.showAtLocation(v, Gravity.CENTER, 0, 0);
        volume.setBackgroundResource(R.drawable.pop_voice_img);
        AnimationDrawable animation = (AnimationDrawable) volume.getBackground();
        animation.start();

        final String name = DateTimeUtil.getTimeSSS() + ".m4a";
        if (mSensor == null) {
            mSensor = new SoundMeter();
        }
        mSensor.setmListener(new SoundMeter.OnSoundMeterListener() {
            @Override
            public void onSuccess(String filePath) {
                if (isRemind) {
                    mSpeakMode.speakText("结束录音");
                }
                if (soundRecordeCallback!=null) {
                    soundRecordeCallback.onSuccess(filePath, name);
                }
            }

            @Override
            public void onfaild(String message) {
                if (isRemind) {
                    ToastUtils.showLong("录制失败！");
                    mSpeakMode.speakText("录制失败");
                }
                if (soundRecordeCallback!=null) {
                    soundRecordeCallback.onfaild(message);
                }
            }
        });
        //增加下目录创建，防止由于目录导致无法录制文件
        if (!new File(Constant.USER_DATA_ATTACHEMNT_PATH).exists()) {
            new File(Constant.USER_DATA_ATTACHEMNT_PATH).mkdirs();
        }
        if (mSensor.isStartSound()) {
            ToastUtils.showLong("已自动结束上一段录音");
            mSpeakMode.speakText("已自动结束上一段录音");
            return null;
        }
        //启动定时器
        mSensor.start(Constant.USER_DATA_ATTACHEMNT_PATH + name);
        if (isRemind) {
            ToastUtils.showLong("开始录音");
            mSpeakMode.speakText("开始录音");
        }
        return name;
    }

    //判断是否启动了录音
    public boolean isStartSound(){
        if(mSensor!=null){
            return mSensor.isStartSound();
        }

        return false;
    }

    //停止语音录制
    public void stopSoundMeter() {
        //先重置标识，防止按钮抬起时触发语音结束

        if (mSensor != null && mSensor.isStartSound()) {
            mSensor.stop();
        }

        if (pop != null && pop.isShowing())
            pop.dismiss();
    }

    public interface SoundRecordeCallback{
        public void onSuccess(String filePath, String fileName);
        public void onfaild(String message);
    }
}

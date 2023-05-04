package com.navinfo.omqs.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import com.navinfo.omqs.ui.dialog.FirstDialog;

import java.util.HashMap;
import java.util.Locale;

//语音类
public class SpeakMode extends Activity implements TextToSpeech.OnInitListener{
    private Activity mActivity;
    private TextToSpeech mTextToSpeech;//TTS对象
    private int status;
    private int MY_DATA_CHECK_CODE = 0;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x11:
                    try {
                        HashMap<String, String> params = new HashMap<String, String>();

                        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, "STREAM_NOTIFICATION");//设置播放类型（音频流类型）

                        mTextToSpeech.speak(msg.obj + "", TextToSpeech.QUEUE_ADD, params);//将这个发音任务添加当前任务之后

                        //BaseToast.makeText(mActivity,msg.obj+"",Toast.LENGTH_LONG).show();

                        mTextToSpeech.playSilence(100, TextToSpeech.QUEUE_ADD, params);//间隔多长时间

                    } catch (Exception e) {

                    }
                    break;
            }
        }
    };

    public SpeakMode(Activity activity) {

        mActivity = activity;

        if (mActivity != null && !mActivity.isFinishing())
            this.mTextToSpeech = new TextToSpeech(this.mActivity, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
    }

    public void setData(String json) {

    }

    @Override
    public void onInit(int status) {
        this.status = status;
        if (this.mTextToSpeech != null) {
            int result = this.mTextToSpeech.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (mActivity != null && !mActivity.isFinishing()) {
                    FirstDialog firstDialog = new FirstDialog(mActivity);
                    firstDialog.setTitle("提示");
                    firstDialog.setMessage("设备不支持语音播报，请先下载语音助手。");
                    firstDialog.setConfirmListener(new FirstDialog.OnClickListener() {
                        @Override
                        public void onClick(Dialog dialog, int which) {
                                dialog.dismiss();
                            }
                    });
                    firstDialog.setNegativeView(View.GONE);
                    firstDialog.show();
                }
            }
        }
        Log.i("TextToSpeechDemo", String.valueOf(status));
    }

    //读语音处理
    public void speakText(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mTextToSpeech != null) {

                    int result = mTextToSpeech.setLanguage(Locale.CHINESE);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    } else {
                        if (mTextToSpeech != null && mTextToSpeech.isSpeaking()) {

                            while (mTextToSpeech.isSpeaking()) {

                                try {
                                    //增加播报停止，解决不能播报最新内容问题
                                    mTextToSpeech.stop();

                                    Thread.sleep(100);

                                } catch (Exception e) {

                                }
                            }
                        }

                        Message msg = new Message();
                        msg.what = 0x11;
                        msg.obj = message;
                        mHandler.sendMessage(msg);

                    }
                }
            }
        }).start();

    }

    public void stopSpeek() {
        try {

            if (this.mTextToSpeech != null && this.mTextToSpeech.isSpeaking()) {
                this.mTextToSpeech.stop();
            }
        } catch (Exception e) {

        }
    }

}

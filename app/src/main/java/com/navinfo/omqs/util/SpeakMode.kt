package com.navinfo.omqs.util

import android.content.Context
import android.os.Handler
import android.os.Message
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import com.navinfo.omqs.ui.dialog.FirstDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

//语音类
class SpeakMode(private val context: Context) : TextToSpeech.OnInitListener {
    private var mTextToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var status = 0
    private val MY_DATA_CHECK_CODE = 0
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0x11 -> try {
                    val params = HashMap<String, String>()
                    params[TextToSpeech.Engine.KEY_PARAM_STREAM] =
                        "STREAM_NOTIFICATION" //设置播放类型（音频流类型）
                    mTextToSpeech.speak(
                        msg.obj.toString() + "",
                        TextToSpeech.QUEUE_ADD,
                        params
                    ) //将这个发音任务添加当前任务之后

                    //BaseToast.makeText(mActivity,msg.obj+"",Toast.LENGTH_LONG).show();
                    mTextToSpeech.playSilence(100, TextToSpeech.QUEUE_ADD, params) //间隔多长时间
                } catch (e: Exception) {
                }
            }
        }
    }

    init {
//        if (mActivity != null && !mActivity.isFinishing) mTextToSpeech = TextToSpeech(
//            mActivity, this
//        )

    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val checkIntent = Intent()
//        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
//        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE)
//    }

    fun setData(json: String?) {}
    override fun onInit(status: Int) {
        this.status = status
        val result = mTextToSpeech.setLanguage(Locale.CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA
            || result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            if (context != null) {
                val firstDialog = FirstDialog(context)
                firstDialog.setTitle("提示")
                firstDialog.setMessage("设备不支持语音播报，请先下载语音助手。")
                firstDialog.setConfirmListener { dialog, _ -> dialog.dismiss() }
                firstDialog.setNegativeView(View.GONE)
                firstDialog.show()
            }
        }
        Log.i("TextToSpeechDemo", status.toString())
    }

    //读语音处理
    fun speakText(message: String) {
        mTextToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "")
//        val result = mTextToSpeech.setLanguage(Locale.CHINESE)
//        if (result == TextToSpeech.LANG_MISSING_DATA
//            || result == TextToSpeech.LANG_NOT_SUPPORTED
//        ) {
//        } else {
//        while (mTextToSpeech.isSpeaking) {
//            try {
//                //增加播报停止，解决不能播报最新内容问题
//                mTextToSpeech.stop()
//            } catch (e: Exception) {
//
//            }
//        }
//            val msg = Message()
//            msg.what = 0x11
//            msg.obj = message
//            mHandler.sendMessage(msg)
//        }
    }

    fun stopSpeech() {
        try {
            if (mTextToSpeech.isSpeaking) {
                mTextToSpeech.stop()
            }
        } catch (e: Exception) {
        }
    }

    fun shutdown() {
        stopSpeech()
        mTextToSpeech.shutdown()
    }
}
package com.navinfo.omqs.ui.dialog;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.navinfo.omqs.R;

/**
 * @author zcs
 * @version V1.0
 * @ClassName: LoadingDialog.java
 * @Date 2015年9月17日 下午1:34:39
 * @Description: 弹出等待框
 */
public class LoadingDialog extends MyDialog {

    private CharSequence text;

    private View.OnClickListener textListener;

    private TextView tv_msg;

    public LoadingDialog(Context context) {

        super(context);

        // requestWindowFeature(Window.FEATURE_NO_TITLE);//不显示标题
    }

    @Override
    public void request(Object obj) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        View customFrame = View.inflate(context, R.layout.dialog_loading_custom_frame_layout, null);

        ((AnimationDrawable) customFrame.findViewById(R.id.customFrameLoadImg).getBackground()).start();

        tv_msg = (TextView) customFrame.findViewById(R.id.customFrameMsg);

        tv_msg.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);

        tv_msg.setText(TextUtils.isEmpty(text) ? "" : text);

        tv_msg.setOnClickListener(textListener);

        setContentView(customFrame);
    }

    /**
     * 设置等待提示文字
     *
     * @param text
     */
    public void setText(CharSequence text) {

        this.text = text;
        if (tv_msg!=null){
            tv_msg.setVisibility(TextUtils.isEmpty(this.text) ? View.GONE : View.VISIBLE);
            tv_msg.setText(TextUtils.isEmpty(this.text) ? "" : this.text);
        }
    }

    /**
     * 设置文本点击事件
     *
     * @param listener
     */
    public void setTextClickListener(View.OnClickListener listener){

        this.textListener = listener;

        if (tv_msg!=null){
            tv_msg.setOnClickListener(listener);
        }
    }
}

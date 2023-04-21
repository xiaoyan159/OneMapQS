package com.navinfo.omqs.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.navinfo.omqs.R;

/**
 * @author zcs
 * @version V1.0
 * @ClassName: FirstDialog.java
 * @Date 2015年11月18日 下午5:25:27
 * @Description: 弹出默认的dialog
 */
public class FirstDialog extends MyDialog {

    private CharSequence mPositiveButtonText;
    private OnClickListener mPositiveButtonListener;
    private CharSequence mNegativeButtonText;
    private OnClickListener mNegativeButtonListener;
    private CharSequence mMiddleButtonText;
    private OnClickListener mMiddleButtonListener;
    private Object tag;

    public FirstDialog(Context context) {

        super(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_default, null);
        setContentView(rootView/*, layoutParams*/);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void request(Object obj) {

    }



    /**
     * 设置标题文字
     */
    @Override
    public void setTitle(CharSequence string) {
        findViewById(R.id.ll_title).setVisibility(View.VISIBLE);
        TextView tv = (TextView) findViewById(R.id.tv_title);
        tv.setText(TextUtils.isEmpty(string) ? "" : string);
    }

    /**
     * 设置标题颜色
     */
    public void setTitleColor(int color) {
        ((TextView) findViewById(R.id.tv_title)).setTextColor(color);
    }

    /**
     * 设置标题2文字
     */
    public void setTitle2(CharSequence string) {
        findViewById(R.id.tv_title2).setVisibility(View.VISIBLE);
        TextView tv = (TextView) findViewById(R.id.tv_title2);
        tv.setText(TextUtils.isEmpty(string) ? "" : string);
    }

    public void setTitle2Color(int color) {
        ((TextView) findViewById(R.id.tv_title2)).setTextColor(color);
    }

    /**
     * 设置标题下分割线显隐
     */
    public void setTitleDividerVisible(int visible) {
        findViewById(R.id.title_divider).setVisibility(visible);
    }

    /**
     * 设置标题下分割线显隐
     */
    public void setTitleDividerVisible2(int visible) {
        findViewById(R.id.title_divider2).setVisibility(visible);
    }

    public void setBottomDividerVisible(int visible) {
        findViewById(R.id.v_divice).setVisibility(visible);

    }

    /**
     * 确认按钮
     *
     * @param string
     */
    public void setConfirm(CharSequence string) {
        mPositiveButtonText = string;
        TextView btn = (TextView) findViewById(R.id.btn_fm_confirm);
        showBottomView();
        btn.setText(TextUtils.isEmpty(string) ? "确定" : string);
    }

    public void setConfirmEnable(boolean enable) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_confirm);
        btn.setEnabled(enable);
    }


    public void setConfirmVisibility(int visibility) {
        findViewById(R.id.btn_fm_confirm).setVisibility(visibility);
    }

    /**
     * 确认按钮
     *
     * @param colors
     */
    public void setConfirmTxtColor(int colors) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_confirm);
        btn.setTextColor(colors);
    }

    /**
     * 确认按钮字体大小
     *
     * @param size
     */
    public void setConfirmSize(float size) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_confirm);
        btn.setTextSize(size);
    }

    /**
     * 取消按钮
     *
     * @param string
     */
    public void setCancel(CharSequence string) {
        mNegativeButtonText = string;
        TextView btn = (TextView) findViewById(R.id.btn_fm_cancel);
        btn.setText(TextUtils.isEmpty(string) ? "取消" : string);
        showBottomView();
        btn.setVisibility(View.VISIBLE);
    }

    /**
     * 取消按钮字体颜色
     *
     * @param color
     */
    public void setCancelTxtColor(int color) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_cancel);
        btn.setTextColor(color);
    }

    /**
     * 取消按钮是否可点
     *
     * @param bl
     */
    public void setCancelIsCanClick(Boolean bl) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_cancel);
        btn.setEnabled(bl);
    }

    /**
     * 取消按钮字体大小
     *
     * @param size
     */
    public void setCancelSize(float size) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_cancel);
        btn.setTextSize(size);
    }

    /**
     * 中间按钮
     *
     * @param string
     */
    public void setMiddle(CharSequence string) {
        mNegativeButtonText = string;
        TextView btn = (TextView) findViewById(R.id.btn_fm_middle);
        btn.setText(TextUtils.isEmpty(string) ? "中间" : string);
        showBottomView();
        btn.setVisibility(View.VISIBLE);
    }

    /**
     * 中间按钮字体颜色
     *
     * @param color
     */
    public void setMiddleTxtColor(int color) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_middle);
        btn.setTextColor(color);
    }

    /**
     * 中间按钮是否可点
     *
     * @param bl
     */
    public void setMiddleIsCanClick(Boolean bl) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_middle);
        btn.setEnabled(bl);
    }

    /**
     * 中间按钮字体大小
     *
     * @param size
     */
    public void setMiddleSize(float size) {
        TextView btn = (TextView) findViewById(R.id.btn_fm_middle);
        btn.setTextSize(size);
    }

    /**
     * 设置中间提示信息
     *
     * @param string
     */
    public void setContentTxt(CharSequence string) {
        TextView tv = (TextView) findViewById(R.id.tv_content);
        tv.setText(TextUtils.isEmpty(string) ? "" : string);
    }

    /**
     * 设置中间提示信息是否可长按复制
     *
     * @param isSelectable
     */
    public void setTextIsSelectable(boolean isSelectable) {
        TextView tv = (TextView) findViewById(R.id.tv_content);
        tv.setTextIsSelectable(isSelectable);
    }

    /**
     * 设置点击事件
     *
     * @param click
     */
    public void setContentClickListener(View.OnClickListener click) {
        findViewById(R.id.tv_content).setOnClickListener(click);
    }


    /**
     * 设置中间显隐
     *
     * @param visable
     */
    public void setContentTxtVisable(int visable) {
        TextView tv = (TextView) findViewById(R.id.tv_content);
        tv.setVisibility(visable);
    }

    /**
     * 设置中间提示信息
     *
     * @param string
     */
    public void setMessage(CharSequence string) {
        setContentTxt(string);
    }

    /**
     * 设置中间提示颜色
     *
     * @param color
     */
    public void setMessageColor(int color) {
        TextView tv = (TextView) findViewById(R.id.tv_content);
        tv.setTextColor(color);
    }

    /**
     * 设置中间提示文字
     *
     * @param txtId
     */
    public void setMessage(int txtId) {
        String txt = context.getResources().getString(txtId);
        TextView tv = (TextView) findViewById(R.id.tv_content);
        tv.setText(txt);
    }

    /**
     * 设置中间显示的内容
     */
    public void setMiddleView(View view) {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl_content_view);
        rl.removeAllViews();
        if (view != null)
            rl.addView(view);
    }

    public void setMiddleViewMatch(View view) {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl_content_view);
        rl.removeAllViews();
        if (view != null)
            rl.addView(view, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void removeMideView() {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl_content_view);
        rl.removeAllViews();
    }

    /**
     * 设置中间显示的内容
     */
    public View setMiddleView(int id) {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl_content_view);
        rl.removeAllViews();
        View inflate = View.inflate(getContext(), id, rl);
        return rl.getChildAt(0);
    }

    /**
     * 确定按钮 点击事件
     *
     * @param click
     */
    public Dialog setConfirmListener(OnClickListener click) {
        return setPositiveButton(mPositiveButtonText, click);
    }

    /**
     * 取消按钮 点击事件
     *
     * @param click
     */
    public Dialog setCancelListener(OnClickListener click) {
        return setNegativeButton(mNegativeButtonText, click);
    }

    /**
     * 取消按钮 点击事件
     *
     * @param click
     */
    public Dialog setMiddleListener(OnClickListener click) {
        return setMiddleButton(mMiddleButtonText, click);
    }

    /**
     * 中间按钮 是否可点
     *
     * @param bl
     */
    public void setMiddleButtonIsCanClick(Boolean bl) {
        TextView btn_fm_confirm = (TextView) findViewById(R.id.btn_fm_middle);
        btn_fm_confirm.setEnabled(bl);
    }

    public Dialog setMiddleButton(CharSequence text, OnClickListener listener) {
        mMiddleButtonText = text;
        mMiddleButtonListener = listener;
        setMiddle(text);
        findViewById(R.id.middle_view).setVisibility(View.VISIBLE);
        TextView btn_fm_middle = (TextView) findViewById(R.id.btn_fm_middle);
        btn_fm_middle.setVisibility(View.VISIBLE);
        btn_fm_middle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mMiddleButtonListener != null) {
                    mMiddleButtonListener.onClick(FirstDialog.this, 3);
                } else {
                    dismiss();
                }
            }
        });
        return this;
    }

    public Dialog setMiddleButton(int txtId, final OnClickListener listener) {
        String text = context.getResources().getString(txtId);
        return setMiddleButton(text, listener);
    }

    /**
     * 确认按钮 是否可点
     *
     * @param bl
     */
    public void setPositiveButtonIsCanClick(Boolean bl) {
        TextView btn_fm_confirm = (TextView) findViewById(R.id.btn_fm_confirm);
        btn_fm_confirm.setEnabled(bl);
    }

    public Dialog setPositiveButton(CharSequence text, OnClickListener listener) {
        mPositiveButtonText = text;
        mPositiveButtonListener = listener;
        setConfirm(text);
        findViewById(R.id.btn_fm_confirm).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mPositiveButtonListener != null) {
                    mPositiveButtonListener.onClick(FirstDialog.this, 2);
                } else {
                    dismiss();
                }
            }
        });
        //如果设置了确定或取消按钮，则不允许点击其他区域隐藏对话框
        this.setCancelable(false);
        return this;
    }

    public Dialog setPositiveButton(int txtId, final OnClickListener listener) {
        String text = context.getResources().getString(txtId);
        return setPositiveButton(text, listener);
    }

    public Dialog setNegativeButton(CharSequence text, OnClickListener listener) {
        mNegativeButtonText = text;
        mNegativeButtonListener = listener;
        setCancel(text);
        findViewById(R.id.btn_fm_cancel).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mNegativeButtonListener != null) {
                    mNegativeButtonListener.onClick(FirstDialog.this, 1);
                } else {
                    dismiss();
                }
            }
        });
        //如果设置了确定或取消按钮，则不允许点击其他区域隐藏对话框
        this.setCancelable(false);
        return this;
    }

    public Dialog setNegativeButton(int txtId, final OnClickListener listener) {
        String text = context.getResources().getString(txtId);
        return setNegativeButton(text, listener);
    }

    public interface OnClickListener {
        /**
         * This method will be invoked when a button in the dialog is clicked.
         *
         * @param dialog The dialog that received the click.
         * @param which  The button that was clicked (e.g.
         *               {@link DialogInterface#BUTTON1}) or the position
         *               of the item clicked.
         */
        /* TODO: Change to use BUTTON_POSITIVE after API council */
        public void onClick(Dialog dialog, int which);
    }

    private void showBottomView() {
        findViewById(R.id.v_divice).setVisibility(View.VISIBLE);
        findViewById(R.id.ll_bottom_btn).setVisibility(View.VISIBLE);
        //如果设置了确定或取消按钮，则不允许点击其他区域隐藏对话框
        this.setCancelable(false);
    }


    public void setNegativeView(int View) {
        findViewById(R.id.btn_fm_cancel).setVisibility(View);
        findViewById(R.id.view_dialog).setVisibility(View);
    }

    public void setCancelVisibility(int isVisibility) {
        findViewById(R.id.btn_fm_cancel).setVisibility(isVisibility);
    }

    public void setBottomLayoutVisibility(int isVisibility) {
        findViewById(R.id.ll_bottom_layout).setVisibility(isVisibility);
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public void setNegativeButtonEnable(boolean enable) {
        findViewById(R.id.btn_fm_cancel).setEnabled(enable);
    }

    /**
     * 设置北京资源
     */
    public void setBackgroundColor(int res) {
        LinearLayout rl = (LinearLayout) findViewById(R.id.ll_dialog);
        if(rl!=null)
            rl.setBackgroundColor(res);
    }
}

package com.navinfo.omqs.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Window;

/**
 * @param <T>
 * @param <T>
 * @author qj
 * @version V1.0
 * @ClassName: MyDialog.java
 * @Date 2023年4月14日 上午10:18:52
 * @Description: 所有提示框的父类（在onCreate设置布局）
 */
public abstract class MyDialog extends Dialog implements IDialog {

    protected Context context;

    private Object obj;

    private DataSource dataSource;

    private String str;

    public MyDialog(Context context) {
        super(context);
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);//不显示标题
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void dismiss() {

        if (dataSource != null) {

            dataSource.Data(str, obj);

        }

        super.dismiss();
    }

    public void setWidthHeight(int width, int height) {
        getWindow().setLayout(width, height);
    }

    @Override
    public void show() {

        request(obj);

        super.show();
    }

    @Override
    public void setData(String str, Object obj) {

        this.str = str;

        this.obj = obj;

    }

    @Override
    public Object getData() {

        return obj;

    }

    public void getResponse(DataSource dataSource) {

        this.dataSource = dataSource;

    }

    /**
     * 设置背景色
     *
     * @param drawable
     */
    public void setBackground(Drawable drawable) {

        getWindow().setBackgroundDrawable(drawable);

    }
}

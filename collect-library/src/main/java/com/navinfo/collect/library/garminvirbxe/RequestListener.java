package com.navinfo.collect.library.garminvirbxe;

import android.graphics.Bitmap;

import com.android.volley.VolleyError;

public interface RequestListener {

    /** 成功 */
    public void requestSuccess(Bitmap bitmap,String savePath);

    /** 错误 */
    public void requestError(VolleyError e);
}

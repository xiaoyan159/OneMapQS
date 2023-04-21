package com.navinfo.collect.library.garminvirbxe;

import android.content.Context;

public class IRequest {

    /**
     * 
     * @param context
     * @param url
     * @param params
     * @param isDebug
     * @param cmaeralistener
     */
    public static void post(Context context, String url, SensorParams params,
            Boolean isDebug, CameraEventListener cmaeralistener,
                            com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType type,HostBean hostBean,int tag) {
        RequestManager
                .post(url, context, params, isDebug, cmaeralistener, type,hostBean,tag);
    }

    /**
     * 
     * @param context
     * @param hostBean
     * @param url
     * @param isthumb
     */
    public static void getbitmap(Context context, HostBean hostBean,String url, Boolean isthumb,
            CameraEventListener cameralistener,int tag) {
        RequestManager.getbitmap(context, hostBean,url, isthumb, cameralistener,tag);
    }

}
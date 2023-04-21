package com.navinfo.collect.library.garminvirbxe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Handler;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.navinfo.collect.library.utils.PreferencesUtils;
import com.navinfo.collect.library.utils.SensorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;

@SuppressLint("NewApi")
public class RequestManager {
    public static RequestQueue mRequestQueue;
    private static Context mContext;
    static String picThumbUrl;
    private static String picUrl;
    private static Handler mHandle;
    private static int currentFailCount = 0; // 记录发送命令失败次数
    private static int failCount = 3; // 记录发送命令失败次数

    public RequestManager(Context mContext) {
        mRequestQueue = Volley.newRequestQueue(mContext);
        RequestManager.mContext = mContext;
    }

    public void SetRequestHandle(Handler mHandle) {
        RequestManager.mHandle = mHandle;
    }

    public static void getbitmap(Object object, HostBean hostBean, String url, Boolean isThumb,
                                 CameraEventListener listener, int tag) {
        ImageRequest imageRequest = new ImageRequest(url,
                responseBitmapListener(listener, url, isThumb, hostBean, tag), 0, 0,
                Config.RGB_565, responseError(listener, false, null,hostBean, tag));
        addRequest(imageRequest, tag);
    }

    /**
     * 返回String
     *
     * @param url      接口
     * @param tag      上下文
     * @param params   post需要传的参数
     * @param listener 回调
     */
    public static void post(String url, Object object, SensorParams params,
                            Boolean isDebug, CameraEventListener listener,
                            com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType commandType, HostBean hostBean, int tag) {
        JsonObjectRequest jsonRequest;
        if (isDebug) {
            jsonRequest = new JsonObjectRequest(Request.Method.POST, url,
                    params.getParams(), responseListener(listener, commandType,
                    isDebug, hostBean, tag), responseError(listener, isDebug, commandType,hostBean, tag));
            jsonRequest
                    .setRetryPolicy(new DefaultRetryPolicy(1 * 1000, 1, 1.0f));
        } else {
            jsonRequest = new JsonObjectRequest(Request.Method.POST, url,
                    params.getParams(), responseListener(listener, commandType,
                    isDebug, hostBean, tag), responseError(listener, isDebug, commandType,hostBean, tag));
            //超时时间
            int timeOut = 10 * 1000;
            //如果为获取GPS状态，修改超时时间，解决连续拍照导致相机性能下降，获取时间过长问题
            if (commandType == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETSTATUS)
                timeOut = 100 * 1000;
            //解决性能问题
            if (commandType == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_MEDIA_LIST) {
                timeOut = 100 * 1000;
            }

            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(timeOut, 1,
                    1.0f));
        }
        addRequest(jsonRequest, tag);
    }

    /**
     * 成功消息监听 返回String
     *
     * @param listener String 接口
     * @return
     */
    protected static Listener<JSONObject> responseListener(
            final CameraEventListener listener, final com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType type,
            final Boolean isDebug, final HostBean hostBean, final int tag) {
        return new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject json) {
                try {
                    if (isDebug) {
                        mHandle.sendEmptyMessage(SensorUtils.HADNLE_STATUS_OK);
                    } else {

                        try {
                            Log.e("AAA", type  + (json == null ? "" : json.toString()));
                        } catch (Exception e) {

                        }

                        if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_MONITORING) {
                            currentFailCount = 0;
                            mHandle.sendEmptyMessage(SensorUtils.HADNLE_MONITORING);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETSTATUS) {
                            currentFailCount = 0;
                            mHandle.sendEmptyMessage(SensorUtils.HADNLE_CONNECT_OK);
                            if (listener != null)
                                listener.OnStatusResponse(hostBean, tag);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_RECORD) {
                            if (listener != null)
                                listener.OnStartRecordResponse(hostBean, tag);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_STOP_RECORD) {
                            if (listener != null)
                                listener.OnStopRecordResponse(hostBean, tag);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_TIME_LASPE_RATE) {
                            if (listener != null)
                                listener.OnContinuousPhototTimeLapseRateResponse(hostBean, tag);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_SNAPPICTURE) {
                            if (listener != null)
                                listener.OnContinuousPhototTimeLapseRateStartResponse(hostBean, tag);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_SNAPPICTURE_SINGLE) {/*增加单独拍照流程，增加时间校验流程*/
                            if (listener != null) {
                                JSONObject mediajson;
                                try {
                                    if(json!=null){
                                        mediajson = new JSONObject(json.getString("media").toString());
                                        String url = mediajson.get("url").toString();
                                        String name = mediajson.get("name").toString();
                                        listener.OnContinuousPhototSingle(hostBean, url, name, tag);
                                        Log.e("AAA", "获取单张拍照" + url);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    listener.OnContinuousPhototSingle(hostBean, null, null, tag);
                                }
                            }
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_MEDIA_LIST) {
                            if (listener != null&&json!=null){
                                listener.OnGetMediaList(hostBean, json.toString(), tag);
                            }

                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_TIME_LASPE_RATE_STOP) {
                            if (listener != null)
                                listener.OnContinuousPhototTimeLapseRateStopResponse(hostBean, tag);
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETGPSSTATUS) {
                            if (listener != null&&json!=null) {
                                String lat = "";
                                String lon = "";
                                try {
                                    lat = json.get("gpsLatitude").toString();
                                    lon = json.get("gpsLongitude").toString();
                                    Log.e("AAA", "获取设备time" + json.get("recordingTime").toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (!lat.equals("") && !lon.equals(""))
                                    listener.OnGetGpsStatusResponse(hostBean, true, tag);
                                else
                                    listener.OnGetGpsStatusResponse(hostBean, false, tag);
                            }
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_CAMERA_GETDEVICESINFO) {
                            JSONArray jsonobj;
                            try {
                                if(json!=null){
                                    jsonobj = new JSONArray(json.getString("deviceInfo").toString());
                                    JSONObject ob = (JSONObject) jsonobj.get(0);
                                    if (listener != null)
                                        Log.e("AAA", "获取设备id");
                                    listener.OnGetDeviceInfo(hostBean, ob.getString(
                                            "deviceId").toString(), tag);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (type == com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType.COMMAND_PHOTO_MODE) {//相机模式设置，增加容错，防止错误解析影响到预览功能

                        } else {//预览命令解析
                            JSONObject mediajson;
                            try {
                                if(json!=null){
                                    mediajson = new JSONObject(json.getString("media").toString());
                                    picUrl = mediajson.get("url").toString();
                                    picThumbUrl = mediajson.get("thumbUrl").toString();
                                    PreferencesUtils.saveSpText(mContext, "pictureUri",
                                            picThumbUrl);
                                    PreferencesUtils.saveSpText(mContext,
                                            "pictureThumbUri", picUrl);
                                    mHandle.sendEmptyMessage(SensorUtils.HADNLE_SNAPPICTURE);
                                    if (Command.createBitmap) {
                                        getbitmap(mContext, hostBean, picThumbUrl, false, listener, tag);
                                    }
                                }
                                // Toast.makeText(mContext, "成功" + picThumbUrl,Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
    }

    protected static Listener<Bitmap> responseBitmapListener(
            final CameraEventListener listener, final String url,
            final Boolean isThumb, final HostBean hostBean, final int tag) {
        return new Response.Listener<Bitmap>() {

            @Override
            public void onResponse(Bitmap bitmap) {
                File file = new File(url);
                if (!isThumb) {
                    listener.OnSnapPictureResponse(hostBean,
                            bitmap,
                            CameraGarminVirbXE.mPicUuid
                                    + "_thumbnail."
                                    + SensorUtils.getExtensionName(file
                                    .getName()), tag);
                    String url = PreferencesUtils.getSpText(mContext,
                            "pictureThumbUri").toString();
                    getbitmap(mContext, hostBean, url, true, listener, tag);
                } else {
                    SaveFileRunnable runable = new SaveFileRunnable(bitmap,
                            CameraGarminVirbXE.mPicUuid
                                    + "."
                                    + SensorUtils.getExtensionName(file
                                    .getName()));
                    Thread th = new Thread(runable);
                    th.start();
                }
            }
        };
    }

    /**
     * String 返回错误监听
     *
     * @param listener String 接口
     * @return
     */
    protected static Response.ErrorListener responseError(
            final CameraEventListener listener, final Boolean isDebug, final com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE.enmCommandType commandType, final HostBean hostBean, final int tag) {
        return new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError e) {
                try {
                    if (isDebug) {
                        mHandle.sendEmptyMessage(SensorUtils.HADNLE_STATUS_FAIL);
                    } else {
                        if (listener != null)
                            listener.requestError(hostBean, e, commandType,tag);
                        currentFailCount++;
                        if (currentFailCount == failCount)
                            mHandle.sendEmptyMessage(SensorUtils.HADNLE_CONNECT_FAIL);
                    }
                } catch (Exception e1) {
                    Log.e("AAA",e1.getStackTrace()+"");
                }
            }
        };
    }

    public static void addRequest(Request<?> request, Object tag) {
        if (tag != null) {
            request.setTag(tag);
        }
        mRequestQueue.add(request);
    }

    /**
     * 当主页面调用协议 在结束该页面调用此方法
     *
     * @param tag
     */
    public static void cancelAll(Object tag) {
        mRequestQueue.cancelAll(tag);
    }

    public static class SaveFileRunnable implements Runnable {
        private Bitmap mBitmap;
        private String fileName;

        public SaveFileRunnable(Bitmap mBitmap, String fileName) {
            this.mBitmap = mBitmap;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try {
                SensorUtils.saveFile(mBitmap, fileName + ".temp");
                String savePath = CameraGarminVirbXE.getCameraPcitureSavePath();
                File oldName = new File(savePath + fileName + ".temp");

                if (oldName != null) {
                    oldName.renameTo(new File(savePath + fileName));
                }
                Log.e("AAA", "保存图片成功");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}

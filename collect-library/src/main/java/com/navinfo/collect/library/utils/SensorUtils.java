package com.navinfo.collect.library.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.text.format.DateUtils;

import com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE;

import org.apache.commons.lang3.StringUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class SensorUtils {
    public static String STR_CAMERA_PICTURE_SAVEPATH = Environment
            .getExternalStorageDirectory() + "/Sensor/Camera/DCIM/";
    public static String ntpHosts = "0.ir.pool.ntp.org,1.ir.pool.ntp.org,2.ir.pool.ntp.org,3.ir.pool.ntp.org";

    // 由于连接命令和扫描测试通讯命令相同，需要通过不通返回来确认。
    public final static int HADNLE_STATUS_OK = 1; // 获取状态成功返回handle
    public final static int HADNLE_STATUS_FAIL = 0; // 获取状态失败返回handle
    public final static int HADNLE_CONNECT_OK = 2; // 连接成功返回handle
    public final static int HADNLE_CONNECT_FAIL = 3; // 连接失败返回handle
    public final static int HADNLE_SNAPPICTURE = 4; // 失败返回handle

    public final static int HADNLE_MONITORING = 5; // 状态监控失败返回handle

    /**
     * 保存文件
     *
     * @param bm
     * @param fileName
     * @throws IOException
     */
    public static void saveFile(Bitmap bm, String fileName) throws IOException {
        String savePath = CameraGarminVirbXE.getCameraPcitureSavePath();
        File dirFile = new File(savePath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        File myCaptureFile = new File(savePath + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(myCaptureFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        bos.flush();
        bos.close();
    }

    /*
     * Java文件操作 获取文件扩展名
     */
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    /*
     * Java文件操作 获取不带扩展名的文件名
     */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    /**
     * 检测网络是否连接
     *
     * @return
     */
    public static boolean isNetworkAvailable(Context context)
    {
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null){
            return false;
        }else{
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0){
                for (int i = 0; i < networkInfo.length; i++){
                    System.out.println(i + "===状态===" + networkInfo[i].getState());
                   // System.out.println(i + "===类型===" + networkInfo[i].getTypeName());
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 同步ntp服务器的网络时间
     * @return
     */
    public static Date syncNow(){
        String[] hostsArrayNTP  = null;
        if(StringUtils.isNotBlank(ntpHosts)){
            hostsArrayNTP =ntpHosts.trim().split(",");
        }

        Date dateNTP = com.navinfo.collect.library.utils.DateUtils.getNTPDate(hostsArrayNTP, 5000);

        return dateNTP;
    }
}

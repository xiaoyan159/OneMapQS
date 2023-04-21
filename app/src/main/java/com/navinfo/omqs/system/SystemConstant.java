package com.navinfo.omqs.system;

import java.util.UUID;

/**
 * 系统变量对象
 */
public class SystemConstant {

    public static String USER_ID = "1";

    //选择相机默认或者外设
    public static String SELECT_CAMERA_STATE = "select_camera_state";
    //是否连接
    public static String CAMERA_CONNECT_STATE = "camera_connect_state";
    //是否可以点击
    public static String CAMERA_CLICK_STATE = "camera_click_state";
    //拍照模式
    public static String TAKE_CAMERA_MODE = "take_camera_mode";

    public static String TAKE_CAMERA_IP = "take_camera_ip";

    public static String TAKE_CAMERA_MAC = "take_camera_mac";


    //选择拍照或者录像
    public static String SELECT_TAKEPHOTO_OR_RECORD = "select_takephoto_or_record";

    /**
     * 获取uuid
     * @param isUpperCase
     * 			true 大写  false 小写
     */
    public static String getUuid(boolean isUpperCase){
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        if(isUpperCase)
            uuid = uuid.toUpperCase();

        return uuid;
    }

}

package com.navinfo.collect.library.sensor;

import com.navinfo.collect.library.garminvirbxe.SensorParams;

public abstract class ISensor {

    public enum enmSensorType {
        SENSOR_LOCATION, /* !< 位置传感器 */
        SENSOR_CAMEAR /* !< 图像传感器 */

    };

    public enum enmSensorModel {
        LOCATION_GPS_INTERANL, /* !< 设备内部GPS */
        LOCATION_GPS_BLUETOOTH, /* !< 蓝牙GPS */
        LOCATION_SINS,
        LOCATION_BAIDU, /* !< BaiDu location api */
        CAMERA_GARMIN_VIRB_XE, /* !< Garmin Virb xe 运动相机 */
        CAMERA_UNKNOWN, /* !< 未知相机类型 */
        LOCATION_SINS_BAIDU_MIX /* 百度和惯导混合定位*/
    }


    //连接状态枚举
    public enum enmConnectionStatus {

        DISCONNECTTED/*断开连接*/, CONNECTTING/*连接中*/, CONNECTTED/*连接*/

    }


    //信号质量枚举
    public enum enmSignalQuality {
        NO_SIGNAL/*无信号*/, POOR/*弱信号*/, MODERATE/*中等信号*/, PERFECT/*强信号*/
    }



    public enum SensorWorkingMode {
        LOCATION_GPS_INTERNAL, /* !< 设备内部GPS */
        LOCATION_GPS_BLUETOOTH, /* !< 外接蓝牙GPS */
        LOCATION_GPS_HYBIRD, /* !< 混合模式 */

        CAMEAR_PHOTO_12MP, /* !< 相机拍照模式 1200万像素 */
        CAMEAR_PHOTO_7MP, /* !< 相机拍照模式 7百万像素 */
        CAMEAR_PHOTO_CONTINUOUS_PHOTO, /* !< 相机自身连拍模式 */

        CAMERA_VEDIO_1080P, /* !< 相机录像模式 1008P */
        CAMEAR_VEDIO_720P, /* !< 相机录像模式 720P */
        CAMERA_VEDIO_TIMELAPSE, /* !< 相机录像模式 缩时 */

        CAMERA_PHOTO_SINGLE/*单拍模式*/
    }


    public abstract enmSensorType GetSensorType();

    public abstract enmConnectionStatus GetConnectionStatus();

    public abstract enmConnectionStatus Connect(SensorParams params);

    public abstract enmConnectionStatus DisConnect();

    public abstract enmSignalQuality GetSignalQuality();

    public abstract void GetGpsStatus();

    public abstract void snapPicture(String picuuid);

    public abstract void Search(boolean isReadCache);

    public abstract void StopSearch();

    public abstract void StartRecording();

    public abstract void StopRecording();

    public abstract int RegisterSensorEvent(
            SensorEventListener sensorEventLister);

    public abstract void SetMode(SensorWorkingMode sensorWorkingMode);

}

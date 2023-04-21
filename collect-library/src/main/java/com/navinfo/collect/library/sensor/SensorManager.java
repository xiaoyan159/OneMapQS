package com.navinfo.collect.library.sensor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.navinfo.collect.library.garminvirbxe.CameraGarminVirbXE;
import com.navinfo.collect.library.garminvirbxe.SensorParams;
import com.navinfo.collect.library.sensor.ISensor.enmSensorModel;
import com.navinfo.collect.library.sensor.ISensor.enmSensorType;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SensorManager {

    private static volatile SensorManager mInstance;

    private Context mContext;

    private long diffTime = 0;

    Handler mainHandler = new Handler(Looper.getMainLooper());

    public static SensorManager getInstance() {
        if (mInstance == null) {
            synchronized (SensorManager.class) {
                if (mInstance == null) {
                    mInstance = new SensorManager();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        mContext = context;
        diffTime = context.getSharedPreferences("nowTime", Context.MODE_PRIVATE).getLong("THE_DIFFERENCE_VALUE", 0);
    }

    public static ISensor CreateSensor(Context mContext, enmSensorType type, enmSensorModel model) {

        ISensor pSensorInstance = null;

        if (type == enmSensorType.SENSOR_CAMEAR) { // Garmin 相机
            if (model == enmSensorModel.CAMERA_GARMIN_VIRB_XE) {
                pSensorInstance = new CameraGarminVirbXE(mContext);
            } else {
                pSensorInstance = new Camera(mContext);
            }
        }

        return pSensorInstance;
    }

    public void setDiffTime(long diffValue) {
        this.diffTime = diffValue;
    }

}

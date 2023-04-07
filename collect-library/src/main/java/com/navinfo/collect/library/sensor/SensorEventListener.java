package com.navinfo.collect.library.sensor;

import com.navinfo.collect.library.sensor.ISensor.enmConnectionStatus;
import com.navinfo.collect.library.sensor.ISensor.enmSignalQuality;

public interface SensorEventListener {

    public void OnSensorEvent();

    public void OnConnectionStatusChanged(enmConnectionStatus cs);

    public void OnSignalQualityChanged(enmSignalQuality sq);

}

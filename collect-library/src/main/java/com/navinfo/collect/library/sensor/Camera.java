package com.navinfo.collect.library.sensor;

import android.content.Context;

import com.navinfo.collect.library.garminvirbxe.Command;
import com.navinfo.collect.library.garminvirbxe.SensorParams;

public class Camera extends ISensor {

    private Context mContext;
    private Command conmand;

    public Camera(Context mContext) {
        this.mContext = mContext;
        conmand = new Command(mContext);
    }

    @Override
    public enmSensorType GetSensorType() {
        return enmSensorType.SENSOR_CAMEAR;
    }

    @Override
    public enmConnectionStatus GetConnectionStatus() {
        return null;
    }

    @Override
    public enmConnectionStatus Connect(SensorParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public enmConnectionStatus DisConnect() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public enmSignalQuality GetSignalQuality() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void snapPicture(String picuuid) {

    }


    @Override
    public void StopRecording() {
        conmand.StopRecording(null,0);
    }


    @Override
    public void StartRecording() {
        conmand.StartRecording(null,0);
    }

    @Override
    public int RegisterSensorEvent(SensorEventListener sensorEventLister) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void SetMode(SensorWorkingMode sensorWorkingMode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void Search(boolean isReadCache) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void StopSearch() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void GetGpsStatus() {
        // TODO Auto-generated method stub
        
    }

    public void OnConnectionStatusChanged(enmConnectionStatus cs){

    }
}

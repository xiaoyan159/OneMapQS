package com.navinfo.collect.library.data.entity;

import android.os.Bundle;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * 表示地图的地理位置类。
 */
@Entity(tableName = "niLocation", indices = {@Index("tilex"),@Index("tiley")})
public class NiLocation extends Feature{
    @ColumnInfo(name = "longitude")
    private double longitude;//纬度坐标
    @ColumnInfo(name = "latitude")
    private double latitude;//经度坐标
    @ColumnInfo(name = "direction")
    private double direction;//定位方位
    @ColumnInfo(name = "altitude")
    private double altitude;//海拔高度 如果返回0.0，说明没有返回海拔高度
    @ColumnInfo(name = "radius")
    private double radius;//定位精度
    @ColumnInfo(name = "time")
    private String time;//定位时间 毫秒时间（距离1970年 1月 1日 00:00:00 GMT的时间）
    @ColumnInfo(name = "adCode")
    private String adCode;// 区域编码
    @ColumnInfo(name = "country")
    private String country;// 所属国家
    @ColumnInfo(name = "province")
    private String province;// 所属省名称
    @ColumnInfo(name = "city")
    private String city;// 所属城市名称
    @ColumnInfo(name = "district")
    private String district;// 所属区（县）名称
    @ColumnInfo(name = "cityCode")
    private String cityCode;// 城市编码
    @ColumnInfo(name = "provider")
    private String provider;// 提供者名称
    @ColumnInfo(name = "speed")
    private float speed;// 定位速度
    @ColumnInfo(name = "floor")
    private String floor;// 楼层
    @ColumnInfo(name = "poiId")
    private String poiId;// 室内地图POI的id
    @ColumnInfo(name = "satelliteNumber")
    private int satelliteNumber;// 卫星数目
    @ColumnInfo(name = "address")
    private String address;// 地址
    @ColumnInfo(name = "street")
    private String street;// 乡镇
    @ColumnInfo(name = "town")
    private String town;// 街道
    @ColumnInfo(name = "streetNumber")
    private String streetNumber;// 街道号码
    @ColumnInfo(name = "errorInfo")
    private String errorInfo;// 定位失败描述信息
    @ColumnInfo(name = "errorCode")
    private String errorCode;// 错误码
    @ColumnInfo(name = "tilex")
    private int tilex;
    @ColumnInfo(name = "tiley")
    private int tiley;
    @ColumnInfo(name = "groupId")
    private String groupId;
    @ColumnInfo(name = "timeStamp")
    private String timeStamp;
    @ColumnInfo(name = "media")
    private int media;
    @ColumnInfo(name = "taskId")
    private String taskId;

    private boolean isAccouracy;
    private boolean isSpeed;
    private boolean isAltitude;
    private boolean isBearing;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAdCode() {
        return adCode;
    }

    public void setAdCode(String adCode) {
        this.adCode = adCode;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getPoiId() {
        return poiId;
    }

    public void setPoiId(String poiId) {
        this.poiId = poiId;
    }

    public int getSatelliteNumber() {
        return satelliteNumber;
    }

    public void setSatelliteNumber(int satelliteNumber) {
        this.satelliteNumber = satelliteNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isAccouracy() {
        return isAccouracy;
    }

    public void setAccouracy(boolean accouracy) {
        isAccouracy = accouracy;
    }

    public boolean isSpeed() {
        return isSpeed;
    }

    public void setSpeed(boolean speed) {
        isSpeed = speed;
    }

    public boolean isAltitude() {
        return isAltitude;
    }

    public void setAltitude(boolean altitude) {
        isAltitude = altitude;
    }

    public boolean isBearing() {
        return isBearing;
    }

    public void setBearing(boolean bearing) {
        isBearing = bearing;
    }

    public int getTilex() {
        return tilex;
    }

    public void setTilex(int tilex) {
        this.tilex = tilex;
    }

    public int getTiley() {
        return tiley;
    }

    public void setTiley(int tiley) {
        this.tiley = tiley;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getMedia() {
        return media;
    }

    public void setMedia(int media) {
        this.media = media;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}

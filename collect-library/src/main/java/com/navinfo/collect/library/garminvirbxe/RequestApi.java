package com.navinfo.collect.library.garminvirbxe;

import java.util.HashMap;

/**
 * @author dongpuxiao
 * @version V1.0
 * @ClassName: RequestApi
 * @Date 2017/1/12
 * @Description: ${TODO}(请求地址类)
 */
public class RequestApi {
    // 该类用于管理http请求地址以及接口函数
    private static String apiWifiIp;
    //缓存多地址集合
    private static HashMap<Integer,String> hashMap = new HashMap<Integer,String>();

    // 获取测试服务器接口
    public static String getApiUri(int key) {
        String apiIp = hashMap.get(key);
        if (apiIp == null || "".equals(apiIp))
            return "http://192.168.0.1/virb";
        else
            return "http://"+apiIp+"/virb";

    }

    // 获取测试服务器接口
    public static String getApiMediaUri(int key) {
        String apiIp = hashMap.get(key);
        if (apiIp == null || "".equals(apiIp))
            return "http://192.168.0.1";
        else
            return "http://"+apiIp+"";

    }

    //设置地址ip
    public static void setApiIp(int key,String ip) {
        hashMap.put(key,ip);
    }

    //获取wifi测试地址ip
    public static String getApiWifiIp() {
        if (apiWifiIp == null || "".equals(apiWifiIp))
            return "http://192.168.0.1/virb";
        else
            return "http://"+apiWifiIp+"/virb";
    }

    //设置wifi测试ip
    public static void setApiWifiIp(String apiWifiIp) {
        RequestApi.apiWifiIp = apiWifiIp;
    }
}

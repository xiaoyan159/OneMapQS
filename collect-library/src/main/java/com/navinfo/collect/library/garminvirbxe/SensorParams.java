package com.navinfo.collect.library.garminvirbxe;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorParams extends JSONObject {

    private JSONObject params;

    public SensorParams() {
        init();
    }

    public SensorParams(String key, String value) {
        init();
        put(key, value);
    }

    private void init() {
        params = new JSONObject();
    }

    /**
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        if (key != null && value != null) {
            try {
                params.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public JSONObject getParams() {
        return params;
    }

}

package com.navinfo.collect.library.utils;

public class NetScanPrefs {
    
    public final static String KEY_RESOLVE_NAME = "resolve_name";
    public final static boolean DEFAULT_RESOLVE_NAME = true;

    public static final String KEY_RATECTRL_ENABLE = "ratecontrol_enable";
    public static final boolean DEFAULT_RATECTRL_ENABLE = true;

    public final static String KEY_TIMEOUT_DISCOVER = "timeout_discover";
    public final static String DEFAULT_TIMEOUT_DISCOVER = "500";

    public static final String KEY_INTF = "interface";
    public static final String DEFAULT_INTF = null;

    public static final String KEY_IP_START = "ip_start";
    public static final String DEFAULT_IP_START = "0.0.0.0";

    public static final String KEY_IP_END = "ip_end";
    public static final String DEFAULT_IP_END = "0.0.0.0";

    public static final String KEY_IP_CUSTOM = "ip_custom";
    public static final boolean DEFAULT_IP_CUSTOM = false;
    
    public static final String KEY_CIDR_CUSTOM = "cidr_custom";
    public static final boolean DEFAULT_CIDR_CUSTOM = false;

    public static final String KEY_CIDR = "cidr";
    public static final String DEFAULT_CIDR = "24";

}

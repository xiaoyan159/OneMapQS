package com.navinfo.collect.library.garminvirbxe;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.navinfo.collect.library.utils.NetScanInfo;
import com.navinfo.collect.library.utils.NetScanPrefs;
import com.navinfo.collect.library.utils.PreferencesUtils;
import com.navinfo.collect.library.utils.SensorUtils;

import static com.navinfo.collect.library.utils.NetScanInfo.NOMAC;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WifiDiscovery extends AsyncTask<Void, HostBean, Void> {

    private final String TAG = "DefaultDiscovery";
    private final static int[] DPORTS = {139, 445, 22, 80};
    private final static int TIMEOUT_SCAN = 3600; // seconds
    private final static int TIMEOUT_SHUTDOWN = 10; // seconds
    private final static int THREADS = 25; // FIXME: Test, plz set in options
    // again ?
    private final int mRateMult = 5; // Number of alive hosts between Rate
    private int pt_move = 2; // 1=backward 2=forward
    private ExecutorService mPool;
    private boolean doRateControl;
    private RateControl mRateControl;
    private int mSearchStatus = 0; // 0为使用缓存ip进行连接测试， 1，从新扫描ip进行连接测试。
    private boolean mBstatus = false;

    protected long ip;
    protected long start = 0;
    protected long end = 0;
    protected long size = 0;

    protected int hosts_done = 0;
    private Context mContext;
    private Command command;
    private int addressIndex = 0;
    private SharedPreferences prefs;
    private NetScanInfo net;
    private CameraEventListener cameraEventListener;
    private ArrayList<HostBean> scanHostBeanList;
    private ArrayList<HostBean> hosts;
    private ArrayList<HostBean> hostsCache;
    private int tag;

    public WifiDiscovery(Context mContext,
                         CameraEventListener cameraEventListener, boolean isReadCache) {
        this.mContext = mContext;
        this.cameraEventListener = cameraEventListener;
        command = new Command(mContext);
        command.SetHandle(mHandler);
        scanHostBeanList = new ArrayList<HostBean>();
        hosts = new ArrayList<HostBean>();
        hostsCache = new ArrayList<HostBean>();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        net = new NetScanInfo(mContext);
        mRateControl = new RateControl();
        setScanInfo();
        if (isReadCache)
            getLocalIpSend();
        else{
            mBstatus = true;
            Log.e("mBstatus","WifiDiscovery==="+mBstatus);
        }

    }

    //优先本地缓存记录连接
    private void getLocalIpSend() {
        String cacheHostBean = PreferencesUtils.getSpText(mContext, "cacheHostBean").toString();
        if (!"".equals(cacheHostBean)) {
            mSearchStatus = 0;
            try {
                JSONArray jsonArray = new JSONArray(cacheHostBean);
                if (jsonArray != null && jsonArray.length() > 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jobj = jsonArray.getJSONObject(i);
                        if (jobj != null) {
                            HostBean hostBean = new HostBean();
                            if (jobj.has("ipAddress"))
                                hostBean.ipAddress = jobj.getString("ipAddress");
                            if (jobj.has("hardwareAddress"))
                                hostBean.hardwareAddress = jobj.getString("hardwareAddress");
                            hostsCache.add(hostBean);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("AAA", "getLocalIpSend：异常" + e.toString());
            }
            //如果缓存转换失败，重新执行扫描
            if (hostsCache == null || hostsCache.size() == 0) {
                mBstatus = true;
            } else {
                //不启动扫描
                mBstatus = false;
                Log.e("mBstatus","getLocalIpSend==="+mBstatus);
                sendConnectCommand();
            }
        } else {
            mBstatus = true;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SensorUtils.HADNLE_STATUS_OK:
                    scanHostBeanList.add(mCurrentHostBean);
                    sendCommand();
                    break;
                case SensorUtils.HADNLE_STATUS_FAIL:
                    //使用缓存获取状态失败了，代表ip发生编化，需要重新扫描
                    if (mSearchStatus == 0) {
                        //重置索引
                        addressIndex = 0;
                        scanHostBeanList.clear();
                        hostsCache.clear();
                        mBstatus = true;
                        //扫描失败后重新启动搜索
                        mSearchStatus = 0;
                        pingAllIp();
                    } else {
                        sendCommand();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private HostBean mCurrentHostBean;

    private void setScanInfo() {
        ip = NetScanInfo.getUnsignedLongFromIp(net.ip);
        if (prefs.getBoolean(NetScanPrefs.KEY_IP_CUSTOM,
                NetScanPrefs.DEFAULT_IP_CUSTOM)) {
            // Custom IP
            start = NetScanInfo.getUnsignedLongFromIp(prefs.getString(
                    NetScanPrefs.KEY_IP_START, NetScanPrefs.DEFAULT_IP_START));
            end = NetScanInfo.getUnsignedLongFromIp(prefs.getString(
                    NetScanPrefs.KEY_IP_END, NetScanPrefs.DEFAULT_IP_END));
        } else {
            // Custom CIDR
            if (prefs.getBoolean(NetScanPrefs.KEY_CIDR_CUSTOM,
                    NetScanPrefs.DEFAULT_CIDR_CUSTOM)) {
                net.cidr = Integer.parseInt(prefs.getString(
                        NetScanPrefs.KEY_CIDR, NetScanPrefs.DEFAULT_CIDR));
            }
            // Detected IP
            int shift = (32 - net.cidr);
            if (net.cidr < 31) {
                start = (ip >> shift << shift) + 1;
                end = (start | ((1 << shift) - 1)) - 1;
            } else {
                start = (ip >> shift << shift);
                end = (start | ((1 << shift) - 1));
            }
            // Reset ip start-end (is it really convenient ?)
            Editor edit = prefs.edit();
            edit.putString(NetScanPrefs.KEY_IP_START,
                    NetScanInfo.getIpFromLongUnsigned(start));
            edit.putString(NetScanPrefs.KEY_IP_END,
                    NetScanInfo.getIpFromLongUnsigned(end));
            edit.commit();
        }

    }

    public void setNetwork(long ip, long start, long end) {
        this.ip = ip;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void onPreExecute() {
        size = (int) (end - start + 1);
        scanHostBeanList.clear();
        doRateControl = prefs.getBoolean(NetScanPrefs.KEY_RATECTRL_ENABLE,
                NetScanPrefs.DEFAULT_RATECTRL_ENABLE);
    }

    @Override
    protected void onProgressUpdate(final HostBean... host) {
        if (!isCancelled()) {
            if (host[0] != null) {
                host[0].position = hosts.size();
                //android 10 获取不到mac地址，所以不进行过滤
                if (Build.VERSION.SDK_INT >= 29 || !host[0].hardwareAddress.equalsIgnoreCase(NOMAC)/*&&host[0].hardwareAddress.startsWith("14:")*/)
                    hosts.add(host[0]);
                Log.e("AAA", "hardwareAddress" + host[0].hardwareAddress + "\n" + host[0].hostname);
            }
            if (size > 0) {
                // discover.setProgress((int) (hosts_done * 10000 / size));
            }
        }
    }

    @Override
    protected void onPostExecute(Void unused) {
        Log.e("AAA", "扫描完成");
        sendConnectCommand();
    }

    @Override
    protected Void doInBackground(Void... params) {
        //与董普校讨论去掉while，调用时自己控制
        //while (true) {
        Log.e("mBstatus", "doInBackground===" + mBstatus);
            /*if (mBstatus) {
                if (mSearchStatus == 1)
                    return null;*/
            pingAllIp();
        //}
        return null;
    }

    private void pingAllIp() {
        if (mBstatus) {
            if (mSearchStatus == 1)
                return;
        }
        mSearchStatus = 1;
        Log.v(TAG, "start=" + NetScanInfo.getIpFromLongUnsigned(start) + " ("
                + start + "), end=" + NetScanInfo.getIpFromLongUnsigned(end)
                + " (" + end + "), length=" + size);
        mPool = Executors.newFixedThreadPool(THREADS);
        if (ip <= end && ip >= start) {
            Log.i(TAG, "Back and forth scanning");
            // gateway
            launch(start);

            // hosts
            long pt_backward = ip;
            long pt_forward = ip + 1;
            long size_hosts = size - 1;

            if (size_hosts > 0) {
                for (int i = 0; i < size_hosts; i++) {
                    // Set pointer if of limits
                    if (pt_backward <= start) {
                        pt_move = 2;
                    } else if (pt_forward > end) {
                        pt_move = 1;
                    }
                    // Move back and forth
                    if (pt_move == 1) {
                        launch(pt_backward);
                        pt_backward--;
                        pt_move = 2;
                    } else if (pt_move == 2) {
                        launch(pt_forward);
                        pt_forward++;
                        pt_move = 1;
                    }
                }
            }

        } else {
            Log.i(TAG, "Sequencial scanning");
            for (long i = start; i <= end; i++) {
                launch(i);
            }
        }

        mPool.shutdown();
        //暂时注释掉线程池管理类阻塞住，等待线程返回的相关代码
        try {
            if (!mPool.awaitTermination(TIMEOUT_SCAN, TimeUnit.SECONDS)) {
                mPool.shutdownNow();
                Log.e(TAG, "Shutting down pool");
                if (!mPool.awaitTermination(TIMEOUT_SHUTDOWN, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
            mPool.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
        }


    }

    @Override
    protected void onCancelled() {
        if (mPool != null) {
            synchronized (mPool) {
                mPool.shutdownNow();
            }
        }
        super.onCancelled();
    }

    private void launch(long i) {
        if (!mPool.isShutdown()) {
            mPool.execute(new CheckRunnable(NetScanInfo
                    .getIpFromLongUnsigned(i)));
        }
    }

    private int getRate() {
        if (doRateControl) {
            return mRateControl.rate;
        }

        return Integer.parseInt(prefs.getString(
                NetScanPrefs.KEY_TIMEOUT_DISCOVER,
                NetScanPrefs.DEFAULT_TIMEOUT_DISCOVER));
    }

    private class CheckRunnable implements Runnable {
        private String addr;
        private String hardwareAddress;

        CheckRunnable(String addr) {
            this.addr = addr;
        }

        @Override
        public void run() {
            // Log.e(TAG, "run=" + addr);
            // Create host object

            final HostBean host = new HostBean();
            host.responseTime = getRate();
            host.ipAddress = addr;

            try {

                InetAddress h = InetAddress.getByName(addr);
                // Rate control check
                if (doRateControl && mRateControl.indicator != null
                        && hosts_done % mRateMult == 0) {
                    mRateControl.adaptRate();
                }
                // Arp Check #1
                //android10 不能访问proc/net 目录
                if (Build.VERSION.SDK_INT < 29) {
                    hardwareAddress = getHardwareAddress(addr);
                }
                if (hardwareAddress != null && !NOMAC.equals(hardwareAddress)) {
                    host.hardwareAddress = hardwareAddress;
                    Log.e(TAG, "CheckRunnable" + addr);
                    Log.e(TAG, "CheckRunnable" + host.hardwareAddress + "===" + hardwareAddress);
                    publish(host);
                    return;
                }
                // Native InetAddress check
                if (h.isReachable(getRate())) {
                    Log.e(TAG, "found using InetAddress ping " + addr);
                    publish(host);
                    if (doRateControl && mRateControl.indicator == null) {
                        mRateControl.indicator = addr;
                        mRateControl.adaptRate();
                    }
                    return;
                }


            } catch (IOException e) {
                publish(null);
                Log.e(TAG, e.getMessage());
            }
        }

    }

    //解析地址
    public String getHardwareAddress(String ip) {
        try {
            if (ip != null) {
                String hw = NOMAC;
                String ptrn = String.format(NetScanInfo.MAC_RE, ip.replace(".", "\\."));
                Pattern pattern = Pattern.compile(ptrn);
                BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"), NetScanInfo.BUF);
                String line;
                Matcher matcher;
                while ((line = bufferedReader.readLine()) != null) {
                    matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hw = matcher.group(1);
                        break;
                    }
                }
                bufferedReader.close();
                return hw;
            } else {
                Log.e(TAG, "ip is null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't open/read file ARP: " + e.getMessage());
            return NOMAC;
        }
        return NOMAC;
    }

    private void publish(final HostBean host) {
        hosts_done++;
        if (host == null) {
            publishProgress((HostBean) null);
            return;
        }

        // if (mDiscover != null) {
        // final ActivityNet discover = mDiscover.get();
        // if (discover != null) {
        // Is gateway ?
        if (net.gatewayIp.equals(host.ipAddress)) {
            host.deviceType = HostBean.TYPE_GATEWAY;
        }

        // FQDN
        // Static
        if (host.hostname == null) {
            // DNS
            if (prefs.getBoolean(NetScanPrefs.KEY_RESOLVE_NAME,
                    NetScanPrefs.DEFAULT_RESOLVE_NAME) == true) {
                try {
                    host.hostname = (InetAddress.getByName(host.ipAddress))
                            .getCanonicalHostName();
                } catch (UnknownHostException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

        publishProgress(host);
    }

    // 发送测试命令
    private void sendConnectCommand() {
        sendCommand();
    }

    /**
     * 发送
     */
    private void sendCommand() {
        if (!mBstatus && addressIndex < hostsCache.size()) {
            mCurrentHostBean = hostsCache.get(addressIndex);
            addressIndex++;
            //Log.e(TAG, "ipAddress:" + mCurrentHostBean.ipAddress.toString());
            RequestApi.setApiWifiIp(mCurrentHostBean.ipAddress.toString());
            command.getWifiTestStatus(mCurrentHostBean, getTag());
        } else if (mBstatus && addressIndex < hosts.size()) {
            mCurrentHostBean = hosts.get(addressIndex);
            addressIndex++;
            //Log.e(TAG, "ipAddress:" + mCurrentHostBean.ipAddress.toString());
            RequestApi.setApiWifiIp(mCurrentHostBean.ipAddress.toString());
            command.getWifiTestStatus(mCurrentHostBean, getTag());
        } else {
            if (scanHostBeanList != null && scanHostBeanList.size() > 0) {

                JSONArray jsonArray = new JSONArray();

                for (int i = 0; i < scanHostBeanList.size(); i++) {
                    try {
                        JSONObject jobj = new JSONObject();
                        jobj.put("ipAddress", scanHostBeanList.get(i).ipAddress);
                        jobj.put("hardwareAddress", scanHostBeanList.get(i).hardwareAddress);
                        //过滤重复内容
                        if (!jsonArray.toString().contains(jobj.toString()))
                            jsonArray.put(jobj);
                    } catch (Exception e) {

                    }
                }
                PreferencesUtils.saveSpText(mContext, "cacheHostBean", jsonArray.toString());
            }
            cameraEventListener.OnSearchResponse(getTag(), scanHostBeanList);
        }
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }
}

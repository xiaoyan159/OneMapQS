package com.navinfo.omqs.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * @ClassName:     NetUtils.java
 * @author         qj
 * @version        V1.0
 * @Date           2016年12月17日 下午1:56:02
 * @Description:   网络类
 */
public class NetUtils {
	//单例对象
	private static volatile NetUtils mInstance;
	//上下文
	private Context mCon;

	public static NetUtils getInstance() {

		if (mInstance == null) {
			synchronized (NetUtils.class) {
				if (mInstance == null) {
					mInstance = new NetUtils();
				}
			}
		}
		return mInstance;
	}

	/**
	 * 初始化
	 * @param  context
	 * 			上下文
	 */
	public void init(Context context){
		
		this.mCon = context;
		

	}

	/**
	 * 是否wifi
	 * @return true 是 false 否
	 */
	public boolean isExistWifi(boolean isNeedMobile){

		//获取系统服务
		ConnectivityManager manager = (ConnectivityManager)mCon.getSystemService(Context.CONNECTIVITY_SERVICE);
		try{
			//获取状态
			final NetworkInfo.State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

			if(wifi == NetworkInfo.State.CONNECTED||wifi==NetworkInfo.State.CONNECTING){
				return true;
			}
		}catch (Exception e){
		}


		if(isNeedMobile){
			try{
				//获取状态
				final NetworkInfo mobileNetwork = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

				if(mobileNetwork!=null&&mobileNetwork.getState()!=null&& (mobileNetwork.getState()== NetworkInfo.State.CONNECTED||mobileNetwork.getState()==NetworkInfo.State.CONNECTING)){
					return true;
				}
			}catch (Exception e){
			}

		}

		return false;
	}
}

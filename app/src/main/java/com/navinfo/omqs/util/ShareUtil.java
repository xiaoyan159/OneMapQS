package com.navinfo.omqs.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.navinfo.omqs.Constant;

/**
 * @ClassName:     ShareUtil.java
 * @author         qj
 * @version        V1.0
 * @Date           2016年12月17日 下午1:56:02
 * @Description:  相机数据存储
 */
public class ShareUtil {
	//系统数据存储对象
	private static SharedPreferences mSharePre=null;
	//安卓编辑器
	private static Editor editor;
	//拍照状态标识
   	private final static String CONTINUS_TAKE_PHOTO_STATE = "continue_take_photo_state";
   	//外设相机按钮状态标识
	private final static String SELECT_TAKE_PHOTO_OR_RECORD = "select_take_photo_or_record";
	//外设相机种别标识
	private final static String SELECT_CAMERA_KIND = "select_take_kind";
	//外设相机连接标识
	private final static String CAMERA_CONNECT_STATE = "camera_connect_state";
	//外设相机工作模式
	private final static String TAKE_CAMERA_MODE = "take_camera_mode";
	//连接相机ip
	private final static String TAKE_CAMERA_IP = "take_camera_ip";
	//连接相机Mac
	private final static String TAKE_CAMERA_MAC = "take_camera_mac";
	//外设相机编号，应对多个相机连接使用，识别存储某一个设备连接状态等信息
	private int mDeviceNum = 1;
	//上下文
	private Context mContext;

	private ShareUtil() {
	}

	public ShareUtil(Context context, int deviceNum) {
		mContext  = context;
		mDeviceNum = deviceNum;
	}

	/**
	 * method : getSelectCameraKind
	 * Author : qj
	 * Describe : 获取相机类型
	 * param :  context 上下文
	 * return true 相机 false视频
	 * Date : 2018/4/23
	 */
	public boolean getSelectCameraKind(){
		if(mContext==null)
			return false;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.SELECT_CAMERA_STATE, Context.MODE_PRIVATE);
		}
		return mSharePre.getBoolean(mDeviceNum+Constant.USER_ID+SELECT_CAMERA_KIND, false);
	}

	/**
	 * method : setSelectCameraKind
	 * Author : qj
	 * Describe : 设置相机类型
	 * param :  context 上下文
	 * param : userid用户id
	 * param : true 内置相机 false 外置相机
	 * Date : 2018/4/23
	 */
	public void setSelectCameraKind(String userId,Boolean bll){
		if(mContext==null)
			return ;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.SELECT_CAMERA_STATE, Context.MODE_PRIVATE);
		}

		editor=mSharePre.edit();

		 editor.putBoolean(mDeviceNum+userId+SELECT_CAMERA_KIND,bll).commit();
	
	}

	/**
	 * method : getSelectTakePhotoOrRecord
	 * Author : qj
	 * Describe : 获取相机使用类型
	 * param :  context 上下文
	 * param : true 相机拍照 false 录像视频
	 * Date : 2018/4/23
	 */
	public boolean getSelectTakePhotoOrRecord(){
		if(mContext==null)
			return true;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.SELECT_TAKEPHOTO_OR_RECORD, Context.MODE_PRIVATE);
		}

		return mSharePre.getBoolean(mDeviceNum+Constant.USER_ID+SELECT_TAKE_PHOTO_OR_RECORD, mDeviceNum==1?true:false);

	}

	/**
	 * method : setSelectTakePhotoOrRecord
	 * Author : qj
	 * Describe : 设置相机使用类型
	 * param :  context 上下文
	 * param : userid 用户id
	 * param : true 相机拍照 false 录像视频
	 * Date : 2018/4/23
	 */
	public void setSelectTakePhotoOrRecord(String userId,Boolean bll){
		if(mContext==null)
			return ;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.SELECT_TAKEPHOTO_OR_RECORD, Context.MODE_PRIVATE);
		}
		editor=mSharePre.edit();

		 editor.putBoolean(mDeviceNum+userId+SELECT_TAKE_PHOTO_OR_RECORD,bll).commit();
	
	}

	/**
	 * method : getContinusTakePhotoState
	 * Author : qj
	 * Describe : 获取相机工作状态
	 * param :  context 上下文
	 * Date : 2018/4/23
	 */
	public boolean getContinusTakePhotoState(){
		if(mContext==null)
			return true;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.SELECT_CAMERA_STATE, Context.MODE_PRIVATE);
		}
		return mSharePre.getBoolean(mDeviceNum+Constant.USER_ID+CONTINUS_TAKE_PHOTO_STATE, true);

	}

	/**
	 * method : setContinusTakePhotoState
	 * Author : qj
	 * Describe : 设置相机工作状态
	 * param :  context 上下文
	 * param : userid 用户id
	 * param : true 停止 false 否
	 * Date : 2018/4/23
	 */
	public void setContinusTakePhotoState(String userId,Boolean bll){
		if(mContext==null)
			return ;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.SELECT_CAMERA_STATE, Context.MODE_PRIVATE);
		}
		editor=mSharePre.edit();

		 editor.putBoolean(mDeviceNum+userId+CONTINUS_TAKE_PHOTO_STATE,bll).commit();
	}

	/**
	 * method : getConnectstate
	 * Author : qj
	 * Describe : 获取相机连接状态
	 * param :  context 上下文
	 * Date : 2018/4/23
	 */
	public boolean getConnectstate(){
		if(mContext==null)
			return false;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.CAMERA_CONNECT_STATE, Context.MODE_PRIVATE);
		}
		return mSharePre.getBoolean(mDeviceNum+Constant.USER_ID+CAMERA_CONNECT_STATE, false);
	}

	/**
	 * method : setConnectstate
	 * Author : qj
	 * Describe : 设置相机连接状态
	 * param :  context 上下文
	 * param : userid 用户id
	 * param : true 连接 false 否
	 * Date : 2018/4/23
	 */
	public void setConnectstate(String userId,Boolean bll){
		if(mContext==null)
			return ;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.CAMERA_CONNECT_STATE, Context.MODE_PRIVATE);
		}
		editor=mSharePre.edit();

		editor.putBoolean(mDeviceNum+userId+CAMERA_CONNECT_STATE,bll).commit();
	}

	/**
	 * method : getTakeCameraMode
	 * Author : qj
	 * Describe : 获取相机模式
	 * param :  context 上下文
	 * Date : 2018/4/23
	 */
	public int getTakeCameraMode(){
		if(mContext==null)
			return 0;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.TAKE_CAMERA_MODE, Context.MODE_PRIVATE);
		}

		return mSharePre.getInt(mDeviceNum+Constant.USER_ID+TAKE_CAMERA_MODE, mDeviceNum==1?0:1);

	}

	/**
	 * method : setTakeCameraMode
	 * Author : qj
	 * Describe : 设置相机模式
	 * param :  context 上下文
	 * param : userid 用户id
	 * param : int 0 视频 1 拍照
	 * Date : 2018/4/23
	 */
	public void setTakeCameraMode(String userId,int mode){
		if(mContext==null)
			return ;


		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.TAKE_CAMERA_MODE, Context.MODE_PRIVATE);
		}
		editor=mSharePre.edit();

		editor.putInt(mDeviceNum+userId+TAKE_CAMERA_MODE,mode).commit();
	}

	/**
	 * method : getTakeCameraIP
	 * Author : qj
	 * Describe : 获取相机ip
	 * param :  context 上下文
	 * Date : 2018/4/23
	 */
	public  String getTakeCameraIP(){
		if(mContext==null)
			return "";

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.TAKE_CAMERA_IP, Context.MODE_PRIVATE);
		}
		String ip=mSharePre.getString(mDeviceNum+Constant.USER_ID+TAKE_CAMERA_IP, "");
		return ip;
	}

	/**
	 * method : setTakeCameraIP
	 * Author : qj
	 * Describe : 设置相机ip
	 * param :  context 上下文
	 * param : userid 用户id
	 * param : ip 连接地址
	 * Date : 2018/4/23
	 */
	public void setTakeCameraIP(String userId,String ip){
		if(mContext==null)
			return ;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.TAKE_CAMERA_IP, Context.MODE_PRIVATE);
		}

		editor=mSharePre.edit();

		editor.putString(mDeviceNum+userId+TAKE_CAMERA_IP,ip).commit();
	}

	/**
	 * method : getTakeCameraMac
	 * Author : qj
	 * param : mac 硬件信息
	 * param :  context 上下文
	 * Date : 2018/4/23
	 */
	public  String getTakeCameraMac(){
		if(mContext==null)
			return "";

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.TAKE_CAMERA_MAC, Context.MODE_PRIVATE);
		}
		String mac=mSharePre.getString(mDeviceNum+Constant.USER_ID+TAKE_CAMERA_MAC, "");
		return mac;
	}

	/**
	 * method : setTakeCameraMac
	 * Author : qj
	 * Describe : 设置相机mac
	 * param :  context 上下文
	 * param : userid 用户id
	 * param : mac 硬件信息
	 * Date : 2018/4/23
	 */
	public void setTakeCameraMac(String userId,String mac){
		if(mContext==null)
			return ;

		if(mSharePre==null){
			mSharePre = mContext.getSharedPreferences(Constant.TAKE_CAMERA_MAC, Context.MODE_PRIVATE);
		}

		editor=mSharePre.edit();

		editor.putString(mDeviceNum+userId+TAKE_CAMERA_MAC,mac).commit();
	}

	/**
	 * method : getConnectstateMac
	 * Author : qj
	 * param : mac 硬件信息
	 * Date : 2018/4/23
	 */
	public static String getConnectstateMac(Context context){
		if(context==null)
			return "";

		ShareUtil shareUtil = new ShareUtil(context,1);

		if(shareUtil.getConnectstate())
			return shareUtil.getTakeCameraMac();

		shareUtil = new ShareUtil(context,2);

		if(shareUtil.getConnectstate())
			return shareUtil.getTakeCameraMac();

		return "";
	}

	/**
	 * method : getConnectstateMac
	 * Author : qj
	 * param : mac 硬件信息
	 * Date : 2018/4/23
	 */
	public static ShareUtil getCameraMode(Context context){
		if(context==null)
			return null;

		ShareUtil shareUtil = new ShareUtil(context,1);

		if(shareUtil.getConnectstate()/*&&shareUtil.getTakeCameraMode()==0不需要判断相机类型*/)
			return shareUtil;

		shareUtil = new ShareUtil(context,2);

		if(shareUtil.getConnectstate()/*&&shareUtil.getTakeCameraMode()==0不需要判断相机类型*/)
			return shareUtil;

		return null;
	}

}

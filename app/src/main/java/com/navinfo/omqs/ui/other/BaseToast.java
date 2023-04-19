package com.navinfo.omqs.ui.other;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.navinfo.omqs.R;

/**
 * 在屏幕中间出现toast提示
 */
public class BaseToast extends Toast{
	final Context mContext;
	
	private static int height;
	static Toast result;
	public BaseToast(Context context) {
		super(context);
		mContext = context;
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);

		height = dm.heightPixels;
	}

	/**
	 * 屏幕中间显示toast
	 * @param context
	 * @param text 显示内容
	 * @param duration 显示时长
	 * @return
	 */
	public static Toast makeText(Context context, CharSequence text, int duration) {
		try{
			DisplayMetrics dm = new DisplayMetrics();
			((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
			height = dm.heightPixels;
			result =result==null? new Toast(context):result;
			LayoutInflater inflate = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflate.inflate(R.layout.transient_notification, null);
			TextView tv = (TextView) v.findViewById(android.R.id.message);
			tv.setText(text);
			result.setView(v);
			if(duration<Toast.LENGTH_SHORT)
				duration = Toast.LENGTH_SHORT;
			result.setDuration(duration);
			result.setGravity(Gravity.CENTER, 0, height/6);
		}catch (Exception e){
		}

		return result;
	}

	/**
	 * 屏幕中心显示toast
	 * @param context
	 * @param resId 文字资源id
	 * @param duration 显示时长
	 * @return
	 * @throws Resources.NotFoundException
	 */
	public static Toast makeText(Context context, int resId, int duration)
			throws Resources.NotFoundException {
		return makeText(context, context.getResources().getText(resId),
				duration);
	}

}


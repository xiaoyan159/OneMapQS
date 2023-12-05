package com.navinfo.omqs.util;


//Download by http://www.codefans.net

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tools for handler picture
 * 
 * @author Ryan.Tang
 * 
 */
public final class ImageLoad {
	static Bitmap bitmap = null;
	Context con;
	
	public ImageLoad(Context con){
		this.con=con;
	}

	/**
	 * Transfer drawable to bitmap
	 * 
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawableToBitmap(Drawable drawable) {
		int w = drawable.getIntrinsicWidth();
		int h = drawable.getIntrinsicHeight();

		Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Config.ARGB_8888
				: Config.RGB_565;
		Bitmap bitmap = Bitmap.createBitmap(w, h, config);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, w, h);
		drawable.draw(canvas);
		return bitmap;
	}

	/**
	 * Bitmap to drawable
	 * 
	 * @param bitmap
	 * @return
	 */
	public static Drawable bitmapToDrawable(Bitmap bitmap) {
		return new BitmapDrawable(bitmap);
	}

	/**
	 * Input stream to bitmap
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static Bitmap inputStreamToBitmap(InputStream inputStream)
			throws Exception {
		return BitmapFactory.decodeStream(inputStream);
	}

	/**
	 * Byte transfer to bitmap
	 * 
	 * @param byteArray
	 * @return
	 */
	public static Bitmap byteToBitmap(byte[] byteArray) {
		if (byteArray.length != 0) {
			return BitmapFactory
					.decodeByteArray(byteArray, 0, byteArray.length);
		} else {
			return null;
		}
	}

	/**
	 * Byte transfer to drawable
	 * 
	 * @param byteArray
	 * @return
	 */
	public static Drawable byteToDrawable(byte[] byteArray) {
		ByteArrayInputStream ins = null;
		if (byteArray != null) {
			ins = new ByteArrayInputStream(byteArray);
		}
		return Drawable.createFromStream(ins, null);
	}

	/**
	 * Bitmap transfer to bytes
	 * 
	 * @param byteArray
	 * @return
	 */
	public static byte[] bitmapToBytes(Bitmap bm) {
		byte[] bytes = null;
		if (bm != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
			bytes = baos.toByteArray();
		}
		return bytes;
	}

	/**
	 * Drawable transfer to bytes
	 * 
	 * @param drawable
	 * @return
	 */
	public static byte[] drawableToBytes(Drawable drawable) {
		BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		Bitmap bitmap = bitmapDrawable.getBitmap();
		byte[] bytes = bitmapToBytes(bitmap);
		;
		return bytes;
	}

	/**
	 * Base64 to byte[] //
	 */
	// public static byte[] base64ToBytes(String base64) throws IOException {
	// byte[] bytes = Base64.decode(base64);
	// return bytes;
	// }
	//
	// /**
	// * Byte[] to base64
	// */
	// public static String bytesTobase64(byte[] bytes) {
	// String base64 = Base64.encode(bytes);
	// return base64;
	// }

	/**
	 * Create reflection images
	 * 
	 * @param bitmap
	 * @return
	 */
	public static Bitmap createReflectionImageWithOrigin(Bitmap bitmap) {
		final int reflectionGap = 4;
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, h / 2, w,
				h / 2, matrix, false);

		Bitmap bitmapWithReflection = Bitmap.createBitmap(w, (h + h / 2),
				Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmapWithReflection);
		canvas.drawBitmap(bitmap, 0, 0, null);
		Paint deafalutPaint = new Paint();
		canvas.drawRect(0, h, w, h + reflectionGap, deafalutPaint);

		canvas.drawBitmap(reflectionImage, 0, h + reflectionGap, null);

		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0,
				bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff,
				0x00ffffff, TileMode.CLAMP);
		paint.setShader(shader);
		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, h, w, bitmapWithReflection.getHeight()
				+ reflectionGap, paint);

		return bitmapWithReflection;
	}

	/**
	 * 圆角
	 * 
	 * @param bitmap
	 * @param roundPx
	 *            5 10
	 * @return
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Bitmap output = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, w, h);
		final RectF rectF = new RectF(rect);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	/**
	 * Resize the bitmap
	 * 
	 * @param bitmap
	 * @param width
	 * @param height
	 * @return
	 */
	public static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix matrix = new Matrix();
		float scaleWidth = ((float) width / w);
		float scaleHeight = ((float) height / h);
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
		return newbmp;
	}
	
	public static Bitmap zoomBitmap(String cameraPath,int width,int height, int size) {
			
		// 获取图片Exif
		ExifInterface exif;
		int degree = 0;
		try {
			exif = new ExifInterface(cameraPath);
			// 获取指定tag的属性值
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				// We only recognize a subset of orientation tag values.
				switch (orientation) {
				case ExifInterface.ORIENTATION_NORMAL:
					degree = 0 ;
					break;
				case ExifInterface.ORIENTATION_UNDEFINED:
					degree = 0 ;
					break;
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
				default:
					degree = 90;					
					break;
				}
			}
		} catch (IOException e) {
		
			e.printStackTrace();
			CMLog.writeLogtoFile("展示图片生成异常", "异常", e.toString());
		}
		
		try {
			BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();	
			bmpFactoryOptions.inJustDecodeBounds = true;
			bitmap = BitmapFactory.decodeFile(cameraPath,bmpFactoryOptions);//此时返回bm为空
			bmpFactoryOptions.inJustDecodeBounds = false;
			bmpFactoryOptions.inSampleSize = size;
			int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight/ (float) height);
			int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth/ (float) width);
			if (heightRatio > 1 && widthRatio > 1) {
				bmpFactoryOptions.inSampleSize = heightRatio < widthRatio ? heightRatio: widthRatio;
			}
			
			bmpFactoryOptions.inPreferredConfig = Config.ARGB_8888;//该模式是默认的,可不设  
			bmpFactoryOptions.inPurgeable = true;// 同时设置才会有效  
			bmpFactoryOptions.inInputShareable = true;//。当系统内存不够时候图片自动被回收  
			if(bitmap!=null&&!bitmap.isRecycled())
				bitmap.recycle();
			bitmap=BitmapFactory.decodeFile(cameraPath, bmpFactoryOptions);
			
			if (degree != 0 && bitmap != null) {
				Matrix m = new Matrix();
				m.setRotate(degree, (float) bitmap.getWidth() / 2,(float) bitmap.getHeight() / 2);

					Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(), m, true);
					if (bitmap != b2) {
						bitmap.recycle();
						bitmap = b2;
						if(b2!=null&&b2.isRecycled())
							b2.recycle();
					}
	        	}
			
		} catch (OutOfMemoryError e) {
			CMLog.writeLogtoFile("照片内存溢出", "异常", "创建展示图异常:OutOfMemoryError");
			System.gc();
		}
		//new File(cameraPath).delete();		
		return bitmap;

	}

	public static Bitmap zoomBitmap(String cameraPath, int multiple) {
		
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = multiple; // 图片宽高都为原来的二分之一，即图片为原来的四分之一
		// 获取图片Exif
		ExifInterface exif;
		int degree = 0;
		try {
			exif = new ExifInterface(cameraPath);
			// 获取指定tag的属性值
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				// We only recognize a subset of orientation tag values.
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
/*				default:
					degree = 90;
					break;
				*/
				}
			}
		} catch (IOException e) {
		
			e.printStackTrace();
			CMLog.writeLogtoFile("缩略图生成异常", "异常", e.toString());
		}
		
		try {

			bitmap = ImageCrop(BitmapFactory.decodeFile(cameraPath, options));
			if (degree != 0 && bitmap != null) {
				Matrix m = new Matrix();
				m.setRotate(degree, (float) bitmap.getWidth() / 2,(float) bitmap.getHeight() / 2);

					Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0,
							bitmap.getWidth(), bitmap.getHeight(), m, true);
					if (bitmap != b2) {
						bitmap.recycle();
						bitmap = b2;
					}
	        	}
		} catch (OutOfMemoryError e) {
			CMLog.writeLogtoFile("照片内存溢出", "异常", "创建缩略图异常:OutOfMemoryError");
			System.gc();
		}
		if (bitmap == null) {

		}
		return bitmap;

	}
	
	private static int getDegree(String filePath){
		if(!new File(filePath).exists())
			return 0;
		ExifInterface exif;
		int degree = 0;
		try {
			exif = new ExifInterface(filePath);
			// 获取指定tag的属性值
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				// We only recognize a subset of orientation tag values.
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
/*				default:
					degree = 90;
					break;
				*/
				}
			}
		} catch (IOException e) {
		
			e.printStackTrace();
			CMLog.writeLogtoFile("缩略图生成异常", "异常", e.toString());
		}
		return degree;
	}

	/**
	 * 按正方形裁切图片
	 */
	public static Bitmap ImageCrop(Bitmap bitmap) {
		if(bitmap == null)
			return null;
		int w = bitmap.getWidth(); // 得到图片的宽，高
		int h = bitmap.getHeight();

		int wh = w > h ? h : w;// 裁切后所取的正方形区域边长

		int retX = w > h ? (w - h) / 2 : 0;// 基于原图,取正方形左上角x坐标
		int retY = w > h ? 0 : (h - w) / 2;

		// 下面这句是关键
		return Bitmap.createBitmap(bitmap, retX, retY, wh, wh, null, false);
	}

	public static Bitmap zoomBitmap(byte[] data, int multiple, int degrees) {

		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = multiple; // 图片宽高都为原来的二分之一，即图片为原来的四分之一
		try {
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,options);

			if (degrees != 0 && bitmap != null) {
				Matrix m = new Matrix();
				m.setRotate(degrees, (float) bitmap.getWidth() / 2,
						(float) bitmap.getHeight() / 2);
				try {
					Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0,
							bitmap.getWidth(), bitmap.getHeight(), m, true);
					if (bitmap != b2) {
						bitmap.recycle();
						bitmap = b2;
					}
				} catch (OutOfMemoryError ex) {
					// We have no memory to rotate. Return the original bitmap.
				}
			}

		} catch (OutOfMemoryError e) {
			CMLog.writeLogtoFile("照片内存溢出", "异常", "创建缩略图异常:OutOfMemoryError");
			System.gc();
		}
		if (bitmap == null) {

		}
		return bitmap;

	}

	/**
	 * Resize the drawable
	 * 
	 * @param drawable
	 * @param w
	 * @param h
	 * @return
	 */
	public static Drawable zoomDrawable(Drawable drawable, int w, int h) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap oldbmp = drawableToBitmap(drawable);
		Matrix matrix = new Matrix();
		float sx = ((float) w / width);
		float sy = ((float) h / height);
		matrix.postScale(sx, sy);
		Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
				matrix, true);
		return new BitmapDrawable(newbmp);
	}

	/**
	 * Get images from SD card by path and the name of image
	 * 
	 * @param photoName
	 * @return
	 */
	public static Bitmap getPhotoFromSDCard(String path, String photoName) {
		Bitmap photoBitmap = BitmapFactory.decodeFile(path + "/" + photoName
				+ ".png");
		if (photoBitmap == null) {
			return null;
		} else {
			return photoBitmap;
		}
	}

	/**
	 * Check the SD card
	 * 
	 * @return
	 */
	public static boolean checkSDCardAvailable() {
		return android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
	}

	/**
	 * Get image from SD card by path and the name of image
	 * 
	 * @param photoName
	 * @return
	 */
	public static boolean findPhotoFromSDCard(String path, String photoName) {
		boolean flag = false;

		if (checkSDCardAvailable()) {
			File dir = new File(path);
			if (dir.exists()) {
				File folders = new File(path);
				File photoFile[] = folders.listFiles();
				for (int i = 0; i < photoFile.length; i++) {
					String fileName = photoFile[i].getName().split("\\.")[0];
					if (fileName.equals(photoName)) {
						flag = true;
					}
				}
			} else {
				flag = false;
			}
			// File file = new File(path + "/" + photoName + ".jpg" );
			// if (file.exists()) {
			// flag = true;
			// }else {
			// flag = false;
			// }

		} else {
			flag = false;
		}
		return flag;
	}

	/**
	 * Save image to the SD card
	 * 
	 * @param photoBitmap
	 * @param photoName
	 * @param path
	 */
	public static void savePhotoToSDCard(Bitmap photoBitmap, String path,
			String photoName) {
		if (checkSDCardAvailable()) {
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			File photoFile = new File(path, photoName + ".png");
			FileOutputStream fileOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(photoFile);
				if (photoBitmap != null) {
					if (photoBitmap.compress(Bitmap.CompressFormat.PNG, 100,
							fileOutputStream)) {
						fileOutputStream.flush();
						// fileOutputStream.close();
					}
				}
			} catch (FileNotFoundException e) {
				photoFile.delete();
				e.printStackTrace();
			} catch (IOException e) {
				photoFile.delete();
				e.printStackTrace();
			} finally {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Delete the image from SD card
	 * 
	 * @param path
	 *            file:///sdcard/temp.jpg
	 */
	public static void deleteAllPhoto(String path) {
		if (checkSDCardAvailable()) {
			File folder = new File(path);
			File[] files = folder.listFiles();
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		}
	}

	public static void deletePhotoAtPathAndName(String path, String fileName) {
		if (checkSDCardAvailable()) {
			File folder = new File(path);
			File[] files = folder.listFiles();
			for (int i = 0; i < files.length; i++) {
				System.out.println(files[i].getName());
				if (files[i].getName().equals(fileName)) {
					files[i].delete();
				}
			}
		}
	}

	public Bitmap setImage(Uri mImageCaptureUri) {
		// 不管是拍照还是选择图片每张图片都有在数据中存储也存储有对应旋转角度orientation值
		// 所以我们在取出图片是把角度值取出以便能正确的显示图片,没有旋转时的效果观看

		ContentResolver cr = con.getContentResolver();
		Cursor cursor = cr.query(mImageCaptureUri, null, null, null, null);// 根据Uri从数据库中找
		if (cursor != null) {
			cursor.moveToFirst();// 把游标移动到首位，因为这里的Uri是包含ID的所以是唯一的不需要循环找指向第一个就是了
			String filePath = cursor.getString(cursor.getColumnIndex("_data"));// 获取图片路
			String orientation = cursor.getString(cursor
					.getColumnIndex("orientation"));// 获取旋转的角度
			cursor.close();
			if (filePath != null) {
				Bitmap bitmap = BitmapFactory.decodeFile(filePath);// 根据Path读取资源图片
				int angle = 0;
				if (orientation != null && !"".equals(orientation)) {
					angle = Integer.parseInt(orientation);
				}
				if (angle != 0) {
					// 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
					Matrix m = new Matrix();
					int width = bitmap.getWidth();
					int height = bitmap.getHeight();
					m.setRotate(angle); // 旋转angle度
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
							m, true);// 从新生成图片
				}
				// photo.setImageBitmap(bitmap);
				return bitmap;
			}
		}
		return null;
	}

	/**
	 * 读取图片属性：旋转的角度
	 * 
	 * @param path
	 *            图片绝对路径
	 * @return degree旋转的角度
	 */
	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, -1);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			}
			Log.i("ImageTools", "获取拍照角度：" + degree);
		} catch (IOException e) {
			e.printStackTrace();
			Log.i("ImageTools", "获取拍照角度异常：" + e.getMessage());
		}
		return degree;
	}

	/*
	 * 旋转图片
	 * 
	 * @param angle
	 * 
	 * @param bitmap
	 * 
	 * @return Bitmap
	 */
	public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
		// 旋转图片 动作
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		System.out.println("angle2=" + angle);
		if (bitmap != null) {
			// 创建新的图片
			Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
					bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			return resizedBitmap;
		}

		return null;
	}

	/*
	 * 旋转图片
	 * 
	 * @param bitmap
	 * 
	 * @param filePath
	 * 
	 * @return Bitmap
	 */
	public static void saveBitmap(Bitmap bitmap, String filePath) {
		File f = new File(filePath);
		if (f.exists()) {
			f.delete();
		}
		try {
			FileOutputStream out = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			CMLog.writeLogtoFile("照片生成", "拍照", "重新生成照片异常FileNotFoundException："
					+ e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			CMLog.writeLogtoFile("照片生成", "拍照", "重新生成照片IO异常：" + e.getMessage());
		}

	}

	/*
	 * 释放Bitmap
	 * 
	 * @param bitmap
	 */
	public static void recycleBitmap(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled()) {

			// 回收并且置为null

			bitmap.recycle();

			bitmap = null;

		}

		System.gc();

	}

	/*
	 * 创建缩略图
	 * 
	 * @param filePath
	 * 
	 * @return Bitmap
	 */
	public static Bitmap createZoominBitmap(String filePath) {
		Bitmap bitmap = null;

		try {

			// 实例化Bitmap

			bitmap = BitmapFactory.decodeFile(filePath);

		} catch (OutOfMemoryError e) {

			//

		}
		return bitmap;
	}

	public static Bitmap getBitmapLittle(Context context, String absolutePath) {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		// 这个isjustdecodebounds很重要
		opt.inJustDecodeBounds = true;
		Bitmap bm = BitmapFactory.decodeFile(absolutePath, opt);

		// 获取到这个图片的原始宽度和高度
		int picWidth = opt.outWidth;
		int picHeight = opt.outHeight;

		// 获取屏的宽度和高度
		WindowManager windowManager = ((Activity)context).getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();

		// isSampleSize是表示对图片的缩放程度，比如值为2图片的宽度和高度都变为以前的1/2
		opt.inSampleSize = 1;
		// 根据屏的大小和图片大小计算出缩放比例
		if (picWidth > picHeight) {
			if (picWidth > screenWidth)
				opt.inSampleSize = picWidth / screenWidth;
		} else {
			if (picHeight > screenHeight)

				opt.inSampleSize = picHeight / screenHeight;
		}

		// 这次再真正地生成一个有像素的，经过缩放了的bitmap
		opt.inJustDecodeBounds = false;
		bm = BitmapFactory.decodeFile(absolutePath, opt);

		return bm;
	}
	
	/**
	* 计算压缩比例值
	* @param options       解析图片的配置信息
	* @param reqWidth            所需图片压缩尺寸最小宽度
	* @param reqHeight           所需图片压缩尺寸最小高度
	* @return
	*/
	public static int calculateInSampleSize(BitmapFactory.Options options,
	             int reqWidth, int reqHeight) {
	       // 保存图片原宽高值
	       final int height = options. outHeight;
	       final int width = options. outWidth;
	       // 初始化压缩比例为1
	       int inSampleSize = 1;

	       // 当图片宽高值任何一个大于所需压缩图片宽高值时,进入循环计算系统
	       if (height > reqHeight || width > reqWidth) {

	             final int halfHeight = height / 2;
	             final int halfWidth = width / 2;

	             // 压缩比例值每次循环两倍增加,
	             // 直到原图宽高值的一半除以压缩值后都~大于所需宽高值为止
	             while ((halfHeight / inSampleSize) >= reqHeight
	                        && (halfWidth / inSampleSize) >= reqWidth) {
	                  inSampleSize *= 2;
	            }
	      }

	       return inSampleSize;
	}

	/*public static void loadImage(int resid, String url, final ImageView imageView){
		final String tmpUrl = url;
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showImageOnLoading(resid)
		.showImageForEmptyUri(resid)
		.showImageOnFail(resid)
		.cacheInMemory(true)
		.cacheOnDisc(true)
		.bitmapConfig(Bitmap.Config.ARGB_8888)
		.build();
		ImageLoader.getInstance().displayImage(tmpUrl, imageView, options,
				new ImageLoadingListener() {

					@Override
					public void onLoadingStarted(String arg0, View arg1) {
						// TODO Auto-generated method stub
					}

					@Override
					public void onLoadingFailed(String arg0, View arg1,
							FailReason arg2) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onLoadingComplete(String arg0, View arg1,
							Bitmap bmp) {
						// TODO Auto-generated method stub
						if (bmp == null)
							return;
						if( ((BaseZoomImageView)arg1).isDestroyed() ){
							bmp.recycle();
							return;
						}
						String path = ((PictureInfor)arg1.getTag()).mFilePath;
						int degree = getDegree(path);
						try {

							if (degree != 0 && bmp != null&&!bmp.isRecycled()) {
								Matrix m = new Matrix();
								m.setRotate(degree, (float) bmp.getWidth() / 2,(float) bmp.getHeight() / 2);

									Bitmap b2 = Bitmap.createBitmap(bmp, 0, 0,
											bmp.getWidth(), bmp.getHeight(), m, true);
									if (bmp != b2) {
										bmp.recycle();
										bmp = b2;
									}
					        	}
						} catch (OutOfMemoryError e) {
							CMLog.writeLogtoFile("照片内存溢出", "异常", "创建缩略图异常:OutOfMemoryError");
							System.gc();
						}

						((BaseZoomImageView)arg1).setImageBitmap(bmp);
						((BaseZoomImageView)arg1).setImageDrawable(null);
						if(new File(path).exists())
							return;
						saveBitmap(bmp, path);
					}

					@Override
					public void onLoadingCancelled(String arg0, View arg1) {
						// TODO Auto-generated method stub

					}
				});
	}*/
	
	
	
	
}

package com.navinfo.omqs.util;


//Download by http://www.codefans.net

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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
import android.graphics.drawable.PictureDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.navinfo.omqs.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools for handler picture
 * 图片处理工具
 * @author Ryan.Tang
 */
public final class ImageTools {
    static Bitmap bitmap = null;
//    static Context con;

//    public ImageTools(Context con) {
//        this.con = con;
//    }

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
        if (byteArray!=null&&byteArray.length != 0) {
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
     * @param bm
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
    public static byte[] base64ToBytes(String base64) throws IOException {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return bytes;
    }

    /**
     * Base64 to Bitmap //
     */
    public static Bitmap base64ToBitmap(String base64) throws IOException {
        if(!TextUtils.isEmpty(base64)){
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            if(bytes!=null)
                return byteToBitmap(bytes);
        }


        return null;
    }

    /**
     * Byte[] to base64
     */
    public static String bytesTobase64(byte[] bytes) {
        String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
        return base64;
    }

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
     * Get rounded corner images
     *
     * @param bitmap
     * @param roundPx 5 10
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

    public static Bitmap zoomBitmap(String cameraPath, int width, int height, int size) {

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
                        degree = 0;
                        break;
                    case ExifInterface.ORIENTATION_UNDEFINED:
                        degree = 0;
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
        }

        try {
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = true;
            //Bitmap tmp = BitmapFactory.decodeFile(cameraPath,bmpFactoryOptions);//此时返回bm为空
            bmpFactoryOptions.inJustDecodeBounds = false;
            bmpFactoryOptions.inSampleSize = size;
            int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
            int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);
            if (heightRatio > 1 && widthRatio > 1) {
                bmpFactoryOptions.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }

            bmpFactoryOptions.inPreferredConfig = Config.ARGB_8888;//该模式是默认的,可不设
            bmpFactoryOptions.inPurgeable = true;// 同时设置才会有效
            bmpFactoryOptions.inInputShareable = true;//。当系统内存不够时候图片自动被回收
            if (bitmap != null && !bitmap.isRecycled())
                bitmap.recycle();
            bitmap = BitmapFactory.decodeFile(cameraPath, bmpFactoryOptions);

            if (degree != 0 && bitmap != null) {
                Matrix m = new Matrix();
                m.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap.recycle();
                    bitmap = b2;
                    if (b2 != null && b2.isRecycled())
                        b2.recycle();
                }
            }

        } catch (OutOfMemoryError e) {
            System.gc();
        }
        new File(cameraPath).delete();
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
        }

        try {

            bitmap = ImageCrop(BitmapFactory.decodeFile(cameraPath, options));
            if (degree != 0 && bitmap != null) {
                Matrix m = new Matrix();
                m.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap.recycle();
                    bitmap = b2;
                }
            }
        } catch (OutOfMemoryError e) {
            System.gc();
        }catch (Exception e){
            System.gc();
        }
        if (bitmap == null) {

        }
        return bitmap;

    }

    private static int getDegree(String filePath) {
        if (!new File(filePath).exists())
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
        }
        return degree;
    }

    /**
     * 按正方形裁切图片
     */
    public static Bitmap ImageCrop(Bitmap bitmap) {
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
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

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

    public static Bitmap getPhotoFromSDCard(String path) {
        Bitmap photoBitmap = BitmapFactory.decodeFile(path);
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
     * @param
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
     * @param
     * @param path file:///sdcard/temp.jpg
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

    public Bitmap setImage(Context con, Uri mImageCaptureUri) {
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
     * @param path 图片绝对路径
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

        if (angle < 0)
            return bitmap;

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
        //如果文件夹不存在，则创建对应的文件夹
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void saveBitmapToPNG(Bitmap bitmap, String filePath) {
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
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        return null;
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
        WindowManager windowManager = ((Activity) context).getWindowManager();
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
     *
     * @param options   解析图片的配置信息
     * @param reqWidth  所需图片压缩尺寸最小宽度
     * @param reqHeight 所需图片压缩尺寸最小高度
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 保存图片原宽高值
        final int height = options.outHeight;
        final int width = options.outWidth;
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

    public static Bitmap getBitmapByNameFromRes(Activity activity, String name) {
        ApplicationInfo appInfo = activity.getApplicationInfo();
        int resID = activity.getResources().getIdentifier(name, "drawable", appInfo.packageName);
        return BitmapFactory.decodeResource(activity.getResources(), resID);
    }

    public static Drawable getDrawableByNameFromRes(Activity activity, String name) {
        ApplicationInfo appInfo = activity.getApplicationInfo();
        int resID = activity.getResources().getIdentifier(name, "drawable", appInfo.packageName);
        return activity.getResources().getDrawable(resID);
    }

    public static int getResIdByNameFromRes(Activity activity, String name) {
        ApplicationInfo appInfo = activity.getApplicationInfo();
        int resID = activity.getResources().getIdentifier(name, "drawable", appInfo.packageName);
        return resID;
    }

    public static String getBitmapString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();// outputstream
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] appicon = baos.toByteArray();// 转为byte数组
        return Base64.encodeToString(appicon, Base64.DEFAULT);
    }

    public interface LoadImageClickListener {
        public void LoadImageClick();
    }

    /**
     * 根据View的大小设置图片的大小
     *
     * @param view
     * @param path 图片路径
     */
    public static void setViewImage(View view, String path) {
        if (view == null || path == null || !new File(path).isFile()) return;
        int width = view.getWidth();
        int height = view.getHeight();
        if (width == 0 || height == 0) {
            setImage(view, ImageLoad.zoomBitmap(path, 4));
        } else {
            Bitmap zoomBitmap = zoomBitmap(path, width, height);
            setImage(view, ImageCrop(zoomBitmap));
        }
    }

    /**
     * 根据给的宽高进行计算压缩比缩，返回原图高宽比的图片
     *
     * @param path      图片路径
     * @param WinWidth  压缩宽度
     * @param WinHeight 压缩高度
     */
    public static Bitmap zoomBitmap(String path, int WinWidth, int WinHeight) {
        //int WinWidth = defaultDisplay.getWidth();
        //int WinHeight = defaultDisplay.getHeight();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;// 只解析头文件
        BitmapFactory.decodeFile(path, options);
        //图片的高和宽
        int height = options.outHeight;
        int width = options.outWidth;

        //用于计算最大的缩放的比例
        int scaleX = height / WinHeight;
        int scaleY = width / WinWidth;
        int scale = scaleX > scaleY ? scaleX : scaleY;

        options.inSampleSize = scale;

        options.inJustDecodeBounds = false;
        //另外，为了节约内存我们还可以使用下面的几个字段：
        options.inPreferredConfig = Config.ARGB_4444;    // 默认是Bitmap.Config.ARGB_8888
		/* 下面两个字段需要组合使用 */
        options.inPurgeable = true;
        options.inInputShareable = true;

        Bitmap decodeFile = BitmapFactory.decodeFile(path, options);
        return decodeFile;
    }

//    /**
//     * 直接设置压缩比，返回原图高宽比的图片
//     *
//     * @param path  图片路径
//     * @param scale
//     * @return
//     */
//
//    public static Bitmap zoomBitmep2(String path, int scale) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;// 只解析头文件
//        BitmapFactory.decodeFile(path, options);
//        options.inSampleSize = scale;
//        options.inJustDecodeBounds = false;
//        options.inPreferredConfig = Bitmap.Config.ARGB_4444;    // 默认是Bitmap.Config.ARGB_8888
//		/* 下面两个字段需要组合使用 */
//        options.inPurgeable = true;
//        options.inInputShareable = true;
//        Bitmap decodeFile = BitmapFactory.decodeFile(path, options);
//        return decodeFile;
//    }


    /**
     * 根据屏幕显示图片
     *
     * @param context
     * @param view
     * @param path
     */
    public static void setViewImage(Context context, View view, String path) {
        if (view == null || path == null || !new File(path).isFile()) return;
        setImage(view, ImageLoad.getBitmapLittle(context, path));
    }

    private static void setImage(View view, Bitmap bitmap) {
        if (view == null || bitmap == null) return;
        if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            iv.setImageBitmap(bitmap);
        } else {
            view.setBackgroundDrawable(new BitmapDrawable(bitmap));
        }
    }

    public static void setViewImage(View view, Bitmap bitmap) {
        if (view == null || bitmap == null) return;
        int width = view.getWidth();
        int height = view.getHeight();
        Log.i("info", "width:" + width + ",height:" + height);
//		if(width==0||height==0){
        setImage(view, bitmap);
//		}else{
//			Log.i("info","view.getWidth():"+view.getWidth());
//			int size = 4;//图片的宽高都大于view时，此值不起作用没有用
//			setImage(view, ImageLoad.zoomBitmap(bitmap, width, height));
//		}
    }

    /**
     * 绘制蓝色圆环带数字
     *
     * @param context
     * @param text
     * @return
     */

    public static Bitmap createNumberPic(Context context, String text) {

        Paint paint = new Paint();
        paint.setAntiAlias(true); //消除锯齿

        int innerCircle = dip2px(context, 9); //内圆半径
        int ringWidth = dip2px(context, 2);   //圆环宽度
        paint.setStrokeWidth(ringWidth);
        int l = (innerCircle + ringWidth + 10) * 2;
        int center = innerCircle + ringWidth + 10;
        Bitmap b = Bitmap.createBitmap(l, l, Config.ARGB_8888);
        Canvas canvas = new Canvas(b);

        paint.setColor(context.getResources().getColor(R.color.white));
        canvas.drawCircle(center, center, innerCircle + ringWidth, paint);

        paint.setStyle(Paint.Style.STROKE);  //绘制空心圆或 空心矩形
        paint.setARGB(255, 26, 187, 254);
        canvas.drawCircle(center, center, innerCircle + ringWidth, paint);

        paint.setTextSize(26f);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, center, center + center / 3, paint);
        return b;
    }

    /* 根据手机的分辨率从 dp 的单位 转成为 px(像素) */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将bitmap中的某种颜色值替换成新的颜色
     *
     * @param oldBitmap
     * @param oldColor
     * @param newColor
     * @return
     */
    public static Bitmap replaceBitmapColor(Bitmap oldBitmap, int oldColor, int newColor) {
        //相关说明可参考 http://xys289187120.blog.51cto.com/3361352/657590/
        Bitmap mBitmap = oldBitmap.copy(Config.ARGB_8888, true);
        //循环获得bitmap所有像素点
        int mBitmapWidth = mBitmap.getWidth();
        int mBitmapHeight = mBitmap.getHeight();
        int mArrayColorLengh = mBitmapWidth * mBitmapHeight;
        int[] mArrayColor = new int[mArrayColorLengh];
        int count = 0;
        for (int i = 0; i < mBitmapHeight; i++) {
            for (int j = 0; j < mBitmapWidth; j++) {
                //获得Bitmap 图片中每一个点的color颜色值
                //将需要填充的颜色值如果不是
                //在这说明一下 如果color 是全透明 或者全黑 返回值为 0
                //getPixel()不带透明通道 getPixel32()才带透明部分 所以全透明是0x00000000
                //而不透明黑色是0xFF000000 如果不计算透明部分就都是0了
                int color = mBitmap.getPixel(j, i);
                //将颜色值存在一个数组中 方便后面修改
                if (color == oldColor) {
                    mBitmap.setPixel(j, i, newColor);  //将白色替换成透明色
                }

            }
        }
        return mBitmap;
    }

    /**
     * 图片饱和度转换 变灰
     */

    public static Drawable getColorMatrixImage(Drawable drawable) {
        if (drawable == null)
            return drawable;
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(cm);
        drawable.hashCode();
        drawable.getCurrent().setColorFilter(cf);
        drawable.getCurrent().hashCode();
        return drawable.getCurrent();
    }

    /**
     * 图片饱和度转换 变灰
     */

    public static Drawable getColorMatrixImage(Drawable drawable, ColorMatrixColorFilter cf) {
        if (drawable == null)
            return drawable;
        drawable.hashCode();
        drawable.getCurrent().setColorFilter(cf);
        drawable.getCurrent().hashCode();
        return drawable.getCurrent();
    }

    public static Bitmap mergeBitmap(Bitmap backBitmap, Bitmap frontBitmap) {

        if (backBitmap == null || backBitmap.isRecycled()
                || frontBitmap == null || frontBitmap.isRecycled()) {
            return null;
        }
        Bitmap bitmap = backBitmap.copy(Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Rect baseRect = new Rect(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        Rect frontRect = new Rect(0, 0, frontBitmap.getWidth(), frontBitmap.getHeight());
        canvas.drawBitmap(frontBitmap, frontRect, baseRect, null);
        return bitmap;
    }

    /**
     * Get image from newwork
     *
     * @param path The path of image
     * @return byte[]
     * @throws Exception
     */
    public static byte[] getImage(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        InputStream inStream = conn.getInputStream();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return readStream(inStream);
        }
        return null;
    }

    /**
     * Get image from newwork
     *
     * @param path The path of image
     * @return InputStream
     * @throws Exception
     */
    public static InputStream getImageStream(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return conn.getInputStream();
        }
        return null;
    }

    /**
     * Get data from stream
     *
     * @param inStream
     * @return byte[]
     * @throws Exception
     */
    public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    public static Bitmap svgToBimap(byte[] bytes) {
        try {
            InputStream input = new ByteArrayInputStream(bytes);
            SVG svg = SVG.getFromInputStream(input);
            Drawable drawable = new PictureDrawable(svg.renderToPicture());
            return ImageTools.drawableToBitmap(drawable);
        } catch (SVGParseException e) {

        }
        return null;
    }
}

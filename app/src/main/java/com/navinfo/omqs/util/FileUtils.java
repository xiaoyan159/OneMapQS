package com.navinfo.omqs.util;

import android.content.Context;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author qj
 * @version V1.0
 * @ClassName: FileUtils
 * @Date 2023/4/17
 * @Description: ${文件类)
 */
public class FileUtils {
    //类标识
    private static final String TAG = "FileUtils";
    // 本类输出的日志文件名称
    private static String TrackFILEName = "Track.txt";
    // 本类输出的日志文件名称
    private static String AdasTrackFILEName = "AdasTrack.txt";
    //日志文件格式
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd");
    //文件集合
    private static List<File> filelist = new ArrayList<File>();


    /**
     * 复制文件到sd卡
     *
     * @param context  上下文
     * @param fileDir  文件目录
     * @param filePath 文件路径
     * @param files    文件集合
     */
    public static void copyFileToSdcard(Context context, String fileDir,
                                        String filePath, Field[] files) {

        for (Field r : files) {
            try {
                int id = context.getResources().getIdentifier(r.getName(),
                        "raw", context.getPackageName());
                Log.i(TAG, new File(filePath).length() + "=====文件长度===="
                        + filePath);
                if (!new File(fileDir).exists()) {
                    new File(fileDir).mkdirs();
                }

                // new File(path).delete();
                if (new File(filePath).exists())
                    new File(filePath).delete();
                new File(filePath).createNewFile();
                BufferedOutputStream bufEcrivain = new BufferedOutputStream(
                        (new FileOutputStream(new File(filePath))));
                BufferedInputStream VideoReader = new BufferedInputStream(
                        context.getResources().openRawResource(id));
                byte[] buff = new byte[20 * 1024];
                int len;
                while ((len = VideoReader.read(buff)) > 0) {
                    bufEcrivain.write(buff, 0, len);
                }
                bufEcrivain.flush();
                bufEcrivain.close();
                VideoReader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static boolean copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { // 文件存在时
                InputStream inStream = new FileInputStream(oldPath); // 读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {

                    bytesum += byteread; // 字节数 文件大小
//                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);

                }
                inStream.close();
                return true;
            }

        } catch (Exception e) {

            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
        return false;

    }


    /**
     * 实时轨迹信息写入文件
     *
     * @param info      信息
     * @param isNewLine 是否换行写入
     */
    public static void writeRealTimeTrackToFile(String filePath, String info, boolean isNewLine) {


        if (new File(filePath).exists() == false) {
            new File(filePath).mkdirs();
        }

        File file = new File(filePath, "monitor_track.txt");

        FileWriter filerWriter = null;

        BufferedWriter bufWriter = null;

        try {
            if (!file.exists())
                file.createNewFile();

            //文件流
            filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖

            //字符缓冲输出流
            bufWriter = new BufferedWriter(filerWriter);

            bufWriter.write(info);

            if (isNewLine)
                bufWriter.newLine();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try{
                if(bufWriter!=null)
                    bufWriter.close();

                if(filerWriter!=null)
                    filerWriter.close();
            }catch (Exception e){

            }

        }
    }


    /**
     * 轨迹信息写入文件
     *
     * @param info      信息
     * @param isNewLine 是否换行写入
     */
    public static void writeTrackToFile(String filePath, String info, boolean isNewLine) {
        Date nowtime = new Date();

        String needWriteFiel = logfile.format(nowtime);

        if (TextUtils.isEmpty(filePath))
            return;

        if (new File(filePath).exists() == false) {
            new File(filePath).mkdirs();
        }
        File file = new File(filePath, needWriteFiel + TrackFILEName);
        FileWriter filerWriter = null;
        BufferedWriter bufWriter = null;
        try {
            if (!file.exists())
                file.createNewFile();

            //文件流
            filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖

            //字符缓冲输出流
            bufWriter = new BufferedWriter(filerWriter);

            bufWriter.write(info);

            if (isNewLine)
                bufWriter.newLine();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {

            try{
                if(bufWriter!=null)
                    bufWriter.close();

                if(filerWriter!=null)
                    filerWriter.close();
            }catch (Exception e){

            }

        }
    }

    /**
     * 实时轨迹偏差信息写入文件
     *
     * @param info      信息
     * @param isNewLine 是否换行写入
     */
    public static void writeTrackLogToFile(String filePath, String info, boolean isNewLine) {


        if (new File(filePath).exists() == false) {
            new File(filePath).mkdirs();
        }
        File file = new File(filePath, "TrackLog.txt");
        FileWriter filerWriter = null;
        BufferedWriter bufWriter = null;
        try {
            if (!file.exists())
                file.createNewFile();

            //文件流
            filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖

            //字符缓冲输出流
            bufWriter = new BufferedWriter(filerWriter);

            bufWriter.write(info);

            if (isNewLine)
                bufWriter.newLine();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try{
                if(bufWriter!=null)
                    bufWriter.close();

                if(filerWriter!=null)
                    filerWriter.close();
            }catch (Exception e){

            }
        }
    }


    /**
     * 逐行写入
     *
     * @param info      信息
     */
    public static void writeLine(String filePath,String fileName, String info) {

        if (TextUtils.isEmpty(filePath))
            return;

        if (new File(filePath).exists() == false) {
            new File(filePath).mkdirs();
        }
        File file = new File(filePath + fileName);
        FileWriter filerWriter = null;
        BufferedWriter bufWriter = null;
        try {
            if (!file.exists())
                file.createNewFile();

            //文件流
            filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖

            //字符缓冲输出流
            bufWriter = new BufferedWriter(filerWriter);

            bufWriter.write(info);

            bufWriter.newLine();


        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if(bufWriter!=null)
                    bufWriter.close();

                if(filerWriter!=null)
                    filerWriter.close();
            }catch (Exception e){

            }
        }
    }

    /**
     * Adas轨迹信息写入文件
     *
     * @param info      信息
     * @param isNewLine 是否换行写入
     */
    public static void writeAdasTrackToFile(String filePath, String info, boolean isNewLine) {

        Date nowtime = new Date();

        String needWriteFiel = logfile.format(nowtime);

        if (TextUtils.isEmpty(filePath))
            return;

        if (new File(filePath).exists() == false) {
            new File(filePath).mkdirs();
        }

        File file = new File(filePath, needWriteFiel + AdasTrackFILEName);
        FileWriter filerWriter = null;
        BufferedWriter bufWriter = null;
        try {
            if (!file.exists())
                file.createNewFile();

            //文件流
            filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖

            //字符缓冲输出流
            bufWriter = new BufferedWriter(filerWriter);

            bufWriter.write(info);

            if (isNewLine)
                bufWriter.newLine();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try{
                if(bufWriter!=null)
                    bufWriter.close();

                if(filerWriter!=null)
                    filerWriter.close();
            }catch (Exception e){

            }
        }
    }

    /**
     * 获取文件绝对路径
     *
     * @return list
     * 文件绝对路径集合
     */
    public static List<File> getFileList(String strPath, String[] suffix) {
        File dir = new File(strPath);
        File[] files = dir.listFiles(); // 该文件目录下文件全部放入数组

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (files[i].isDirectory()) { // 判断是文件还是文件夹
                    getFileList(files[i].getAbsolutePath(), suffix); // 获取文件绝对路径
                } else {
                    if (suffix != null && suffix.length > 0) {
                        for (String str : suffix) {
                            if (fileName.endsWith(str)) { // 判断文件名是否以
                                String strFileName = files[i].getAbsolutePath();
//                                System.out.println("---" + strFileName);
                                filelist.add(files[i]);
                            }
                        }
                    }
                }
            }

        }
        return filelist;
    }

    /**
     * 清理缓存文件
     */
    public static void clearCacheFile() {
        filelist.clear();
    }

    /**
     * 获取文件目录大小
     *
     * @param file
     */
    public static double getDirSize(File file) {
        //判断文件是否存在
        if (file.exists()) {
            //如果是目录则递归计算其内容的总大小
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                double size = 0;
                for (File f : children)
                    size += getDirSize(f);
                return size;
            } else {//如果是文件则直接返回其大小,以“兆”为单位
                double size = (double) file.length() / 1024 / 1024;
                return size;
            }
        } else {
            System.out.println("文件或者文件夹不存在，请检查路径是否正确！");
            return 0.0;
        }
    }

    public static byte[] readFileByBytes(String url) throws IOException {
        File file = new File(url);
        if (file.exists() && !file.mkdir()) {
            long fileSize = file.length();
            if (fileSize > Integer.MAX_VALUE) {
//                System.out.println("file too big...");
                return null;
            }

            FileInputStream fi = new FileInputStream(file);
            byte[] buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length
                    && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            // 确保所有数据均被读取
            if (offset != buffer.length) {
                throw new IOException("Could not completely read file "
                        + file.getName());
            }
            fi.close();
            return buffer;
        }
        return null;
    }


    //把从服务器获得图片的输入流InputStream写到本地磁盘
    public static void saveImageToDisk(String url, String savePath) {

        InputStream inputStream = getInputStream(url);
        if(inputStream==null)
            return;

        byte[] data = new byte[1024];
        int len = 0;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(savePath);
            while ((len = inputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, len);

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

    }

    // 从服务器获得一个输入流(本例是指从服务器获得一个image输入流)
    public static InputStream getInputStream(String urlPath) {
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;

        try {
            URL url = new URL(urlPath);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            // 设置网络连接超时时间
            httpURLConnection.setConnectTimeout(3000);
            // 设置应用程序要从网络连接读取数据
            httpURLConnection.setDoInput(true);

            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) {
                // 从服务器返回一个输入流
                inputStream = httpURLConnection.getInputStream();

            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return inputStream;

    }

    public static String getExifTime(String jpegFile) {
        try {

            ExifInterface exif = new ExifInterface(jpegFile);

            String time = exif.getAttribute(ExifInterface.TAG_DATETIME);

            return time;


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 轨迹信息写入文件
     *
     * @param info      信息
     * @param isNewLine 是否换行写入
     */
    public static void writeToFile(String filePath, String info, boolean isNewLine) {

        if (TextUtils.isEmpty(filePath))
            return;

        File file = new File(filePath);
        FileWriter filerWriter = null;
        BufferedWriter bufWriter = null;
        try {
            if (!file.exists())
                file.createNewFile();

            //文件流
            filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖

            //字符缓冲输出流
            bufWriter = new BufferedWriter(filerWriter);

            bufWriter.write(info);

            if (isNewLine)
                bufWriter.newLine();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try{
                if(bufWriter!=null)
                    bufWriter.close();

                if(filerWriter!=null)
                    filerWriter.close();
            }catch (Exception e){

            }
        }
    }
}






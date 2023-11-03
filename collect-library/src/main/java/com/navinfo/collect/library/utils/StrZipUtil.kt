package com.navinfo.collect.library.utils

import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.*

object StrZipUtil {

    /**
     * @param input 需要压缩的字符串
     * @return 压缩后的字符串
     * @throws IOException IO
     */
    fun compress(input: String): String {
        if (input.isEmpty()) {
            return input
        }
        try {
            val out = ByteArrayOutputStream()
            val gzipOs = GZIPOutputStream(out)
            gzipOs.write(input.toByteArray())
            gzipOs.close()
            return BASE64Encoder().encode(out.toByteArray())
        } catch (e: Exception) {
            return input
        }

    }

    /**
     * @param zippedStr 压缩后的字符串
     * @return 解压缩后的
     * @throws IOException IO
     */
    fun uncompress(zippedStr: String): String {
        if (zippedStr.isEmpty()) {
            return zippedStr
        }
        try {
            val out = ByteArrayOutputStream()
            val `in` = ByteArrayInputStream(
                BASE64Decoder().decodeBuffer(zippedStr)
            )
            val gzipIs = GZIPInputStream(`in`)
            val buffer = ByteArray(256)
            var n: Int
            while (gzipIs.read(buffer).also { n = it } >= 0) {
                out.write(buffer, 0, n)
            }
            // toString()使用平台默认编码，也可以显式的指定如toString("GBK")
            return out.toString()
        } catch (e: Exception) {
            return zippedStr
        }

    }

    /***
     * 压缩GZip
     *
     * @param data
     * @return
     */
    fun gZip(data: ByteArray?): ByteArray? {
        var b: ByteArray? = null
        try {
            val bos = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(bos)
            gzip.write(data)
            gzip.finish()
            gzip.close()
            b = bos.toByteArray()
            bos.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return b
    }

    /***
     * 解压GZip
     *
     * @param data
     * @return
     */
    fun unGZip(data: ByteArray?): ByteArray? {
        var b: ByteArray? = null
        try {
            val bis = ByteArrayInputStream(data)
            val gzip = GZIPInputStream(bis)
            val buf = ByteArray(1024)
            var num = -1
            val baos = ByteArrayOutputStream()
            while (gzip.read(buf, 0, buf.size).also { num = it } != -1) {
                baos.write(buf, 0, num)
            }
            b = baos.toByteArray()
            baos.flush()
            baos.close()
            gzip.close()
            bis.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return b
    }


    /***
     * 压缩Zip
     *
     * @param data
     * @return
     */
    fun zip(data: ByteArray): ByteArray? {
        var b: ByteArray? = null
        try {
            val bos = ByteArrayOutputStream()
            val zip = ZipOutputStream(bos)
            val entry = ZipEntry("zip")
            entry.size = data.size.toLong()
            zip.putNextEntry(entry)
            zip.write(data)
            zip.closeEntry()
            zip.close()
            b = bos.toByteArray()
            bos.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return b
    }

    /***
     * 解压Zip
     *
     * @param data
     * @return
     */
    fun unZip(data: ByteArray?): ByteArray? {
        var b: ByteArray? = null
        try {
            val bis = ByteArrayInputStream(data)
            val zip = ZipInputStream(bis)
            while (zip.nextEntry != null) {
                val buf = ByteArray(1024)
                var num = -1
                val baos = ByteArrayOutputStream()
                while (zip.read(buf, 0, buf.size).also { num = it } != -1) {
                    baos.write(buf, 0, num)
                }
                b = baos.toByteArray()
                baos.flush()
                baos.close()
            }
            zip.close()
            bis.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return b
    }

//    /***
//     * 压缩BZip2
//     *
//     * @param data
//     * @return
//     */
//    public static byte[] bZip2(byte[] data) {
//        byte[] b = null;
//        try {
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            CBZip2OutputStream bzip2 = new CBZip2OutputStream(bos);
//            bzip2.write(data);
//            bzip2.flush();
//            bzip2.close();
//            b = bos.toByteArray();
//            bos.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return b;
//    }

//    /***
//     * 解压BZip2
//     *
//     * @param data
//     * @return
//     */
//    public static byte[] unBZip2(byte[] data) {
//        byte[] b = null;
//        try {
//            ByteArrayInputStream bis = new ByteArrayInputStream(data);
//            CBZip2InputStream bzip2 = new CBZip2InputStream(bis);
//            byte[] buf = new byte[1024];
//            int num = -1;
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            while ((num = bzip2.read(buf, 0, buf.length)) != -1) {
//                baos.write(buf, 0, num);
//            }
//            b = baos.toByteArray();
//            baos.flush();
//            baos.close();
//            bzip2.close();
//            bis.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return b;
//    }

    //    /***
    //     * 压缩BZip2
    //     *
    //     * @param data
    //     * @return
    //     */
    //    public static byte[] bZip2(byte[] data) {
    //        byte[] b = null;
    //        try {
    //            ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //            CBZip2OutputStream bzip2 = new CBZip2OutputStream(bos);
    //            bzip2.write(data);
    //            bzip2.flush();
    //            bzip2.close();
    //            b = bos.toByteArray();
    //            bos.close();
    //        } catch (Exception ex) {
    //            ex.printStackTrace();
    //        }
    //        return b;
    //    }
    //    /***
    //     * 解压BZip2
    //     *
    //     * @param data
    //     * @return
    //     */
    //    public static byte[] unBZip2(byte[] data) {
    //        byte[] b = null;
    //        try {
    //            ByteArrayInputStream bis = new ByteArrayInputStream(data);
    //            CBZip2InputStream bzip2 = new CBZip2InputStream(bis);
    //            byte[] buf = new byte[1024];
    //            int num = -1;
    //            ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //            while ((num = bzip2.read(buf, 0, buf.length)) != -1) {
    //                baos.write(buf, 0, num);
    //            }
    //            b = baos.toByteArray();
    //            baos.flush();
    //            baos.close();
    //            bzip2.close();
    //            bis.close();
    //        } catch (Exception ex) {
    //            ex.printStackTrace();
    //        }
    //        return b;
    //    }
    /**
     * 把字节数组转换成16进制字符串
     *
     * @param bArray
     * @return
     */
    fun bytesToHexString(bArray: ByteArray): String? {
        val sb = StringBuffer(bArray.size)
        var sTemp: String
        for (i in bArray.indices) {
            sTemp = Integer.toHexString(0xFF and bArray[i].toInt())
            if (sTemp.length < 2) sb.append(0)
            sb.append(sTemp.uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }
}
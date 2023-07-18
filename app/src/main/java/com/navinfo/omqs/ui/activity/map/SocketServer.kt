package com.navinfo.omqs.ui.activity.map

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.TextUtils
import android.util.Log
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.Constant
import com.navinfo.omqs.util.DateTimeUtil
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.net.Socket
import java.util.Collections
import kotlin.math.abs


enum class IndoorToolsStatus {
   PAUSE,
    PLAY,
    NEXT,
    REWIND
}

/**
 * @author qj
 * @version V1.0
 * @Date 2018/4/18.
 * @Description: 轨迹反向控制服务
 */
class SocketServer(
    private val mapController: NIMapController,
    private val traceDataBase: TraceDataBase,
    private val sharedPreferences: SharedPreferences
) : Service() {
    //类标识
    private val TAG = "SocketServer"

    //线程池
    private val threadConnect = ThreadLocal<Socket>()

    //读的线程
    private var tRecv: RecvThread? = null

    //解析线程
    private var tParse: ParseThread? = null

    //输出流
    private var outStr: OutputStream? = null

    //输入流
    private var inStr: InputStream? = null

    //状态
    var connectstatus = false

    //socket
    private var client: Socket? = null

    //接收缓存
    private val sData = ByteArray(512)

    //反馈接口
    private var mListener: OnConnectSinsListener? = null

    //服务
    private val mBinder: MyBinder = MyBinder()

    //接收集合
    private val mTaskList = Collections.synchronizedList(ArrayList<String>())

    //连接线程
    private var connectThread: Thread? = null

    //缓存ip
    private var lastIp = ""
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0x11 -> if (mListener != null) {
                    if (msg.obj != null && msg.obj is NiLocation) {
                        mListener!!.onReceiveLocation(msg.obj as NiLocation)
                    } else {
                        mListener!!.onReceiveLocation(null)
                    }
                }

                0x22 ->                     //索引定位中
                    if (mListener != null) {
                        mListener!!.onIndexing()
                    }

                0x33 -> if (mListener != null) {
                    mListener!!.onConnect(true)
                }

                0x44 -> if (mListener != null) {
                    mListener!!.onConnect(false)
                }

                0x55 -> if (mListener != null) {
                    mListener!!.onPlay()
                }

                0x66 -> if (mListener != null) {
                    mListener!!.onStop()
                }

                0x99 -> if (mListener != null) {
                    mListener!!.onParseEnd()
                }

                0x999 -> if (mListener != null) {
                    mListener!!.onConnect(false)
                    disconnect()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class MyBinder : Binder() {
        // 返回Activity所关联的Service对象，这样在Activity里，就可调用Service里的一些公用方法  和公用属性
        val service: SocketServer
            get() =// 返回Activity所关联的Service对象，这样在Activity里，就可调用Service里的一些公用方法  和公用属性
                this@SocketServer
    }

    /**
     * 启动sock连接
     *
     * @param ip
     * @param listener 结果回调
     */
    fun connect(ip: String, listener: OnConnectSinsListener?) {
        if (connectThread != null && connectThread!!.isAlive && TextUtils.equals(lastIp, ip)) {
            return
        }
        mListener = listener
        lastIp = ip
        connectThread = object : Thread() {
            override fun run() {
                try {
                    client = threadConnect.get()
                    if (client == null) {
                        client = Socket(ip, 8010)
                        client!!.soTimeout = 3000000
                        client!!.keepAlive = true
                        threadConnect.set(client)
                    }
                    outStr = client!!.getOutputStream()
                    inStr = client!!.getInputStream()
                    if (tRecv != null) {
                        tRecv!!.cancel()
                    }
                    tRecv = RecvThread()
                    val thread = Thread(tRecv)
                    thread.start()

                    //解析线程
                    if (tParse != null) {
                        tParse!!.cancel()
                    }
                    tParse = ParseThread()
                    val parsethread = Thread(tParse)
                    parsethread.start()

                    //socket启动成功
                    val msg = Message()
                    msg.what = 0x33
                    mHandler.sendMessage(msg)
                    if (!connectstatus) {
                        connectstatus = true // 更改连接状态
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    //启动失败
                    val msg = Message()
                    msg.what = 0x44
                    mHandler.sendMessage(msg)
                }
            }
        }
        (connectThread as Thread).start()
    }

    /**
     * sock是否启动
     *
     * @return true 启动 false停止
     */
    val isStart: Boolean
        get() = if (connectThread != null && connectThread!!.isAlive) {
            true
        } else false

    /**
     * 销毁连接
     */
    fun disconnect() {
        try {

            //销毁线程
            if (tRecv != null) {
                tRecv!!.cancel()
            }

            //销毁线程
            if (tParse != null) {
                tParse!!.cancel()
            }
        } catch (e: Exception) {
        }
        try {
            if (outStr != null) outStr!!.close()
            if (inStr != null) inStr!!.close()
            if (client != null) client!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 解析接收到得线程
     */
    private inner class ParseThread : Runnable {
        private var runFlag = true

        //轨迹时间buffer
        private val traceTimeBuffer = 1500
        private var timeIndex = 0
        fun cancel() {
            runFlag = false
        }

        override fun run() {
            try {
                while (runFlag) {
                    if (mTaskList.size > 0) {
                        timeIndex = mTaskList.size - 1
                        val result = parseResult(mTaskList[timeIndex])
                        var resultNiLocation: NiLocation? = null
                        if (result != null) {
                            when (result.type) {
                                1 -> {
                                    //先暂停播放
                                    val msg = Message()
                                    msg.what = 0x22
                                    mHandler.sendMessage(msg)
                                    val currentTime: Long = DateTimeUtil.getTimePointSSS(
                                        result.data
                                    )
                                    val currentTimeStr: String = DateTimeUtil.TimePointSSSToTime(
                                        result.data
                                    )
                                    val startTime = currentTime - traceTimeBuffer
                                    val endTme = currentTime + traceTimeBuffer

                                    //转换为数据库时间
                                    val startTimeStr: String =
                                        DateTimeUtil.getDateSimpleTime(startTime)

                                    //转换为数据库时间
                                    val endTimeStr: String =
                                        DateTimeUtil.getDateSimpleTime(endTme)
                                    if (!TextUtils.isEmpty(startTimeStr) && !TextUtils.isEmpty(
                                            endTimeStr
                                        )
                                    ) {
                                        Log.e(TAG, "getTraceData开始")
                                        val list: List<NiLocation>? =
                                            getTrackList(startTimeStr, endTimeStr, currentTimeStr)
                                        Log.e(TAG, "getTraceData结束")
                                        if (list != null && list.size > 0) {
                                            var disTime: Long = 0


                                            //只有一个点不进行判断直接返回结果
                                            if (list.size == 1) {
                                                resultNiLocation = list[0]
                                            } else {

                                                //遍历集合取最近时间的轨迹点
                                                b@ for (nilocation in list) {
                                                    if (!TextUtils.isEmpty(nilocation.time)) {

                                                        //只获取到秒的常量
                                                        val time: Long =
                                                            nilocation.timeStamp.toLong()

                                                        val disTimeTemp = abs(time - currentTime)

                                                        //如果时间相同直接返回该点
                                                        if (disTimeTemp == 0L) {
                                                            resultNiLocation = nilocation
                                                            break@b
                                                        } else {

                                                            //第一次不对比，取当前值
                                                            if (disTime == 0L) {
                                                                disTime = disTimeTemp
                                                                resultNiLocation =
                                                                    nilocation
                                                            } else {

                                                                //前一个差值大于当前差值则取当前相对小的值
                                                                if (disTime - disTimeTemp > 0) {
                                                                    disTime = disTimeTemp
                                                                    resultNiLocation =
                                                                        nilocation
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    val msg1 = Message()
                                    msg1.what = 0x11
                                    msg1.obj = resultNiLocation
                                    mHandler.sendMessage(msg1)
                                }

                                2 -> {
                                    val msg4 = Message()
                                    msg4.what = 0x55
                                    mHandler.sendMessage(msg4)
                                }

                                3 -> {
                                    val msg5 = Message()
                                    msg5.what = 0x66
                                    mHandler.sendMessage(msg5)
                                }
                            }
                        }


                        //解析时索引与集合索引对比，如果不相同代表有新命令，需要继续解析最后一条，否则清空集合不在解析
                        try {
                            if (timeIndex == mTaskList.size - 1) {
                                mTaskList.clear()
                            }
                        } catch (e: Exception) {
                        }
                        val msg2 = Message()
                        msg2.what = 0x99
                        mHandler.sendMessage(msg2)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = Message()
                msg.what = 0x99
                mHandler.sendMessage(msg)
            }
        }
    }

    /**
     * 获取轨迹数据
     *
     * @param startTimeStr   起始时间
     * @param endTimeStr     结束时间
     * @param currentTimeStr 当前点时间，如果存在便直接获取一个点
     * @return list  数据集合
     */
    private fun getTrackList(
        startTimeStr: String,
        endTimeStr: String,
        currentTimeStr: String
    ): List<NiLocation>? {
        if (!TextUtils.isEmpty(startTimeStr) && !TextUtils.isEmpty(endTimeStr)) {
            var startTime: Long = 0
            var endTime: Long = 0
            try {
                startTime = startTimeStr.toLong()
                endTime = endTimeStr.toLong()
            } catch (e: java.lang.Exception) {
            }
            if (startTime != 0L && endTime != 0L) {

                val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)

                val list: MutableList<NiLocation> = traceDataBase.niLocationDao.taskIdAndTimeTofindList(id.toString(),startTime,endTime)

                if (list.size > 0) return list
            }
        }
        return null
    }

    /**
     * 接收管道数据
     */
    private inner class RecvThread : Runnable {
        private var runFlag = true
        fun cancel() {
            runFlag = false
        }

        override fun run() {
            var rlRead: Int
            try {
                while (runFlag) {
                    var line: String = ""
                    if (!isServerClose) {
                        rlRead = inStr!!.read(sData) //对方断开返回-1
                        if (rlRead > 0) {
                            Log.e(TAG, sData.toString() + "")
                            line = String(sData, 0, rlRead)
                            mTaskList.add(line)
                        } else {
                            connectFaild("连接断开")
                        }
                    } else {
                        connectFaild("连接断开")
                    }
                }
            } catch (e: IOException) {
                connectFaild(e.toString())
                e.printStackTrace()
            }
        }
    }

    /**
     * 连接失败
     * @param e 原因
     */
    private fun connectFaild(e: String) {
        val msg2 = Message()
        msg2.what = 0x999
        mHandler.sendMessage(msg2)
    }

    /**
     * 判断是否断开连接，断开返回true,没有返回false
     * @return
     */
    val isServerClose: Boolean
        get() {
            return try {
                client!!.sendUrgentData(0) //发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
                false
            } catch (se: Exception) {
                true
            }
        }

    /**
     * 停止接收管道数据
     */
    fun stop() {
        Log.e(TAG, "stop!")
        connectstatus = false
        if (tRecv != null) {
            tRecv!!.cancel()
        }
        if (tParse != null) {
            tParse!!.cancel()
        }
    }

    /**
     * 开始接收管道数据
     */
    fun start() {
        Log.e(TAG, "start!")
        if (tRecv != null) {
            tRecv!!.cancel()
        }
        tRecv = RecvThread()
        val thread = Thread(tRecv)
        thread.start()

        //解析线程
        if (tParse != null) {
            tParse!!.cancel()
        }
        tParse = ParseThread()
        val parsethread = Thread(tParse)
        parsethread.start()
    }

    fun setTraceMap() {

    }

    /**
     * 轨迹反向控制回调接口
     */
    interface OnConnectSinsListener {
        /**
         * 连接状态
         *
         * @param success true 连接成功 false 连接失败
         */
        fun onConnect(success: Boolean)

        /**
         * 索引中
         */
        fun onIndexing()

        /**
         * 暂停
         */
        fun onStop()

        /**
         * 播放
         */
        fun onPlay()

        /**
         * 结束完成
         */
        fun onParseEnd()

        /**
         * 轨迹点
         *
         * @param mNiLocation
         */
        fun onReceiveLocation(mNiLocation: NiLocation?)
    }

    /**
     * 解析返回值
     *
     * @return 时间信息
     */
    private fun parseResult(data: String): Result? {
        var data = data
        if (!TextUtils.isEmpty(data)) {
            try {
                data = data.replace("\n".toRegex(), "")
                val json = JSONObject(data)
                val type = json.optInt("type")
                val mResult: Result = Result()
                mResult.type = type
                if (type == 1) {
                    mResult.data = json.optString("data", "")
                }
                return mResult
            } catch (e: Exception) {
            }
        }
        return null
    }

    //结果类对象
    internal inner class Result : Serializable {
        var type = 0
        var data: String? = null
    }
}
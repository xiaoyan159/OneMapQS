package com.navinfo.omqs.util

private data class TimePeriod(
    //年
    var year: String = "",
    //月
    var month: String = "",
    //日
    var day: String = "",
    //时
    var hour: String = "",
    //分
    var minutes: String = "",
    //周
    var week: String = "",
)


private data class TimePeriodObject(
    //开始时间
    var startTime: TimePeriod? = null,
    //结束时间
    var endTime: TimePeriod? = null,
    //是否是节假日
    var bH: Boolean = false,
    //是否是节假日除外
    var b_H: Boolean = false,
    //动态变化
    var bVMS: Boolean = false,
    //最终输出
    var res: String = ""
) {
    /**
     * 交集 *
     */
    fun intersection(periodObject: TimePeriodObject) {
        if (res != "" && periodObject.res == "") {
            res = "${res}${periodObject.toText()}"

        } else if (res == "" && periodObject.res != "") {
            res = "${toText()}${periodObject.res}"
        } else if (res != "" && periodObject.res != "") {
            res = "${res}${periodObject.res}"
        } else {
            if (startTime == null) {
                startTime = periodObject.startTime
            } else if (periodObject.startTime != null) {
                if (startTime!!.year == "")
                    startTime!!.year = periodObject.startTime!!.year
                if (startTime!!.month == "")
                    startTime!!.month = periodObject.startTime!!.month
                if (startTime!!.day == "")
                    startTime!!.day = periodObject.startTime!!.day
                if (startTime!!.hour == "")
                    startTime!!.hour = periodObject.startTime!!.hour
                if (startTime!!.week == "")
                    startTime!!.week = periodObject.startTime!!.week
                if (startTime!!.minutes == "")
                    startTime!!.minutes = periodObject.startTime!!.minutes
            }

            if (endTime == null) {
                endTime = periodObject.endTime
            } else if (periodObject.endTime != null) {
                if (endTime!!.year == "")
                    endTime!!.year = periodObject.endTime!!.year
                if (endTime!!.month == "")
                    endTime!!.month = periodObject.endTime!!.month
                if (endTime!!.day == "")
                    endTime!!.day = periodObject.endTime!!.day
                if (endTime!!.hour == "")
                    endTime!!.hour = periodObject.endTime!!.hour
                if (endTime!!.week == "")
                    endTime!!.week = periodObject.endTime!!.week
                if (endTime!!.minutes == "")
                    endTime!!.minutes = periodObject.endTime!!.minutes
            }

            if (!bH)
                bH = periodObject.bH
            if (!b_H)
                b_H = periodObject.b_H
            if (!bVMS)
                bVMS = periodObject.bVMS
        }
        println("交集 $res")
    }

    /**
     *  合集 +
     */
    fun compilation(periodObject: TimePeriodObject) {
        res = if (res != "" && periodObject.res == "") {
            "${res},${periodObject.toText()}"
        } else if (res == "" && periodObject.res != "") {
            "${toText()},${periodObject.res}"
        } else if (res != "" && periodObject.res != "") {
            "${res},${periodObject.res}"
        } else
            "${toText()},${periodObject.toText()}"
        println("合集 $res")
    }


    /**
     *
     */
    fun toText(): String {
        if (res == "") {
            startTime?.let {
                if (it.year != "") {
                    res = "${it.year}年"
                }
                if (it.month != "") {
                    res = if (it.year == "") {
                        "每年${it.month}月"
                    } else {
                        "$res${it.month}月"
                    }
                }
                if (it.day != "") {
                    res = if (it.month == "") {
                        "${res}每月${it.day}日"
                    } else
                        "$res${it.day}日"
                }
            }
            endTime?.let {
                if (it.year != "" || it.month != "" || it.day != "")
                    res = "${res}到"
                if (it.year != "") {
                    res = "${res}${it.year}年"
                }
                if (it.month != "") {
                    res = "$res${it.month}月"
                }
                if (it.day != "") {
                    res = "$res${it.day}日"
                }
            }

            startTime?.let {
                if (it.week != "") {
                    res = "${res}每${getWeekEnum(it.week)}"
                }
            }

            endTime?.let {
                if (it.week != "") {
                    res = "${res}到${getWeekEnum(it.week)}"
                }
            }

            startTime?.let {
                if (it.hour != "") {
                    res = if (it.minutes != "") {
                        if (it.minutes.length == 1) {
                            "${res}${it.hour}:0${it.minutes}"
                        } else {
                            "${res}${it.hour}:${it.minutes}"
                        }
                    } else {
                        "${res}:00"
                    }
                }
            }

            endTime?.let {
                if (it.hour != "") {
                    res = if (it.minutes != "") {
                        if (it.minutes.length == 1) {
                            "${res}-${it.hour}:0${it.minutes}"
                        } else {
                            "${res}-${it.hour}:${it.minutes}"
                        }
                    } else {
                        "${res}-${it.hour}:00"
                    }
                }
            }
            if (bH) {
                res = "节假日:$res"
            }
            if (b_H) {
                res = "节假日除外${res}:"
            }
            if (bVMS) {
                res = "动态变化:$res"
            }
        }
        println("中间：$res")
        return res
    }

    private fun getWeekEnum(s: String): String {
        return when (s) {
            "1" -> "周日"
            "2" -> "周一"
            "3" -> "周二"
            "4" -> "周三"
            "5" -> "周四"
            "6" -> "周五"
            "7" -> "周六"
            else -> ""
        }
    }
}

private enum class TimeType {
    NONE,
    YEAR,
    MONTH,
    DAY,
    HOUR,
    MINUTES,
    WEEK,
    HLD,
    _HLD,
    VMS
}

class TimePeriodUtil {

    companion object {
        fun getTimePeriod(time: String): String {
            println("时间段：$time")
            var i = 0
            val charArray = time.toCharArray()
            val list = mutableListOf<TimePeriodObject>()
            while (i < charArray.size) {
                when (charArray[i]) {
                    '[' -> {
                        i = getPeriodObject(charArray, i + 1, list)
                    }
                    '*' -> {
                        i = getPeriodObject(charArray, i + 1, list)
                        if (list.size > 1) {
                            list[0].intersection(list[1])
                            list.removeAt(1)
                        }
                    }
                    '+' -> {
                        i = getPeriodObject(charArray, i + 1, list)
                        if (list.size > 1) {
                            list[0].compilation(list[1])
                            list.removeAt(1)
                        }
                    }
                }
                i++
            }
            if (list.size > 0)
                return list[0].toText()
            return ""
        }

        private fun getPeriodObject(
            charArray: CharArray,
            index: Int,
            parentList: MutableList<TimePeriodObject>,
        ): Int {
            var i = index
            val list = mutableListOf<TimePeriodObject>()
            while (i < charArray.size) {
                when (charArray[i]) {
                    '[' -> {
                        i = getPeriodObject(charArray, i + 1, list)
                    }
                    '(' -> {
                        var parentPeriodObject = TimePeriodObject()
                        i = getMixUnit(charArray, i, parentPeriodObject)
                        parentList.add(parentPeriodObject)
                        return i
                    }
                    '*' -> {
                        i = getPeriodObject(charArray, i + 1, list)
                        if (list.size > 1) {
                            list[0].intersection(list[1])
                            list.removeAt(1)
                        }
                    }
                    '+' -> {
                        i = getPeriodObject(charArray, i + 1, list)
                        if (list.size > 1) {
                            list[0].compilation(list[1])
                            list.removeAt(1)
                        }
                    }
                    'H', '-', 'V' -> {
                        i = getOther(charArray, i, parentList)
                        return i
                    }
                    ']' -> {
                        if (list.isNotEmpty()) {
                            parentList.add(list[0])
                        }
                        return i
                    }
                }
                i++
            }
            return i
        }

        private fun getOther(
            charArray: CharArray,
            index: Int,
            parentList: MutableList<TimePeriodObject>
        ): Int {
            var i = index
            var timeType = TimeType.NONE
            while (i < charArray.size) {
                when (charArray[i]) {
                    'H' -> {
                        if (timeType == TimeType.NONE) {
                            timeType = TimeType.HLD
                            parentList.add(TimePeriodObject(bH = true))
                        }
                    }
                    '-' -> {
                        if (timeType == TimeType.NONE) {
                            timeType = TimeType._HLD
                            parentList.add(TimePeriodObject(b_H = true))
                        }
                    }
                    'V' -> {
                        if (timeType == TimeType.NONE) {
                            timeType = TimeType.VMS
                            parentList.add(TimePeriodObject(bVMS = true))
                        }
                    }
                    ']' -> {
                        return i - 1
                    }
                }
                i++
            }
            return i
        }

        /**
         * 单元
         */
        private fun getMixUnit(
            charArray: CharArray,
            index: Int,
            parentPeriodObject: TimePeriodObject
        ): Int {
            var i = index

            while (i < charArray.size) {
                when (charArray[i]) {
                    '(' -> {
                        val timePeriod = TimePeriod()
                        i = getNumString(charArray, i + 1, timePeriod)
                        if (parentPeriodObject.startTime == null) {
                            parentPeriodObject.startTime = timePeriod
                        } else {
                            parentPeriodObject.endTime = timePeriod
                        }
                    }
//                    '{' -> {
//                        val timePeriod = TimePeriod()
//                        i = getNumString(charArray, i + 1, timePeriod)
//                        parentPeriodObject.allTime = true
//                    }
                    ']' -> {
                        return i - 1
                    }
                }
                i++
            }
            return i
        }

        /**
         * 解析数字
         */
        private fun getNumString(
            charArray: CharArray,
            index: Int,
            parentPeriod: TimePeriod
        ): Int {
            var i = index
            var timeType = TimeType.NONE
            val resBuffer = StringBuffer()
            while (i < charArray.size) {
                val char = charArray[i]
                //拼数字
                if (char.isDigit() && timeType != TimeType.NONE) {
                    resBuffer.append(char)
                } else {
                    when (timeType) {
                        TimeType.YEAR -> parentPeriod.year = resBuffer.toString()
                        TimeType.MONTH -> parentPeriod.month = resBuffer.toString()
                        TimeType.DAY -> parentPeriod.day = resBuffer.toString()
                        TimeType.HOUR -> parentPeriod.hour = resBuffer.toString()
                        TimeType.MINUTES -> parentPeriod.minutes = resBuffer.toString()
                        TimeType.WEEK -> parentPeriod.week = resBuffer.toString()
                        else -> {}
                    }
                    timeType = when (char) {
                        'y' -> TimeType.YEAR
                        'M' -> TimeType.MONTH
                        'd' -> TimeType.DAY
                        'h' -> TimeType.HOUR
                        'm' -> TimeType.MINUTES
                        't' -> TimeType.WEEK
                        ')', '}' -> {
                            return i
                        }
                        else -> TimeType.NONE
                    }
                    resBuffer.setLength(0)
                }
                i++
            }
            return i
        }

    }
}

fun main() {
    var t = ""
    var text = ""
    t = "[(y2010M8d16)(y2010M9d17)]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("1:$text")
    t = "[(y2010M8d17){d1}]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("2:$text")
    t = "[(M8d17)(M8d31)]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("3:$text")
    t = "[(M8d17){d1}]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("4:$text")
    t = "[(h9m0)(h23m59)]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("5:$text")
    t = "[(y2010)(y2030)]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("6:$text")
    t = "[(y2010){y1}]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("8:$text")
    t = "[(y2010M8t1){t1}]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("9:$text")
    t = "[(y2010M8t1)(y2010M8t3)]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("10:$text")
    t = "[(t4)(t1)]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("11:$text")
    t = "[(t4){d1}]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("12:$text")

    t = "[[(y2010M8d8)(y2010M8d24)]*[(h7m0)(h22m0)]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("13:$text")

    t = "[[(M8d1)(M8d31)]*[(t3){d1}]*[(h6m0)(h19m0)]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("14:$text")

    t = "[[[(t4)(t6)]*[(h7m30)(h12m0)]]+[[(t4)(t6)]*[(h14m0)(h20m0)]]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("15:$text")

    t = "[[[(M5d7){d1}]*[(h9m0)(h23m59)]]+[(M5d8)(M8d14)]+[[(M8d15){d1}]*[(h0m0)(h21m0)]]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("16:$text")

    t = "[[HLD]*[(h8m0)(h16m0)]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("17:$text")

    t = "[[-HLD]*[[[(M5d7){d1}]*[(h9m0)(h23m59)]]+[(M5d8)(M8d14)]+[[(M8d15){d1}]*[(h0m0)(h21m0)]]]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("18:$text")

    t = "[[VMS]*[(h8m0)(h16m0)]]"
    text = TimePeriodUtil.getTimePeriod(t)
    println("19:$text")
}
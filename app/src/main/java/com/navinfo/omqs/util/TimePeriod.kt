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
    //大括号，全部
    var allTime: TimePeriod? = null,
) {
    /**
     * 交集 *
     */
    fun intersection(periodObject: TimePeriodObject) {
        startTime?.let { one ->
            periodObject.startTime?.let { two ->
                if (one.year == "") {
                    one.year = two.year
                }
                if (one.month == "") {
                    one.month = two.month
                }
                if (one.day == "") {
                    one.day = two.day
                }
                if (one.hour == "") {
                    one.hour = two.hour
                }
                if (one.minutes == "") {
                    one.minutes = two.minutes
                }
                if (one.week == "") {
                    one.week = two.week
                }
            }
        }
    }

    /**
     *  合集 +
     */
    fun compilation(periodObject: TimePeriodObject) {

    }

    /**
     *
     */
    fun toText(): String {
        if(endTime == null && allTime == null){

        }
        return ""
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
}

class TimePeriodUtil {

    companion object {
        private fun getTimePeriod(time: String): String {
            val list = mutableListOf<TimePeriodObject>()
            getPeriodObject(time.toCharArray(), 0, list)
            return list[0].toText()
        }

        private fun getPeriodObject(
            charArray: CharArray,
            index: Int,
            parentList: MutableList<TimePeriodObject>
        ): Int {
            var i = index
            while (i < charArray.size) {
                when (charArray[i]) {
                    '[' -> {
                        i = getPeriodObject(charArray, i, parentList)
                    }
                    '(' -> {
                        var parentPeriodObject = TimePeriodObject()
                        i = getMixUnit(charArray, i, parentPeriodObject)
                        parentList.add(parentPeriodObject)
                    }
                    '*' -> {
                        i = getPeriodObject(charArray, i + 1, parentList)
                        if (parentList.size > 1) {
                            val o = parentList[0]
                            parentList[0].intersection(parentList[1])
                            parentList.removeAt(1)
                        }
                    }
                    '+' -> {
                        i = getPeriodObject(charArray, i + 1, parentList)
                        if (parentList.size > 1) {
                            val o = parentList[0]
                            parentList[0].compilation(parentList[1])
                            parentList.removeAt(1)
                        }
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
                    '{' -> {
                        val timePeriod = TimePeriod()
                        i = getNumString(charArray, i + 1, timePeriod)
                        parentPeriodObject.allTime = timePeriod
                    }
                    ']' -> {
                        return i
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

}
package com.navinfo.omqs.bean

data class RoadNameBean(
    /**
     * 道路名称
     */
    val name: String = "",
    /**
     * 0 普通
     * 1 立交桥名（连接路）
     * 2 立交桥名 （主路）
     * 3 风景线路
     * 5 隧道
     * 6 虚拟名称
     */
    val type: Int = 0,
    /**
     * 1 不论“名称分类”是官方名，别名还是曾用名都统一从1开始递增
     * 2 若取第一官方名时，需判断“名称”分类 [nameClass]=="官方名"，且[seqNum] 最小的
     */
    val seqNum: Int = 1,
    /**
     * 1 官方名
     * 2 别名
     * 3 曾用名
     */
    val nameClass: Int = 1,
) {
    fun getNameClassStr(): String {
        when (nameClass) {
            1 -> return "官方名"
            2 -> return "别名"
            3 -> return "曾用名"
        }
        return ""
    }

    fun getTypeStr(): String {
        when (type) {
            0 -> return "普通"
            1 -> return "立交桥名(连接路)"
            2 -> return "立交桥名(主路)"
            3 -> return "风景线路"
            5 -> return "隧道"
            6 -> return "虚拟名称"
        }
        return ""
    }
}
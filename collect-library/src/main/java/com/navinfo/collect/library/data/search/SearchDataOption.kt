package com.navinfo.collect.library.data.search

class SearchDataOption private constructor(
    /**
     *  关键字
     */
    val keyword: String,

    /**
     * 当前页数
     */
    val pageNum: Int,

    /**
     * 每页最多数量
     */
    val pageCapacity: Int,
    /**
     * 在工程级别范围内搜索
     */
    val projectItemList: ArrayList<OptionProjectItem>,

    /**
     * 在图层级别范围内搜
     */
    val layerItemList: ArrayList<OptionLayerItem>,

    /**
     * 搜索某个工程下，某个图层下的，某个字段
     */
    val fieldItemList: ArrayList<OptionFieldItem>,

    /**
     *  时间范围，开始时间
     */
    val startTime: Int,

    /**
     * 时间范围，结束时间
     */
    val endTime: Int

) {
    private constructor (builder: Builder) : this(
        builder.keyword,
        builder.pageNum,
        builder.pageCapacity,
        builder.projectItemList,
        builder.layerItemList,
        builder.fieldItemList,
        builder.startTime,
        builder.endTime,
    )

    class Builder {
        var keyword: String = ""
            private set
        var pageNum: Int = 0
            private set
        var pageCapacity: Int = 20
            private set
        var projectItemList = ArrayList<OptionProjectItem>()
            private set
        var layerItemList = ArrayList<OptionLayerItem>()
            private set
        var fieldItemList = ArrayList<OptionFieldItem>()
            private set
        var startTime: Int = -1
            private set
        var endTime: Int = -1
            private set


        fun build() = SearchDataOption(this)

        fun setKeyword(id: String) = apply {
            this.keyword = id;
        }

        fun setStartTime(time: Int) = apply {
            this.startTime = time;
        }

        fun setEndTime(time: Int) = apply {
            this.endTime = time;
        }

        fun setPageNum(num: Int) = apply {
            this.pageNum = num;
        }

        fun setPageCapacity(capacity: Int) = apply {
            this.pageCapacity = capacity;
        }

        fun setLayerItems(list: ArrayList<OptionLayerItem>) = apply {
            this.layerItemList = list
        }


        fun setProjectItems(list: ArrayList<OptionProjectItem>) = apply {
            this.projectItemList = list
        }

        fun setFieldItems(list: ArrayList<OptionFieldItem>) = apply {
            this.fieldItemList = list
        }
    }
}
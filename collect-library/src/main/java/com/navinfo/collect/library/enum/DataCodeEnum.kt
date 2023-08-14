package com.navinfo.collect.library.enum

/*
* 要素枚举定义
* */
enum class DataCodeEnum(var tableName: String, var code: String) {
    OMDB_CHECKPOINT("检查点", "1012"), OMDB_RD_LINK(
        "道路线",
        "2001"
    ),
    OMDB_RD_LINK_FUNCTION_CLASS("道路功能等级", "2002"),
    OMDB_LINK_ATTRIBUTE_SA("道路属性-SA", "2004-1"), OMDB_LINK_ATTRIBUTE_PA(
        "道路属性-PA",
        "2004-2"
    ),
    OMDB_RD_LINK_KIND("道路种别", "2008"),
    OMDB_LINK_DIRECT("道路方向", "2010"), OMDB_LINK_NAME(
        "道路名",
        "2011"
    ),
    OMDB_LANE_MARK_BOUNDARYTYPE("车道边界类型", "2013"),
    OMDB_LINK_SPEEDLIMIT("常规线限速", "2019"), OMDB_LINK_SPEEDLIMIT_COND(
        "条件线限速",
        "2020"
    ),
    OMDB_LINK_SPEEDLIMIT_VAR("可变线限速", "2021"),
    OMDB_CON_ACCESS("全封闭", "2022"), OMDB_FORM_OF_WAY(
        "匝道",
        "2037"
    ),
    OMDB_MULTI_DIGITIZED("上下线分离", "2040"), OMDB_LANE_NUM("车道数", "2041"),
    OMDB_RDBOUND_BOUNDARYTYPE("道路边界类型", "2083"), OMDB_BRIDGE(
        "桥",
        "2201"
    ),
    OMDB_TUNNEL("隧道", "2202"), OMDB_INTERSECTION("路口", "4001"),
    OMDB_SPEEDLIMIT("常规点限速", "4002"), OMDB_SPEEDLIMIT_COND(
        "条件点限速",
        "4003"
    ),
    OMDB_SPEEDLIMIT_VAR("可变点限速", "4004"), OMDB_RESTRICTION("普通交限", "4006"),
    OMDB_ELECTRONICEYE("电子眼", "4010"), OMDB_TRAFFICLIGHT(
        "交通灯",
        "4022"
    ),
    OMDB_LANEINFO("车信", "4601"), OMDB_LANE_LINK_LG("车道中心线", "5001")
}


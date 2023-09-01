package com.navinfo.collect.library.enums

/*
* 要素枚举定义
* */
enum class DataCodeEnum(var tableName: String, var code: String) {
    OMDB_NODE_FORM("点形态", "1007-6"),
    OMDB_NODE_PA("点形态PA", "1007-6"),
    OMDB_CHECKPOINT("检查点", "1012"),
    OMDB_RD_LINK("道路线", "2001"),
    OMDB_RD_LINK_FUNCTION_CLASS("道路功能等级", "2002"),
    OMDB_LINK_ATTRIBUTE("道路属性", "2004"),
    OMDB_LINK_ATTRIBUTE_SA("道路属性-SA", "2004-1"),
    OMDB_LINK_ATTRIBUTE_PA("道路属性-PA", "2004-2"),
    OMDB_LINK_ATTRIBUTE_FORNTAGE("道路属性-辅路", "2004-3"),
    OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS("道路属性-主辅路出入口", "2004-4"),
    OMDB_RD_LINK_KIND("道路种别", "2008"),
    OMDB_LINK_DIRECT("道路方向", "2010"),
    OMDB_LINK_NAME("道路名", "2011"),
    OMDB_LANE_MARK_BOUNDARYTYPE("车道边界类型", "2013"),
    OMDB_LINK_CONSTRUCTION("道路施工", "2017"),
    OMDB_LINK_SPEEDLIMIT("常规线限速", "2019"),
    OMDB_LINK_SPEEDLIMIT_COND("条件线限速", "2020"),
    OMDB_LINK_SPEEDLIMIT_VAR("可变线限速", "2021"),
    OMDB_CON_ACCESS("全封闭", "2022"),
    OMDB_RAMP("匝道", "2037"),
    OMDB_RAMP_1("普通路连接匝道", "2037-1"),
    OMDB_RAMP_2("高速入口匝道", "2037-2"),
    OMDB_RAMP_3("高速出口匝道", "2037-3"),
    OMDB_RAMP_4("高速连接匝道", "2037-4"),
    OMDB_RAMP_5("高速直连入口匝道", "2037-5"),
    OMDB_RAMP_6("高速直连出口匝道", "2037-6"),
    OMDB_RAMP_7("高速直连出口匝道高速出入口匝道", "2037-7"),
    OMDB_MULTI_DIGITIZED("上下线分离", "2040"),
    OMDB_LANE_NUM("车道数", "2041"),
    OMDB_VIADUCT("高架", "2043"),
    OMDB_RDBOUND_BOUNDARYTYPE("道路边界类型", "2083"),
    OMDB_LANE_CONSTRUCTION("车道施工", "2090"),
    OMDB_BRIDGE("桥", "2201"),
    OMDB_TUNNEL("隧道", "2202"),
    OMDB_ROUNDABOUT("环岛", "2204"),
    OMDB_LINK_FORM1("道路形态1", "2205"),
    OMDB_LINK_FORM1_1("U-Turn", "2205-1"),
    OMDB_LINK_FORM1_2("提前右转", "2205-2"),
    OMDB_LINK_FORM1_3("提前左转", "2205-3"),
    OMDB_LINK_FORM2("道路形态2", "2206"),
    OMDB_LINK_FORM2_1("IC", "2206-1"),
    OMDB_LINK_FORM2_2("JCT", "2206-2"),
    OMDB_LINK_FORM2_3("跨线地道", "2206-3"),
    OMDB_LINK_FORM2_4("私道", "2206-4"),
    OMDB_LINK_FORM2_5("步行街", "2206-5"),
    OMDB_LINK_FORM2_6("公交专用道", "2206-6"),
    OMDB_LINK_FORM2_7("POI 连接路", "2206-7"),
    OMDB_LINK_FORM2_8("区域内道路", "2206-8"),
    OMDB_LINK_FORM2_9("停车场出入口连接路", "2206-9"),
    OMDB_LINK_FORM2_10("停车场内部虚拟连接路", "2206-10"),
    OMDB_LINK_FORM2_11("风景路线", "2206-11"),
    OMDB_LINK_FORM2_12("车辆测试路段", "2206-12"),
    OMDB_LINK_FORM2_13("驾照考试路段", "2206-13"),
    OMDB_OBJECT_TEXT("文字","3002"),
    OMDB_CROSS_WALK("人行横道", "3014"),
    OMDB_OBJECT_STOPLOCATION("停止位置", "3016"),
    OMDB_INTERSECTION("路口", "4001"),
    OMDB_SPEEDLIMIT("常规点限速", "4002"),
    OMDB_SPEEDLIMIT_COND("条件点限速", "4003"),
    OMDB_SPEEDLIMIT_VAR("可变点限速", "4004"),
    OMDB_RESTRICTION("普通交限", "4006"),
    OMDB_WARNINGSIGN("警示信息", "4009"),
    OMDB_ELECTRONICEYE("电子眼", "4010"),
    OMDB_TRAFFICLIGHT("交通灯", "4022"),
    OMDB_TOLLGATE("收费站", "4023"),
    OMDB_LANEINFO("车信", "4601"),
    OMDB_LANE_LINK_LG("车道中心线", "5001");

    companion object {
        fun findTableNameByCode(code: String): String {
            for (enumInstance in DataCodeEnum.values()) {
                if (enumInstance.code == code) {
                    return enumInstance.tableName
                }
            }
            return "" // 若未找到匹配的 code，则返回 null 或其他适当的默认值
        }
    }
}



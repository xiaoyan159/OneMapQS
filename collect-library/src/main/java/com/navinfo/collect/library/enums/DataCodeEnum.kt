package com.navinfo.collect.library.enums

/*
* 要素枚举定义
* */
public enum class DataCodeEnum(var tableName: String,var tableSubName: String, var code: String) {
    OMDB_NODE_FORM("点形态","点形态", "1007-6"),
    OMDB_NODE_PA("点形态PA","点形态PA", "1007-6"),
    OMDB_CHECKPOINT("检查点","检查点", "1012"),
    OMDB_RD_LINK("道路线","道路线", "2001"),
    OMDB_RD_LINK_FUNCTION_CLASS("道路功能等级", "功能等级", "2002"),
    OMDB_LINK_ATTRIBUTE("道路属性","道路属性", "2004"),
    OMDB_LINK_ATTRIBUTE_SA("道路属性-SA","道路属性", "2004-1"),
    OMDB_LINK_ATTRIBUTE_PA("道路属性-PA","道路属性", "2004-2"),
    OMDB_LINK_ATTRIBUTE_FORNTAGE("道路属性-辅路","道路属性", "2004-3"),
    OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS("道路属性-主辅路出入口","道路属性", "2004-4"),
    OMDB_RD_LINK_KIND("道路种别","道路种别", "2008"),
    OMDB_LINK_DIRECT("道路方向","道路方向", "2010"),
    OMDB_LINK_NAME("道路名","道路名", "2011"),
    OMDB_LANE_MARK_BOUNDARYTYPE("车道边界类型","车道边界", "2013"),
    OMDB_LINK_CONSTRUCTION("道路施工","道路施工", "2017"),
    OMDB_LINK_SPEEDLIMIT("常规线限速","常规线限速", "2019"),
    OMDB_LINK_SPEEDLIMIT_COND("条件线限速","条件线限速", "2020"),
    OMDB_LINK_SPEEDLIMIT_VAR("可变线限速","可变线限速", "2021"),
    OMDB_CON_ACCESS("全封闭","全封闭", "2022"),
    OMDB_RAMP("匝道","匝道", "2037"),
    OMDB_RAMP_1("普通路连接匝道","匝道", "2037-1"),
    OMDB_RAMP_2("高速入口匝道","匝道", "2037-2"),
    OMDB_RAMP_3("高速出口匝道","匝道", "2037-3"),
    OMDB_RAMP_4("高速连接匝道","匝道", "2037-4"),
    OMDB_RAMP_5("高速直连入口匝道","匝道", "2037-5"),
    OMDB_RAMP_6("高速直连出口匝道", "匝道","2037-6"),
    OMDB_RAMP_7("高速直连出口匝道高速出入口匝道", "匝道","2037-7"),
    OMDB_MULTI_DIGITIZED("上下线分离", "上下线分离","2040"),
    OMDB_LANE_NUM("车道数", "车道数","2041"),
    OMDB_PHY_LANENUM("物理车道数", "物理车道数","2097"),
    OMDB_VIADUCT("高架", "高架","2043"),
    OMDB_LINK_SEPARATION("设施分离","设施分离", "2070"),
    OMDB_LINK_MEDIAN("中央隔离带", "中央隔离带","2071"),
    OMDB_RDBOUND_BOUNDARYTYPE("道路边界类型", "道路边界","2083"),
    OMDB_LANE_CONSTRUCTION("车道施工", "车道施工","2090"),
    OMDB_LANE_TYPE_ACCESS("车道类型","车道类型","2092"),
    OMDB_BRIDGE("桥", "桥", "2201"),
    OMDB_BRIDGE_1("固定桥", "固定桥","2201-1"),
    OMDB_BRIDGE_2("可移动桥", "可移动桥", "2201-2"),
    OMDB_BRIDGE_3("跨线天桥", "跨线天桥","2201-2"),
    OMDB_TUNNEL("隧道","隧道",  "2202"),
    OMDB_ROUNDABOUT("环岛", "环岛", "2204"),
    OMDB_LINK_FORM1("道路形态1","形态", "2205"),
    OMDB_LINK_FORM1_1("U-Turn", "形态","2205-1"),
    OMDB_LINK_FORM1_2("提前右转","形态", "2205-2"),
    OMDB_LINK_FORM1_3("提前左转", "形态","2205-3"),
    OMDB_LINK_FORM2("道路形态2", "形态","2206"),
    OMDB_LINK_FORM2_1("IC", "形态","2206-1"),
    OMDB_LINK_FORM2_2("JCT", "形态","2206-2"),
    OMDB_LINK_FORM2_3("跨线地道", "形态","2206-3"),
    OMDB_LINK_FORM2_4("私道", "形态","2206-4"),
    OMDB_LINK_FORM2_5("步行街", "形态","2206-5"),
    OMDB_LINK_FORM2_6("公交专用道", "形态","2206-6"),
    OMDB_LINK_FORM2_7("POI 连接路", "形态","2206-7"),
    OMDB_LINK_FORM2_8("区域内道路", "形态","2206-8"),
    OMDB_LINK_FORM2_9("停车场出入口连接路", "形态","2206-9"),
    OMDB_LINK_FORM2_10("停车场内部虚拟连接路","形态", "2206-10"),
    OMDB_LINK_FORM2_11("风景路线", "形态","2206-11"),
    OMDB_LINK_FORM2_12("车辆测试路段", "形态","2206-12"),
    OMDB_LINK_FORM2_13("驾照考试路段", "形态","2206-13"),
    OMDB_LANE_ACCESS("通行车辆类型Lane","通行车辆", "2638"),
    OMDB_OBJECT_OH_STRUCT("上方障碍物","上方障碍物","3001"),
    OMDB_OBJECT_TEXT("文字", "文字", "3002"),
    OMDB_OBJECT_SYMBOL("符号", "符号", "3003"),
    OMDB_OBJECT_ARROW("箭头", "箭头", "3004"),
    OMDB_TRAFFIC_SIGN("交通标牌", "交通标牌","3005"),
    OMDB_POLE("杆状物", "杆状物","3006"),
    OMDB_OBJECT_WARNING_AREA("警示区","警示区",  "3007"),
    OMDB_OBJECT_BARRIER("护栏", "护栏","3009"),
    OMDB_OBJECT_WALL("平行墙", "平行墙","3010"),
    OMDB_FILL_AREA("导流区", "导流区","3012"),
    OMDB_CROSS_WALK("人行横道", "人行横道","3014"),
    OMDB_OBJECT_STOPLOCATION("停止位置", "停止位置", "3016"),
    OMDB_OBJECT_CURB("路牙", "路牙","3019"),
    OMDB_OBJECT_REFUGE_ISLAND("路口内交通岛", "交通岛", "3028"),
    OMDB_INTERSECTION("路口", "路口","4001"),
    OMDB_SPEEDLIMIT("常规点限速", "常规点限速","4002"),
    OMDB_SPEEDLIMIT_COND("条件点限速", "条件点限速", "4003"),
    OMDB_SPEEDLIMIT_VAR("可变点限速", "可变点限速","4004"),
    OMDB_LANE_SPEEDLIMIT("车道点限速", "车道点限速", "4005"),
    OMDB_RESTRICTION("普通交限", "普通交限", "4006"),
    OMDB_WARNINGSIGN("警示信息", "警示信息", "4009"),
    OMDB_ELECTRONICEYE("电子眼","电子眼",  "4010"),
    OMDB_ZLEVEL("立交", "立交", "4016"),
    OMDB_TRAFFICLIGHT("交通灯", "交通灯","4022"),
    OMDB_TOLLGATE("收费站", "收费站","4023"),
    OMDB_LANEINFO("车信", "车信","4601"),
    OMDB_CLM_LANEINFO("车信CLM", "车信CLM","4602"),
    OMDB_LANE_LINK_LG("车道中心线", "车道中心线","5001");

    companion object {
        fun findTableNameByCode(code: String): String {
            for (enumInstance in DataCodeEnum.values()) {
                if (enumInstance.code == code) {
                    return enumInstance.tableName
                }
            }
            return "" // 若未找到匹配的 code，则返回 null 或其他适当的默认值
        }
        fun findTableSubNameByCode(code: String): String {
            for (enumInstance in DataCodeEnum.values()) {
                if (enumInstance.code == code) {
                    return enumInstance.tableSubName
                }
            }
            return "" // 若未找到匹配的 code，则返回 null 或其他适当的默认值
        }
    }
}



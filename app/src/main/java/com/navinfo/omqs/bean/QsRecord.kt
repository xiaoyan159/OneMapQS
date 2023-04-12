package com.navinfo.omqs.bean

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


/**
 * @author zhjch
 * @version V1.0
 * @ClassName: Rd_qcRecord
 * @Date 2016/1/12
 * @Description: ${TODO}(质检对象)
 */
open class QsRecord @JvmOverloads constructor(
    /**
     * id 主键
     *
     */
    @PrimaryKey var id: String = "",
    /**
     * linkPid 绑定的道路ID
     */
    var linkPid: String = "",
    /**
     *问题分类
     */
    var classType: String = "",
    /**
     * 问题类型
     */
    var type: String = "",

    /**
     * 问题现象
     */
    var phenomenon: String = "",
    /**
     * 问题描述
     */
    var description: String = "",
    /**
     * 设置initial_cause
     * @param initial_cause
     * initial_cause
     */
//    var initial_cause: String? = StringEntity.STRING_DEFAULT
//    /**
//     * 获取root_cause
//     * @return root_cause
//     */
//    /**
//     * 设置root_cause
//     * @param root_cause
//     * root_cause
//     */
////根本原因（RCA）
//    var root_cause: String? = StringEntity.STRING_DEFAULT
//    /**
//     * 获取check_userid
//     * @return check_userid
//     */
//    /**
//     * 设置check_userid
//     * @param check_userid
//     * check_userid
//     */
////质检员
//    var check_userid: String? = StringEntity.STRING_DEFAULT
//    /**
//     * 获取check_time
//     * @return check_time
//     */
//    /**
//     * 设置check_time
//     * @param check_time
//     * check_time
//     */
////质检日期
//    var check_time: String? = StringEntity.STRING_DEFAULT
//    /**
//     * 获取confirm_userid
//     * @return confirm_userid
//     */
//    /**
//     * 设置confirm_userid
//     * @param confirm_userid
//     * confirm_userid
//     */
////确认人
//    var confirm_userid: String? = StringEntity.STRING_DEFAULT
//    /**
//     * 获取t_lifecycle
//     * @return t_lifecycle
//     */
//    /**
//     * 设置t_lifecycle
//     * @param t_lifecycle
//     * t_lifecycle
//     */
////状态  0 无； 1 删除；2 更新；3 新增；
//    var t_lifecycle = 0
//    /**
//     * 获取t_status
//     * @return t_status
//     */
//    /**
//     * 设置t_status
//     * @param t_status
//     * t_status
//     */
////问题记录提交状态   0 未提交；1 已提交；
//    var t_status = 0
) : RealmObject(

)
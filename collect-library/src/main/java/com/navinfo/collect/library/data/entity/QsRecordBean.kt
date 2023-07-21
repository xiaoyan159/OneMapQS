package com.navinfo.collect.library.data.entity

import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.PrimaryKey

/**
 * @author zhjch
 * @version V1.0
 * @ClassName: Rd_qcRecord
 * @Date 2016/1/12
 * @Description: ${TODO}(质检对象)
 */
//@RealmClass
open class QsRecordBean @JvmOverloads constructor(
    var taskId: Int = -1,
    /**
     * id 主键
     *
     */
    @PrimaryKey
    var id: String = "",
    /**
     * 关联要素id
     */
    var elementId: String = "",
    /**
     * linkPid 绑定的道路ID
     */
    var linkId: String = "84207223282277331",
    /**
     *问题分类
     */
    var classType: String = "",
    /**
     *要素代码
     */
    var classCode: String = "",
    /**
     * 问题类型
     */
    var problemType: String = "",

    /**
     * 问题现象
     */
    var phenomenon: String = "",
    /**
     * 描述信息
     */
    var description: String = "",
    /**
     * 问题环节
     */
    var problemLink: String = "",

    /**
     * 问题原因
     *    根本原因（RCA）
     */

    var cause: String = "",
    /**
     * 质检员ID
     */
    var checkUserId: String = "",
    /**
     * 质检日期
     */
    var checkTime: String = "",
    /**
     * 确认人
     */
    var confirmUserId: String = "",
    /**
     * 状态  0 无； 1 删除；2 更新；3 新增；
     */

    var t_lifecycle: Int = 3,
    /**
     * 问题记录提交状态   0 未提交；1 已提交；
     */
    var t_status: Int = 0,

    var attachmentBeanList: RealmList<AttachmentBean> = RealmList<AttachmentBean>(),
    /**
     * 显示坐标
     */
//    var geometry: String = "",
    /**
     * 显示坐标
     */
    var guideGeometry: String = "",

    ) : RealmObject() {

    fun copy(): QsRecordBean {
        val qs = QsRecordBean(
            taskId = taskId,
            id = id,
            elementId = elementId,
            linkId = linkId,
            classType = classType,
            classCode = classCode,
            problemType = problemType,
            phenomenon = phenomenon,
            description = description,
            problemLink = problemLink,
            cause = cause,
            checkUserId = checkUserId,
            checkTime = checkTime,
            confirmUserId = confirmUserId,
            t_lifecycle = t_lifecycle,
            t_status = t_status,
            attachmentBeanList = attachmentBeanList,
        )
        qs.geometry = geometry
        return qs
    }


    private val tileX = RealmSet<Int>() // x方向的tile编码
    private val tileY = RealmSet<Int>() // y方向的tile编码
    var geometry: String = ""
        set(value) {
            field = value
            // 根据geometry自动计算当前要素的x-tile和y-tile
            GeometryToolsKt.getTileXByGeometry(value, tileX)
            GeometryToolsKt.getTileYByGeometry(value, tileY)
        }
}
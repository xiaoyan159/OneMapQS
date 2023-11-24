package com.navinfo.omqs.ui.fragment.tasklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterTaskBinding
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


interface TaskAdapterCallback {
    /**
     * 点击整体item
     */
    fun itemOnClick(bean: HadLinkDvoBean)

    /**
     * 点击编辑不作业理由按钮
     */
    fun editOnClick(position: Int, bean: HadLinkDvoBean)

    /**
     * 地图点击link，定位到项目条
     */
    fun scrollPosition(position: Int)

    /**
     * 设置起点
     */
    fun setNaviStart(position: Int, bean: HadLinkDvoBean)

    /**
     * 设置终点
     */
    fun setNaviEnd(position: Int, bean: HadLinkDvoBean)

    /**
     * 设置不参与路径计算的link
     */
    fun setNavSkipLink(position: Int, bean: HadLinkDvoBean)
}

/**
 * 当前任务适配器
 */
class TaskAdapter(
    private val realmOperateHelper: RealmOperateHelper,
    private val coroutineScope: CoroutineScope,
    private val callback: TaskAdapterCallback
) : BaseRecyclerViewAdapter<HadLinkDvoBean>() {
    private var selectPosition = -1
    private lateinit var taskBean: TaskBean

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterTaskBinding =
            holder.viewBinding as AdapterTaskBinding
        val context = binding.root.context
        val bean = data[position]
        if (bean.linkStatus == 1) {
            binding.taskHead.background =
                context.getDrawable(R.drawable.selector_task_head)
        } else {
            binding.taskHead.background =
                context.getDrawable(R.drawable.selector_task_head_add_link)
        }

        if (taskBean.navInfo != null && taskBean.navInfo!!.naviStartLinkId == bean.linkPid) {
            binding.taskNaviIcon.visibility = View.VISIBLE
            binding.taskNaviIcon.background = context.getDrawable(R.drawable.navi_start_icon)
        } else if (taskBean.navInfo != null && taskBean.navInfo!!.naviEndLinkId == bean.linkPid) {
            binding.taskNaviIcon.visibility = View.VISIBLE
            binding.taskNaviIcon.background = context.getDrawable(R.drawable.navi_end_icon)
        } else if (!bean.isNavi) {
            binding.taskNaviIcon.visibility = View.VISIBLE
            binding.taskNaviIcon.background = context.getDrawable(R.drawable.navi_skip)
        } else {
            binding.taskNaviIcon.visibility = View.GONE
        }


        binding.taskLinkPid.text = "PID:${bean.linkPid}"
        binding.taskMesh.text = "mesh:${bean.mesh}"
        binding.root.isSelected = selectPosition == position
        //当前被选中
        if (selectPosition != position) {
            binding.naviLayout.visibility = View.GONE
        } else {
            binding.naviLayout.visibility = View.VISIBLE
            if (bean.isNavi) {
                binding.naviRouteSetSkip.text = "不参与路径计算"
            } else {
                binding.naviRouteSetSkip.text = "参与路径计算"
            }
        }
        binding.root.setOnClickListener {
            callback.itemOnClick(bean)
        }
        if (bean.reason == "") {
            binding.taskBadge.visibility = View.GONE
        } else {
            binding.taskBadge.visibility = View.VISIBLE
        }
        binding.taskEdit.setOnClickListener {
            callback.editOnClick(position, bean)
        }
        binding.naviRouteSetStartLink.setOnClickListener() {
            callback.setNaviStart(position, bean)
        }

        binding.naviRouteSetEndLink.setOnClickListener() {
            callback.setNaviEnd(position, bean)
        }
        binding.naviRouteSetSkip.setOnClickListener(){
            bean.isNavi = !bean.isNavi
            callback.setNavSkipLink(position,bean)
            notifyItemChanged(position)
        }

    }


    fun resetSelect() {
        selectPosition = -1
    }

    fun setSelectTag(tag: String) {
        for (i in data.indices) {
            if (data[i].linkPid == tag) {
                val lastPosition = selectPosition
                selectPosition = i
                if (lastPosition > -1)
                    notifyItemChanged(lastPosition)
                notifyItemChanged(i)
                if (callback != null) {
                    callback.scrollPosition(i)
                }
                break
            }
        }
    }

    fun setTaskBean(taskBean: TaskBean) {
        this.taskBean = taskBean
        notifyDataSetChanged()
    }
}



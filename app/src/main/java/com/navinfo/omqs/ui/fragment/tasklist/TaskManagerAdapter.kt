package com.navinfo.omqs.ui.fragment.tasklist

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 离线地图主页面，viewpage适配器
 */
class TaskManagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {
    private val taskListFragment = TaskListFragment()
    private val taskFragment = TaskFragment()
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> taskFragment
            else ->
                taskListFragment
        }
    }
}
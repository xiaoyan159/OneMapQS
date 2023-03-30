package com.navinfo.omqs.ui.fragment.offlinemap

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 离线地图主页面，viewpage适配器
 */
class OfflineMapAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {
    private val stateFragment = OfflineMapStateListFragment()
    private val cityListFragment = OfflineMapCityListFragment()
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> stateFragment
            else ->
                cityListFragment
        }
    }
}
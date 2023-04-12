package com.navinfo.omqs.ui.fragment.evaluationresult

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EvaluationResultAdapter(activity: FragmentActivity, val fragmentList: List<Fragment>) :
    FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
}
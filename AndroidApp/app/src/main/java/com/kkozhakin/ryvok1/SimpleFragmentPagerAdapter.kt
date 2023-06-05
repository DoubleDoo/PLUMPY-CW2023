package com.kkozhakin.ryvok1

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.kkozhakin.ryvok1.ui.home.ActivityFragment
import com.kkozhakin.ryvok1.ui.home.SleepFragment

class SimpleFragmentPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                ActivityFragment()
            }
            else -> {
                SleepFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 2
    }
}

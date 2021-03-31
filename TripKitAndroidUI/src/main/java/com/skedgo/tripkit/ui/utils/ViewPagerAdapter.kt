package com.skedgo.tripkit.ui.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

open class ViewPagerAdapter(manager: FragmentManager, private val fragmentBundle: Bundle? = null) : FragmentStatePagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()

    override fun getItem(position: Int): Fragment {
        val fragment = mFragmentList[position]
        fragment.arguments = this.fragmentBundle
        return fragment
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    fun addFragment(fragment: Fragment, title: String) {
        fragment.arguments = fragmentBundle
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    fun clearFragments() {
        mFragmentList.clear()
        mFragmentTitleList.clear()
        notifyDataSetChanged()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mFragmentTitleList[position]
    }

    fun getRegisteredFragment(position: Int): Fragment {
        return mFragmentList[position]
    }

}
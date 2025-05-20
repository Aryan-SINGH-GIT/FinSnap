package com.example.finsnap.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CashViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CashSummaryFragment.newInstance() // Use factory method
            1 -> CashCreditDebitChartsFragment.newInstance() // Use factory method
            2 -> CashCategoryFragment.newInstance() // Use factory method
            else -> CashSummaryFragment.newInstance()
        }
    }
}

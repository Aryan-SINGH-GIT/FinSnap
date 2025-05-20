package com.example.finsnap.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3  // Three pages as requested

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SummaryFragment()     // Show total amount, cash, savings
            1 -> TransactionChartsFragment() // Show graph of credits vs debits
            2 -> CategoryBreakdownFragment() // Show category-wise breakdown
            else -> SummaryFragment()
        }
    }
}
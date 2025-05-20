package com.example.finsnap.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.finsnap.R
import com.example.finsnap.model.UserCash
import com.example.finsnap.viewmodel.FinanceViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class CashCategoryFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var viewModel: FinanceViewModel
    
    companion object {
        fun newInstance(): CashCategoryFragment {
            return CashCategoryFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cash_category, container, false)
        pieChart = view.findViewById(R.id.cashCategoryChart)
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Move observers to onViewCreated
        setupObservers()
    }
    
    private fun setupObservers() {
        // Observe filtered transactions just once
        viewModel.filteredCashTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isEmpty()) {
                // If filtered transactions are empty, observe the main transactions list
                viewModel.cashTransactions.observe(viewLifecycleOwner) { allTransactions ->
                    updatePieChart(allTransactions)
                }
            } else {
                updatePieChart(transactions)
            }
        }
    }
    
    private fun updatePieChart(transactions: List<UserCash>) {
        val grouped = transactions.groupBy { it.categoryName.ifEmpty { "Miscellaneous" } }

        val entries = grouped.mapNotNull { (category, txns) ->
            val total = txns.sumOf {
                it.CashamtChange.substring(1).toDoubleOrNull() ?: 0.0
            }
            if (total > 0) PieEntry(total.toFloat(), category) else null
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No data available"
        } else {
            val dataSet = PieDataSet(entries, "Category Breakdown")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.sliceSpace = 2f
            dataSet.valueTextSize = 14f

            val pieData = PieData(dataSet)
            pieChart.data = pieData
            pieChart.setUsePercentValues(true)
            pieChart.description.isEnabled = false
            pieChart.centerText = "Cash Categories"
            pieChart.setEntryLabelTextSize(12f)
            pieChart.setDrawEntryLabels(true)
            pieChart.animateY(1000)
            pieChart.invalidate()
        }
    }
}

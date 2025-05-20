package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentCashCreditDebitChartsBinding
import com.example.finsnap.model.UserCash
import com.example.finsnap.viewmodel.FinanceViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class CashCreditDebitChartsFragment : Fragment() {
    private lateinit var pieChart: PieChart
    private lateinit var viewModel: FinanceViewModel
    
    companion object {
        fun newInstance(): CashCreditDebitChartsFragment {
            return CashCreditDebitChartsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cash_credit_debit_charts, container, false)
        pieChart = view.findViewById(R.id.cashPieChart)
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Move observers to onViewCreated
        setupObservers()
    }
    
    private fun setupObservers() {
        // Observe transactions just once
        viewModel.filteredCashTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isEmpty()) {
                // If filtered transactions are empty, observe the main list
                viewModel.cashTransactions.observe(viewLifecycleOwner) { allTransactions ->
                    updateCreditDebitChart(allTransactions)
                }
            } else {
                updateCreditDebitChart(transactions)
            }
        }
    }
    
    private fun updateCreditDebitChart(transactions: List<UserCash>) {
        val credit = transactions.filter { it.CashamtChange.startsWith("+") }
            .sumOf { it.CashamtChange.substring(1).toDoubleOrNull() ?: 0.0 }
        val debit = transactions.filter { it.CashamtChange.startsWith("-") }
            .sumOf { it.CashamtChange.substring(1).toDoubleOrNull() ?: 0.0 }

        val entries = listOf(
            PieEntry(credit.toFloat(), "Credit"),
            PieEntry(debit.toFloat(), "Debit")
        )

        val dataSet = PieDataSet(entries, "Cash Transactions")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        pieChart.data = PieData(dataSet)
        pieChart.centerText = "Credit vs Debit"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
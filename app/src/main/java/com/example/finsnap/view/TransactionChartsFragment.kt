package com.example.finsnap.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.finsnap.databinding.FragmentTransactionChartsBinding
import com.example.finsnap.model.UserAmount
import com.example.finsnap.viewmodel.FinanceViewModel
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import android.util.Log
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry

class TransactionChartsFragment : Fragment() {

    private var _binding: FragmentTransactionChartsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe filtered transaction data
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            if (!transactions.isNullOrEmpty()) {
                Log.i("TransactionChartsFragment"," data update")
                updatePieChart(transactions)
            } else {
                Log.i("TransactionChartsFragment","no data")
                showNoData()
            }
        }
    }

    private fun updatePieChart(transactions: List<UserAmount>) {
        val creditAmount = transactions
            .filter { it.isCredit }
            .sumOf { it.amount ?: 0.0 }

        val debitAmount = transactions
            .filter { !it.isCredit }
            .sumOf { it.amount ?: 0.0 }

        Log.d("ChartDebug", "Credit: $creditAmount, Debit: $debitAmount")

        val entries = ArrayList<PieEntry>().apply {
            if (creditAmount > 0) add(PieEntry(creditAmount.toFloat(), "Credit"))
            if (debitAmount > 0) add(PieEntry(debitAmount.toFloat(), "Debit"))
        }

        if (entries.isEmpty()) {
            showNoData()
        } else {
            setPieChartData(entries)
        }
    }

    private fun setPieChartData(entries: List<PieEntry>) {
        val pieChart = binding.pieChart

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#90EE90"), // Green for Credit
            Color.parseColor("#f02525")  // Red for Debit
        )
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.valueTextSize = 0f

        val data = PieData(dataSet)
        data.setDrawValues(false)

        // Extract values for legend labels
        var creditAmount = 0f
        var debitAmount = 0f

        for (entry in entries) {
            when (entry.label) {
                "Credit" -> creditAmount = entry.value
                "Debit" -> debitAmount = entry.value
            }
        }

        pieChart.apply {
            this.data = data
            description = Description().apply { text = ""; isEnabled = false }
            centerText = "Transactions"
            setUsePercentValues(false)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            setEntryLabelColor(Color.TRANSPARENT)
            setEntryLabelTextSize(0f)

            // ✅ Legend with values
            legend.isEnabled = true
            legend.form = Legend.LegendForm.CIRCLE
            legend.textSize = 14f
            legend.textColor = Color.BLACK
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.setCustom(
                listOf(
                    LegendEntry("Credit: ₹${creditAmount.toInt()}", Legend.LegendForm.CIRCLE, 10f, 2f, null, Color.parseColor("#90EE90")),
                    LegendEntry("Debit: ₹${debitAmount.toInt()}", Legend.LegendForm.CIRCLE, 10f, 2f, null, Color.parseColor("#f02525"))
                )
            )

            animateY(1000)
            invalidate()
        }
    }



    private fun showNoData() {
        binding.pieChart.clear()
        binding.pieChart.setNoDataText("No data available.")
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.finsnap.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finsnap.databinding.FragmentCategoryBreakdownBinding
import com.example.finsnap.model.UserAmount
import com.example.finsnap.viewmodel.FinanceViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class CategoryBreakdownFragment : Fragment() {

    private var _binding: FragmentCategoryBreakdownBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()

        // Observe filtered transaction data
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            if (!transactions.isNullOrEmpty()) {
                val categorySummary = getCategorySummary(transactions)
                updatePieChart(categorySummary)
            } else {
                // Handle empty transaction list
                binding.categoryPieChart.clear()
                binding.categoryPieChart.setNoDataText("No transactions available.")
                binding.categoryPieChart.invalidate()
            }
        }
    }

    private fun setupPieChart() {
        binding.categoryPieChart.apply {
            description.isEnabled = false
            setEntryLabelColor(Color.WHITE)
            legend.isEnabled = true
            setHoleColor(Color.TRANSPARENT)
        }
    }

    private fun getCategorySummary(transactions: List<UserAmount>): Map<String, Double> {
        val categorySummary = mutableMapOf<String, Double>()

        transactions.forEach { transaction ->
            // Handle null or empty categories
            val category = if (transaction.category.isNullOrBlank()) "Uncategorized" else transaction.category
            val amount = transaction.amount

            // Add the amount to the category total
            categorySummary[category] = (categorySummary[category] ?: 0.0) + amount
        }

        return categorySummary
    }

    private fun updatePieChart(categorySummary: Map<String, Double>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // Check if we have any categories to display
        if (categorySummary.isEmpty()) {
            binding.categoryPieChart.clear()
            binding.categoryPieChart.setNoDataText("No category data available.")
            binding.categoryPieChart.invalidate()
            return
        }

        // Get categories sorted by amount
        val sortedCategories = categorySummary.entries.sortedByDescending { it.value }

        // Add entries for each category
        sortedCategories.forEachIndexed { index, entry ->
            entries.add(PieEntry(entry.value.toFloat(), entry.key))
            colors.add(CATEGORY_COLORS[index % CATEGORY_COLORS.size])
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())

        binding.categoryPieChart.data = data
        binding.categoryPieChart.invalidate()
    }

    /**
     * Custom formatter to display values as percentages
     */
    inner class PercentFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format("₹%.1f", value)
        }
    }

    companion object {
        private val CATEGORY_COLORS = listOf(
            Color.rgb(76, 175, 80),    // Green
            Color.rgb(33, 150, 243),   // Blue
            Color.rgb(255, 193, 7),    // Yellow
            Color.rgb(156, 39, 176),   // Purple
            Color.rgb(244, 67, 54),    // Red
            Color.rgb(0, 188, 212),    // Cyan
            Color.rgb(255, 87, 34),    // Orange
            Color.rgb(63, 81, 181)     // Indigo
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
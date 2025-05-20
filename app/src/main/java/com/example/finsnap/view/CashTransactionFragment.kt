package com.example.finsnap.view

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentCashTransactionBinding
import com.example.finsnap.model.UserCash
import com.example.finsnap.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale


class CashTransactionFragment : Fragment() {
    private lateinit var binding: FragmentCashTransactionBinding
    private lateinit var viewModel: FinanceViewModel
    private lateinit var cashAdapter: CashAdapter





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCashTransactionBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]


        setupRecyclerView()
        setupFilterSpinner()
        setupFab()

        observeData()
        loadInitialData()
      //  viewModel.loadUserBankDetails()

        val pagerAdapter = CashViewPagerAdapter(requireActivity())
        binding.CashViewPager.adapter = pagerAdapter



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        setupRecyclerView()
//        setupFilterSpinner()
//        setupFab()
//        observeData()
//        loadInitialData()
    }



    private fun setupRecyclerView() {
       // binding.tvCashBalance.text = "Current Cash Balance: ₹..."

        setupFilterSpinner()
        binding.btnPickDatecash.setOnClickListener {
            viewModel.cashTransactions.value?.let { transactions ->
                openDatePickerAndFilterCashByDate(transactions)
            }
        }


        cashAdapter = CashAdapter()
        binding.rvCashTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cashAdapter
        }
    }

    private fun setupFilterSpinner() {
        binding.filterSpinnercash.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.cashTransactions.observe(viewLifecycleOwner) { allTransactions ->
                    applyCashFilter(allTransactions, position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupDatePickerButton() {
        binding.btnPickDatecash.setOnClickListener {
            viewModel.cashTransactions.value?.let { transactions ->
                openDatePickerAndFilterCashByDate(transactions)
            }
        }
    }

    private fun applyCashFilter(transactions: List<UserCash>, filterType: Int) {
        val filteredList = viewModel.applyAndStoreCashFilter(transactions, filterType)
        cashAdapter.updateItems(filteredList)
        updateTotalBalance(filteredList)
        toggleNoTransactionsMessage(filteredList)
    }

    private fun openDatePickerAndFilterCashByDate(transactions: List<UserCash>) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val selectedDate = selectedCalendar.time

            val filtered = viewModel.applyAndStoreCashFilter(transactions, 3, selectedDate)
            cashAdapter.updateItems(filtered)
            updateTotalBalance(filtered)
            toggleNoTransactionsMessage(filtered)
        }, year, month, day)

        dialog.show()
    }

    private fun updateTotalBalance(transactions: List<UserCash>) {
        val transactionSum = transactions.sumOf { cash ->
            val amount = cash.CashamtChange.substring(1).toDoubleOrNull() ?: 0.0
            if (cash.CashamtChange.startsWith("+")) amount else -amount
        }

      //  binding.tvCashBalance.text = String.format("Filtered Transaction Total: ₹%.2f", transactionSum)
    }


    private fun toggleNoTransactionsMessage(transactions: List<UserCash>) {
        binding.tvNoTransactions.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
    }








    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {

            findNavController().navigate(R.id.action_cashTransactionFragment_to_addCashFragment)
        }
    }

    private fun observeData() {
        viewModel.cashTransactions.observe(viewLifecycleOwner) { transactions ->
            cashAdapter.updateItems(transactions)
            toggleNoTransactionsMessage(transactions)
            // Do not call updateCashBalanceFromTransactions here
            // This was causing unnecessary updates during navigation
        }
    }


    private fun loadInitialData() {
        // Only load balance details once
        viewModel.loadCashTransactions()
    }

    override fun onResume() {
        super.onResume()
        // Do not reload data on every resume - this is causing unnecessary updates
        // We'll rely on the ViewModel to manage data refreshes when needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear any observers or resources
        binding.rvCashTransactions.adapter = null
    }


}
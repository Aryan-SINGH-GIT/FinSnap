package com.example.finsnap.view

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentHomeBinding
import com.example.finsnap.model.UserAmount
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var amountAdapter: AmoutAdapter
    private lateinit var myFinanceViewModel: FinanceViewModel
    private lateinit var database: UserDatabase
    private val SMS_PERMISSION_CODE = 100

    // Track current filter state
    private var currentFilterPosition = 0
    private var currentFilterDate: Date? = null

    // Store the full list of transactions to avoid repeated database queries
    private var allTransactions: List<UserAmount> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Use activity scope to share the ViewModel
        myFinanceViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        // Database setup
        database = Room.databaseBuilder(
            requireContext(),
            UserDatabase::class.java,
            "UserDatabase"
        ).fallbackToDestructiveMigration().build()

        // Setup balance observers
        lifecycleScope.launch {
            val userId = SessionManager.getUserToken().toString().toInt()
            val initialAmount = database.UsersDao().getCurrentAmount(userId)
            myFinanceViewModel.calculateCurrentBalance(initialAmount)
//            val userToken = SessionManager.getUserToken() ?: ""
//            val initialAmount = database.UsersDao().getCurrentAmount(userToken)
//            myFinanceViewModel.calculateCurrentBalance(initialAmount)

        }

        // Setup RecyclerView for transactions
        recyclerView = binding.myrecyclerview
        amountAdapter = AmoutAdapter(onItemClick = { transaction ->
            navigateToTransactionDetail(transaction)
        })
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = amountAdapter
        }

        checkSmsPermission()
        setupViewModel()
        setupFilterSpinner()
        setupDateFilter()

        // Setup ViewPager2 with the adapter
        val pagerAdapter = ViewPagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Ensure we refresh data when returning to this fragment
        myFinanceViewModel.loadSmsTransactionsFromDb()
    }

    private fun setupDateFilter() {
        binding.btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                currentFilterDate = selectedCalendar.time
                myFinanceViewModel.updateFilterState(currentFilterPosition, currentFilterDate)
            }, year, month, day)

            datePicker.show()
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("All", "Daily", "Monthly")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = adapter

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFilterPosition = position
                if (position == 0) currentFilterDate = null // reset if "All"
                myFinanceViewModel.updateFilterState(currentFilterPosition, currentFilterDate)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupViewModel() {
        // Use a single observer for the database LiveData
        myFinanceViewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNotEmpty()) {
                // Save the full list for filtering
                allTransactions = transactions
                amountAdapter.updateItems(transactions)
                calculateAndDisplayTotal(transactions)
                binding.myrecyclerview.scrollToPosition(0) // Scroll to top for better UX
            } else {
                // If no transactions, try loading from database
                myFinanceViewModel.loadSmsTransactionsFromDb()
            }
        }

        // Listen for errors
        myFinanceViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        // Listen for loading state
//        myFinanceViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
//            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }

        // Listen for specific transaction updates
        myFinanceViewModel.updatedTransaction.observe(viewLifecycleOwner) { updatedTransaction ->
            android.util.Log.d("HomeFragment", "Received updated transaction: ${updatedTransaction.id}")
            android.util.Log.d("HomeFragment", "Updated sender: ${updatedTransaction.sender}")
            android.util.Log.d("HomeFragment", "Updated category: ${updatedTransaction.category}")
            android.util.Log.d("HomeFragment", "Updated image: ${updatedTransaction.categoryImage}")
            
            // Update in adapter directly for immediate UI update
            amountAdapter.updateItem(updatedTransaction)
        }
    }

    private fun calculateAndDisplayTotal(transactions: List<UserAmount>) {
        val total = transactions.sumOf {
            it.amtChange.substring(1).toDoubleOrNull()?.let { amt ->
                if (it.amtChange.startsWith("+")) amt else -amt
            } ?: 0.0
        }


    }

    private fun navigateToTransactionDetail(transaction: UserAmount) {
        val action = HomeFragmentDirections.actionHomeFragmentToTransactionDetailFragment(transaction)
        findNavController().navigate(action)
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            // Permission already granted
            loadSmsData()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSmsData()
            } else {
                Toast.makeText(
                    requireContext(),
                    "SMS permission is required to show messages",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadSmsData() {
        if (myFinanceViewModel.filteredTransactions.value.isNullOrEmpty()) {
            myFinanceViewModel.loadSmsTransactionsFromDb()
        }
    }
}
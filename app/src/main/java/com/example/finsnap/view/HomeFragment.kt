package com.example.finsnap.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var amountAdapter: AmoutAdapter
    private lateinit var myFinanceViewModel: FinanceViewModel
    private lateinit var database: UserDatabase
    private val SMS_PERMISSION_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Use activity scope to share the ViewModel with TransactionDetailFragment
        myFinanceViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        database = Room.databaseBuilder(
            requireContext(),
            UserDatabase::class.java,
            "UserDatabase"
        ).fallbackToDestructiveMigration().build()

        // First set up the observer for the balance
        myFinanceViewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            binding.textView.text = String.format("%.2f", balance)
        }

        // Then load the initial amount and trigger the calculation
        lifecycleScope.launch {
            val userId = SessionManager.getUserToken().toString().toInt()
            val initialAmount = database.UsersDao().getCurrentAmount(userId)

            // Only show initialAmount temporarily
            binding.textView.text = String.format("%.2f", initialAmount)

            // Calculate the updated balance based on transactions
            myFinanceViewModel.calculateCurrentBalance(initialAmount)
        }

        recyclerView = binding.myrecyclerview

        // Initialize Adapter
        amountAdapter = AmoutAdapter(onItemClick = { transaction ->
            navigateToTransactionDetail(transaction)
        })

        // Setup RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = amountAdapter
        }

        setupViewModel()

        // Check for permissions
        checkSmsPermission()

        return binding.root
    }

    private fun navigateToTransactionDetail(transaction: UserAmount) {
        val action = HomeFragmentDirections.actionHomeFragmentToTransactionDetailFragment(transaction)
        findNavController().navigate(action)
    }

    private fun setupViewModel() {
        // Observe the full list of transactions
        myFinanceViewModel.smsTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNotEmpty()) {
                amountAdapter.updateItems(transactions)
            } else {
                // Show a message for empty transactions
                Toast.makeText(requireContext(), "No transactions found", Toast.LENGTH_SHORT).show()
            }
        }

        myFinanceViewModel.getAllUserAmountsLive().observe(viewLifecycleOwner) { transactions ->
            amountAdapter.updateItems(transactions)
        }


        myFinanceViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        }
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
        Toast.makeText(requireContext(), "Loading SMS transactions...", Toast.LENGTH_SHORT).show()
        myFinanceViewModel.loadSmsTransactions()
    }
}
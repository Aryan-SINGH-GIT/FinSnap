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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.finsnap.databinding.FragmentHomeBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var amounadapter: AmoutAdapter
    private lateinit var myfinanaceViewModel: FinanceViewModel
    private lateinit var database: UserDatabase
    private val SMS_PERMISSION_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        myfinanaceViewModel = ViewModelProvider(this).get(FinanceViewModel::class.java)

        database = Room.databaseBuilder(
            requireContext(),
            UserDatabase::class.java,
            "finsnap_database"
        ).fallbackToDestructiveMigration().build()

        var currentAmount = 0.0
        val userId = SessionManager.getUserToken().toString().toInt()

        GlobalScope.launch {
            currentAmount = database.UsersDao().getCurrentAmount(userId)
            binding.textView.text = currentAmount.toString()
        }

        recyclerView = binding.myrecyclerview

        // Initialize Adapter
        amounadapter = AmoutAdapter()

        // Setup RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = amounadapter
        }

        setupViewModel()

        // Check for permissions
        checkSmsPermission()

        return binding.root
    }

    private fun setupViewModel() {
        myfinanaceViewModel = ViewModelProvider(this)[FinanceViewModel::class.java]

        myfinanaceViewModel.smsTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNotEmpty()) {
                amounadapter.updateItems(transactions)
            } else {
                // Show a message for empty transactions
                Toast.makeText(requireContext(), "No transactions found", Toast.LENGTH_SHORT).show()
            }
        }

        myfinanaceViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
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
        // You can add a toast to indicate loading
        Toast.makeText(requireContext(), "Loading SMS transactions...", Toast.LENGTH_SHORT).show()
        myfinanaceViewModel.loadSmsTransactions()
    }
}
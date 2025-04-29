package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentAddCashBinding
import com.example.finsnap.model.UserCash
import com.example.finsnap.viewmodel.FinanceViewModel

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddCashFragment : Fragment() {
   private lateinit var binding: FragmentAddCashBinding
    private lateinit var viewModel: FinanceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentAddCashBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        setupClickListeners()


        return binding.root
    }

    private fun setupClickListeners() {
        binding.saveBtn.setOnClickListener {
        saveCashTransaction()
    }

        binding.cancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }
    }


        private fun saveCashTransaction() {
            val description = binding.dicription.text.toString().trim()
            val amountText = binding.amount.text.toString().trim()

            if (description.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return
            }

            // Determine if it's a credit or debit transaction
            val imageResource = if (amountText.startsWith("+")) {
                R.drawable.logo
            } else {
                R.drawable.ic_cash
            }

            // Format the current time
            val currentTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(
                java.util.Date()
            )

            val cashTransaction = UserCash(
                cashSender = description,
                cashTime = currentTime,
                CashamtChange = amountText,
                cashImage = imageResource
            )

            // Save the transaction via ViewModel
            viewModel.insertCashTransaction(cashTransaction)

            // Navigate back to the CashTransactionFragment
            findNavController().navigateUp()
        }


}
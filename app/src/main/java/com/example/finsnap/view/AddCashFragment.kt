package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
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
    private var selectedCategory: String = "Miscellaneous"
    private var selectedCategoryImage: Int = R.drawable.ic_miscellaneous


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentAddCashBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        listenForCategoryResult()
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
        
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.categoryImage.setOnClickListener {
            findNavController().navigate(R.id.action_addCashFragment_to_categoryFragment)

        }
        binding.categorycash.setOnClickListener {
            findNavController().navigate(R.id.action_addCashFragment_to_categoryFragment)

        }
    }


    private fun listenForCategoryResult() {
        setFragmentResultListener("category_result") { _, bundle ->
            selectedCategory = bundle.getString("selectedCategory") ?: "Miscellaneous"
            selectedCategoryImage = bundle.getInt("categoryImage", R.drawable.ic_miscellaneous)

            binding.categorycash.text = selectedCategory
            binding.categoryImage.setImageResource(selectedCategoryImage)
        }
    }


    private fun saveCashTransaction() {
        val description = binding.description.text.toString().trim()
        val amountText = binding.amount.text.toString().trim()

        if (description.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate that amount starts with + or -
        if (!amountText.startsWith("+") && !amountText.startsWith("-")) {
            Toast.makeText(requireContext(), "Amount must start with + (income) or - (expense)", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse the amount without the + or - sign
        val amountValue = amountText.substring(1).toDoubleOrNull()
        if (amountValue == null) {
            Toast.makeText(requireContext(), "Invalid amount format", Toast.LENGTH_SHORT).show()
            return
        }

        // Format the current time
        val currentTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val cashTransaction = UserCash(
            cashSender = description,
            cashTime = currentTime,
            CashamtChange = amountText,
            cashImage = selectedCategoryImage,
            categoryName = selectedCategory
        )

        // Save the transaction via ViewModel
        viewModel.insertCashTransaction(cashTransaction)

        // Show success message
        Toast.makeText(requireContext(), "Transaction saved successfully", Toast.LENGTH_SHORT).show()

        // Navigate back to the Cash Transaction Fragment with clear back stack
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.cashTransactionFragment, false)
            .build()
        findNavController().navigate(R.id.cashTransactionFragment, null, navOptions)
    }


}
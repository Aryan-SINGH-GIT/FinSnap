package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentCashSummaryBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CashSummaryFragment : Fragment() {
    private lateinit var binding: FragmentCashSummaryBinding
    private lateinit var viewModel: FinanceViewModel

    companion object {
        fun newInstance(): CashSummaryFragment {
            return CashSummaryFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCashSummaryBinding.inflate(inflater, container, false)
        // Use the correct scope for the ViewModel
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup the observers in onViewCreated, which is the appropriate lifecycle method
        observeData()
    }

    private fun observeData() {
        // Observe cash balance changes
        viewModel.cashBalance.observe(viewLifecycleOwner) { balance ->
            binding.totalCashAmount.text = String.format("₹%.2f", balance)
        }

        // Observe cash transactions - removed nested observers
        // We'll rely on the ViewModel to properly update the balance
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Not strictly necessary since viewLifecycleOwner handles this automatically,
        // but it's a good practice to be explicit
        // This helps ensure observers are properly cleaned up
    }
}
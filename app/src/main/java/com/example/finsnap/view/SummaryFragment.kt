package com.example.finsnap.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.finsnap.databinding.FragmentSummaryBinding
import com.example.finsnap.viewmodel.FinanceViewModel

class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinanceViewModel by activityViewModels()
    
    // Keep track of the balances
    private var onlineBalance: Double = 0.0
    private var cashBalance: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe current balance (online transactions)
        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            onlineBalance = balance ?: 0.0
            binding.onlineAmountValue.text = String.format("₹%.2f", onlineBalance)
            updateTotalAmount()
        }
        
        // Observe cash balance
        viewModel.cashBalance.observe(viewLifecycleOwner) { balance ->
            cashBalance = balance ?: 0.0
            binding.cashAmountValue.text = String.format("₹%.2f", cashBalance)
            updateTotalAmount()
        }

        // Show savings target
        viewModel.savingsTarget.observe(viewLifecycleOwner) { target ->
            binding.savingsTargetValue.text = String.format("₹%.2f", target)
        }
    }
    
    private fun updateTotalAmount() {
        // Calculate total amount (sum of online and cash transactions)
        val totalAmount = onlineBalance + cashBalance
        binding.totalAmountValue.text = String.format("₹%.2f", totalAmount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

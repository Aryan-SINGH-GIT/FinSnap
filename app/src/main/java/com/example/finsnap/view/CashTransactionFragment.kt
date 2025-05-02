package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentCashTransactionBinding
import com.example.finsnap.viewmodel.FinanceViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CashTransactionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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
        setupFab()
        observeTransactions()

        return binding.root
    }
    private fun setupRecyclerView() {
        cashAdapter = CashAdapter()
        binding.rvCashTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cashAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {

            findNavController().navigate(R.id.action_cashTransactionFragment_to_addCashFragment)
        }
    }

    private fun observeTransactions() {
        viewModel.cashTransactions.observe(viewLifecycleOwner) { transactions ->
            cashAdapter.updateItems(transactions)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCashTransactions()
    }


}
package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentAcountBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.SessionManager

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AcountFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AcountFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentAcountBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FinanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAcountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        // Set up UI with user information
        setupUserInfo()
        
        // Set up bank details
        setupBankDetails()

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupUserInfo() {
        val email = SessionManager.getUserEmail()
        binding.emailText.text = "Email: $email"
    }

    private fun setupBankDetails() {
        viewModel.loadUserBankDetails()
        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            binding.currentBalanceText.text = "Current Balance: ₹$balance"
        }
        viewModel.cashBalance.observe(viewLifecycleOwner) { cashBalance ->
            binding.cashBalanceText.text = "Cash Balance: ₹$cashBalance"
        }
        viewModel.savingsTarget.observe(viewLifecycleOwner) { target ->
            binding.savingsTargetText.text = "Monthly Savings Target: ₹$target"
        }
    }

    private fun setupClickListeners() {


        binding.logoutButton.setOnClickListener {
            SessionManager.logout(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AcountFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AcountFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentTransactionDetailBinding
import com.example.finsnap.model.UserAmount
//import com.example.finsnap.model.UserAmount
import com.example.finsnap.viewmodel.FinanceViewModel

class TransactionDetailFragment : Fragment() {
    private lateinit var binding: FragmentTransactionDetailBinding
    private lateinit var transaction: UserAmount
    private lateinit var viewModel: FinanceViewModel

    // Use Safe Args to retrieve arguments
    private val args: TransactionDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel - use the activity scope to share it with HomeFragment
        viewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]

        // Get transaction from Safe Args
        transaction = args.transaction

        binding.editSender.setText(transaction.sender)
        binding.transactionImage.setImageResource(transaction.amtImage)
        binding.transactionTime.text = transaction.time
        binding.transactionAmount.text = transaction.amtChange
        binding.rawMessage.text = transaction.rawMessage
        binding.upiRef.text = extractUpiReference(transaction.rawMessage)

        binding.categoryText.text = transaction.category

        binding.categoryLabel.setOnClickListener {

            findNavController().navigate(R.id.action_transactionDetailFragment_to_categoryFragment)


        }
        binding.categoryText.setOnClickListener {

            findNavController().navigate(R.id.action_transactionDetailFragment_to_categoryFragment)


        }

        binding.transactionImage.setOnClickListener {
            findNavController().navigate(R.id.action_transactionDetailFragment_to_categoryFragment)


        }

        setFragmentResultListener("category_result") { _, bundle ->
            val category = bundle.getString("selectedCategory") ?: "Miscellaneous"
            val image = bundle.getInt("categoryImage", R.drawable.ic_miscellaneous)

            transaction = transaction.copy(category = category, categoryImage = image)
            binding.transactionImage.setImageResource(image)
            binding.categoryText.text = category
        }








        binding.saveButton.setOnClickListener {






            val newName = binding.editSender.text.toString()
            // ✅ Preserve updated category + name
            val updatedTransaction = transaction.copy(
                sender = newName,
                category = transaction.category,
                categoryImage = transaction.categoryImage
            )


            // Update the transaction in the ViewModel
            viewModel.updateTransactionInDb(updatedTransaction)


            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()

            // Navigate back to home using Navigation Component
            findNavController().navigate(
                TransactionDetailFragmentDirections.actionTransactionDetailFragmentToHomeFragment()
            )
        }

    }

    private fun extractUpiReference(message: String): String {
        // Match common UPI reference patterns including "Ref no", "UPI Ref", or "to:UPI/<ref>"
        val pattern = Regex("(?i)(?:(?:Ref(?:erence)?\\s*(?:No\\.?|:)?)|UPI Ref(?:erence)?(?: No\\.?|:)?)\\s*([A-Za-z0-9]+)|to:UPI/([A-Za-z0-9]+)")
        val match = pattern.find(message)
        return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
            ?: match?.groupValues?.getOrNull(2)?.takeIf { it.isNotEmpty() }
            ?: "N/A"
    }
}
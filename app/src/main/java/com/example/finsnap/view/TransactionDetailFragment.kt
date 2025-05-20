package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentTransactionDetailBinding
import com.example.finsnap.model.UserAmount
import com.example.finsnap.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionDetailFragment : Fragment() {
    private lateinit var binding: FragmentTransactionDetailBinding
    private lateinit var transaction: UserAmount
    private lateinit var viewModel: FinanceViewModel
    
    // Track if we've changed the category
    private var categoryChanged = false
    private var selectedCategory = ""
    private var selectedCategoryImage = 0

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
        
        // Initialize category values
        selectedCategory = transaction.category
        selectedCategoryImage = transaction.categoryImage
        
        // Set up the UI with transaction details
        setupUI()

        // Listen for updates from ViewModel
        observeViewModel()
        
        // Set click listeners
        setupClickListeners()
        
        // Set up category result listener
        setupCategoryListener()
    }
    
    private fun setupUI() {
        binding.editSender.setText(transaction.sender)
        
        // Display the proper category image (always use categoryImage if available)
        val imageResource = transaction.categoryImage
        binding.transactionImage.setImageResource(imageResource)
        
        // Clean up timestamp display by removing any hidden suffixes
        val displayTime = if (transaction.time.contains("-")) {
            transaction.time.substringBefore("-")
        } else if (transaction.time.contains("(")) {
            transaction.time.substringBefore(" (")
        } else {
            transaction.time
        }
        binding.transactionTime.text = displayTime
        
        binding.transactionAmount.text = transaction.amtChange
        binding.rawMessage.text = transaction.rawMessage
        binding.upiRef.text = extractUpiReference(transaction.rawMessage)
        binding.categoryText.text = transaction.category
        
        // Debug logs for image resources
        android.util.Log.d("TransactionDetail", "Setting image resource: $imageResource")
        android.util.Log.d("TransactionDetail", "Category from transaction: ${transaction.category}")
    }
    
    private fun observeViewModel() {
        // Observe transaction updates from ViewModel
        viewModel.updatedTransaction.observe(viewLifecycleOwner) { updatedTxn ->
            if (updatedTxn.id == transaction.id || 
                updatedTxn.rawMessage == transaction.rawMessage) {
                // Update our local transaction with the updated one
                transaction = updatedTxn
                
                // Update our category tracking values
                if (!categoryChanged) {
                    selectedCategory = transaction.category
                    selectedCategoryImage = transaction.categoryImage
                }
                
                setupUI()  // Refresh UI with new data
            }
        }
    }
    
    private fun setupCategoryListener() {
        setFragmentResultListener("category_result") { _, bundle ->
            val category = bundle.getString("selectedCategory") ?: "Miscellaneous"
            val image = bundle.getInt("categoryImage", R.drawable.ic_miscellaneous)
            
            // Mark that we've changed the category
            categoryChanged = true
            
            // Store the selected category and image
            selectedCategory = category
            selectedCategoryImage = image
            
            // Update our local transaction
            transaction = transaction.copy(category = category, categoryImage = image)
            
            // Update UI
            binding.transactionImage.setImageResource(image)
            binding.categoryText.text = category
            
            // Debug log
            android.util.Log.d("TransactionDetail", "Category selected: $category, image: $image")
        }
    }
    
    private fun setupClickListeners() {
        binding.categoryLabel.setOnClickListener {
            findNavController().navigate(R.id.action_transactionDetailFragment_to_categoryFragment)
        }
        
        binding.categoryText.setOnClickListener {
            findNavController().navigate(R.id.action_transactionDetailFragment_to_categoryFragment)
        }

        binding.transactionImage.setOnClickListener {
            findNavController().navigate(R.id.action_transactionDetailFragment_to_categoryFragment)
        }

        binding.saveButton.setOnClickListener {
            val newName = binding.editSender.text.toString()

            // Create the updated transaction with all new values
            val updatedTransaction = transaction.copy(
                sender = newName,
                category = selectedCategory,
                categoryImage = selectedCategoryImage
            )

            // Log what we're saving
            android.util.Log.d("TransactionDetailFragment", "Saving transaction: ${updatedTransaction.id}")
            android.util.Log.d("TransactionDetailFragment", "New Name: $newName")
            android.util.Log.d("TransactionDetailFragment", "Category: $selectedCategory")
            android.util.Log.d("TransactionDetailFragment", "Image resource: $selectedCategoryImage")

            // Update in ViewModel (which updates the database)
            viewModel.updateTransactionInDb(updatedTransaction)

            // Show success message
            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()

            // Navigate back to home using Navigation Component with clear back stack
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build()
            findNavController().navigate(R.id.homeFragment, null, navOptions)
        }
        
        // Add a back button listener
        binding.backButton?.setOnClickListener {
            findNavController().navigateUp()
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
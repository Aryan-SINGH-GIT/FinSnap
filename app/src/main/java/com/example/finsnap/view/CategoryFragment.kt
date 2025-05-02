package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController

import com.example.finsnap.R

import com.example.finsnap.databinding.FragmentCategoryBinding


class CategoryFragment : Fragment() {
        lateinit var binding: FragmentCategoryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCategoryBinding.inflate(inflater, container, false)


        binding.icFoodanddrink.setOnClickListener {
            updateCategoryAndGoBack( "Food & Drink", R.drawable.ic_foodanddrink)
        }
        binding.icTravel.setOnClickListener {
            updateCategoryAndGoBack( "Travel", R.drawable.ic_travel)
        }
        binding.icShopping.setOnClickListener {
            updateCategoryAndGoBack( "Shopping", R.drawable.ic_shopping)
        }
        binding.icEntertainment.setOnClickListener {
            updateCategoryAndGoBack( "Entertainment", R.drawable.ic_entertainment)
        }
        binding.icInvoice.setOnClickListener {
            updateCategoryAndGoBack("Bill", R.drawable.invoice)
        }
        binding.icEducation.setOnClickListener {
            updateCategoryAndGoBack("Education", R.drawable.education)
        }
        binding.icGrocerries.setOnClickListener {
            updateCategoryAndGoBack("Groceries", R.drawable.ic_grocerries)
        }
        binding.icWorkoutandhealth.setOnClickListener {
            updateCategoryAndGoBack("Workout and Health", R.drawable.ic_workoutandhealth)
        }
        binding.icTranportation.setOnClickListener {
            updateCategoryAndGoBack("Transportation", R.drawable.ic_tranportation)
        }
        binding.icMiscellaneous.setOnClickListener {
            updateCategoryAndGoBack("Food & Drink", R.drawable.ic_miscellaneous)
        }



        return binding.root
    }



    private fun updateCategoryAndGoBack(selectedCategory: String, imageRes: Int) {
        val result = Bundle().apply {
            putString("selectedCategory", selectedCategory)
            putInt("categoryImage", imageRes)
        }
        setFragmentResult("category_result", result)
        findNavController().navigateUp()
    }



}
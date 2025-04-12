package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.FinSnap.view.AmoutAdapter

import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentHomeBinding
import com.example.finsnap.model.Amout_Repository


class HomeFragment : Fragment() {
   private lateinit var binding: FragmentHomeBinding
    private lateinit var recyclerView:RecyclerView
    private lateinit var amounadapter: AmoutAdapter
    private val repository = Amout_Repository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentHomeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        val view = FragmentHomeBinding.inflate(inflater, container, false)

        recyclerView = binding.myrecyclerview

        // Initialize Adapter
        amounadapter= AmoutAdapter()

        // Setup RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = amounadapter
        }
        val amountItems = repository.getAmountItems()
        amounadapter.updateItems(amountItems)


        return binding.root
    }


}
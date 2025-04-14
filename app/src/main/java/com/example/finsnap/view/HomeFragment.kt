package com.example.finsnap.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

//import com.example.finsnap.view.AmoutAdapter

import com.example.finsnap.databinding.FragmentHomeBinding

import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.UserDatabase


class HomeFragment : Fragment() {
   private lateinit var binding: FragmentHomeBinding
    private lateinit var recyclerView:RecyclerView
    private lateinit var amounadapter: AmoutAdapter
    private lateinit var myfinanaceViewModel: FinanceViewModel
    private lateinit var database: UserDatabase


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentHomeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        val view = FragmentHomeBinding.inflate(inflater, container, false)
        myfinanaceViewModel= ViewModelProvider(this).get(FinanceViewModel::class.java)

        database= Room.databaseBuilder(
            requireContext(),
            UserDatabase::class.java,
            "finsnap_database"
        ).fallbackToDestructiveMigration().build()

//        var currentAmount=0.0;
//        var bankId=1;
//        GlobalScope.launch {
////            currentAmount = database.UsersDao().getCurrentAmount(bankId);
////            binding.textView.text=currentAmount.toString()
//
//            database.UsersDao().getUserWithBank(userId).observe(this) { userWithBank ->
//                val amount = userWithBank.bank.currentAmount
//                // use this to update UI
//            }
//
//
//
//        }



            recyclerView = binding.myrecyclerview

        // Initialize Adapter
        amounadapter= AmoutAdapter()

        // Setup RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = amounadapter
        }
        val amountItems = myfinanaceViewModel.getRepo().getAmountItems()
        amounadapter.updateItems(amountItems)


        return binding.root
    }


}
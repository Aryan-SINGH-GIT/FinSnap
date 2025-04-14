package com.example.finsnap.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.finsnap.R

import com.example.finsnap.model.UserAmount


class AmoutAdapter(private val chatList: MutableList<UserAmount> = mutableListOf()) :
    RecyclerView.Adapter<AmoutAdapter.AmoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.amount_item, parent, false)
        return AmoutViewHolder(view)
    }

    override fun getItemCount() = chatList.size

    override fun onBindViewHolder(holder: AmoutViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    fun updateItems(items: List<UserAmount>) {
        chatList.clear()
        chatList.addAll(items)
        notifyDataSetChanged()
    }

    class AmoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sender = view.findViewById<TextView>(R.id.sender)
        val time = view.findViewById<TextView>(R.id.time)
        val amtImage = view.findViewById<ImageView>(R.id.amtImage)
        val amtChange = view.findViewById<TextView>(R.id.amtChange)


        fun bind(userAmount: UserAmount){
            sender.text=userAmount.sender
            time.text=userAmount.time
            amtImage.setImageResource(userAmount.amtImage)
            amtChange.text=userAmount.amtChange



        }


    }
}
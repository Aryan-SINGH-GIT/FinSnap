package com.example.finsnap.view



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.finsnap.R


import com.example.finsnap.model.UserCash


class CashAdapter(private val chatList: MutableList<UserCash> = mutableListOf()) :
    RecyclerView.Adapter<CashAdapter.CashViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cash_transaction_item, parent, false)
        return CashViewHolder(view)
    }

    override fun getItemCount() = chatList.size

    override fun onBindViewHolder(holder: CashViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    fun updateItems(items: List<UserCash>) {
        chatList.clear()
        chatList.addAll(items)
        notifyDataSetChanged()
    }

    class CashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cashsender = view.findViewById<TextView>(R.id.tvDescription)
        val cashtime = view.findViewById<TextView>(R.id.tvTimestamp)
        val cashamtImage = view.findViewById<ImageView>(R.id.ivTransactionIcon)
        val cashamtChange = view.findViewById<TextView>(R.id.tvAmount)


        fun bind(userCash: UserCash) {
            cashsender.text = userCash.cashSender
            cashtime.text = userCash.cashTime
            cashamtImage.setImageResource(userCash.cashImage)
            cashamtChange.text = userCash.CashamtChange


        }


    }
}








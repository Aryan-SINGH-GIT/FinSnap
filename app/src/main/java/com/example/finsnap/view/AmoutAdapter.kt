package com.example.finsnap.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finsnap.R
import com.example.finsnap.model.UserAmount

class AmoutAdapter(
    private val items: MutableList<UserAmount> = mutableListOf(),
    private val onItemClick: (UserAmount) -> Unit
) : RecyclerView.Adapter<AmoutAdapter.AmoutViewHolder>() {

    // Add logging for better debugging
    private val TAG = "AmoutAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.amount_item, parent, false)
        return AmoutViewHolder(view, onItemClick)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: AmoutViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun updateItems(newItems: List<UserAmount>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Update a specific item
    fun updateItem(updatedTransaction: UserAmount) {
        // Log before update
        android.util.Log.d(TAG, "Updating transaction: ${updatedTransaction.sender}")
        android.util.Log.d(TAG, "Current items count: ${items.size}")

        val position = items.indexOfFirst {
            // More flexible matching based on content that won't change
            it.rawMessage == updatedTransaction.rawMessage && it.time == updatedTransaction.time
        }

        android.util.Log.d(TAG, "Found position: $position")

        if (position != -1) {
            // Log the old sender
            android.util.Log.d(TAG, "Old sender: ${items[position].sender}")

            // Replace the item
            items[position] = updatedTransaction

            // Log the new sender
            android.util.Log.d(TAG, "New sender: ${items[position].sender}")

            // Force the adapter to redraw the item
            notifyItemChanged(position)
        } else {
            android.util.Log.e(TAG, "Could not find transaction to update!")
            android.util.Log.d(TAG, "Raw message: ${updatedTransaction.rawMessage}")
            android.util.Log.d(TAG, "Time: ${updatedTransaction.time}")

            // Dump first few transactions for debugging
            if (items.isNotEmpty()) {
                val sample = items.take(Math.min(3, items.size))
                sample.forEachIndexed { index, item ->
                    android.util.Log.d(TAG, "Item $index - rawMessage: ${item.rawMessage}")
                    android.util.Log.d(TAG, "Item $index - time: ${item.time}")
                }
            }
        }
    }

    class AmoutViewHolder(
        view: View,
        private val onItemClick: (UserAmount) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val sender = view.findViewById<TextView>(R.id.sender)
        private val time = view.findViewById<TextView>(R.id.time)
        private val amtImage = view.findViewById<ImageView>(R.id.amtImage)
        private val amtChange = view.findViewById<TextView>(R.id.amtChange)

        fun bind(userAmount: UserAmount) {
            sender.text = userAmount.sender
            time.text = userAmount.time
            amtImage.setImageResource(userAmount.amtImage)
            amtChange.text = userAmount.amtChange

            itemView.setOnClickListener {
                onItemClick(userAmount)
            }
        }
    }
}
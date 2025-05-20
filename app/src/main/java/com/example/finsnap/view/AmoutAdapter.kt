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
    val items: MutableList<UserAmount> = mutableListOf(),
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
        val transaction = items[position]
        holder.bind(transaction)
        
        // Set text color based on transaction type
        val textColor = if (transaction.isCredit) {
            holder.itemView.context.getColor(android.R.color.holo_green_dark)
        } else {
            holder.itemView.context.getColor(android.R.color.holo_red_dark)
        }
        holder.amtChange.setTextColor(textColor)
        
        // Also set the amount text with proper sign
        val amountText = if (transaction.isCredit) {
            "+₹${transaction.amount}"
        } else {
            "-₹${transaction.amount}"
        }
        holder.amtChange.text = amountText
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
        android.util.Log.d(TAG, "Transaction ID: ${updatedTransaction.id}")
        
        // First try to find by ID if it's valid
        var position = -1
        
        if (updatedTransaction.id > 0) {
            position = items.indexOfFirst { it.id == updatedTransaction.id }
            android.util.Log.d(TAG, "Search by ID result: $position")
        }
        
        // If not found by ID, try with unique key
        if (position == -1) {
            position = items.indexOfFirst {
                generateUniqueKey(it) == generateUniqueKey(updatedTransaction)
            }
            android.util.Log.d(TAG, "Search by unique key result: $position")
        }
        
        // Last resort: try to match by raw message
        if (position == -1) {
            position = items.indexOfFirst {
                it.rawMessage == updatedTransaction.rawMessage
            }
            android.util.Log.d(TAG, "Search by raw message result: $position")
        }

        android.util.Log.d(TAG, "Final position found: $position")

        if (position != -1) {
            // Log the old sender
            android.util.Log.d(TAG, "Old sender: ${items[position].sender}")
            android.util.Log.d(TAG, "Old time: ${items[position].time}")

            // Replace the item
            items[position] = updatedTransaction

            // Log the new sender
            android.util.Log.d(TAG, "New sender: ${items[position].sender}")
            android.util.Log.d(TAG, "New time: ${items[position].time}")

            // Force the adapter to redraw the item
            notifyItemChanged(position)
        } else {
            android.util.Log.e(TAG, "Could not find transaction to update!")
            android.util.Log.d(TAG, "Raw message: ${updatedTransaction.rawMessage}")
            android.util.Log.d(TAG, "Time: ${updatedTransaction.time}")
            android.util.Log.d(TAG, "Amount: ${updatedTransaction.amount}")

            // Dump first few transactions for debugging
            if (items.isNotEmpty()) {
                val sample = items.take(Math.min(3, items.size))
                sample.forEachIndexed { index, item ->
                    android.util.Log.d(TAG, "Item $index - ID: ${item.id}")
                    android.util.Log.d(TAG, "Item $index - rawMessage: ${item.rawMessage}")
                    android.util.Log.d(TAG, "Item $index - time: ${item.time}")
                    android.util.Log.d(TAG, "Item $index - amount: ${item.amount}")
                    android.util.Log.d(TAG, "Item $index - unique key: ${generateUniqueKey(item)}")
                }
            }
        }
    }
    
    // Helper function to generate a unique key for transactions (matching the Repository impl)
    private fun generateUniqueKey(transaction: UserAmount): String {
        return "${transaction.rawMessage.hashCode()}_${transaction.amount}_${transaction.sender}"
    }

    inner class AmoutViewHolder(
        view: View,
        private val onItemClick: (UserAmount) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        val amountSender = view.findViewById<TextView>(R.id.sender)
        val amountTime = view.findViewById<TextView>(R.id.time)
        val amtImage = view.findViewById<ImageView>(R.id.amtImage)
        val amtChange = view.findViewById<TextView>(R.id.amtChange)
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }
        
        fun bind(transaction: UserAmount) {
            amountSender.text = transaction.sender
            
            // Clean up timestamp display by removing any hidden suffixes
            val displayTime = if (transaction.time.contains("-")) {
                transaction.time.substringBefore("-")
            } else if (transaction.time.contains("(")) {
                transaction.time.substringBefore(" (")
            } else {
                transaction.time
            }
            amountTime.text = displayTime
            
            // Always use the category image for the icon
            amtImage.setImageResource(transaction.categoryImage)
            
            amtChange.text = transaction.amtChange
            // Don't try to set text on an ImageView
        }
    }

}
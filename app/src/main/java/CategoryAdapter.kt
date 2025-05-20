//package com.example.finsnap.view
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.finsnap.R
//
//class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
//
//    private val categories = mutableListOf<Pair<String, Double>>()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
//        return CategoryViewHolder(view)
//    }
//
//    override fun getItemCount(): Int = categories.size
//
//    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
//        holder.bind(categories[position])
//    }
//
//    fun updateCategories(categorySummary: Map<String, Double>) {
//        categories.clear()
//        categories.addAll(categorySummary.entries.map { Pair(it.key, it.value) }
//            .sortedByDescending { it.second })
//        notifyDataSetChanged()
//    }
//
//    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        private val categoryName: TextView = view.findViewById(R.id.categoryName)
//        private val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
//        private val categoryPercentage: TextView = view.findViewById(R.id.categoryPercentage)
//
//        fun bind(category: Pair<String, Double>) {
//            categoryName.text = category.first
//            categoryAmount.text = String.format("₹%.2f", category.second)
//            // Calculate percentage based on total - would need to pass total in real implementation
//            // categoryPercentage.text = String.format("%.1f%%", percentage)
//        }
//    }
//}
package com.example.finsnap.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finsnap.R
import com.example.finsnap.model.Article
import org.jsoup.Jsoup

class ArticleAdapter(
    private val articles: List<Article>,
    private val onClick: (Article) -> Unit
) : RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.articleTitle)
        val image: ImageView = itemView.findViewById(R.id.articleImage)
        val author: TextView = itemView.findViewById(R.id.articleAuthor)
        val pubDate: TextView = itemView.findViewById(R.id.articlePubDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.title.text = article.title
        holder.author.text = article.author
        holder.pubDate.text = formatDate(article.pubDate)

        // Extract image from content if thumbnail is empty
        val imageUrl = if (!article.thumbnail.isNullOrEmpty()) {
            article.thumbnail
        } else {
            extractImageFromContent(article.description) ?: extractImageFromContent(article.content ?: "")
        }

        Glide.with(holder.itemView)
            .load(imageUrl)
            .placeholder(R.drawable.ic_article) // Add a placeholder drawable in your resources
            .error(R.drawable.ic_article) // Use the same placeholder for errors
            .into(holder.image)

        // Set up click listeners for both the entire view and individual elements
        holder.itemView.setOnClickListener { onClick(article) }
        holder.title.setOnClickListener { onClick(article) }
        holder.image.setOnClickListener { onClick(article) }
    }

    override fun getItemCount(): Int = articles.size

    /**
     * Formats the publication date to a more user-friendly format
     */
    private fun formatDate(pubDate: String): String {
        return try {
            // Simple formatting for demonstration - you might want a more sophisticated approach
            val parts = pubDate.split(" ")
            if (parts.size >= 2) {
                "${parts[0]} ${parts[1]}"
            } else {
                pubDate
            }
        } catch (e: Exception) {
            pubDate
        }
    }

    /**
     * Extracts image URL from HTML content
     */
    private fun extractImageFromContent(content: String): String? {
        return try {
            val doc = Jsoup.parse(content)
            val imgElement = doc.select("img").firstOrNull()
            imgElement?.attr("src")
        } catch (e: Exception) {
            null
        }
    }
}
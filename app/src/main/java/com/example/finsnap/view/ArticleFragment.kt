package com.example.finsnap.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finsnap.R
import com.example.finsnap.databinding.FragmentAddCashBinding
import com.example.finsnap.databinding.FragmentArticleBinding
import com.example.finsnap.model.RssResponse
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArticleAdapter
    private lateinit var binding: FragmentArticleBinding

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//
//
//    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentArticleBinding.inflate(inflater, container, false)
        recyclerView = binding.articleRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        fetchArticles()
        return binding.root
    }

    private fun fetchArticles() {
        val rssUrl ="https://medium.com/feed/financial-strategy"
        RetrofitClient.api.getArticles(rssUrl).enqueue(object : Callback<RssResponse> {
            override fun onResponse(call: Call<RssResponse>, response: Response<RssResponse>) {
                if (response.isSuccessful) {
                    val articles = response.body()?.items ?: emptyList()
                    adapter = ArticleAdapter(articles) { article ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                        startActivity(intent)
                    }
                    recyclerView.adapter = adapter
                }
            }

            override fun onFailure(call: Call<RssResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
package com.example.finsnap.viewmodel


import com.example.finsnap.model.RssResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface RssApiService {
    @GET("v1/api.json")
    fun getArticles(
        @Query("rss_url") rssUrl: String
    ): Call<RssResponse>
}
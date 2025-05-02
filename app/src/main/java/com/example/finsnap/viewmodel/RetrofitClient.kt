package com.example.finsnap.viewmodel



import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.rss2json.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: RssApiService = retrofit.create(RssApiService::class.java)
}
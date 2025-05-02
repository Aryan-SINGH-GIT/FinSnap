package com.example.finsnap.model

data class RssResponse(
    val items: List<Article>
)

data class Article(
    val title: String,
    val link: String,
    val pubDate: String,
    val thumbnail: String?,
    val description: String,
    val author: String,
    val content:String
)
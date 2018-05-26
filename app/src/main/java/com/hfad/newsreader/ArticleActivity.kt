package com.hfad.newsreader

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_article.*
import java.net.URL

class ArticleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        val index = intent.getIntExtra("index", -1)
        val url = MainActivity.urls[index]

        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
    }
}

package com.hfad.newsreader

import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var titles : ArrayList<String>
        lateinit var urls : ArrayList<String>
    }
    lateinit var adapter: ArrayAdapter<String>
    lateinit var articlesDB : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        articlesDB = this.openOrCreateDatabase("Articles", android.content.Context.MODE_PRIVATE, null)
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, url VARCHAR)")
        titles = ArrayList<String>()
        urls = ArrayList<String>()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)


        if (!articlesDB.isOpen){
            val task = DownloadTask()
            try {

                task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty")

            }catch (e : Exception){
                e.printStackTrace()
            }
        } else{
            updateListView()
        }



        listView.adapter = adapter
        listView.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val intent = Intent(applicationContext, ArticleActivity::class.java)
                intent.putExtra("index", position)
                startActivity(intent)
            }
        }


    }

    fun updateListView(){
        val c : Cursor = articlesDB.rawQuery("SELECT * FROM articles", null)
        val titleIndex = c.getColumnIndex("title")
        val urlIndex = c.getColumnIndex("url")

        if (c.moveToFirst()){
            titles.clear()
            urls.clear()

            do {
                titles.add(c.getString(titleIndex))
                urls.add(c.getString(urlIndex))
            } while (c.moveToNext())

            adapter.notifyDataSetChanged()
        }
    }

    inner class DownloadTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg urls: String?): String {
            var result = ""
            var url : URL
            var httpURLConnection : HttpURLConnection

            try {
                url = URL(urls[0])
                httpURLConnection = url.openConnection() as HttpURLConnection
                var inputStream = httpURLConnection.inputStream
                var inputStreamReader = InputStreamReader(inputStream)
                var data = inputStream.read()

                while (data != -1){
                    val current= data.toChar()
                    result += current
                    data = inputStreamReader.read()
                }

                val jsonArray = JSONArray(result)
                var numberOfItems = 20

                if (jsonArray.length() < 20){
                    numberOfItems = jsonArray.length()
                }

                articlesDB.execSQL("DELETE FROM articles")

                var i = 0
                while (i < numberOfItems){
                    val articleID = jsonArray.getString(i)
                    url = URL("https://hacker-news.firebaseio.com/v0/item/" + articleID + ".json?print=pretty")

                    httpURLConnection = url.openConnection() as HttpURLConnection
                    inputStream = httpURLConnection.inputStream
                    inputStreamReader = InputStreamReader(inputStream)
                    data = inputStream.read()

                    var articleInfo = ""

                    while (data != -1){
                        val current= data.toChar()
                        articleInfo += current
                        data = inputStreamReader.read()
                    }
                    val jsonObject = JSONObject(articleInfo)
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        val articleTitle = jsonObject.getString("title")
                        val articleUrl = jsonObject.getString("url")

                        val sql = "INSERT INTO articles (articleID, title, url) VALUES (?, ?, ?)"
                        val statement : SQLiteStatement = articlesDB.compileStatement(sql)
                        statement.bindString(1, articleID)
                        statement.bindString(2, articleTitle)
                        statement.bindString(3, articleUrl)
                        statement.execute()
                    }
                    i++
                }

                return result

            }catch (e : Exception){
                e.printStackTrace()
            }

            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            updateListView()
        }
    }
}

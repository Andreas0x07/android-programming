package com.example.bugsgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import java.io.InputStream

class RulesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rules, container, false)
        val webView: WebView = view.findViewById(R.id.wvRules)

        // Load HTML from raw resource
        val inputStream: InputStream = resources.openRawResource(R.raw.rules)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        webView.loadData(htmlContent, "text/html", "UTF-8")

        return view
    }
}
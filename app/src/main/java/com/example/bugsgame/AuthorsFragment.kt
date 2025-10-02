package com.example.bugsgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment

class AuthorsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_authors, container, false)
        val listView: ListView = view.findViewById(R.id.lvAuthors)

        val authorNames = resources.getStringArray(R.array.author_names)
        val authorPhotos = intArrayOf(
            R.drawable.weibert,
            R.drawable.savchenko
        )

        val adapter = AuthorsAdapter(authorNames, authorPhotos)
        listView.adapter = adapter

        return view
    }

    private inner class AuthorsAdapter(
        private val names: Array<String>,
        private val photos: IntArray
    ) : BaseAdapter() {

        override fun getCount(): Int = names.size

        override fun getItem(position: Int): Any = names[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.item_author, parent, false
            )
            val ivPhoto: ImageView = view.findViewById(R.id.ivAuthorPhoto)
            val tvName: TextView = view.findViewById(R.id.tvAuthorName)

            tvName.text = names[position]
            ivPhoto.setImageResource(photos[position])

            return view
        }
    }
}
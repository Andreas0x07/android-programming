package com.example.bugsgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bugsgame.data.AppDao
import com.example.bugsgame.data.DatabaseProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScoresFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scores, container, false)
        val listView: ListView = view.findViewById(R.id.lvScores)

        val adapter = ScoresAdapter()
        listView.adapter = adapter

        lifecycleScope.launch {
            DatabaseProvider.getDatabase(requireContext()).appDao().getAllScores()
                .collectLatest { scores ->
                    adapter.updateScores(scores)
                }
        }

        return view
    }

    private inner class ScoresAdapter : BaseAdapter() {
        private val scores = mutableListOf<AppDao.ScoreWithPlayer>()

        override fun getCount(): Int = scores.size

        override fun getItem(position: Int): Any = scores[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context).inflate(
                R.layout.item_score, parent, false
            )

            val tvScore: TextView = view.findViewById(R.id.tvScore)
            val tvPlayerName: TextView = view.findViewById(R.id.tvPlayerName)
            val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
            val tvDate: TextView = view.findViewById(R.id.tvDate)

            val score = scores[position]
            tvScore.text = "Очки: ${score.score}"
            tvPlayerName.text = "Игрок: ${score.fullName}"
            tvDifficulty.text = "Сложность: ${score.difficulty}"
            tvDate.text = "Дата: ${
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(score.timestamp))
            }"

            return view
        }

        fun updateScores(newScores: List<AppDao.ScoreWithPlayer>) {
            scores.clear()
            scores.addAll(newScores)
            notifyDataSetChanged()
        }
    }
}
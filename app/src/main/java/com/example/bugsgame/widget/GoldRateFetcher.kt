package com.example.bugsgame.widget

import android.content.Context
import com.example.bugsgame.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simpleframework.xml.core.Persister
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object GoldRateFetcher {

    suspend fun fetchAndSave(context: Context): Float? {
        return withContext(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayCalendar = Calendar.getInstance()
                val todayDateString = dateFormat.format(todayCalendar.time)

                val sevenDaysAgoCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                val sevenDaysAgoDateString = dateFormat.format(sevenDaysAgoCalendar.time)

                val responseBody = RetrofitClient.instance.getDailyRates(
                    sevenDaysAgoDateString,
                    todayDateString
                )
                val xmlString = String(responseBody.bytes(), Charset.forName("windows-1251"))

                val serializer = Persister()
                val response: Metals = serializer.read(Metals::class.java, xmlString)

                val goldRecord = response.metallList.filter { it.code == "1" }.lastOrNull()
                val rate = goldRecord?.price?.replace(",", ".")?.toFloatOrNull()
                val ratePerGram = rate ?: 0f

                val sharedPrefs =
                    context.getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
                sharedPrefs.edit().putFloat("gold_rate", ratePerGram).apply()

                ratePerGram
            } catch (e: Exception) {
                android.util.Log.e("GoldRateFetcher", "Failed to fetch gold rate", e)
                null
            }
        }
    }
}
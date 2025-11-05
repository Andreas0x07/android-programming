package com.example.bugsgame.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.bugsgame.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoldRateWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)
        views.setTextViewText(R.id.tvGoldRate, "...")

        scope.launch {
            val ratePerGram = GoldRateFetcher.fetchAndSave(context)

            val displayText = if (ratePerGram != null) {
                val formattedRate = DecimalFormat("#,##0.00").format(ratePerGram)
                val dateStr = SimpleDateFormat("MM.dd", Locale.getDefault()).format(Date())
                "Au ($dateStr)\n$formattedRate"
            } else {
                "Error"
            }

            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.tvGoldRate, displayText)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        job.cancel()
    }
}
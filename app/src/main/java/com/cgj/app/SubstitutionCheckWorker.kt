package com.cgj.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private val LAST_VPLAN_URL = stringPreferencesKey("last_vplan_url")

class SubstitutionCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val (pdfUrl, imageUrl) = fetchSubstitutionUrls()
            val latestUrl = pdfUrl ?: imageUrl

            if (latestUrl != null) {
                val prefs = applicationContext.dataStore.data.first()
                val last = prefs[LAST_VPLAN_URL]
                if (last != latestUrl) {
                    applicationContext.dataStore.edit { it[LAST_VPLAN_URL] = latestUrl }
                    createNotificationChannel(applicationContext)
                    showNewVPlanNotification(applicationContext, latestUrl)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun fetchSubstitutionUrls(): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val pageUrl = "https://www.c-g-j.de/aktuelles-und-termine/vertretungsplan/"
            val connection = URL(pageUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            val content = BufferedInputStream(connection.inputStream).reader().use { it.readText() }

            val pdfRegex = """<a href="(/asset/[^"]+/vertretungsplan[^"]*\.pdf)">""".toRegex()
            val imageRegex = """<img[^>]+src="(/asset/[^"]+/vplan\.png)"[^>]*>""".toRegex()

            val pdfUrl = pdfRegex.find(content)?.groupValues?.get(1)?.let { path ->
                "https://www.c-g-j.de$path"
            }
            val imageUrl = imageRegex.find(content)?.groupValues?.get(1)?.let { path ->
                "https://www.c-g-j.de$path"
            }
            Pair(pdfUrl, imageUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "vertretungsplan_check"

        fun scheduleSubstitutionChecks(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SubstitutionCheckWorker>(1, TimeUnit.HOURS, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
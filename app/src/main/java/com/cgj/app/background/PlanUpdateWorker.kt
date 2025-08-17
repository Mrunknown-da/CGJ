package com.cgj.app.background

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cgj.app.dataStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cgj.app.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit


class PlanUpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
		try {
			val content = fetchSubstitutionPage()
			if (content.isNullOrEmpty()) return@withContext Result.success()
			val newHash = sha256(content)

			val key = stringPreferencesKey("last_substitution_hash")
			val prefs = applicationContext.dataStore.data.first()
			val lastHash = prefs[key]

			if (lastHash == null || lastHash != newHash) {
				applicationContext.dataStore.edit { it[key] = newHash }
				NotificationHelper.createNotificationChannel(applicationContext)
				NotificationHelper.showPlanUpdateNotification(applicationContext)
			}

			Result.success()
		} catch (_: Exception) {
			Result.retry()
		}
	}

	private fun fetchSubstitutionPage(): String? {
		return try {
			val url = URL("https://www.c-g-j.de/aktuelles-und-termine/vertretungsplan/")
			val connection = url.openConnection() as HttpURLConnection
			connection.requestMethod = "GET"
			connection.connectTimeout = 10000
			connection.readTimeout = 10000
			connection.inputStream.bufferedReader().use { it.readText() }
		} catch (_: Exception) {
			null
		}
	}

	private fun sha256(input: String): String {
		val md = MessageDigest.getInstance("SHA-256")
		val digest = md.digest(input.toByteArray())
		return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
	}

	companion object {
		private const val UNIQUE_WORK_NAME = "plan_update_worker"

		fun schedule(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			val request = PeriodicWorkRequestBuilder<PlanUpdateWorker>(15, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build()
			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(
					UNIQUE_WORK_NAME,
					ExistingPeriodicWorkPolicy.UPDATE,
					request
				)
		}

		fun cancel(context: Context) {
			WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
		}
	}
}


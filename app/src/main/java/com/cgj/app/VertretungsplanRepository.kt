package com.cgj.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*

class VertretungsplanRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "VertretungsplanRepository"
        private const val SUBSTITUTION_BASE_URL = "https://www.c-g-j.de"
        private const val SUBSTITUTION_PAGE_URL = "$SUBSTITUTION_BASE_URL/aktuelles-und-termine/vertretungsplan/"
    }
    
    data class VertretungsplanData(
        val pdfUrl: String?,
        val imageUrl: String?,
        val contentHash: String,
        val lastUpdated: Long
    )
    
    suspend fun fetchCurrentVertretungsplan(): VertretungsplanData? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(SUBSTITUTION_PAGE_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "CGJ-App/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                
                // Extract PDF URL
                val pdfRegex = """<a href="(/asset/[^"]+/vertretungsplan[^"]*\.pdf)">""".toRegex()
                val pdfMatch = pdfRegex.find(content)
                val pdfUrl = pdfMatch?.groupValues?.get(1)?.let { pdfPath ->
                    "$SUBSTITUTION_BASE_URL$pdfPath"
                }
                
                // Extract image URL
                val imageRegex = """<img[^>]+src="(/asset/[^"]+/vplan\.png)"[^>]*>""".toRegex()
                val imageMatch = imageRegex.find(content)
                val imageUrl = imageMatch?.groupValues?.get(1)?.let { imagePath ->
                    "$SUBSTITUTION_BASE_URL$imagePath"
                }
                
                // Generate content hash
                val contentHash = generateHash(content)
                
                VertretungsplanData(
                    pdfUrl = pdfUrl,
                    imageUrl = imageUrl,
                    contentHash = contentHash,
                    lastUpdated = System.currentTimeMillis()
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Vertretungsplan: ${e.message}")
                null
            }
        }
    }
    
    suspend fun checkForChanges(lastKnownHash: String?): Boolean {
        if (lastKnownHash == null) {
            return true // First time check
        }
        
        val currentData = fetchCurrentVertretungsplan()
        return currentData?.contentHash != lastKnownHash
    }
    
    suspend fun downloadVertretungsplan(url: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "CGJ-App/1.0")
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                
                val inputStream = BufferedInputStream(connection.inputStream)
                inputStream.readBytes()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading Vertretungsplan: ${e.message}")
                null
            }
        }
    }
    
    private fun generateHash(content: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(content.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating hash: ${e.message}")
            UUID.randomUUID().toString() // Fallback
        }
    }
    
    fun isSchoolHours(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Monday = 2, Sunday = 1
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
            return false
        }
        
        // School hours: 7:00 - 17:00
        return hour in 7..17
    }
    
    fun isQuietHours(startHour: Int, endHour: Int): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return if (startHour > endHour) {
            // Quiet hours span midnight (e.g., 22:00 - 07:00)
            currentHour >= startHour || currentHour < endHour
        } else {
            // Quiet hours within same day (e.g., 22:00 - 06:00)
            currentHour >= startHour && currentHour < endHour
        }
    }
}
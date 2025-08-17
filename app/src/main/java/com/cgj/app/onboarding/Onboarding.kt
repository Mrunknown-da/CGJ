package com.cgj.app.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.cgj.app.dataStore
import com.cgj.app.R
import com.cgj.app.background.PlanUpdateWorker
import com.cgj.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.collectAsState

private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    var page by remember { mutableStateOf(0) }
    val onboardingDone by context.dataStore.data
        .map { it[ONBOARDING_DONE] ?: false }
        .collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (onboardingDone) onFinished()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted || NotificationHelper.areNotificationsEnabled(context)) {
                NotificationHelper.createNotificationChannel(context)
                scope.launch { context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = true } }
                PlanUpdateWorker.schedule(context)
            }
            page = 2
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> {
                Text(text = context.getString(R.string.onboarding_title_1), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = context.getString(R.string.onboarding_body_1), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { page = 1 }) {
                    Text(text = context.getString(R.string.action_continue))
                }
            }
            1 -> {
                Text(text = context.getString(R.string.onboarding_title_2), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = context.getString(R.string.onboarding_body_2), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationHelper.createNotificationChannel(context)
                        scope.launch { context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = true } }
                        PlanUpdateWorker.schedule(context)
                        page = 2
                    }
                }) {
                    Text(text = context.getString(R.string.action_allow))
                }
            }
            2 -> {
                Text(text = context.getString(R.string.onboarding_title_3), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = context.getString(R.string.onboarding_body_3), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    scope.launch { context.dataStore.edit { it[ONBOARDING_DONE] = true } }
                    onFinished()
                }) {
                    Text(text = context.getString(R.string.action_finish))
                }
            }
        }
    }
}
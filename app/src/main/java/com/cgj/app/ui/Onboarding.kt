package com.cgj.app.ui

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cgj.app.R
import com.cgj.app.createNotificationChannel
import com.cgj.app.dataStore
import com.cgj.app.SubstitutionCheckWorker
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import com.cgj.app.ONBOARDING_DONE

@Composable
fun OnboardingScreen(onFinished: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionRequested by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            scope.launch {
                context.dataStore.edit { it[ONBOARDING_DONE] = true }
                createNotificationChannel(context)
                SubstitutionCheckWorker.scheduleSubstitutionChecks(context)
                onFinished?.invoke()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = context.getString(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(text = context.getString(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Spacer(Modifier.height(24.dp))

        ElevatedCard(colors = CardDefaults.elevatedCardColors()) {
            Column(Modifier.padding(16.dp)) {
                Text(text = "• " + context.getString(R.string.onboarding_benefit_vplan), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text(text = "• " + context.getString(R.string.onboarding_benefit_others), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(24.dp))
        if (Build.VERSION.SDK_INT >= 33 && !permissionRequested) {
            Text(text = context.getString(R.string.onboarding_permission_rationale), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
        }

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= 33 && !permissionRequested) {
                permissionRequested = true
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scope.launch {
                    context.dataStore.edit { it[ONBOARDING_DONE] = true }
                    createNotificationChannel(context)
                    SubstitutionCheckWorker.scheduleSubstitutionChecks(context)
                    onFinished?.invoke()
                }
            }
        }) {
            Text(text = if (Build.VERSION.SDK_INT >= 33) context.getString(R.string.onboarding_cta_enable) else context.getString(R.string.onboarding_cta_continue))
        }
    }
}
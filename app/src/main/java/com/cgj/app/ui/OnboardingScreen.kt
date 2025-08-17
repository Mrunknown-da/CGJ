package com.cgj.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cgj.app.NotificationPreferences
import com.cgj.app.NotificationService
import com.cgj.app.NotificationWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { NotificationPreferences(context) }
    
    // Permission states
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var backgroundPermissionGranted by remember { mutableStateOf(false) }
    
    // Settings states
    var notificationsEnabled by remember { mutableStateOf(false) }
    var checkInterval by remember { mutableStateOf(15) }
    var quietHoursEnabled by remember { mutableStateOf(true) }
    var quietHoursStart by remember { mutableStateOf(22) }
    var quietHoursEnd by remember { mutableStateOf(7) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var autoDownload by remember { mutableStateOf(false) }
    
    // Permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
        if (isGranted) {
            scope.launch {
                preferences.setNotificationsEnabled(true)
                notificationsEnabled = true
            }
        }
    }
    
    // Check current permissions
    LaunchedEffect(Unit) {
        notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        backgroundPermissionGranted = true // Background permissions are handled by system
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (currentStep + 1) / 5f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        // Step content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { if (targetState > initialState) it else -it }
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { if (targetState > initialState) -it else it }
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { step ->
            when (step) {
                0 -> WelcomeStep()
                1 -> FeaturesStep()
                2 -> PermissionsStep(
                    notificationPermissionGranted = notificationPermissionGranted,
                    backgroundPermissionGranted = backgroundPermissionGranted,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationPermissionGranted = true
                        }
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
                3 -> SettingsStep(
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsEnabledChange = { enabled ->
                        notificationsEnabled = enabled
                        scope.launch {
                            preferences.setNotificationsEnabled(enabled)
                        }
                    },
                    checkInterval = checkInterval,
                    onCheckIntervalChange = { interval ->
                        checkInterval = interval
                        scope.launch {
                            preferences.setCheckInterval(interval)
                        }
                    },
                    quietHoursEnabled = quietHoursEnabled,
                    onQuietHoursEnabledChange = { enabled ->
                        quietHoursEnabled = enabled
                        scope.launch {
                            preferences.setQuietHours(quietHoursStart, quietHoursEnd, enabled)
                        }
                    },
                    quietHoursStart = quietHoursStart,
                    onQuietHoursStartChange = { start ->
                        quietHoursStart = start
                        scope.launch {
                            preferences.setQuietHours(start, quietHoursEnd, quietHoursEnabled)
                        }
                    },
                    quietHoursEnd = quietHoursEnd,
                    onQuietHoursEndChange = { end ->
                        quietHoursEnd = end
                        scope.launch {
                            preferences.setQuietHours(quietHoursStart, end, quietHoursEnabled)
                        }
                    },
                    soundEnabled = soundEnabled,
                    onSoundEnabledChange = { enabled ->
                        soundEnabled = enabled
                        scope.launch {
                            preferences.setSoundEnabled(enabled)
                        }
                    },
                    vibrationEnabled = vibrationEnabled,
                    onVibrationEnabledChange = { enabled ->
                        vibrationEnabled = enabled
                        scope.launch {
                            preferences.setVibrationEnabled(enabled)
                        }
                    },
                    autoDownload = autoDownload,
                    onAutoDownloadChange = { enabled ->
                        autoDownload = enabled
                        scope.launch {
                            preferences.setAutoDownload(enabled)
                        }
                    }
                )
                4 -> CompletionStep()
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zurück")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = {
                    if (currentStep < 4) {
                        currentStep++
                    } else {
                        // Complete onboarding
                        scope.launch {
                            preferences.setOnboardingCompleted(true)
                            
                            // Start notification service if enabled
                            if (notificationsEnabled) {
                                val serviceIntent = Intent(context, NotificationService::class.java).apply {
                                    action = "START_FOREGROUND"
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                
                                // Schedule notification worker
                                NotificationWorker.schedulePeriodicWork(context, checkInterval)
                            }
                            
                            onOnboardingComplete()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = when (currentStep) {
                    2 -> notificationPermissionGranted
                    3 -> notificationsEnabled
                    else -> true
                }
            ) {
                Text(if (currentStep < 4) "Weiter" else "Fertig")
                if (currentStep < 4) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Weiter")
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Willkommen bei der CGJ App!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Lass uns deine App einrichten, damit du keine wichtigen Updates verpasst.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturesStep() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "App-Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        val features = listOf(
            Triple(
                Icons.Default.Notifications,
                "Vertretungsplan-Benachrichtigungen",
                "Erhalte sofortige Benachrichtigungen, wenn sich der Vertretungsplan ändert."
            ),
            Triple(
                Icons.Default.Schedule,
                "Hintergrund-Updates",
                "Die App überprüft automatisch auf neue Inhalte, auch wenn sie geschlossen ist."
            ),
            Triple(
                Icons.Default.Download,
                "Offline-Zugriff",
                "Lade wichtige Dokumente herunter und greife offline darauf zu."
            ),
            Triple(
                Icons.Default.Settings,
                "Anpassbare Einstellungen",
                "Konfiguriere Benachrichtigungen nach deinen Wünschen."
            )
        )
        
        features.forEach { (icon, title, description) ->
            FeatureCard(
                icon = icon,
                title = title,
                description = description,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    notificationPermissionGranted: Boolean,
    backgroundPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Berechtigungen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "Die App benötigt einige Berechtigungen, um optimal zu funktionieren:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Notification permission
        PermissionCard(
            title = "Benachrichtigungen",
            description = "Erlaubt der App, dich über neue Vertretungspläne zu informieren.",
            granted = notificationPermissionGranted,
            onRequest = onRequestNotificationPermission,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Background permission
        PermissionCard(
            title = "Hintergrund-Aktivität",
            description = "Ermöglicht der App, im Hintergrund nach Updates zu suchen.",
            granted = backgroundPermissionGranted,
            onRequest = { /* Background permissions are automatic */ },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (!notificationPermissionGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "Benachrichtigungen sind erforderlich, um Updates zu erhalten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (granted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Berechtigung erteilt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp)
                )
            } else {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("Erteilen")
                }
            }
        }
    }
}

@Composable
private fun SettingsStep(
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    checkInterval: Int,
    onCheckIntervalChange: (Int) -> Unit,
    quietHoursEnabled: Boolean,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    quietHoursStart: Int,
    onQuietHoursStartChange: (Int) -> Unit,
    quietHoursEnd: Int,
    onQuietHoursEndChange: (Int) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationEnabledChange: (Boolean) -> Unit,
    autoDownload: Boolean,
    onAutoDownloadChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Benachrichtigungseinstellungen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Enable notifications
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Benachrichtigungen aktivieren",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Erhalte Updates über neue Vertretungspläne",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsEnabledChange
                )
            }
        }
        
        if (notificationsEnabled) {
            // Check interval
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Überprüfungsintervall",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Wie oft soll die App nach Updates suchen?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    val intervals = listOf(5, 15, 30, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        intervals.forEach { interval ->
                            FilterChip(
                                selected = checkInterval == interval,
                                onClick = { onCheckIntervalChange(interval) },
                                label = { Text("${interval} Min") }
                            )
                        }
                    }
                }
            }
            
            // Quiet hours
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Ruhezeiten",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keine Benachrichtigungen in bestimmten Stunden",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = onQuietHoursEnabledChange
                        )
                    }
                    
                    if (quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column {
                                Text(
                                    text = "Von",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                TimePickerButton(
                                    hour = quietHoursStart,
                                    onHourChange = onQuietHoursStartChange
                                )
                            }
                            Column {
                                Text(
                                    text = "Bis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                TimePickerButton(
                                    hour = quietHoursEnd,
                                    onHourChange = onQuietHoursEndChange
                                )
                            }
                        }
                    }
                }
            }
            
            // Sound and vibration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Benachrichtigungseffekte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Ton abspielen",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = onSoundEnabledChange
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Vibration",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = onVibrationEnabledChange
                        )
                    }
                }
            }
            
            // Auto-download
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Automatischer Download",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Lade neue Vertretungspläne automatisch herunter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDownload,
                        onCheckedChange = onAutoDownloadChange
                    )
                }
            }
        }
    }
}

@Composable
private fun TimePickerButton(
    hour: Int,
    onHourChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Button(
        onClick = { showDialog = true },
        modifier = Modifier.width(80.dp)
    ) {
        Text("${hour.toString().padStart(2, '0')}:00")
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Zeit auswählen") },
            text = {
                Column {
                    (0..23).forEach { h ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onHourChange(h)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = hour == h,
                                onClick = { 
                                    onHourChange(h)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${h.toString().padStart(2, '0')}:00")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun CompletionStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Einrichtung abgeschlossen!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Deine App ist jetzt bereit! Du wirst über alle wichtigen Änderungen im Vertretungsplan informiert.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "Du kannst diese Einstellungen jederzeit in den App-Einstellungen ändern.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
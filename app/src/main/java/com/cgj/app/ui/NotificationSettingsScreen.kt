package com.cgj.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cgj.app.NotificationPreferences
import com.cgj.app.NotificationService
import com.cgj.app.NotificationWorker
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import android.content.Intent
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { NotificationPreferences(context) }
    
    // Settings states
    var notificationsEnabled by remember { mutableStateOf(false) }
    var checkInterval by remember { mutableStateOf(15) }
    var quietHoursEnabled by remember { mutableStateOf(true) }
    var quietHoursStart by remember { mutableStateOf(22) }
    var quietHoursEnd by remember { mutableStateOf(7) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var autoDownload by remember { mutableStateOf(false) }
    var lastCheckTime by remember { mutableStateOf(0L) }
    
    // Load current settings
    LaunchedEffect(Unit) {
        notificationsEnabled = preferences.notificationsEnabled.first()
        checkInterval = preferences.checkInterval.first()
        quietHoursEnabled = preferences.quietHoursEnabled.first()
        quietHoursStart = preferences.quietHoursStart.first()
        quietHoursEnd = preferences.quietHoursEnd.first()
        soundEnabled = preferences.soundEnabled.first()
        vibrationEnabled = preferences.vibrationEnabled.first()
        autoDownload = preferences.autoDownload.first()
        lastCheckTime = preferences.lastCheckTime.first()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Benachrichtigungen") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                preferences.resetToDefaults()
                                // Reload settings
                                notificationsEnabled = preferences.notificationsEnabled.first()
                                checkInterval = preferences.checkInterval.first()
                                quietHoursEnabled = preferences.quietHoursEnabled.first()
                                quietHoursStart = preferences.quietHoursStart.first()
                                quietHoursEnd = preferences.quietHoursEnd.first()
                                soundEnabled = preferences.soundEnabled.first()
                                vibrationEnabled = preferences.vibrationEnabled.first()
                                autoDownload = preferences.autoDownload.first()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notificationsEnabled) 
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
                    Icon(
                        imageVector = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (notificationsEnabled) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column {
                        Text(
                            text = if (notificationsEnabled) "Benachrichtigungen aktiv" else "Benachrichtigungen deaktiviert",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (notificationsEnabled) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (notificationsEnabled && lastCheckTime > 0) {
                            Text(
                                text = "Letzte Überprüfung: ${formatTimestamp(lastCheckTime)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (notificationsEnabled) 
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
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
                        onCheckedChange = { enabled ->
                            notificationsEnabled = enabled
                            scope.launch {
                                preferences.setNotificationsEnabled(enabled)
                                
                                if (enabled) {
                                    // Start service and schedule worker
                                    val serviceIntent = Intent(context, NotificationService::class.java).apply {
                                        action = "START_FOREGROUND"
                                    }
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                    
                                    NotificationWorker.schedulePeriodicWork(context, checkInterval)
                                } else {
                                    // Stop service and cancel worker
                                    val serviceIntent = Intent(context, NotificationService::class.java).apply {
                                        action = "STOP_FOREGROUND"
                                    }
                                    context.stopService(serviceIntent)
                                    
                                    NotificationWorker.cancelPeriodicWork(context)
                                }
                            }
                        }
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
                                    onClick = { 
                                        checkInterval = interval
                                        scope.launch {
                                            preferences.setCheckInterval(interval)
                                            // Reschedule worker with new interval
                                            NotificationWorker.cancelPeriodicWork(context)
                                            NotificationWorker.schedulePeriodicWork(context, interval)
                                        }
                                    },
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
                                onCheckedChange = { enabled ->
                                    quietHoursEnabled = enabled
                                    scope.launch {
                                        preferences.setQuietHours(quietHoursStart, quietHoursEnd, enabled)
                                    }
                                }
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
                                        onHourChange = { start ->
                                            quietHoursStart = start
                                            scope.launch {
                                                preferences.setQuietHours(start, quietHoursEnd, quietHoursEnabled)
                                            }
                                        }
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
                                        onHourChange = { end ->
                                            quietHoursEnd = end
                                            scope.launch {
                                                preferences.setQuietHours(quietHoursStart, end, quietHoursEnabled)
                                            }
                                        }
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
                                onCheckedChange = { enabled ->
                                    soundEnabled = enabled
                                    scope.launch {
                                        preferences.setSoundEnabled(enabled)
                                    }
                                }
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
                                onCheckedChange = { enabled ->
                                    vibrationEnabled = enabled
                                    scope.launch {
                                        preferences.setVibrationEnabled(enabled)
                                    }
                                }
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
                            onCheckedChange = { enabled ->
                                autoDownload = enabled
                                scope.launch {
                                    preferences.setAutoDownload(enabled)
                                }
                            }
                        )
                    }
                }
                
                // Manual check button
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
                            text = "Manuelle Überprüfung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Überprüfe jetzt manuell auf neue Vertretungspläne",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    NotificationWorker.scheduleOneTimeWork(context, 0)
                                    // Update last check time
                                    preferences.setLastCheckTime(System.currentTimeMillis())
                                    lastCheckTime = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Jetzt überprüfen")
                        }
                    }
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

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Nie"
    
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Gerade eben" // Less than 1 minute
        diff < 3600000 -> "${diff / 60000} Minuten her" // Less than 1 hour
        diff < 86400000 -> "${diff / 3600000} Stunden her" // Less than 1 day
        else -> "${diff / 86400000} Tage her" // More than 1 day
    }
}
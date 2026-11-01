package com.thestudytimer

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF071426)
private val Panel = Color(0xFF13243A)
private val Cyan = Color(0xFF72D5FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        setContent { StudyTimerApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTimerApp() {
    val context = LocalContext.current
    val store = remember { AlarmStore(context) }
    val scheduler = remember { AlarmScheduler(context) }
    var alarms by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var timerHours by remember { mutableStateOf("0") }
    var timerMinutes by remember { mutableStateOf("5") }
    var timerSeconds by remember { mutableStateOf("0") }
    var timerRunning by remember { mutableStateOf(false) }
    var remaining by remember { mutableLongStateOf(0L) }
    var stopwatchRunning by remember { mutableStateOf(false) }
    var stopwatchMillis by remember { mutableLongStateOf(0L) }
    var stopwatchStart by remember { mutableLongStateOf(0L) }

    LaunchedEffect(stopwatchRunning) {
        while (stopwatchRunning) {
            stopwatchMillis = System.currentTimeMillis() - stopwatchStart
            delay(50)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Color(0xFF9B8CFF),
        background = Navy, surface = Panel
    )) {
        Scaffold(
            containerColor = Navy,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("THE STUDY TIMER", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Text("Your time. Your focus.", fontSize = 12.sp, color = Cyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    listOf("Alarms", "Timer", "Stopwatch", "Settings").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = {
                                Icon(
                                    when (index) {
                                        0 -> Icons.Default.Alarm
                                        1 -> Icons.Default.HourglassTop
                                        2 -> Icons.Default.Timer
                                        else -> Icons.Default.Settings
                                    }, contentDescription = label
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == 0) FloatingActionButton(onClick = { showAdd = true }, containerColor = Cyan, contentColor = Navy) {
                    Icon(Icons.Default.Add, contentDescription = "Add alarm")
                }
            }
        ) { padding ->
            when (tab) {
                0 -> AlarmScreen(alarms, padding,
                    onToggle = { a, enabled ->
                        val updated = alarms.map { if (it.id == a.id) it.copy(enabled = enabled) else it }
                        alarms = updated; store.save(updated)
                        if (enabled) scheduler.schedule(a.copy(enabled = true)) else scheduler.cancel(a.id)
                    },
                    onDelete = { a ->
                        scheduler.cancel(a.id)
                        alarms = alarms.filterNot { it.id == a.id }; store.save(alarms)
                    },
                    onTone = { a, uri ->
                        val updated = alarms.map { if (it.id == a.id) it.copy(toneUri = uri.toString()) else it }
                        alarms = updated; store.save(updated)
                        scheduler.schedule(updated.first { it.id == a.id })
                    }
                )
                1 -> TimerScreen(padding, timerHours, { timerHours = it }, timerMinutes, { timerMinutes = it },
                    timerSeconds, { timerSeconds = it }, timerRunning, remaining,
                    onStart = {
                        val total = (timerHours.toLongOrNull() ?: 0) * 3600 +
                            (timerMinutes.toLongOrNull() ?: 0) * 60 + (timerSeconds.toLongOrNull() ?: 0)
                        if (total > 0) {
                            remaining = total * 1000
                            scheduler.scheduleTimer(1, "Timer finished", System.currentTimeMillis() + remaining)
                            timerRunning = true
                        }
                    },
                    onReset = { timerRunning = false; remaining = 0L }
                )
                2 -> StopwatchScreen(padding, stopwatchMillis, stopwatchRunning,
                    onStart = { if (!stopwatchRunning) { stopwatchStart = System.currentTimeMillis() - stopwatchMillis; stopwatchRunning = true } },
                    onPause = { stopwatchRunning = false },
                    onReset = { stopwatchRunning = false; stopwatchMillis = 0L }
                )
                else -> SettingsScreen(padding)
            }
        }
        if (showAdd) {
            AddAlarmDialog(
                onDismiss = { showAdd = false },
                onSave = { hour, minute, label, days ->
                    val alarm = AlarmModel(store.nextId(), hour, minute, label.ifBlank { "Study alarm" }, true, days)
                    val updated = alarms + alarm
                    alarms = updated; store.save(updated); scheduler.schedule(alarm); showAdd = false
                }
            )
        }
    }
}

@Composable
private fun AlarmScreen(
    alarms: List<AlarmModel>, padding: PaddingValues,
    onToggle: (AlarmModel, Boolean) -> Unit,
    onDelete: (AlarmModel) -> Unit,
    onTone: (AlarmModel, Uri) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            alarms.firstOrNull()?.let { onTone(it, uri) }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Your alarms", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Stay on track, one session at a time.", color = Cyan)
        Spacer(Modifier.height(16.dp))
        if (alarms.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Alarm, null, tint = Cyan, modifier = Modifier.size(42.dp))
                    Text("No alarms yet", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Tap + to create your first alarm.", color = Color.LightGray)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(alarms, key = { it.id }) { alarm ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                                    fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Light)
                                Text(alarm.label, color = Cyan)
                                Text(if (alarm.repeatDays.isEmpty()) "One time" else "Repeats: " +
                                    alarm.repeatDays.sorted().joinToString { dayName(it) }, color = Color.LightGray, fontSize = 12.sp)
                            }
                            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm, it) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                                Icon(Icons.Default.MusicNote, null); Spacer(Modifier.width(4.dp)); Text("Choose tone")
                            }
                            OutlinedButton(onClick = { onDelete(alarm) }) {
                                Icon(Icons.Default.Delete, null); Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, Set<Int>) -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("Study session") }
    var days by remember { mutableStateOf(emptySet<Int>()) }
    val dayValues = listOf(
        java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY,
        java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY,
        java.util.Calendar.SATURDAY
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                }, modifier = Modifier.fillMaxWidth()) { Text(String.format("%02d:%02d", hour, minute), fontSize = 26.sp) }
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Alarm label") })
                Text("Repeat days")
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    dayValues.forEach { day ->
                        val selected = day in days
                        FilterChip(selected = selected, onClick = {
                            days = if (selected) days - day else days + day
                        }, label = { Text(dayName(day).take(2)) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(hour, minute, label, days) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TimerScreen(
    padding: PaddingValues, hours: String, onHours: (String) -> Unit,
    minutes: String, onMinutes: (String) -> Unit, seconds: String, onSeconds: (String) -> Unit,
    running: Boolean, remaining: Long, onStart: () -> Unit, onReset: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("COUNTDOWN TIMER", color = Cyan, fontWeight = FontWeight.Bold)
        Text(if (running) "Timer scheduled" else "Set your focus time", color = Color.White, fontSize = 25.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField("HH", hours, onHours); TimeField("MM", minutes, onMinutes); TimeField("SS", seconds, onSeconds)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, enabled = !running) { Text("Start timer") }
        OutlinedButton(onClick = onReset) { Text("Reset") }
        Text("The timer alarm is scheduled by Android and can fire while the app is closed.", color = Color.LightGray)
    }
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(value = value, onValueChange = { onChange(it.filter(Char::isDigit).take(2)) },
            label = { Text(label) }, modifier = Modifier.width(88.dp), singleLine = true)
    }
}

@Composable
private fun StopwatchScreen(padding: PaddingValues, millis: Long, running: Boolean,
                            onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit) {
    val total = millis.coerceAtLeast(0)
    val formatted = String.format(Locale.getDefault(), "%02d:%02d.%02d",
        total / 60000, (total / 1000) % 60, (total % 1000) / 10)
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("STOPWATCH", color = Cyan, fontWeight = FontWeight.Bold)
        Text(formatted, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Light)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = if (running) onPause else onStart) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = onReset) { Text("Reset") }
        }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp)) {
                Text("THE STUDY TIMER", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Galaxy-inspired focus clock", color = Color.White)
                Text("Version 1.0.0 • Offline-first", color = Color.LightGray)
            }
        }
        Text("For reliable exact alarms, allow Alarms & reminders in Android settings when prompted or from App info.", color = Color.LightGray)
        Text("Tone picker currently assigns the selected audio to the first alarm in the list. Per-alarm tone editing is planned for the next build.", color = Color.LightGray)
    }
}

private fun dayName(day: Int): String = when (day) {
    java.util.Calendar.SUNDAY -> "Sunday"
    java.util.Calendar.MONDAY -> "Monday"
    java.util.Calendar.TUESDAY -> "Tuesday"
    java.util.Calendar.WEDNESDAY -> "Wednesday"
    java.util.Calendar.THURSDAY -> "Thursday"
    java.util.Calendar.FRIDAY -> "Friday"
    else -> "Saturday"
}

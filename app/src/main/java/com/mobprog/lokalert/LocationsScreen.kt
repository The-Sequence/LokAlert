package com.mobprog.lokalert

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onViewOnMap: (LatLng) -> Unit, // Callback to show a location on the map
    recentSearches: List<String>,
    viewModel: MapsViewModel
) {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    val alarms by repository.getAllAlarms().collectAsState(initial = emptyList())
    
    var alarmToEdit by remember { mutableStateOf<LocationAlarm?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Recent Searches
        Text(
            "Recent Searches",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        if (recentSearches.isEmpty()) {
            Text("No recent searches.", color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
            ) {
                items(recentSearches) { search ->
                    val isFavorite = alarms.any { it.name == search }
                    SearchHistoryItem(
                        location = search,
                        isFavorite = isFavorite,
                        onToggleFavorite = { 
                            val alarm = alarms.find { it.name == search }
                            if (alarm != null) {
                                viewModel.toggleFavorite(alarm)
                            }
                         }
                    )
                    HorizontalDivider()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // My Locations
        Text(
            "My Locations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        if (alarms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't added any locations yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alarms, key = { it.id }) { alarm ->
                    LocationItem(
                        alarm = alarm,
                        onClick = {
                            alarmToEdit = alarm
                            showEditSheet = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    
    if (showEditSheet && alarmToEdit != null) {
        EditLocationSheet(
            alarm = alarmToEdit!!,
            onDismiss = { showEditSheet = false },
            onSave = { updatedAlarm ->
                CoroutineScope(Dispatchers.IO).launch {
                    repository.updateAlarm(updatedAlarm)
                }
            },
            onDelete = {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.deleteAlarm(it)
                }
            },
            onViewOnMap = onViewOnMap
        )
    }
}

@Composable
fun LocationItem(alarm: LocationAlarm, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(alarm.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Radius: ${alarm.radius.toInt()}m", 
                fontSize = 14.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Default.Edit, contentDescription = "Edit Location")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLocationSheet(
    alarm: LocationAlarm,
    onDismiss: () -> Unit,
    onSave: (LocationAlarm) -> Unit,
    onDelete: (LocationAlarm) -> Unit,
    onViewOnMap: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var alarmName by remember { mutableStateOf(alarm.name) }
    var radius by remember { mutableStateOf(alarm.radius) }
    var selectedDays by remember { mutableStateOf(alarm.activeDays) }
    var alarmSoundUri by remember { mutableStateOf(alarm.soundUri) }
    var isGradualVolume by remember { mutableStateOf(alarm.isGradualVolume) }
    var showSoundSelectionDialog by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { alarmSoundUri = it.toString() }
    }
    val customFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                alarmSoundUri = it.toString()
            } catch (e: Exception) { alarmSoundUri = it.toString() }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = alarmName,
                onValueChange = { newValue ->
                    // Limit alarm name to 50 characters
                    if (newValue.length <= 50) {
                        alarmName = newValue
                    }
                },
                label = { Text("Alarm Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("${alarmName.length}/50 characters")
                },
                isError = alarmName.isBlank(),
            )
            
            Column {
                Text("Alert Radius: ${radius.toInt()} meters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Slider(value = radius, onValueChange = { radius = it }, valueRange = 100f..1000f)
            }
            
            Column {
                Text("Active Days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                LocationsDaySelector(selectedDays) { selectedDays = it }
            }

            LocationsPickerRow(label = "Alarm Sound", text = getMapRingtoneTitle(context, alarmSoundUri)) {
                showSoundSelectionDialog = true
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { isGradualVolume = !isGradualVolume }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gradual Volume", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Alarm starts soft and gets louder", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Switch(checked = isGradualVolume, onCheckedChange = { isGradualVolume = it })
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = {
                        onDelete(alarm)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
                
                Button(onClick = {
                    onViewOnMap(LatLng(alarm.latitude, alarm.longitude))
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) { Text("View on Map") }

                Button(
                    onClick = {
                        if (alarmName.isBlank()) {
                            // Show error - name is required
                            return@Button
                        }
                        val updatedAlarm = alarm.copy(
                            name = alarmName.trim(),
                            radius = radius,
                            soundUri = alarmSoundUri,
                            activeDays = selectedDays,
                            isGradualVolume = isGradualVolume
                        )
                        onSave(updatedAlarm)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    enabled = alarmName.isNotBlank()
                ) { Text("Done") }
            }
        }
    }

    if (showSoundSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showSoundSelectionDialog = false },
            title = { Text("Choose Sound Source") },
            text = {
                Column {
                    TextButton(onClick = {
                        showSoundSelectionDialog = false
                        ringtonePickerLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmSoundUri))
                            }
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text("System Ringtones") }
                    
                    TextButton(onClick = {
                        showSoundSelectionDialog = false
                        customFilePickerLauncher.launch("audio/*")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Custom File (.mp3)") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSoundSelectionDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LocationsPickerRow(label: String, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationsDaySelector(selectedDays: Set<Int>, onSelectionChange: (Set<Int>) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        val calendarDays = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)

        daysOfWeek.forEachIndexed { index, dayLabel ->
            val day = calendarDays[index]
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectionChange(if (isSelected) selectedDays - day else selectedDays + day) },
                label = { Text(dayLabel) },
                leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null
            )
        }
    }
}

@Composable
fun SearchHistoryItem(location: String, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Implement navigation/search on click */ }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Location",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(location, fontSize = 16.sp)
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (isFavorite) Color.Red else Color.Gray
            )
        }
    }
}




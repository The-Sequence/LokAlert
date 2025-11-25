package com.mobprog.lokalert

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(favoriteLocations: List<String>) {
    val context = LocalContext.current
    var alarms by remember {
        mutableStateOf(
            listOf(
                // UPDATED: Initial alarms use new location/radius structure
                Alarm(locationName = "Home", radius = 100f, sound = "Chimes", isEnabled = true),
                Alarm(locationName = "Work", radius = 500f, sound = "Radar", isEnabled = false),
            )
        )
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    var alarmIndexToEdit by remember { mutableIntStateOf(-1) }
    var isNewAlarm by remember { mutableStateOf(false) } // Track if we are creating a new alarm

    val openAlertDialog = remember { mutableStateOf(false) } // TESTING VARIABLE

    if (showEditDialog && alarmToEdit != null) {
        EditAlarmDialog(
            alarm = alarmToEdit!!,
            favoriteLocations = favoriteLocations,
            onDismiss = { showEditDialog = false },
            onSave = { updatedAlarm ->
                val newList = alarms.toMutableList()
                if (isNewAlarm) {
                    newList.add(updatedAlarm)
                } else {
                    newList[alarmIndexToEdit] = updatedAlarm
                }
                alarms = newList
                showEditDialog = false
                isNewAlarm = false

                if (updatedAlarm.isEnabled) {
                    updateAlarmSchedule(context, updatedAlarm)
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("Location Alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = { openAlertDialog.value = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Test")
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (favoriteLocations.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please add a favorite location first!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isNewAlarm = true
                            // Create a default alarm using the first favorite location
                            alarmToEdit = Alarm(
                                locationName = favoriteLocations.first(),
                                radius = 100f,
                                sound = "Chimes",
                                isEnabled = true
                            )
                            alarmIndexToEdit = -1 // Indicates a new alarm
                            showEditDialog = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Alarm",
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp
                    ),

                    text = { Text(text = "New Alarm", fontSize = 16.sp) }

                )
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
        ) {
            LazyColumn {
                itemsIndexed(alarms) { index, alarm ->
                    AlarmItem(
                        alarm = alarm,
                        onToggle = { isEnabled ->
                            val newList = alarms.toMutableList()
                            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                            newList[index] = updatedAlarm
                            alarms = newList

                            if (isEnabled) {
                                updateAlarmSchedule(context, updatedAlarm)
                            } else {
                                // Cancel the alarm if it was disabled
                                Toast.makeText(
                                    context,
                                    "Alarm for ${updatedAlarm.locationName} cancelled!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDelete = {
                            val newList = alarms.toMutableList()
                            newList.removeAt(index)
                            alarms = newList
                        },
                        onClick = {
                            isNewAlarm = false
                            alarmToEdit = alarm
                            alarmIndexToEdit = index
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Display the test alert dialog when openAlertDialog is true
    if (openAlertDialog.value) {
        TestAlertDialog(onDismissRequest = { openAlertDialog.value = false })
    }
    }



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(
    alarm: Alarm,
    favoriteLocations: List<String>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    var selectedLocation by remember(alarm, favoriteLocations) {
        val initialLocation = alarm.locationName
        if (favoriteLocations.contains(initialLocation)) {
            mutableStateOf(initialLocation)
        } else if (favoriteLocations.isNotEmpty()) {
            mutableStateOf(favoriteLocations.first())
        } else {
            mutableStateOf(initialLocation)
        }
    }

    var radius by remember(alarm) { mutableFloatStateOf(alarm.radius) }
    var sound by remember(alarm) { mutableStateOf(alarm.sound) }

    // Determine if the Save button should be enabled
    val saveEnabled = favoriteLocations.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Location Alarm") },
        text = {
            Column {
                Text(
                    text = "Select Location:",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (favoriteLocations.isEmpty()) {
                    Text(
                        "No favorite locations added. Add some in Favorites tab!",
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) { // Limit height of the list
                        items(favoriteLocations) { location ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = saveEnabled) {
                                        selectedLocation = location
                                    }
                                    .background(
                                        if (selectedLocation == location) Color.LightGray else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = "Location",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    location,
                                    fontSize = 16.sp,
                                    fontWeight = if (selectedLocation == location) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Radius Slider
                Text("Radius: ${radius.toInt()}m", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 100f..2000f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Sound Input (keeping existing logic)
                OutlinedTextField(
                    value = sound,
                    onValueChange = { sound = it },
                    label = { Text("Sound") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        alarm.copy(
                            locationName = selectedLocation,
                            radius = radius,
                            sound = sound
                        )
                    )
                },
                enabled = saveEnabled && selectedLocation.isNotEmpty() && selectedLocation != "No favorite locations added"
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun updateAlarmSchedule(context: Context, alarm: Alarm) {
    // This function would typically implement Geofencing APIs (e.e., Google Location Services)
    // to trigger the alarm when the user enters/exits the specified radius around the location.
    // For this task, we treat it as a UI/placeholder implementation.

    if (alarm.isEnabled) {
        // Placeholder for Geofence registration
        Toast.makeText(
            context,
            "Location Alarm set for ${alarm.locationName} (Radius: ${alarm.radius.toInt()}m)!",
            Toast.LENGTH_LONG
        ).show()
    }
}


@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // UPDATED: Display Location Name and Radius
            Text(
                text = alarm.locationName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Radius: ${alarm.radius.toInt()}m | Sound: ${alarm.sound}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Alarm")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestAlertDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Filled.Security, contentDescription = "Security Icon") },
        title = {
            Text(text = "Safety Check")
        },
        text = {
            Text(
                text = "Sleeping in public makes you vulnerable to theft. " +
                        "Before you nap, loop your bag straps around your arm or leg " +
                        "and keep your phone in a zipped pocket, not in your hand."
            )
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest
            ) {
                Text("I'm secured")
            }
        }
    )
}
package com.mobprog.lokalert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

/**
 * A compact search section for quick alarm dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchSection(
    onSearch: (String) -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Places Client Setup
    val placesClient = remember {
        try {
            if (!Places.isInitialized()) {
                val packageInfo = context.packageManager.getApplicationInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_META_DATA
                )
                val apiKey = packageInfo.metaData?.getString("com.google.android.geo.API_KEY")
                if (apiKey != null) {
                    Places.initialize(context, apiKey)
                }
            }
            if (Places.isInitialized()) Places.createClient(context) else null
        } catch (e: Exception) {
            null
        }
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    // Autocomplete Logic
    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && placesClient != null) {
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(token)
                    .setQuery(searchText)
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        suggestions = response.autocompletePredictions.map { 
                            it.getFullText(null).toString() 
                        }
                        expanded = suggestions.isNotEmpty()
                    }
                    .addOnFailureListener { 
                        suggestions = emptyList()
                        expanded = false
                    }
            } catch (e: Exception) {
                suggestions = emptyList()
                expanded = false
            }
        } else {
            suggestions = emptyList()
            expanded = false
        }
    }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search location...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchText = ""
                        suggestions = emptyList()
                        expanded = false
                    }) {
                        Icon(Icons.Default.Close, "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchText.isNotEmpty()) {
                        onSearch(searchText)
                        keyboardController?.hide()
                        expanded = false
                    }
                }
            )
        )

        // Suggestions dropdown
        if (expanded && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSuggestionClick(suggestion)
                                    searchText = ""
                                    expanded = false
                                    keyboardController?.hide()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (suggestion != suggestions.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Form for editing location alarm details in bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationForm(
    viewModel: MapsViewModel,
    onPickRingtone: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onSliderActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            if (viewModel.editingAlarmId != null) "Edit Alarm" else "New Location Alarm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Alarm Name
        OutlinedTextField(
            value = viewModel.alarmName,
            onValueChange = { viewModel.alarmName = it },
            label = { Text("Alarm Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Active Days
        Column {
            Text("Active Days", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            DaySelector(
                selectedDays = viewModel.selectedDays,
                onSelectionChange = { viewModel.selectedDays = it }
            )
        }

        // Radius Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alert Radius", fontWeight = FontWeight.SemiBold)
                Text("${viewModel.radius.toInt()}m", color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = viewModel.radius,
                onValueChange = { 
                    viewModel.radius = it
                    onSliderActiveChange(true)
                },
                onValueChangeFinished = { onSliderActiveChange(false) },
                valueRange = 100f..1000f,
                steps = 17
            )
        }

        // Sound Picker
        MapsPickerRow(
            label = "Alarm Sound",
            text = getMapRingtoneTitle(context, viewModel.alarmSoundUri),
            onClick = onPickRingtone
        )

        // Gradual Volume Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gentle wake-up", fontWeight = FontWeight.Medium)
                Text(
                    "Starts quiet, gets louder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = viewModel.isGradualVolume,
                onCheckedChange = { viewModel.isGradualVolume = it }
            )
        }

        HorizontalDivider()

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = viewModel.alarmName.isNotBlank() && viewModel.markerPosition != null
            ) {
                Text("Save")
            }
        }
    }
}

/**
 * Simple day selector for the form
 */
@Composable
fun DaySelector(
    selectedDays: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val calendarDays = listOf(
        java.util.Calendar.SUNDAY,
        java.util.Calendar.MONDAY,
        java.util.Calendar.TUESDAY,
        java.util.Calendar.WEDNESDAY,
        java.util.Calendar.THURSDAY,
        java.util.Calendar.FRIDAY,
        java.util.Calendar.SATURDAY
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysOfWeek.forEachIndexed { index, dayLabel ->
            val day = calendarDays[index]
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) selectedDays - day else selectedDays + day
                    onSelectionChange(newDays)
                },
                label = { Text(dayLabel, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

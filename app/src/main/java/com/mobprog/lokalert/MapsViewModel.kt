package com.mobprog.lokalert

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MapsViewModel(application: Application) : AndroidViewModel(application) {

    internal val dao = LokAlertDatabase.getDatabase(application).alarmDao()

    val savedLocations = dao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Trashed alarms
    val trashedAlarms = dao.getAllTrashedAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var locationToFocus by mutableStateOf<LatLng?>(null)
    var editingAlarmId by mutableStateOf<Int?>(null) // Track which alarm is being edited
    var pendingSearchQuery by mutableStateOf<String?>(null) // Search query to execute when Maps screen opens
    
    // Selection state for multi-delete
    var isSelectionMode by mutableStateOf(false)
    var selectedAlarmIds by mutableStateOf<Set<Int>>(emptySet())

    // Form State
    var markerPosition by mutableStateOf<LatLng?>(null)
    var radius by mutableFloatStateOf(100f)
    var alarmName by mutableStateOf("")
    var alarmSoundUri by mutableStateOf("") // Initialize in UI or init block
    var selectedDays by mutableStateOf(emptySet<Int>())
    var isGradualVolume by mutableStateOf(false)
    var showBottomSheet by mutableStateOf(false)

    // For new alarm creation using saved location
    var selectedSavedLocationForNewAlarm by mutableStateOf<LocationAlarm?>(null)


    fun resetForm() {
        markerPosition = null
        alarmName = ""
        radius = 100f
        selectedDays = emptySet()
        isGradualVolume = false
        showBottomSheet = false
        editingAlarmId = null
        selectedSavedLocationForNewAlarm = null
        pendingSearchQuery = null
    }
    
    /**
     * Set up a saved location to be used as a starting point for creating a NEW alarm.
     * This pre-fills the marker position and focuses the camera on the location.
     */
    fun useLocationForNewAlarm(alarm: LocationAlarm) {
        // Store the selected location for feedback display
        selectedSavedLocationForNewAlarm = alarm
        
        // Set up the marker at the saved location's coordinates
        val position = LatLng(alarm.latitude, alarm.longitude)
        markerPosition = position
        locationToFocus = position
        
        // Pre-fill form with the saved location's data as defaults
        // User can modify these before saving the new alarm
        alarmName = "" // Leave blank so user can give a new name
        radius = alarm.radius
        selectedDays = alarm.activeDays
        alarmSoundUri = alarm.soundUri
        isGradualVolume = alarm.isGradualVolume
        
        // Important: Do NOT set editingAlarmId - this creates a NEW alarm
        editingAlarmId = null
    }
    
    /**
     * Clear the selected saved location feedback state
     */
    fun clearSelectedSavedLocation() {
        selectedSavedLocationForNewAlarm = null
    }
    
    // Selection mode functions
    fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        if (!isSelectionMode) {
            selectedAlarmIds = emptySet()
        }
    }
    
    fun toggleAlarmSelection(alarmId: Int) {
        selectedAlarmIds = if (selectedAlarmIds.contains(alarmId)) {
            selectedAlarmIds - alarmId
        } else {
            selectedAlarmIds + alarmId
        }
    }
    
    fun selectAllAlarms() {
        selectedAlarmIds = savedLocations.value.map { it.id }.toSet()
    }
    
    fun clearSelection() {
        selectedAlarmIds = emptySet()
        isSelectionMode = false
    }
    
    fun deleteSelectedAlarms() {
        viewModelScope.launch {
            val alarmsToDelete = savedLocations.value.filter { selectedAlarmIds.contains(it.id) }
            alarmsToDelete.forEach { alarm ->
                moveToTrash(alarm)
            }
            clearSelection()
        }
    }

    fun deleteLocation(alarm: LocationAlarm) {
        viewModelScope.launch {
            moveToTrash(alarm)
        }
    }
    
    // Move alarm to trash instead of permanent delete
    private suspend fun moveToTrash(alarm: LocationAlarm) {
        val trashedAlarm = TrashedAlarm(
            originalId = alarm.id,
            name = alarm.name,
            latitude = alarm.latitude,
            longitude = alarm.longitude,
            radius = alarm.radius,
            soundUri = alarm.soundUri,
            isEnabled = alarm.isEnabled,
            isGradualVolume = alarm.isGradualVolume,
            activeDays = alarm.activeDays,
            isFavorite = alarm.isFavorite,
            deletedAt = System.currentTimeMillis()
        )
        dao.insertTrashedAlarm(trashedAlarm)
        dao.deleteAlarm(alarm)
    }
    
    // Trash operations
    fun restoreFromTrash(trashedAlarm: TrashedAlarm) {
        viewModelScope.launch {
            val restoredAlarm = LocationAlarm(
                name = trashedAlarm.name,
                latitude = trashedAlarm.latitude,
                longitude = trashedAlarm.longitude,
                radius = trashedAlarm.radius,
                soundUri = trashedAlarm.soundUri,
                isEnabled = trashedAlarm.isEnabled,
                isGradualVolume = trashedAlarm.isGradualVolume,
                activeDays = trashedAlarm.activeDays,
                isFavorite = trashedAlarm.isFavorite
            )
            dao.insertAlarm(restoredAlarm)
            dao.deleteTrashedAlarm(trashedAlarm)
        }
    }
    
    fun permanentlyDelete(trashedAlarm: TrashedAlarm) {
        viewModelScope.launch {
            dao.deleteTrashedAlarm(trashedAlarm)
        }
    }
    
    fun emptyTrash() {
        viewModelScope.launch {
            dao.clearTrash()
        }
    }
    
    fun restoreAllFromTrash() {
        viewModelScope.launch {
            trashedAlarms.value.forEach { trashedAlarm ->
                val restoredAlarm = LocationAlarm(
                    name = trashedAlarm.name,
                    latitude = trashedAlarm.latitude,
                    longitude = trashedAlarm.longitude,
                    radius = trashedAlarm.radius,
                    soundUri = trashedAlarm.soundUri,
                    isEnabled = trashedAlarm.isEnabled,
                    isGradualVolume = trashedAlarm.isGradualVolume,
                    activeDays = trashedAlarm.activeDays,
                    isFavorite = trashedAlarm.isFavorite
                )
                dao.insertAlarm(restoredAlarm)
                dao.deleteTrashedAlarm(trashedAlarm)
            }
        }
    }

    // 3. Logic to Toggle Favorite
    fun toggleFavorite(alarm: LocationAlarm) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isFavorite = !alarm.isFavorite)
            dao.updateAlarm(updatedAlarm)
        }
    }
    
    // Toggle alarm enabled/disabled
    fun toggleAlarmEnabled(alarm: LocationAlarm) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            dao.updateAlarm(updatedAlarm)
        }
    }

    // 4. Logic to Update Name (Edit)
    fun updateLocationName(alarm: LocationAlarm, newName: String) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(name = newName)
            dao.updateAlarm(updatedAlarm)
        }
    }

    fun saveAlarm(onSuccess: () -> Unit) {
        val pos = markerPosition ?: return

        val newAlarm = LocationAlarm(
            name = alarmName.ifBlank { "Location Alarm" },
            latitude = pos.latitude,
            longitude = pos.longitude,
            radius = radius,
            soundUri = alarmSoundUri,
            isGradualVolume = isGradualVolume,
            activeDays = selectedDays
        )

        viewModelScope.launch {
            dao.insertAlarm(newAlarm)
            resetForm()
            onSuccess()
        }
    }

    fun updateLocationDetails(alarm: LocationAlarm, newName: String, newRadius: Float) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(
                name = newName.ifBlank { "Location Alarm" },
                radius = newRadius
            )
            dao.updateAlarm(updatedAlarm)
        }
    }
    
    fun updateAlarmAllDetails(
        alarm: LocationAlarm, 
        newName: String, 
        newActiveDays: Set<Int>,
        newSoundUri: String,
        newIsGradualVolume: Boolean
    ) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(
                name = newName.ifBlank { "Location Alarm" },
                activeDays = newActiveDays,
                soundUri = newSoundUri,
                isGradualVolume = newIsGradualVolume
            )
            dao.updateAlarm(updatedAlarm)
        }
    }
    
    fun updateRadiusOnly(alarmId: Int, newRadius: Float) {
        viewModelScope.launch {
            val alarm = savedLocations.value.find { it.id == alarmId }
            if (alarm != null) {
                val updatedAlarm = alarm.copy(radius = newRadius)
                dao.updateAlarm(updatedAlarm)
            }
        }
    }
    
    // Update all alarm fields when editing from Locations screen
    fun updateAlarmComplete(
        alarmId: Int,
        newName: String,
        newActiveDays: Set<Int>,
        newSoundUri: String,
        newIsGradualVolume: Boolean,
        newRadius: Float
    ) {
        viewModelScope.launch {
            val alarm = savedLocations.value.find { it.id == alarmId }
            if (alarm != null) {
                val updatedAlarm = alarm.copy(
                    name = newName.ifBlank { "Location Alarm" },
                    activeDays = newActiveDays,
                    soundUri = newSoundUri,
                    isGradualVolume = newIsGradualVolume,
                    radius = newRadius
                )
                dao.updateAlarm(updatedAlarm)
            }
        }
    }
    
    /**
     * Clear all existing alarms and insert demo profile locations
     */
    fun loadDemoProfile(demoLocations: List<DemoLocation>) {
        viewModelScope.launch {
            // Delete all existing alarms
            savedLocations.value.forEach { alarm ->
                dao.deleteAlarm(alarm)
            }
            
            // Insert demo locations
            demoLocations.forEach { demo ->
                val alarm = LocationAlarm(
                    name = demo.name,
                    latitude = demo.latitude,
                    longitude = demo.longitude,
                    radius = demo.radius,
                    soundUri = "",
                    isEnabled = demo.isEnabled,
                    isGradualVolume = false,
                    activeDays = demo.activeDays,
                    isFavorite = demo.isFavorite
                )
                dao.insertAlarm(alarm)
            }
        }
    }
    
    /**
     * Clear all saved locations
     */
    fun clearAllLocations() {
        viewModelScope.launch {
            dao.deleteAllAlarms()
        }
    }
}
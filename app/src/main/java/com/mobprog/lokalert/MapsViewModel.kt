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

    private val dao = LokAlertDatabase.getDatabase(application).alarmDao()

    val savedLocations = dao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var locationToFocus by mutableStateOf<LatLng?>(null)
    var editingAlarmId by mutableStateOf<Int?>(null) // Track which alarm is being edited

    // Form State
    var markerPosition by mutableStateOf<LatLng?>(null)
    var radius by mutableFloatStateOf(100f)
    var alarmName by mutableStateOf("")
    var alarmSoundUri by mutableStateOf("") // Initialize in UI or init block
    var selectedDays by mutableStateOf(emptySet<Int>())
    var isGradualVolume by mutableStateOf(false)
    var showBottomSheet by mutableStateOf(false)


    fun resetForm() {
        markerPosition = null
        alarmName = ""
        radius = 100f
        selectedDays = emptySet()
        isGradualVolume = false
        showBottomSheet = false
        editingAlarmId = null
    }

    fun deleteLocation(alarm: LocationAlarm) {
        viewModelScope.launch {
            dao.deleteAlarm(alarm)
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
}
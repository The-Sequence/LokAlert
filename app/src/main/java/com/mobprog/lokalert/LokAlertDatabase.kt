package com.mobprog.lokalert

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

// 1. The Entity (The Data Model)
@Entity(tableName = "location_alarms")
data class LocationAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val soundUri: String,
    val isEnabled: Boolean = true,
    val isGradualVolume: Boolean,
    val activeDays: Set<Int>,
    val isFavorite: Boolean = false
)

// Trashed Alarm Entity (for trash bin feature)
@Entity(tableName = "trashed_alarms")
data class TrashedAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalId: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val soundUri: String,
    val isEnabled: Boolean = true,
    val isGradualVolume: Boolean,
    val activeDays: Set<Int>,
    val isFavorite: Boolean = false,
    val deletedAt: Long = System.currentTimeMillis()
)

// 2. The DAO (How we access data)
@Dao
interface AlarmDao {
    @Query("SELECT * FROM location_alarms")
    fun getAllAlarms(): Flow<List<LocationAlarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: LocationAlarm)

    @Delete
    suspend fun deleteAlarm(alarm: LocationAlarm)

    @Update
    suspend fun updateAlarm(alarm: LocationAlarm)
    
    // Trash operations
    @Query("SELECT * FROM trashed_alarms ORDER BY deletedAt DESC")
    fun getAllTrashedAlarms(): Flow<List<TrashedAlarm>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashedAlarm(alarm: TrashedAlarm)
    
    @Delete
    suspend fun deleteTrashedAlarm(alarm: TrashedAlarm)
    
    @Query("DELETE FROM trashed_alarms")
    suspend fun clearTrash()
    
    @Query("DELETE FROM trashed_alarms WHERE deletedAt < :timestamp")
    suspend fun deleteOldTrashedAlarms(timestamp: Long)
    
    @Query("DELETE FROM location_alarms")
    suspend fun deleteAllAlarms()
}

// 3. Type Converters (To store the Set<Int> days as a String)
class Converters {
    @TypeConverter
    fun fromString(value: String): Set<Int> {
        val listType = object : TypeToken<Set<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromSet(set: Set<Int>): String {
        return Gson().toJson(set)
    }
}

// 4. The Database Instance
@Database(entities = [LocationAlarm::class, TrashedAlarm::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LokAlertDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var INSTANCE: LokAlertDatabase? = null

        fun getDatabase(context: Context): LokAlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LokAlertDatabase::class.java,
                    "lokalert_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
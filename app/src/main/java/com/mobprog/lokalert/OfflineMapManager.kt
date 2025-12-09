package com.mobprog.lokalert

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Manager class for handling offline map data for the Philippines
 * Uses OpenStreetMap tiles via osmdroid
 */
class OfflineMapManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineMapManager"
        
        // Philippines bounding box (approximate)
        // North: 21.12°N, South: 4.58°N, West: 116.93°E, East: 126.60°E
        const val PH_NORTH = 21.12
        const val PH_SOUTH = 4.58
        const val PH_WEST = 116.93
        const val PH_EAST = 126.60
        
        // Zoom levels to download (lower = larger area, higher = more detail)
        // Level 6-12 provides good coverage for navigation without excessive data
        const val MIN_ZOOM = 6
        const val MAX_ZOOM = 14
        
        // Tile server URL (OpenStreetMap)
        const val TILE_SERVER = "https://tile.openstreetmap.org"
    }
    
    private val appPreferences = AppPreferences(context)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    // Download progress state
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress
    
    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus
    
    private val _estimatedSize = MutableStateFlow("~500-800 MB")
    val estimatedSize: StateFlow<String> = _estimatedSize
    
    // Flag to cancel download
    @Volatile
    private var isCancelled = false
    
    init {
        // Initialize osmdroid configuration safely
        try {
            Configuration.getInstance().apply {
                userAgentValue = context.packageName
                osmdroidBasePath = File(context.filesDir, "osmdroid")
                osmdroidTileCache = File(osmdroidBasePath, "tiles")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize osmdroid configuration", e)
        }
    }
    
    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.let { cm ->
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            false
        }
    }
    
    /**
     * Check if device has enough storage space (in MB)
     */
    fun hasEnoughStorage(requiredMB: Long = 1000): Boolean {
        return try {
            val path = context.filesDir
            val stat = android.os.StatFs(path.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val availableMB = availableBytes / (1024 * 1024)
            availableMB >= requiredMB
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage space", e)
            true // Assume there's enough space if check fails
        }
    }
    
    /**
     * Get available storage in MB
     */
    fun getAvailableStorageMB(): Long {
        return try {
            val path = context.filesDir
            val stat = android.os.StatFs(path.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes / (1024 * 1024)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available storage", e)
            0L
        }
    }
    
    /**
     * Get the bounding box for the Philippines
     */
    fun getPhilippinesBoundingBox(): BoundingBox {
        return BoundingBox(PH_NORTH, PH_EAST, PH_SOUTH, PH_WEST)
    }
    
    /**
     * Calculate the estimated number of tiles and storage size
     */
    fun calculateEstimatedTiles(): TileEstimate {
        var totalTiles = 0L
        
        for (zoom in MIN_ZOOM..MAX_ZOOM) {
            val tilesAtZoom = calculateTilesForZoom(zoom)
            totalTiles += tilesAtZoom
        }
        
        // Average tile size is about 15-20 KB
        val estimatedSizeBytes = totalTiles * 18 * 1024 // 18 KB average
        val estimatedSizeMB = estimatedSizeBytes / (1024 * 1024)
        
        return TileEstimate(totalTiles, estimatedSizeMB)
    }
    
    private fun calculateTilesForZoom(zoom: Int): Long {
        val n = 1 shl zoom // 2^zoom
        
        val xMin = ((PH_WEST + 180.0) / 360.0 * n).toInt()
        val xMax = ((PH_EAST + 180.0) / 360.0 * n).toInt()
        
        val latRadNorth = Math.toRadians(PH_NORTH)
        val latRadSouth = Math.toRadians(PH_SOUTH)
        
        val yMin = ((1.0 - Math.log(Math.tan(latRadNorth) + 1.0 / Math.cos(latRadNorth)) / Math.PI) / 2.0 * n).toInt()
        val yMax = ((1.0 - Math.log(Math.tan(latRadSouth) + 1.0 / Math.cos(latRadSouth)) / Math.PI) / 2.0 * n).toInt()
        
        val tilesX = (xMax - xMin + 1).toLong()
        val tilesY = (yMax - yMin + 1).toLong()
        
        return tilesX * tilesY
    }
    
    /**
     * Download all map tiles for the Philippines
     */
    suspend fun downloadPhilippinesMaps(
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        isCancelled = false
        _downloadStatus.value = DownloadStatus.Downloading
        
        // Pre-flight checks
        if (!isNetworkAvailable()) {
            _downloadStatus.value = DownloadStatus.Error("No internet connection. Please connect to the internet and try again.")
            return@withContext false
        }
        
        if (!hasEnoughStorage(1000)) {
            val available = getAvailableStorageMB()
            _downloadStatus.value = DownloadStatus.Error("Not enough storage space. Need ~800MB but only ${available}MB available.")
            return@withContext false
        }
        
        val tileDir = File(context.filesDir, "osmdroid/tiles/Mapnik")
        try {
            if (!tileDir.exists() && !tileDir.mkdirs()) {
                _downloadStatus.value = DownloadStatus.Error("Failed to create storage directory")
                return@withContext false
            }
        } catch (e: SecurityException) {
            _downloadStatus.value = DownloadStatus.Error("Storage permission denied: ${e.message}")
            return@withContext false
        }
        
        try {
            var totalDownloaded = 0L
            var totalFailed = 0L
            var consecutiveFailures = 0
            val maxConsecutiveFailures = 50 // Stop if too many consecutive failures
            
            val estimate = calculateEstimatedTiles()
            val totalTiles = estimate.totalTiles
            var processedTiles = 0L
            
            for (zoom in MIN_ZOOM..MAX_ZOOM) {
                if (isCancelled) {
                    _downloadStatus.value = DownloadStatus.Cancelled
                    return@withContext false
                }
                
                val n = 1 shl zoom
                
                val xMin = ((PH_WEST + 180.0) / 360.0 * n).toInt()
                val xMax = ((PH_EAST + 180.0) / 360.0 * n).toInt()
                
                val latRadNorth = Math.toRadians(PH_NORTH)
                val latRadSouth = Math.toRadians(PH_SOUTH)
                
                val yMin = ((1.0 - Math.log(Math.tan(latRadNorth) + 1.0 / Math.cos(latRadNorth)) / Math.PI) / 2.0 * n).toInt()
                val yMax = ((1.0 - Math.log(Math.tan(latRadSouth) + 1.0 / Math.cos(latRadSouth)) / Math.PI) / 2.0 * n).toInt()
                
                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        if (isCancelled) {
                            _downloadStatus.value = DownloadStatus.Cancelled
                            return@withContext false
                        }
                        
                        // Check for network periodically
                        if (processedTiles % 100 == 0L && !isNetworkAvailable()) {
                            _downloadStatus.value = DownloadStatus.Error("Lost internet connection during download. Progress saved - you can resume later.")
                            // Save partial progress
                            val partialSize = calculateCacheSize()
                            if (partialSize > 0) {
                                appPreferences.setOfflineMapsSizeMB(partialSize.toInt())
                            }
                            return@withContext false
                        }
                        
                        val tileFile = File(tileDir, "$zoom/$x/$y.png")
                        
                        // Skip if already downloaded
                        if (tileFile.exists() && tileFile.length() > 0) {
                            totalDownloaded++
                            processedTiles++
                            consecutiveFailures = 0
                            continue
                        }
                        
                        // Download tile with retry
                        val success = downloadTileWithRetry(zoom, x, y, tileFile, maxRetries = 3)
                        if (success) {
                            totalDownloaded++
                            consecutiveFailures = 0
                        } else {
                            totalFailed++
                            consecutiveFailures++
                            
                            // Stop if too many consecutive failures (likely network issue)
                            if (consecutiveFailures >= maxConsecutiveFailures) {
                                _downloadStatus.value = DownloadStatus.Error(
                                    "Too many download failures. Please check your internet connection and try again."
                                )
                                return@withContext false
                            }
                        }
                        
                        processedTiles++
                        
                        // Update progress every 10 tiles
                        if (processedTiles % 10 == 0L) {
                            val progress = processedTiles.toFloat() / totalTiles.toFloat()
                            _downloadProgress.value = progress
                            val statusText = "Zoom $zoom: ${(progress * 100).toInt()}% ($processedTiles / $totalTiles tiles)"
                            withContext(Dispatchers.Main) {
                                onProgress(progress, statusText)
                            }
                        }
                    }
                }
            }
            
            // Calculate actual size
            val actualSizeMB = calculateCacheSize()
            
            // Save metadata
            appPreferences.setOfflineMapsDownloaded(true)
            appPreferences.setOfflineMapsSizeMB(actualSizeMB.toInt())
            appPreferences.setOfflineMapsLastUpdated(
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
            )
            
            _downloadProgress.value = 1f
            _downloadStatus.value = DownloadStatus.Completed(totalDownloaded, totalFailed, actualSizeMB)
            
            Log.d(TAG, "Download complete: $totalDownloaded tiles, $totalFailed failed, ${actualSizeMB}MB")
            true
            
        } catch (e: IOException) {
            Log.e(TAG, "IO error during download", e)
            _downloadStatus.value = DownloadStatus.Error("Download failed: ${e.message ?: "IO error"}")
            false
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during download", e)
            _downloadStatus.value = DownloadStatus.Error("Out of memory. Please free up some RAM and try again.")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _downloadStatus.value = DownloadStatus.Error(e.message ?: "Unknown error occurred")
            false
        }
    }
    
    private fun downloadTileWithRetry(zoom: Int, x: Int, y: Int, outputFile: File, maxRetries: Int): Boolean {
        var attempts = 0
        while (attempts < maxRetries) {
            if (downloadTile(zoom, x, y, outputFile)) {
                return true
            }
            attempts++
            // Brief delay before retry
            Thread.sleep(100L * attempts)
        }
        return false
    }
    
    private fun downloadTile(zoom: Int, x: Int, y: Int, outputFile: File): Boolean {
        return try {
            val url = "$TILE_SERVER/$zoom/$x/$y.png"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LokAlert Android App")
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.let { body ->
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            fos.write(body.bytes())
                        }
                        return true
                    }
                }
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download tile $zoom/$x/$y: ${e.message}")
            false
        }
    }
    
    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        isCancelled = true
    }
    
    /**
     * Calculate the total size of cached map tiles
     */
    fun calculateCacheSize(): Long {
        val tileDir = File(context.filesDir, "osmdroid/tiles")
        return if (tileDir.exists()) {
            getFolderSize(tileDir) / (1024 * 1024) // Convert to MB
        } else {
            0L
        }
    }
    
    private fun getFolderSize(folder: File): Long {
        var size = 0L
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }
    
    /**
     * Delete all cached map tiles
     */
    suspend fun clearMapCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val tileDir = File(context.filesDir, "osmdroid/tiles")
            if (tileDir.exists()) {
                tileDir.deleteRecursively()
            }
            
            appPreferences.setOfflineMapsDownloaded(false)
            appPreferences.setOfflineMapsSizeMB(0)
            appPreferences.setOfflineMapsLastUpdated("")
            appPreferences.setOfflineModeEnabled(false)
            
            _downloadStatus.value = DownloadStatus.Idle
            _downloadProgress.value = 0f
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            false
        }
    }
    
    /**
     * Check if offline maps are available
     */
    fun isOfflineMapsAvailable(): Boolean {
        val tileDir = File(context.filesDir, "osmdroid/tiles/Mapnik")
        return tileDir.exists() && tileDir.listFiles()?.isNotEmpty() == true
    }
    
    /**
     * Get center point of Philippines
     */
    fun getPhilippinesCenter(): GeoPoint {
        return GeoPoint(12.8797, 121.7740) // Center of Philippines
    }
    
    /**
     * Reset download status to idle (useful for dismissing error/completed states)
     */
    fun resetDownloadStatus() {
        _downloadStatus.value = DownloadStatus.Idle
        _downloadProgress.value = 0f
    }
}

/**
 * Tile estimate data class
 */
data class TileEstimate(
    val totalTiles: Long,
    val estimatedSizeMB: Long
)

/**
 * Download status sealed class
 */
sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Downloading : DownloadStatus()
    data object Cancelled : DownloadStatus()
    data class Completed(val downloaded: Long, val failed: Long, val sizeMB: Long) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

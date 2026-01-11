package com.mobprog.lokalert

/**
 * Demo profiles for developer options.
 * Each profile simulates a different user with their own theme preferences,
 * alarm settings, and saved locations.
 */

data class DemoProfile(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    // Theme settings
    val darkMode: Int, // 0=Light, 1=Dark, 3=Auto
    val appTheme: Int = 0, // 0=Standard, 1=Expressive, 2=Ocean, 3=Sunset, 4=Forest, 5=Retro, 6=Monochrome
    // Notification settings
    val cooldownEnabled: Boolean,
    val cooldownMinutes: Int,
    // Sound & Haptics
    val vibrationIntensity: Int, // 0=Low, 1=Medium, 2=Strong
    // Alarm Display settings
    val overlayDismissStyle: Int, // 0=Slider, 1=SwipeUp, 2=Button
    val overlayBackgroundStyle: Int, // 0=Gradient, 1=Solid, 2=Dark
    val overlayShowDistance: Boolean,
    val overlayShowEmoji: Boolean,
    val overlayPrimaryColor: String, // Hex color
    val overlayEmoji: String,
    // Saved locations
    val locations: List<DemoLocation>
)

data class DemoLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val isFavorite: Boolean,
    val isEnabled: Boolean,
    val activeDays: Set<Int> // 0=Sun, 1=Mon, ... 6=Sat
)

/**
 * Preset demo profiles simulating different user personas
 */
object DemoProfiles {
    
    // Sarah - A busy professional who uses the app for work commute
    val sarahTheCommuter = DemoProfile(
        id = "sarah_commuter",
        name = "Sarah the Commuter",
        description = "Busy professional with work & gym reminders",
        emoji = "👩‍💼",
        darkMode = 3, // Auto
        appTheme = 0, // Standard - professional look
        cooldownEnabled = true,
        cooldownMinutes = 15,
        vibrationIntensity = 2, // Strong
        overlayDismissStyle = 0, // Slider
        overlayBackgroundStyle = 0, // Gradient
        overlayShowDistance = true,
        overlayShowEmoji = true,
        overlayPrimaryColor = "4A90D9", // Professional blue
        overlayEmoji = "📍",
        locations = listOf(
            DemoLocation(
                name = "Office - Downtown",
                latitude = 40.7128,
                longitude = -74.0060,
                radius = 200f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(1, 2, 3, 4, 5) // Mon-Fri
            ),
            DemoLocation(
                name = "Home Sweet Home",
                latitude = 40.7282,
                longitude = -73.7949,
                radius = 150f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(0, 1, 2, 3, 4, 5, 6) // All days
            ),
            DemoLocation(
                name = "Fitness First Gym",
                latitude = 40.7589,
                longitude = -73.9851,
                radius = 100f,
                isFavorite = false,
                isEnabled = true,
                activeDays = setOf(1, 3, 5) // Mon, Wed, Fri
            ),
            DemoLocation(
                name = "Coffee Bean Café",
                latitude = 40.7484,
                longitude = -73.9857,
                radius = 50f,
                isFavorite = false,
                isEnabled = false,
                activeDays = setOf(1, 2, 3, 4, 5)
            )
        )
    )
    
    // Marcus - A student who uses dark mode and has campus locations
    val marcusTheStudent = DemoProfile(
        id = "marcus_student",
        name = "Marcus the Student",
        description = "College student with campus & study spots",
        emoji = "👨‍🎓",
        darkMode = 1, // Dark mode
        appTheme = 1, // Expressive - vibrant purple theme
        cooldownEnabled = false,
        cooldownMinutes = 5,
        vibrationIntensity = 1, // Medium
        overlayDismissStyle = 2, // Button
        overlayBackgroundStyle = 2, // Dark
        overlayShowDistance = true,
        overlayShowEmoji = true,
        overlayPrimaryColor = "9C27B0", // Purple
        overlayEmoji = "📚",
        locations = listOf(
            DemoLocation(
                name = "University Library",
                latitude = 34.0689,
                longitude = -118.4452,
                radius = 250f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(1, 2, 3, 4, 5) // Weekdays
            ),
            DemoLocation(
                name = "Dorm Room",
                latitude = 34.0711,
                longitude = -118.4434,
                radius = 100f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(0, 1, 2, 3, 4, 5, 6)
            ),
            DemoLocation(
                name = "Engineering Building",
                latitude = 34.0695,
                longitude = -118.4429,
                radius = 150f,
                isFavorite = false,
                isEnabled = true,
                activeDays = setOf(1, 3) // Mon, Wed
            ),
            DemoLocation(
                name = "Pizza Palace",
                latitude = 34.0623,
                longitude = -118.4472,
                radius = 75f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(5, 6) // Fri, Sat
            )
        )
    )
    
    // Elena - A traveler who uses light mode and minimal UI
    val elenaTheTraveler = DemoProfile(
        id = "elena_traveler",
        name = "Elena the Traveler",
        description = "Frequent traveler with destination reminders",
        emoji = "✈️",
        darkMode = 0, // Light mode
        appTheme = 3, // Sunset - warm travel vibes
        cooldownEnabled = true,
        cooldownMinutes = 30,
        vibrationIntensity = 0, // Gentle
        overlayDismissStyle = 1, // Swipe Up
        overlayBackgroundStyle = 1, // Solid
        overlayShowDistance = false,
        overlayShowEmoji = true,
        overlayPrimaryColor = "FF6B6B", // Coral red
        overlayEmoji = "🌍",
        locations = listOf(
            DemoLocation(
                name = "Airport Terminal",
                latitude = 33.9425,
                longitude = -118.4081,
                radius = 500f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(0, 1, 2, 3, 4, 5, 6)
            ),
            DemoLocation(
                name = "Hotel Marriott",
                latitude = 34.0522,
                longitude = -118.2437,
                radius = 200f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(0, 1, 2, 3, 4, 5, 6)
            ),
            DemoLocation(
                name = "Convention Center",
                latitude = 34.0407,
                longitude = -118.2668,
                radius = 300f,
                isFavorite = false,
                isEnabled = true,
                activeDays = setOf(1, 2, 3, 4, 5)
            )
        )
    )
    
    // Jake - A fitness enthusiast with outdoor locations
    val jakeTheFitness = DemoProfile(
        id = "jake_fitness",
        name = "Jake the Athlete",
        description = "Fitness enthusiast with workout locations",
        emoji = "🏃‍♂️",
        darkMode = 1, // Dark
        appTheme = 4, // Forest - nature green theme
        cooldownEnabled = true,
        cooldownMinutes = 10,
        vibrationIntensity = 2, // Strong
        overlayDismissStyle = 0, // Slider
        overlayBackgroundStyle = 0, // Gradient
        overlayShowDistance = true,
        overlayShowEmoji = true,
        overlayPrimaryColor = "4CAF50", // Green
        overlayEmoji = "💪",
        locations = listOf(
            DemoLocation(
                name = "CrossFit Box",
                latitude = 37.7749,
                longitude = -122.4194,
                radius = 100f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(1, 2, 3, 4, 5, 6) // Mon-Sat
            ),
            DemoLocation(
                name = "Golden Gate Park Run",
                latitude = 37.7694,
                longitude = -122.4862,
                radius = 400f,
                isFavorite = true,
                isEnabled = true,
                activeDays = setOf(0, 3, 6) // Sun, Wed, Sat
            ),
            DemoLocation(
                name = "Climbing Gym",
                latitude = 37.7599,
                longitude = -122.4148,
                radius = 100f,
                isFavorite = false,
                isEnabled = true,
                activeDays = setOf(2, 4) // Tue, Thu
            ),
            DemoLocation(
                name = "Healthy Eats Cafe",
                latitude = 37.7850,
                longitude = -122.4100,
                radius = 75f,
                isFavorite = false,
                isEnabled = true,
                activeDays = setOf(0) // Sunday
            )
        )
    )
    
    // Minimal - Clean slate profile for testing
    val minimalProfile = DemoProfile(
        id = "minimal",
        name = "Fresh Start",
        description = "Clean slate with default settings",
        emoji = "✨",
        darkMode = 3, // Auto
        appTheme = 0, // Standard - clean default
        cooldownEnabled = false,
        cooldownMinutes = 5,
        vibrationIntensity = 2,
        overlayDismissStyle = 0,
        overlayBackgroundStyle = 0,
        overlayShowDistance = true,
        overlayShowEmoji = true,
        overlayPrimaryColor = "FF6B6B",
        overlayEmoji = "🚨",
        locations = emptyList()
    )
    
    val allProfiles = listOf(
        sarahTheCommuter,
        marcusTheStudent,
        elenaTheTraveler,
        jakeTheFitness,
        minimalProfile
    )
}

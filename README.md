# LokAlert

LokAlert is an intelligent location-based alarm application for Android that helps you stay on track with your destinations. Never miss your stop again!

## Features

- 📍 **Location-Based Alarms**: Set alarms that trigger when you reach a specific location
- 🗺️ **Interactive Maps**: Visualize your alarm locations on Google Maps
- 🔍 **Smart Search**: Search for locations using Google Places API with autocomplete
- 📅 **Day-Specific Alerts**: Configure alarms for specific days of the week
- 🔊 **Custom Sounds**: Choose from system ringtones or your own audio files
- 🌈 **Customizable Theme**: Personalize the app title color with rainbow effects
- 📱 **Modern UI**: Built with Jetpack Compose and Material 3 design

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Room Database
- **Maps**: Google Maps SDK & Places API
- **Permissions**: Location, Notifications, and Exact Alarms

## Setup

### Prerequisites

1. Android Studio (Arctic Fox or later)
2. Android SDK (API 24+)
3. Google Maps API Key

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/The-Sequence/LokAlert.git
   cd LokAlert
   ```

2. Add your Google Maps API Key in `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE" />
   ```

3. Build and run the project:
   ```bash
   ./gradlew assembleDebug
   ```

## Usage

### Setting Up a Location Alarm

1. **Navigate to Map Screen**: Open the app and you'll see the map view
2. **Search or Pin Location**: 
   - Use the search bar to find a location, or
   - Tap anywhere on the map to set a pin
3. **Configure Alarm**:
   - Set the alarm name (max 50 characters)
   - Adjust the alert radius (100-1000 meters)
   - Choose active days
   - Select an alarm sound
   - Enable gradual volume if desired
4. **Save**: Tap "Done" to save your location alarm

### Managing Alarms

- View all your saved locations in the "Locations" tab
- Edit or delete alarms by tapping on them
- Toggle alarms on/off as needed

## Permissions

The app requires the following permissions:

- **Location (Fine & Coarse)**: To detect when you reach your destination
- **Notifications**: To alert you when you reach your location
- **Exact Alarms**: To ensure timely and accurate alerts
- **Internet**: For Google Maps and Places API

## Project Structure

```
app/src/main/java/com/mobprog/lokalert/
├── MainActivity.kt              # Main entry point and navigation
├── MapsScreen.kt               # Map view and alarm creation
├── LocationsScreen.kt          # List of saved alarms
├── OnboardingScreen.kt         # First-time user experience
├── AlarmRepository.kt          # Data layer abstraction
├── LokAlertDatabase.kt         # Room database setup
├── AlarmService.kt             # Background alarm service
├── AlarmReceiver.kt            # Broadcast receiver for alarms
├── MapsViewModel.kt            # ViewModel for map state
├── SearchSection.kt            # Location search UI
└── UserPreferences.kt          # User settings storage
```

## Code Quality Improvements

Recent improvements include:

- ✅ Added missing `AlarmRepository` class for better data abstraction
- ✅ Fixed build configuration issues
- ✅ Improved error handling in location search with user feedback
- ✅ Added input validation (50 character limit for alarm names)
- ✅ Enhanced accessibility with proper content descriptions
- ✅ Improved ringtone management with fallback error handling
- ✅ Added Gson dependency for Room type converters

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is created as part of a mobile programming course project.

## Acknowledgments

- Google Maps Platform for mapping and location services
- Material Design 3 for UI components
- Android Jetpack for modern Android development

## Support

For issues, questions, or suggestions, please open an issue in the GitHub repository.

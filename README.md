# 📍 LokAlert (Checkpoint 1)

**Welcome to the initial release of LokAlert!** 🚀

This repository contains the **Week 1 / First Checkpoint** code for our Android application. In this phase, our primary focus was establishing the **User Interface (UI)** foundation, setting up navigation, and experimenting with Jetpack Compose animations.

While the core backend logic is still in development, this version demonstrates the visual structure and flow of the application.

---

## 🎨 What's Inside: UI & Experiments

For this first checkpoint, we wanted to ensure the app felt responsive and looked good. Here is what we laid out:

### 1. Navigation & Layout 🧭
We implemented a robust **Bottom Navigation Bar** using Material 3, allowing users to switch between four main screens:
*   **🔍 Search:** The main dashboard with a Map placeholder and search bar.
*   **❤️ Favorites:** A placeholder list for saved locations.
*   **⏰ Alarms:** A visual representation of the alarm list (UI only).
*   **⚙️ Settings:** Customization options for the app.

### 2. Visual Experiments (The Fun Stuff) 🌈
We spent time playing with **State and Animations** in Jetpack Compose to make the app feel alive.
*   **Rainbow Title Effect:** Go to *Settings* and toggle the switch! The "LokAlert" title in the top bar will cycle through colors smoothly.
*   **Custom Theme Color:** You can manually select the primary brand color (Red, Blue, Green, etc.) which updates the UI instantly.

### 3. Alarm UI Prototyping 📝
We designed the `LazyColumn` list for alarms.
*   Users can see what an alarm looks like.
*   There is an "Edit Dialog" that pops up when clicking an alarm item.
*   *Note:* In this version, alarms reset when navigating away from the screen (State Hoisting coming in V2!).

---

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Navigation:** State-based composable switching
*   **Animation:** `Animatable` & `Tween` specs

---

## 📸 Current Status

| Navigation | Alarm List (UI) | Rainbow Settings |
|:---:|:---:|:---:|
| 🖼️ | 🖼️ | 🖼️ |
*(Screenshots of the initial UI layout)*

---

## 🚧 Known Limitations (WIP)

Since this is the **First Checkpoint**, please note the following behavior:
*   **Data Persistence:** Alarms reset to default when switching tabs.
*   **Map:** The map is currently a static placeholder box.
*   **Ringtones:** The sound selection is currently just a text label, not a real file picker.

*These features are currently being built for Checkpoint 2!*

---

## 📥 Download Pre-built APKs

Don't want to build from source? You can download pre-built APKs directly from our [Releases page](https://github.com/The-Sequence/LokAlert/releases). 

We automatically build APKs for all branches:
- **main** - Latest stable version
- **milestone_1** - Checkpoint 1 features
- **milestone_2** - Checkpoint 2 with advanced features
- **NoAlarmNavigation** - Alternative navigation

Each release includes installation instructions and release notes detailing the features and recent changes.

---

## 🏃‍♀️ How to Run

1.  Clone the repository.
2.  Open in **Android Studio**.
3.  Sync Gradle.
4.  Run on an Emulator or Device.

---

**Stay tuned for the next update where we connect the wiring and make these alarms ring!** 

# TapTarget 🎯

**TapTarget** is a modern, high-performance, fast-paced reflex arcade game built for Android using **Jetpack Compose** and **Kotlin**. Test your reaction time, precision, and hand-eye coordination by tapping spawning targets across multiple dynamic difficulty modes!

---

## ✨ Key Features

- **Dynamic Arcade Gameplay**: Fast-paced target tapping with real-time countdown timers, streak multipliers, and precision tracking.
- **Multiple Difficulty Levels**: Choose from Easy, Normal, Hard, Expert, and custom modes with scaling speeds, target sizes, and timer mechanics.
- **Immersive Feedback**: 
  - **Sound Effects**: Integrated audio cues for taps, misses, combos, and game over events via `SoundManager`.
  - **Haptic Feedback**: Tactile vibration responses powered by `HapticManager`.
- **Statistics & Progress Tracking**: Persistent stats tracking high scores, accuracy percentage, total targets hit, best streaks, and games played via `GamePreferencesRepository`.
- **Modern Material 3 UI**: Built entirely with **Jetpack Compose**, featuring smooth animations, custom canvas rendering (`GameCanvas`), responsive layouts, and dynamic themes.
- **Robust Architecture**: Follows Android architectural best practices using `ViewModel`, `StateFlow`, and Kotlin Coroutines.

---

## 🛠️ Tech Stack & Architecture

- **UI Toolkit**: Jetpack Compose & Material 3 (`androidx.compose.material3`)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Concurrency**: Kotlin Coroutines & `StateFlow` for state management and game loop timers
- **Data Persistence**: SharedPreferences / Data Repository pattern for high scores and user settings
- **Testing**: JUnit 4 unit tests (`TargetMathTest`, `GameLogicTest`) and Android instrumented UI tests (`ExampleInstrumentedTest`)

---

## 📱 Project Structure

```text
com.example.taptarget/
│
├── audio/          # SoundManager & HapticManager for immersive feedback
├── data/           # Models (GameModels.kt) & Preferences Repository
├── game/           # Core game math, spawning algorithms, & scoring (TargetMath.kt)
├── ui/             # Jetpack Compose UI screens & components
│   ├── about/      # About & info screen
│   ├── game/       # Game canvas, rendering, & active gameplay screen
│   ├── home/       # Main menu & mode selection
│   ├── navigation/ # Navigation graph & screens definition
│   ├── result/     # Game over / summary & stats screen
│   ├── settings/   # Audio, haptic, and game preferences
│   ├── splash/     # Splash screen
│   └── theme/      # Material 3 colors, typography, & theme
└── viewmodel/      # TapTargetViewModel & SettingsViewModel
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala / Jellyfish or newer recommended)
- **JDK 11** or higher
- Android SDK (Compile SDK 37, Min SDK 26+)

### Cloning & Building
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/TapTarget.git
   cd TapTarget
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run unit tests:
   ```bash
   ./gradlew test
   ```
5. Build and run the app on an emulator or physical Android device (`app:assembleDebug`).

---

## 🧪 Testing

The project includes comprehensive unit tests covering game mathematics, timer calculations, target spawning bounds, and scoring logic:
- `TargetMathTest`: Tests target positioning, radius calculations, and timer scaling.
- `GameLogicTest`: Tests game state transitions, scoring rules, and streak management.

Run tests via Gradle:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is developed as a showcase of modern Android development with Jetpack Compose. Feel free to explore, learn, and contribute!

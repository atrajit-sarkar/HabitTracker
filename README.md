# Habit Tracker Android App

A beautifully designed habit tracker app built with Jetpack Compose and modern Android architecture.

## Features ✨

- **Beautiful UI**: Material 3 design with gradient cards and smooth animations
- **Local Storage**: Persistent habit data using Room database with Hilt dependency injection
- **Smart Notifications**: Scheduled reminders for uncompleted habits using AlarmManager
- **Easy Habit Management**: 
  - Add habits with custom names, descriptions, and reminder times
  - Toggle reminders on/off
  - Mark habits as completed for the day
  - Delete habits with confirmation
- **Permission Handling**: Intelligent notification permission requests on Android 13+

## Architecture 🏗️

- **MVVM Pattern**: Clean separation of concerns with ViewModels
- **Dependency Injection**: Hilt for managing dependencies
- **Database**: Room for local storage with type converters
- **UI**: Jetpack Compose with Material 3 theming
- **Background Work**: AlarmManager for precise habit reminders

## Technical Stack 🛠️

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room
- **DI**: Hilt
- **Architecture**: MVVM
- **Notifications**: Android Notification System with AlarmManager
- **Build System**: Gradle with Kotlin DSL

## Key Components

### Data Layer
- `Habit` entity with Room annotations
- `HabitDao` for database operations
- `HabitRepository` for data abstraction
- Type converters for LocalDate and Instant

### Domain Layer
- `HabitViewModel` for UI state management
- Repository pattern for data access

### UI Layer
- `HabitHomeRoute` - Main screen composable
- `AddHabitSheet` - Bottom sheet for adding new habits
- Custom UI components with Material 3 theming

### Notification System
- `HabitReminderScheduler` for scheduling reminders
- `HabitReminderReceiver` for handling alarm triggers
- `HabitReminderService` for creating notifications

## Recent Fixes 🔧

### UI Improvements
- **Fixed TimePicker State Management**: Removed conflicting state synchronization that was causing UI glitches
- **Improved Modal Sheet**: Simplified bottom sheet interaction and fixed dismiss handling
- **Added Proper Spacing**: Added bottom spacing to prevent content clipping in the add habit sheet
- **Fixed Save Functionality**: Streamlined the save habit flow to work correctly

### Technical Fixes
- **Updated Lifecycle Imports**: Fixed deprecated LocalLifecycleOwner usage
- **Corrected State Flow**: Simplified time picker state management to prevent infinite updates
- **Enhanced Sheet Stability**: Improved modal bottom sheet configuration

## Build Instructions 📱

1. Clone the repository
2. Open in Android Studio
3. Sync the project
4. Run `./gradlew assembleDebug` to build
5. Install on device or emulator

## Permissions

- `POST_NOTIFICATIONS`: For habit reminders (Android 13+)

## Minimum Requirements

- Android API 29 (Android 10)
- Target SDK 36

---

*Built with ❤️ using modern Android development practices*
# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Development Commands

### Build & Installation
```powershell
# Clean build
.\gradlew clean assembleDebug

# Install debug APK on device
.\gradlew installDebug

# Get signing report (required for Firebase setup)
.\gradlew signingReport

# Run on device after installation
adb shell am start -n it.atraj.habittracker/.MainActivity
```

### Testing
```powershell
# Run unit tests
.\gradlew test

# Run instrumented tests
.\gradlew connectedAndroidTest

# Run specific test file
.\gradlew test --tests "ClassNameTest"

# Generate test report
.\gradlew test --info
# Reports are in: app/build/reports/tests/testDebugUnitTest/index.html
```

### Code Quality & Linting
```powershell
# Analyze with Android lint
.\gradlew lint
# Lint reports are in: app/build/reports/lint-results.html

# Check for Kotlin code style issues (if ktlint is configured)
.\gradlew ktlintCheck

# Auto-format Kotlin code (if ktlint is configured)
.\gradlew ktlintFormat
```

## Architecture Overview

This is a modern Android application built with **Jetpack Compose**, **Firebase**, and **MVVM architecture** using **Hilt** for dependency injection.

### Core Architecture Components

- **MVVM Pattern**: ViewModels manage UI state, Repositories handle data operations
- **Dependency Injection**: Hilt provides singleton instances and manages dependencies
- **Cloud-First**: Firebase Firestore for real-time data sync with offline persistence
- **Modern UI**: Jetpack Compose with Material 3 theming
- **Background Work**: AlarmManager for precise habit reminders, WorkManager for reliability checks

### Package Structure

```
it.atraj.habittracker/
├── auth/                    # Authentication & user management
│   ├── ui/                  # Login, profile screens, dialogs
│   ├── AuthRepository.kt    # Firebase Auth operations
│   └── GoogleSignInHelper.kt # Google Sign-In configuration
├── data/                    # Data layer
│   ├── local/               # Domain models (Habit, HabitCompletion, etc.)
│   ├── firestore/           # Firestore repositories & models
│   └── HabitRepository.kt   # Main data interface
├── ui/                      # UI layer
│   ├── navigation/          # Navigation setup
│   ├── chat/               # Social chat features
│   ├── social/             # Friends, leaderboard
│   ├── statistics/         # Analytics & charts
│   └── theme/              # Material 3 theming
├── notification/           # Reminder system
├── avatar/                 # Avatar management with GitHub upload
└── di/                     # Hilt dependency injection modules
```

### Key Data Flow

1. **Authentication**: Firebase Auth → `AuthRepository` → `AuthViewModel` → UI
2. **Habits**: Firestore → `FirestoreHabitRepository` → `HabitViewModel` → UI
3. **Notifications**: `HabitReminderScheduler` → AlarmManager → `HabitReminderReceiver` → Notification

## Firebase Configuration

**CRITICAL**: This app requires Firebase setup to function. The `google-services.json` file is NOT included in the repository for security.

### Required Setup:
1. Download `google-services.json` from Firebase Console
2. Place in `app/` directory (never commit this file)
3. Enable Authentication (Email/Password + Google Sign-In)
4. Create Firestore database
5. Add SHA-1 fingerprint: `.\gradlew signingReport`

See `README.md` and `FIREBASE_SETUP.md` for detailed instructions.

## Key Technical Details

### Package Names
- **Current**: `it.atraj.habittracker` (used in build.gradle.kts)
- **Legacy**: `com.example.habittracker` (still used in actual source files)
- **IMPORTANT**: All source code files currently use `com.example.habittracker` package
- When creating new files, match the existing package structure in surrounding files

### Build Configuration
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Kotlin**: 2.0.21
- **JVM Target**: 17

### Security Configuration
- `keystore.properties` stores signing keys and tokens (never committed, see `keystore.properties.template`)
  - GitHub token for avatar uploads
  - Gmail SMTP credentials for email notifications
  - Release signing credentials
- Encrypted SharedPreferences for runtime token storage (`SecureEmailStorage`)
- Firestore security rules enforce user data isolation
- `google-services.json` excluded from repository

### Notification System Architecture
The app uses a multi-layered notification reliability system:

1. **AlarmManager**: Primary timing mechanism for habit reminders
2. **WorkManager**: Periodic verification that alarms are working
3. **Boot Receiver**: Reschedules reminders after device restart
4. **Battery Optimization**: Requests whitelist exemption for reliable notifications

## Development Patterns

### State Management
- Use `StateFlow` in ViewModels for UI state
- Firebase provides real-time data streams via `Flow`
- Compose state is managed with `remember` and `mutableStateOf`

### Error Handling
- Repository layer catches exceptions and returns `Result` types where appropriate
- ViewModels handle errors and expose them as UI state
- Use `Log.d/e` for debugging with consistent tag patterns

### Testing Approach
- Unit tests for business logic in repositories and ViewModels
- UI tests use Compose testing framework
- Firebase operations can be mocked using test doubles

### Navigation
- Uses Jetpack Navigation Compose
- Centralized navigation graph in `NavGraph.kt`
- Deep linking support for notifications (habit details, chat screens)

### Firebase Cloud Functions
The project includes Cloud Functions for chat notifications:
```powershell
# Deploy functions (requires Firebase CLI)
cd functions
npm run deploy

# Test locally
npm run serve

# View logs
npm run logs
```

## Common Development Tasks

### Adding a New Feature
1. Create domain models in `data/local/`
2. Add repository interface in `data/`
3. Implement Firestore repository in `data/firestore/`
4. Create ViewModel (with `@HiltViewModel` annotation) in appropriate UI package
5. Build Compose UI screens
6. Update navigation graph in `ui/navigation/`
7. Add to dependency injection modules in `di/`
8. Update Firestore security rules if needed

### Working with Firebase
- Firebase instances are provided via Hilt DI in `AppModule`
- Firestore has offline persistence enabled (100MB cache)
- Use `@Inject` to get Firebase instances in repositories
- User authentication state is managed globally in `AuthRepository`
- Always handle offline scenarios - Firestore handles sync automatically
- User data structure:
  - `users/{userId}` - Profile data (customAvatar, customDisplayName)
  - `users/{userId}/habits/{habitId}` - Habit documents
  - `users/{userId}/completions/{completionId}` - Completion records
  - `userProfiles/{userId}` - Social features (friends, stats)
  - `chatChannels/{channelId}` - Chat rooms and messages

### Notification Development
- Test on physical device (emulator notifications are unreliable)
- Use `HabitReminderScheduler` interface for scheduling
- Verify permissions: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`
- Consider battery optimization impact on different manufacturers

## Troubleshooting

### Build Issues
- Ensure `google-services.json` is in `app/` directory (download from Firebase Console)
- Check package name in Firebase matches `it.atraj.habittracker`
- Verify `keystore.properties` exists (copy from `keystore.properties.template`)
- Clean and rebuild: `.\gradlew clean assembleDebug`
- If Hilt errors occur: `.\gradlew clean` and rebuild

### Google Sign-In Issues
- Verify SHA-1 fingerprint is added to Firebase
- Check Web Client ID in `GoogleSignInHelper.kt`
- Wait 5 minutes after Firebase configuration changes

### Notification Issues
- Test on physical device only
- Check battery optimization settings
- Verify exact alarm permissions on Android 12+
- Use `NotificationReliabilityHelper` for permission requests

## External Dependencies

### Key Libraries
- **Jetpack Compose**: UI framework
- **Hilt**: Dependency injection
- **Firebase SDK**: Authentication, Firestore, Messaging
- **Vico Charts**: Statistics visualization
- **Coil**: Image loading
- **Lottie**: Animations
- **WorkManager**: Background processing
- **OkHttp**: Update mechanism

### Security Libraries
- **androidx.security:security-crypto**: Encrypted SharedPreferences
- Keystore system for release signing

## Build Variants & Configuration

### Debug Build
- Uses debug signing config
- Enables BuildConfig with GitHub token
- Full logging enabled

### Release Build
```powershell
# Build signed release APK
.\gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# Build signed release AAB (for Play Store)
.\gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```
- ProGuard/R8 enabled for code shrinking and obfuscation
- Resource shrinking enabled
- Requires `keystore.properties` with signing credentials
- GitHub token and email credentials loaded from `keystore.properties`

The codebase follows modern Android development best practices with clean architecture principles and comprehensive error handling.

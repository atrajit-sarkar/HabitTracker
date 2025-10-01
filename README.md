# Habit Tracker Android App

A beautifully designed habit tracker app built with Jetpack Compose, Firebase Authentication, and modern Android architecture.

## Features ✨

### 🔐 Authentication
- **Firebase Authentication**: Email/Password and Google Sign-In support
- **User Profiles**: Customizable display names and emoji avatars
- **Secure**: All sensitive data properly secured with Firestore

### 📊 Habit Tracking
- **Beautiful UI**: Material 3 design with gradient cards and smooth animations
- **Cloud Sync**: All habits synchronized with Firebase Firestore
- **Smart Notifications**: Scheduled reminders for uncompleted habits using AlarmManager
- **Streak Tracking**: Track your daily habit completion streaks
- **Statistics & Analytics**: Comprehensive analytics with professional charts (Vico Charts)
  - Overview tab with performance metrics
  - Trends tab with completion rate analysis
  - Comparison tab for habit performance

### 🎨 Personalization
- **Custom Avatars**: Choose from emoji avatars or use your profile picture
- **Display Names**: Set and edit your display name anytime
- **Dark/Light Mode**: Adaptive theming support
- **Profile Management**: Complete profile customization

### 📱 Easy Habit Management
- Add habits with custom names, descriptions, and reminder times
- Toggle reminders on/off
- Mark habits as completed for the day
- Delete habits with confirmation
- Real-time synchronization across devices

## Architecture 🏗️

- **MVVM Pattern**: Clean separation of concerns with ViewModels
- **Dependency Injection**: Hilt for managing dependencies
- **Database**: Firebase Firestore for cloud storage
- **Local Cache**: Room for offline support (if implemented)
- **UI**: Jetpack Compose with Material 3 theming
- **Authentication**: Firebase Auth with multiple providers
- **Background Work**: AlarmManager for precise habit reminders

## Technical Stack 🛠️

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Cloud Database**: Firebase Firestore
- **Authentication**: Firebase Authentication
- **DI**: Hilt
- **Architecture**: MVVM
- **Charts**: Vico Charts 2.0.0-alpha.28
- **Notifications**: Android Notification System with AlarmManager
- **Build System**: Gradle with Kotlin DSL

## 🔧 Setup Instructions

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK API 29+
- Firebase account

### Firebase Configuration

**⚠️ IMPORTANT:** This repository does NOT include the `google-services.json` file for security reasons.

To run this project, you need to:

1. **Create a Firebase project** and configure it
2. **Download your own `google-services.json`**
3. **Place it in the `app/` directory**

📚 **See detailed setup instructions in:** [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md)

#### Quick Setup:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app with package name: `com.example.habittracker`
4. Download `google-services.json`
5. Place in `app/` directory
6. Enable Authentication (Email/Password and Google)
7. Create a Firestore database
8. Add your SHA-1 fingerprint (get it with `.\gradlew signingReport`)

### Google Sign-In Configuration

For Google Sign-In to work, you MUST add your SHA-1 fingerprint to Firebase:

```powershell
# Get your SHA-1 fingerprint
.\gradlew signingReport

# Or use the helper script
.\get-sha1.ps1
```

Then add the SHA-1 to Firebase Console → Project Settings → Your apps → Add fingerprint

📚 **See:** [`ADD_SHA1_FINGERPRINT.md`](ADD_SHA1_FINGERPRINT.md) for detailed instructions

## Build Instructions 📱

```powershell
# 1. Clone the repository
git clone https://github.com/atrajit-sarkar/HabitTracker.git
cd HabitTracker

# 2. Add your google-services.json file
# (Download from Firebase Console and place in app/ directory)

# 3. Build the project
.\gradlew clean assembleDebug

# 4. Install on connected device
.\gradlew installDebug

# 5. Run the app
adb shell am start -n com.example.habittracker/.MainActivity
```

## Key Components

### Authentication Layer
- `AuthRepository` - Firebase Authentication and user management
- `AuthViewModel` - Authentication state management
- `GoogleSignInHelper` - Google Sign-In configuration
- `AuthScreen` - Login/signup UI with email and Google options
- `ProfileScreen` - User profile with customization options

### Data Layer
- `Habit` entity with Firestore integration
- `HabitRepository` - Cloud data operations
- Real-time Firestore listeners for live updates
- User data synchronization

### UI Layer
- `HabitHomeRoute` - Main habits screen
- `AddHabitSheet` - Bottom sheet for adding habits
- `StatisticsScreen` - Analytics dashboard with charts
- `ProfileScreen` - User profile and settings
- Custom UI components with Material 3 theming

### Analytics
- `StatisticsCalculator` - Performance metrics computation
- Vico Charts integration for visualizations
- Real-time data updates from Firestore

### Analytics
- `StatisticsCalculator` - Performance metrics computation
- Vico Charts integration for visualizations
- Real-time data updates from Firestore

### Notification System
- `HabitReminderScheduler` for scheduling reminders
- `HabitReminderReceiver` for handling alarm triggers
- `HabitReminderService` for creating notifications

## Recent Updates �

### Google Sign-In Fix (October 2025)
- ✅ Fixed Web Client ID configuration
- ✅ Added comprehensive error handling and logging
- ✅ Enhanced security with proper .gitignore
- ✅ Added SHA-1 fingerprint helper script
- ✅ Created detailed setup documentation

### Display Name Management
- ✅ Custom display names for all users
- ✅ Automatic Google account name integration
- ✅ Real-time name updates via Firestore
- ✅ Professional edit dialogs with validation

### Statistics & Analytics
- ✅ Comprehensive analytics dashboard
- ✅ 3-tab layout (Overview, Trends, Compare)
- ✅ Professional Vico charts integration
- ✅ Performance score calculation
- ✅ Real-time data visualization

### UI Improvements
- ✅ Fixed TimePicker state management
- ✅ Improved modal sheet interactions
- ✅ Centered footer with proper alignment
- ✅ Enhanced profile screen design

## 📚 Documentation

Comprehensive documentation available:

- **[FIREBASE_SETUP.md](FIREBASE_SETUP.md)** - Complete Firebase configuration guide
- **[GOOGLE_SIGNIN_COMPLETE_FIX.md](GOOGLE_SIGNIN_COMPLETE_FIX.md)** - Google Sign-In fix summary
- **[ADD_SHA1_FINGERPRINT.md](ADD_SHA1_FINGERPRINT.md)** - SHA-1 configuration details
- **[GOOGLE_SIGNIN_DEBUG.md](GOOGLE_SIGNIN_DEBUG.md)** - Troubleshooting guide
- **[QUICK_FIX_GOOGLE_SIGNIN.md](QUICK_FIX_GOOGLE_SIGNIN.md)** - Quick reference
- **[SAFE_TO_COMMIT.md](SAFE_TO_COMMIT.md)** - Git security guide
- **[STATISTICS_FEATURE_SUMMARY.md](STATISTICS_FEATURE_SUMMARY.md)** - Analytics features
- **[DISPLAY_NAME_FEATURE.md](DISPLAY_NAME_FEATURE.md)** - Name management system

## 🔒 Security

- ✅ `google-services.json` excluded from repository
- ✅ Firebase configuration template provided
- ✅ Comprehensive .gitignore for sensitive files
- ✅ Firestore security rules enforced
- ✅ User data properly isolated

## Permissions

- `POST_NOTIFICATIONS`: For habit reminders (Android 13+)
- `INTERNET`: For Firebase cloud sync
- `ACCESS_NETWORK_STATE`: For connectivity checks

## Requirements

- **Minimum SDK**: API 29 (Android 10)
- **Target SDK**: API 36
- **Compile SDK**: API 36
- **Kotlin**: 2.0.21
- **Gradle**: 8.13

## Troubleshooting 🔧

### Google Sign-In Not Working?
1. Verify SHA-1 is added to Firebase Console
2. Check Web Client ID in `GoogleSignInHelper.kt`
3. Ensure Google Sign-In is enabled in Firebase
4. Wait 5 minutes after adding SHA-1
5. Reinstall the app

See [GOOGLE_SIGNIN_DEBUG.md](GOOGLE_SIGNIN_DEBUG.md) for detailed troubleshooting.

### Build Fails?
- Ensure `google-services.json` is in `app/` directory
- Check Firebase project configuration
- Sync Gradle files
- Clean and rebuild: `.\gradlew clean assembleDebug`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add your `google-services.json` (don't commit it!)
5. Test thoroughly
6. Submit a pull request

## License

This project is open source and available under the MIT License.

---

*Built with ❤️ using modern Android development practices*

**Note:** Remember to add your own `google-services.json` file before building. See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for instructions.
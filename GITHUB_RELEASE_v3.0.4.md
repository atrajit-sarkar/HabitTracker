# 🎯 Habit Tracker v3.0.4 Release Notes

## 🎨 Profile Screen Redesign

### ✨ What's New

**Centered Profile Layout**
- Profile photo now displays prominently at the top center
- User information arranged vertically for better readability
- Name, email, and account type are now center-aligned
- More modern and intuitive profile presentation

### 📋 Detailed Changes

#### Profile Header Improvements
- ✅ **Profile Photo**: Now centered at the top with glittering animation
- ✅ **Display Name**: Centered below the profile photo
- ✅ **Email Address**: Centered for better visual hierarchy
- ✅ **Account Badge**: Shows "Google Account" or "Email Account" in a centered badge

#### Layout Benefits
- 🎯 **Better Focus**: Profile photo gets more prominence
- 📱 **Modern Design**: Follows current mobile UI/UX trends
- 🌟 **Clean Layout**: Vertical arrangement creates better visual flow
- 💎 **Professional Look**: Centered alignment improves aesthetics

## 🔔 Notification Reliability Improvements

### Enhanced Idle-Mode Notification Delivery
- ✅ **Exact Alarm Permission**: Added support for Android 12+ exact alarm permission with user-friendly prompts
- ✅ **Time Change Handling**: Reminders now reschedule automatically when system time, timezone, or date changes
- ✅ **Daylight Saving Support**: Handles timezone transitions seamlessly
- ✅ **Better Permission Flow**: Sequential prompts for battery optimization and exact alarms (no dialog stacking)

### What This Fixes
- � **Long Idle Periods**: Notifications now fire reliably even after hours of device sleep
- 🐛 **Time Zone Changes**: Reminders stay accurate when traveling across time zones
- 🐛 **Manual Time Changes**: Alarms reschedule when user manually adjusts system time
- 🐛 **Android 12+ Devices**: Proper exact alarm permission handling for newer Android versions

### Technical Improvements
- Added `TimeChangeReceiver` for system time/timezone/date change events
- Enhanced `NotificationReliabilityHelper` with exact alarm permission methods
- Updated manifest with time change broadcast receivers
- Integrated permission prompts in MainActivity onResume flow

## 🐛 Bug Fixes

### Home Screen Completion Status Fix
- ✅ **Fixed**: Habits completed yesterday no longer show as completed today
- ✅ **Accurate Status**: Home screen now always displays current day completion status
- ✅ **Fresh Data**: Completion check now evaluates against current date in real-time

**Problem**: Previously, if you completed a habit yesterday and opened the app today, the home screen would still show it as completed instead of showing the "Done" button.

**Solution**: Refactored completion status check to evaluate against the current date when rendering, ensuring accurate display even if the app stays open across midnight.

### �🔧 Technical Updates
- **Version Code**: 5
- **Version Name**: 3.0.4
- Refactored ProfileScreen layout from Row to Column
- Added center alignment for all profile elements
- Enhanced notification reliability with multi-layered approach
- Fixed home screen date comparison logic
- Maintained all existing functionality and animations

---

## 📦 Installation

Download the APK from the assets below and install on your Android device (Android 10+).

## 🐛 Bug Reports

Found a bug? Please [open an issue](https://github.com/atrajit-sarkar/HabitTracker/issues) with detailed steps to reproduce.

## 💬 Feedback

We'd love to hear your thoughts! Share your feedback in the [Discussions](https://github.com/atrajit-sarkar/HabitTracker/discussions) section.

---

**Full Changelog**: https://github.com/atrajit-sarkar/HabitTracker/compare/v3.0.3...v3.0.4

# Widget Promotion System 🎯

## Overview
Automatically promotes the Habit Tracker widget to new users after they complete the initial onboarding, helping them stay on track with their habits.

## Features Implemented

### 1. **Smart Timing** ⏰
- Widget promotion dialog appears **after** the first launch notification dialog
- Only shows if user has **at least one habit** created
- One-time promotion (tracked via SharedPreferences)

### 2. **Widget Preview** 🖼️
- **Live preview** shown in promotion dialog using actual widget layout
- **Preview layout** (widget_preview_layout.xml) shows:
  - "🎉 All Done!" state with dummy data
  - "3/3 completed • 🔥 7 day streak" example text
  - Widget all-done celebration image
- **Android 12+ previewLayout** attribute for native widget picker
- Visual representation helps users understand what they're adding

### 3. **Direct Widget Addition** 📲
Widget can be added directly from the dialog:
- **Android 8.0+**: Uses `requestPinAppWidget()` API to open widget picker
- **Android 7.1 and below**: Shows instructions for manual addition
- **Unsupported launchers**: Gracefully falls back to manual instructions

### 3. **Beautiful Dialog UI** 🎨
- Material Design 3 components
- Gradient background on widget icon
- Clear benefits list with emoji indicators:
  - ✅ See overdue habits at a glance
  - ⚡ Quick access to pending tasks
  - 🔥 Never miss a habit again
  - 🎨 Beautiful, dynamic updates
- Two-button layout: "Maybe Later" and "Add Widget"
- Helpful tip about manual addition at bottom

## Technical Implementation

### Files Created
- `ui/dialogs/WidgetPromotionDialog.kt` - Dialog composable with widget pinning logic

### Files Modified
- `ui/HomeScreen.kt`:
  - Added `WidgetPromotionDialog` import
  - Added `showWidgetPromotionDialog` state
  - Integrated dialog after first launch dialog dismissal
  - Checks for habits existence before showing

### Widget Pinning API
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        appWidgetManager.requestPinAppWidget(widgetProvider, null, successCallback)
    }
}
```

## User Flow

### New User Journey:
1. User opens app for first time
2. After 2 seconds delay → **First Launch Notification Dialog** appears
3. User dismisses or opens notification guide
4. **IF** user has created at least one habit → **Widget Promotion Dialog** appears
5. User can:
   - **Add Widget**: Opens widget picker (Android 8+) or shows instructions
   - **Maybe Later**: Dismisses dialog (won't show again)

### Widget Addition Methods:

#### Method 1: From Dialog (Android 8.0+)
1. User clicks "Add Widget" button
2. System opens widget picker
3. User places widget on home screen
4. Widget immediately shows current habit status

#### Method 2: Manual Addition
1. Long press on home screen
2. Select "Widgets"
3. Find "Habit Tracker"
4. Drag to home screen

## Persistence

### SharedPreferences Keys:
- `widget_promotion_shown` (Boolean): Tracks if dialog has been shown
- `notification_guide_shown` (Boolean): Tracks if first launch dialog shown

### Reset for Testing:
```kotlin
// Clear in app settings or via adb
adb shell pm clear it.atraj.habittracker
```

## Benefits

### For Users:
- **Increased Engagement**: Widget keeps habits visible
- **Better Completion Rates**: Quick access to overdue habits
- **Reduced App Opens**: Check status without launching app
- **Visual Reminders**: Always-visible habit status

### For App:
- **Higher Retention**: Users with widgets are more engaged
- **Feature Discovery**: Many users don't know about widgets
- **Professional Polish**: Guided feature adoption
- **Smart Timing**: Only shows when relevant (has habits)

## Edge Cases Handled

1. **No Habits**: Dialog doesn't show if user has no habits
2. **Unsupported Launcher**: Falls back to manual instructions
3. **Older Android**: Shows manual addition steps
4. **Already Shown**: Uses SharedPreferences to prevent repeat shows
5. **Dialog Dismissal**: User can skip without being pestered

## Testing Checklist

- [ ] Dialog appears after first launch dialog
- [ ] Dialog only shows when user has habits
- [ ] "Add Widget" button works on Android 8+
- [ ] Manual instructions show on older devices
- [ ] Dialog doesn't repeat after dismissal
- [ ] Widget updates automatically after addition
- [ ] UI looks good in light/dark themes
- [ ] Toast messages are clear and helpful

## Future Enhancements

1. **Re-promotion Logic**: Show again after 30 days if user hasn't added widget
2. **Analytics**: Track widget adoption rate
3. **In-App Tutorial**: Show how to use widget features
4. **Widget Customization**: Let users configure widget from dialog
5. **A/B Testing**: Test different dialog timings and messaging

## Related Features

- **Widget Data Observer**: Automatically updates widget when habits change
- **First Launch Dialog**: Educates users about notifications
- **Habit Widget**: 4-state widget with dynamic updates
- **Overdue Habits Activity**: Deep-linked from widget for multiple overdue habits

---

**Implementation Date**: November 1, 2025  
**Android Version Support**: API 21+ (with graceful degradation for widget pinning)

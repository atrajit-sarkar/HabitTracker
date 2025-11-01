# Changelog - Version 7.1.6

## 🎯 Widget Enhancement Update

### New Features

#### 🪄 **Professional Widget System**
- **4 Dynamic States**: Widget automatically adapts based on habit status
  - 🌅 **Morning State**: No habits due yet (gradient: yellow → orange)
  - ⚠️ **One Overdue**: Single overdue habit with navigation to details
  - 🔥 **Multiple Overdue**: Multiple overdue habits with urgency screen
  - 🎉 **All Done**: All habits completed celebration (gradient: green → blue)

#### 📱 **Widget Promotion System**
- **Smart Onboarding**: Automatic widget promotion for new users
  - Shows after first launch dialog completes
  - Only displays if user has created habits
  - One-time promotion with visual preview
- **Settings Integration**: Add widget anytime from Profile → Home Screen Widget
- **Live Preview**: Shows actual widget appearance before adding
- **Direct Addition**: One-tap widget placement (Android 8+)

#### 🎨 **Overdue Habits Screen**
- **Urgency-Based UI**: Visual indicators based on hours overdue
  - Level 0 (0-6 hours): Gentle reminder
  - Level 1 (6-12 hours): Attention needed
  - Level 2 (12-24 hours): Urgent
  - Level 3 (24+ hours): Critical with enhanced animations
- **Smart Navigation**: Deep-links to habit details for quick completion
- **Dynamic Animations**: Scale and shimmer effects based on urgency

#### 🔄 **Automatic Widget Updates**
- **Data Observer Pattern**: Widget updates automatically on any habit change
  - Habit completion (from notifications, details, home screen)
  - Habit creation or deletion
  - Reminder settings changes
- **Efficient Updates**: Uses Flow + distinctUntilChanged for performance
- **User-Aware**: Automatically switches data when user logs out/in

### Technical Improvements

#### Widget Infrastructure
- `WidgetDataObserver`: Singleton observer using Kotlin Flows
- `WidgetHelper`: Centralized widget management utilities
- `HabitWidgetProvider`: Professional 4-state widget with gradients
- `OverdueHabitsActivity`: Dedicated screen for multiple overdue habits

#### UI/UX
- Widget preview layout for picker and promotion dialog
- Theme-specific gradient backgrounds
- PNG images for each widget state
- Material Design 3 dialog components

#### Performance
- Signature-based change detection to avoid unnecessary updates
- Background coroutine with SupervisorJob
- Efficient widget refresh only on meaningful data changes

### Files Added
- `widget/WidgetDataObserver.kt` - Automatic widget update observer
- `widget/WidgetHelper.kt` - Widget utility functions
- `ui/OverdueHabitsActivity.kt` - Urgency-based overdue habits screen
- `ui/dialogs/WidgetPromotionDialog.kt` - Widget promotion with preview
- `res/layout/widget_preview_layout.xml` - Widget preview for picker
- `res/drawable/widget_*.png` - State-specific widget images

### Bug Fixes
- Fixed navigation from widget to MainActivity using launcher intent
- Resolved array index issues with negative habit IDs in color palette
- Fixed Hilt dependency injection for Context in WidgetDataObserver

### Developer Notes
- Widget updates happen automatically via data observation
- Manual widget update triggers kept as fallback
- Compatible with Android 8+ widget pinning API
- Graceful degradation for older Android versions

---

**Version**: 7.1.6  
**Build**: 35  
**Release Date**: November 1, 2025  
**Target SDK**: Android 14 (API 36)  
**Min SDK**: Android 10 (API 29)

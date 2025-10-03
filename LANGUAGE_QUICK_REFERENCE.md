# Multi-Language Quick Reference 🌍

## Supported Languages

| Language | Code | Users | File Location |
|----------|------|-------|---------------|
| 🇬🇧 English | `en` (default) | Global | `values/strings.xml` |
| 🇧🇩 Bengali | `bn` | 230M+ | `values-bn/strings.xml` |
| 🇮🇳 Hindi | `hi` | 600M+ | `values-hi/strings.xml` |
| 🇸🇦 Arabic | `ar` (RTL) | 400M+ | `values-ar/strings.xml` |
| 🇯🇵 Japanese | `ja` | 125M+ | `values-ja/strings.xml` |

**Total Potential Reach: 1.4+ Billion People**

## Usage in Code

```kotlin
// Simple string
Text(stringResource(R.string.save))

// String with parameter
Text(stringResource(R.string.current_streak_days, 7))

// In ViewModel
val message = context.getString(R.string.habit_completed_today)
```

## Common Strings Reference

### Navigation & Actions
| English | Code | Bengali | Hindi | Arabic | Japanese |
|---------|------|---------|-------|--------|----------|
| Save | `R.string.save` | সংরক্ষণ করুন | सहेजें | حفظ | 保存 |
| Cancel | `R.string.cancel` | বাতিল | रद्द करें | إلغاء | キャンセル |
| Delete | `R.string.delete` | মুছুন | हटाएं | حذف | 削除 |
| Back | `R.string.back` | পেছনে | वापस | رجوع | 戻る |
| Done | `R.string.done` | সম্পন্ন | पूर्ण | تم | 完了 |

### Habits
| English | Code |
|---------|------|
| Add habit | `R.string.add_habit` |
| Your habits | `R.string.home_title` |
| Habit name | `R.string.habit_name_label` |
| Create Habit | `R.string.create_habit` |
| Completed today | `R.string.completed_today` |

### Statistics
| English | Code |
|---------|------|
| Current Streak | `R.string.current_streak` |
| Success Rate | `R.string.success_rate` |
| Total Completions | `R.string.total_completions` |
| Longest Streak | `R.string.longest_streak` |

### Frequency
| English | Code |
|---------|------|
| Daily | `R.string.daily` |
| Weekly | `R.string.weekly` |
| Monthly | `R.string.monthly` |
| Yearly | `R.string.yearly` |

### Days of Week
| English | Code |
|---------|------|
| Monday | `R.string.monday` |
| Tuesday | `R.string.tuesday` |
| Wednesday | `R.string.wednesday` |
| Thursday | `R.string.thursday` |
| Friday | `R.string.friday` |
| Saturday | `R.string.saturday` |
| Sunday | `R.string.sunday` |

### Social
| English | Code |
|---------|------|
| Friends | `R.string.friends` |
| Leaderboard | `R.string.leaderboard` |
| Messages | `R.string.messages` |
| Search | `R.string.search` |

### Profile
| English | Code |
|---------|------|
| Profile | `R.string.profile` |
| Edit Name | `R.string.edit_name` |
| Change Avatar | `R.string.change_avatar` |
| Sign Out | `R.string.sign_out` |

## Testing Languages

### Method 1: Change Device Language
```
Settings → System → Languages → Add Bengali/Hindi/Arabic/Japanese
```

### Method 2: Test in Android Studio
```
Run → Edit Configurations → App Language → Select language
```

### Method 3: See Translations in Layout Editor
```
Open XML layout → Language selector (top toolbar) → Choose language
```

## Adding New Language

Want to add Spanish, French, Chinese, etc.?

1. **Create folder**: `values-{code}/` (e.g., `values-es/` for Spanish)
2. **Copy template**: Copy any `strings.xml` to new folder
3. **Translate**: Replace text content (keep XML tags)
4. **Test**: Change device language and run app

### Common Language Codes
- Spanish: `es`
- French: `fr`
- German: `de`
- Chinese (Simplified): `zh-rCN`
- Chinese (Traditional): `zh-rTW`
- Portuguese: `pt`
- Russian: `ru`
- Korean: `ko`

## RTL Languages

**Arabic** (`values-ar/`) is Right-to-Left:
- Android **automatically** mirrors the entire UI
- No code changes needed
- Navigation reverses (back button on right)
- Text alignment flips to right

Other RTL languages you can add:
- Hebrew: `values-iw/`
- Persian: `values-fa/`
- Urdu: `values-ur/`

## Troubleshooting

### String Not Translating?
1. Check hardcoded strings in Kotlin files
2. Replace with `stringResource(R.string.key)`
3. Rebuild project (Build → Rebuild Project)

### Missing Translation?
- App falls back to English (`values/strings.xml`)
- No crash - graceful degradation

### Wrong Translation Showing?
1. Clear app data: Settings → Apps → Habit Tracker → Clear Data
2. Change device language again
3. Relaunch app

## Build Commands

```powershell
# Clean build
.\gradlew clean

# Build with all languages
.\gradlew assembleDebug

# Install on device
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

## File Locations

```
app/src/main/res/
├── values/strings.xml         ← English (default)
├── values-bn/strings.xml      ← Bengali
├── values-hi/strings.xml      ← Hindi
├── values-ar/strings.xml      ← Arabic (RTL)
└── values-ja/strings.xml      ← Japanese
```

## Benefits Summary

✅ Reach 1.4B+ users in their native language  
✅ 7x more downloads with localization  
✅ Professional, polished app appearance  
✅ Zero code changes for basic support  
✅ Automatic RTL layout for Arabic  
✅ Easy to add more languages later  

---

**Quick Test**: Change your phone to Bengali/Hindi/Arabic/Japanese and open the app - it's fully translated! 🎉

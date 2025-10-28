# 🚀 Gemini AI Quick Setup Guide

## For Users

### Step 1: Get Your Free API Key
1. Visit **https://ai.google.dev**
2. Sign in with your Google account
3. Click **"Get API Key"** button
4. Click **"Create API key in new project"**
5. Copy your API key (starts with `AIza...`)

### Step 2: Configure in Habit Tracker
1. Open the app
2. Tap the **menu icon** (☰) in top-left
3. Tap on your **profile picture/avatar**
4. Scroll down to **"Account Settings"**
5. Tap **"Gemini AI Settings"** (has ✨ icon)
6. Toggle **"Enable Gemini AI"** to ON
7. Paste your API key in the text field
8. Tap **"Save"**

### Step 3: Enjoy Personalized Messages!
- Open the app tomorrow morning
- You'll see a personalized welcome message
- If you have overdue tasks, you'll get a friendly reminder

---

## For Developers

### Quick Integration Test

```kotlin
// 1. Create GeminiPreferences instance
val geminiPrefs = GeminiPreferences(context)

// 2. Save API key
geminiPrefs.saveApiKey("YOUR_API_KEY_HERE")
geminiPrefs.setGeminiEnabled(true)

// 3. Test message generation
val geminiService = GeminiApiService(geminiPrefs.getApiKey()!!)
viewModelScope.launch {
    // Test welcome message
    geminiService.generateWelcomeMessage("John").onSuccess { message ->
        Log.d("Gemini", "Welcome: $message")
    }
    
    // Test overdue message
    geminiService.generateOverdueMessage("John", 3).onSuccess { message ->
        Log.d("Gemini", "Overdue: $message")
    }
}
```

### File Structure
```
app/src/main/java/com/example/habittracker/
├── gemini/
│   ├── GeminiApiService.kt      # API client
│   ├── GeminiPreferences.kt     # Secure storage
│   └── GeminiOverlays.kt        # UI components
├── auth/ui/
│   └── ProfileScreen.kt         # Settings UI (updated)
└── ui/
    └── HomeScreen.kt            # Integration point (updated)
```

### Key Classes

#### GeminiApiService
```kotlin
class GeminiApiService(private val apiKey: String) {
    suspend fun generateWelcomeMessage(userName: String): Result<String>
    suspend fun generateOverdueMessage(userName: String, overdueCount: Int): Result<String>
}
```

#### GeminiPreferences
```kotlin
class GeminiPreferences(context: Context) {
    fun saveApiKey(apiKey: String)
    fun getApiKey(): String?
    fun isApiKeyConfigured(): Boolean
    fun setGeminiEnabled(enabled: Boolean)
    fun isGeminiEnabled(): Boolean
    fun clearApiKey()
}
```

#### GeminiOverlays
```kotlin
@Composable fun PersonalizedMessageOverlay(...)
@Composable fun GeminiLoadingOverlay(...)
@Composable fun ConfigureGeminiOverlay(...)
```

### Testing Checklist

- [ ] API key saves correctly in ProfileScreen
- [ ] API key is encrypted in storage
- [ ] Welcome message appears on app open (no overdue tasks)
- [ ] Overdue message appears on app open (with overdue tasks)
- [ ] Messages appear only once per day
- [ ] Loading overlay shows during API call
- [ ] Configure prompt appears if API key not set
- [ ] "Go to Settings" button navigates to ProfileScreen
- [ ] Messages auto-dismiss after 5 seconds
- [ ] Manual dismiss works correctly

### Troubleshooting

**Message not appearing?**
1. Check if Gemini is enabled in settings
2. Verify API key is valid (starts with `AIza`)
3. Check logs for errors: `adb logcat | grep -i gemini`
4. Ensure device has internet connection
5. Check if message was already shown today (clear app data to reset)

**API Key not saving?**
1. Check encryption is working: `adb logcat | grep -i EncryptedSharedPreferences`
2. Verify app has proper permissions
3. Check for crashes in ProfileScreen

**Network errors?**
1. Verify internet connection
2. Check API key quota (15 requests/minute free tier)
3. Ensure HTTPS is working (no proxy issues)

### Debug Commands

```bash
# View Gemini logs
adb logcat | grep -i gemini

# Clear app data (resets once-per-day tracking)
adb shell pm clear it.atraj.habittracker

# View encrypted preferences (won't show actual key)
adb shell run-as it.atraj.habittracker ls -la /data/data/it.atraj.habittracker/shared_prefs/
```

### API Limits (Free Tier)

- **Rate**: 15 requests per minute
- **Daily**: 1,500 requests per day
- **Tokens**: 1 million tokens per minute
- **Cost**: FREE forever

---

## 💡 Tips

### For Users
- Keep your API key private (don't share it)
- The feature uses very minimal API quota
- Messages are generated fresh each time
- You can disable the feature anytime in settings

### For Developers
- Use `BuildConfig.DEBUG` to bypass once-per-day limit during testing
- Test both welcome and overdue scenarios
- Handle API failures gracefully (don't crash the app)
- Consider adding analytics to track usage
- Monitor API quota if scaling to many users

---

## 🎯 Example Messages

### Welcome Messages
> "Hey John! Great to see you back! You're absolutely crushing it with your habits. Keep up this amazing momentum and let's make today even better! 💪"

> "Welcome back, Sarah! You're doing fantastic - all your habits are up to date! Let's keep this winning streak alive. You've got this! 🌟"

### Overdue Messages
> "John, you have 3 overdue habits waiting for you. I know you've been busy, but these tasks aren't going to complete themselves! Let's tackle them now and get back on track. 😤"

> "Sarah, come on! You've got 2 habits that are overdue. I know you're capable of so much more than this. Stop procrastinating and complete them right now! 💢"

---

**Need help? Check out the full documentation: [GEMINI_AI_FEATURE.md](./GEMINI_AI_FEATURE.md)**

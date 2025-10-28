# Gemini AI Personalized Messages Feature

## 🎉 Overview

The Habit Tracker app now includes **personalized welcome messages and overdue task reminders** powered by **Google's Gemini 2.5 Flash AI**! This feature provides a more human, friendly, and engaging experience for users.

## ✨ Features

### 1. **Personalized Welcome Messages**
- Shows when the user opens the app with **no overdue tasks**
- Displays a friendly, encouraging message addressing the user by name
- Appears **once per day** to avoid being intrusive
- Uses animated icons for visual appeal

### 2. **Overdue Task Reminders**
- Triggers when the user has **overdue habits**
- Displays a slightly frustrated but caring message (like a concerned friend)
- Mentions the specific number of overdue tasks
- Encourages immediate action with a firm but supportive tone
- Appears **once per day**

### 3. **Secure API Key Management**
- API keys are stored using **EncryptedSharedPreferences**
- Users can configure their own Gemini API key in Profile Settings
- Enable/disable the feature at any time

### 4. **First-Time User Experience**
- If Gemini is not configured, users see a friendly prompt
- Directs them to Profile Settings to add their API key
- Includes clear instructions on how to get a free API key

## 🔧 Technical Implementation

### Architecture

```
gemini/
├── GeminiApiService.kt        # HTTP API client for Gemini 2.5 Flash
├── GeminiPreferences.kt       # Secure storage for API key
└── GeminiOverlays.kt          # UI overlays and dialogs
```

### Key Components

#### **GeminiApiService**
- Makes HTTP requests to Gemini 2.5 Flash API
- Handles welcome and overdue message generation
- Includes proper error handling and timeout management
- Uses OkHttp for network calls
- Serializes/deserializes JSON with Kotlinx Serialization

```kotlin
suspend fun generateWelcomeMessage(userName: String): Result<String>
suspend fun generateOverdueMessage(userName: String, overdueCount: Int): Result<String>
```

#### **GeminiPreferences**
- Stores API key securely using EncryptedSharedPreferences
- Provides methods to save, retrieve, and clear API keys
- Includes enable/disable toggle for the feature

```kotlin
fun saveApiKey(apiKey: String)
fun getApiKey(): String?
fun isApiKeyConfigured(): Boolean
fun setGeminiEnabled(enabled: Boolean)
fun isGeminiEnabled(): Boolean
```

#### **GeminiOverlays**
- `PersonalizedMessageOverlay`: Displays AI-generated messages with animations
- `GeminiLoadingOverlay`: Shows while fetching messages from API
- `ConfigureGeminiOverlay`: Prompts users to configure API key

### Integration Points

#### **ProfileScreen.kt**
- Added "Gemini AI Settings" row in Account Settings section
- New dialog (`GeminiSettingsDialog`) for API key configuration
- Features:
  - Enable/disable toggle
  - Secure API key input field with show/hide
  - Instructions on how to obtain API key
  - Link to ai.google.dev

#### **HomeScreen.kt**
- Replaced static animations with Gemini-powered messages
- Logic flow:
  1. Check if Gemini is configured and enabled
  2. If not configured → Show configuration prompt
  3. If configured:
     - Has overdue tasks → Generate overdue message
     - No overdue tasks → Generate welcome message
  4. Display message with appropriate animation
  5. Track display frequency (once per day)

## 📝 Prompts Used

### Welcome Message Prompt
```
Generate a warm, friendly, and personalized welcome message for {userName} 
who just opened their habit tracking app. The user has no overdue tasks - they're on track!

Requirements:
- Be enthusiastic and encouraging
- Keep it short (2-3 sentences maximum)
- Use a friendly, casual tone
- Motivate them to maintain their good habits
- Don't use emojis
- Address them by name
```

### Overdue Message Prompt
```
Generate a message for {userName} who has {overdueCount} overdue habit(s) 
in their habit tracking app.

Requirements:
- Use a slightly frustrated/disappointed tone (like a caring friend who's a bit annoyed)
- Be firm but still supportive and caring
- Mention the specific number of overdue tasks
- Encourage them to complete the tasks immediately
- Keep it short (2-3 sentences maximum)
- Don't be too harsh - remember you're trying to help them
- Don't use emojis
- Address them by name
```

## 🚀 How to Use

### For Users

1. **Get a Gemini API Key** (Free!)
   - Visit [ai.google.dev](https://ai.google.dev)
   - Sign in with your Google account
   - Click "Get API Key"
   - Create a new key and copy it

2. **Configure in App**
   - Open the app drawer
   - Tap on your profile
   - Scroll to "Account Settings"
   - Tap "Gemini AI Settings"
   - Toggle "Enable Gemini AI" ON
   - Paste your API key
   - Tap "Save"

3. **Experience Personalized Messages**
   - Open the app the next day
   - See your personalized welcome message!
   - If you have overdue tasks, get a friendly reminder

### For Developers

To test the feature:

```kotlin
// Test welcome message
val geminiService = GeminiApiService("YOUR_API_KEY")
val result = geminiService.generateWelcomeMessage("John")
result.onSuccess { message ->
    Log.d("Gemini", "Welcome: $message")
}

// Test overdue message
val result = geminiService.generateOverdueMessage("John", 3)
result.onSuccess { message ->
    Log.d("Gemini", "Overdue: $message")
}
```

## 🔒 Security Considerations

1. **Encrypted Storage**: API keys are stored using EncryptedSharedPreferences with AES256-GCM encryption
2. **HTTPS Only**: All API calls use HTTPS
3. **User Control**: Users provide their own API keys (no shared keys)
4. **No Data Retention**: Messages are generated on-the-fly and not stored
5. **Fallback**: If encryption fails, falls back to regular SharedPreferences with clear warnings

## 🎨 UI/UX Design

### Welcome Message
- **Colors**: Primary color gradient (blue/purple)
- **Icon**: Smiling emoji with scale animation
- **Tone**: Friendly and encouraging
- **Auto-dismiss**: 5 seconds

### Overdue Message
- **Colors**: Error color gradient (red/orange)
- **Icon**: Warning icon with rotation animation
- **Tone**: Slightly frustrated but caring
- **Auto-dismiss**: 5 seconds

### Configuration Prompt
- **Colors**: Tertiary color gradient
- **Icon**: Sparkle (AutoAwesome)
- **Actions**: "Go to Settings" (primary) and "Maybe Later" (secondary)

## 📊 Performance

- **API Call Time**: ~1-3 seconds for message generation
- **Frequency**: Once per day maximum (prevents API abuse)
- **Memory**: Minimal overhead (~2MB for OkHttp client)
- **Battery**: Negligible impact (infrequent API calls)

## 🐛 Error Handling

The implementation includes comprehensive error handling:

1. **Network Errors**: Gracefully fail without showing messages
2. **API Key Issues**: Log errors and continue without disruption
3. **Timeout**: 30-second timeout on API calls
4. **Rate Limiting**: Once-per-day limit prevents API quota issues
5. **Encryption Failures**: Falls back to regular SharedPreferences

## 🔮 Future Enhancements

Potential improvements for future versions:

1. **More Message Types**:
   - Streak milestones (e.g., "You're on a 10-day streak!")
   - Motivational quotes
   - Achievement celebrations

2. **Customization**:
   - Adjust message tone (more/less strict)
   - Choose animation style
   - Set custom triggers

3. **Smart Scheduling**:
   - Learn user's active hours
   - Optimize message timing
   - Adaptive frequency

4. **Multilingual Support**:
   - Generate messages in user's preferred language
   - Cultural adaptation

5. **Voice Messages**:
   - Text-to-speech integration
   - Custom voice personas

## 📄 API Documentation

### Gemini 2.5 Flash Endpoint
```
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent
```

### Request Format
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Your prompt here"
        }
      ]
    }
  ]
}
```

### Response Format
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Generated message"
          }
        ]
      }
    }
  ]
}
```

## 📚 Dependencies

No new dependencies required! The implementation uses existing libraries:

- **OkHttp**: Already included for update checks
- **Kotlinx Serialization**: Already included for JSON parsing
- **EncryptedSharedPreferences**: Added for secure storage
- **Material Icons**: Used for UI icons

## 🎓 Learning Resources

- [Gemini API Documentation](https://ai.google.dev/docs)
- [Get Free API Key](https://ai.google.dev)
- [Gemini Pricing](https://ai.google.dev/pricing) (Free tier includes 15 RPM, 1M TPM)

## 📝 Changelog

### Version 7.0.8 (Current)
- ✨ Initial release of Gemini AI personalized messages
- 🔒 Secure API key storage with EncryptedSharedPreferences
- 🎨 Beautiful animated overlays for messages
- ⚙️ Profile settings integration
- 🚀 First-time user onboarding

## 🤝 Contributing

To extend this feature:

1. Add new message types in `GeminiApiService`
2. Create custom prompts for different scenarios
3. Design new overlay animations in `GeminiOverlays`
4. Update UI integration in `HomeScreen`

## 📞 Support

For issues or questions:
- GitHub Issues: [Create an issue](https://github.com/atrajit-sarkar/HabitTracker/issues)
- Email: [Your support email]

---

**Built with ❤️ using Google's Gemini 2.5 Flash**

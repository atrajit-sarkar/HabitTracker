# Gemini AI Feature - Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER EXPERIENCE                          │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                         HomeScreen.kt                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  LaunchedEffect (Check Gemini Configuration)              │  │
│  │                                                            │  │
│  │  ┌──────────────┐    ┌──────────────┐   ┌──────────────┐ │  │
│  │  │   No Config  │───▶│   Loading    │──▶│   Message    │ │  │
│  │  │   Prompt     │    │   Overlay    │   │   Overlay    │ │  │
│  │  └──────────────┘    └──────────────┘   └──────────────┘ │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GeminiOverlays.kt (UI Layer)                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  PersonalizedMessageOverlay                               │  │
│  │  - Displays AI-generated message                          │  │
│  │  - Animated icon (😊 or ⚠️)                               │  │
│  │  - Auto-dismiss (5s)                                      │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  GeminiLoadingOverlay                                     │  │
│  │  - Shows while API call in progress                       │  │
│  │  - Circular progress indicator                            │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  ConfigureGeminiOverlay                                   │  │
│  │  - Prompts user to configure API key                      │  │
│  │  - "Go to Settings" button                                │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                  GeminiApiService.kt (Network Layer)             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  generateWelcomeMessage(userName: String)                 │  │
│  │  ────────────────────────────────────────                 │  │
│  │  Input:  User's name                                      │  │
│  │  Output: "Hey John! Great to see you back!..."           │  │
│  │  Tone:   Enthusiastic, encouraging                        │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  generateOverdueMessage(userName, count: Int)             │  │
│  │  ────────────────────────────────────────────             │  │
│  │  Input:  User's name + overdue count                      │  │
│  │  Output: "John, you have 3 overdue habits!..."           │  │
│  │  Tone:   Firm but caring                                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Uses: OkHttp + Kotlinx Serialization                           │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│              Google Gemini 2.5 Flash API (External)              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  POST https://generativelanguage.googleapis.com/...      │  │
│  │  Header: x-goog-api-key: {USER_API_KEY}                  │  │
│  │  Body: { "contents": [{ "parts": [{ "text": "..." }] }] }│  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▲
┌─────────────────────────────────────────────────────────────────┐
│              GeminiPreferences.kt (Storage Layer)                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Encrypted SharedPreferences (AES256-GCM)                │  │
│  │  ──────────────────────────────────────────              │  │
│  │  • saveApiKey(key: String)                               │  │
│  │  • getApiKey(): String?                                  │  │
│  │  • isApiKeyConfigured(): Boolean                         │  │
│  │  • setGeminiEnabled(enabled: Boolean)                    │  │
│  │  • isGeminiEnabled(): Boolean                            │  │
│  │  • clearApiKey()                                         │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 ▲
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                    ProfileScreen.kt (Settings UI)                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Account Settings Section                                 │  │
│  │  ├─ Notification Setup                                    │  │
│  │  ├─ Language Settings                                     │  │
│  │  ├─ Theme Settings                                        │  │
│  │  ├─ Email Settings                                        │  │
│  │  ├─ ✨ Gemini AI Settings  ◀── NEW!                       │  │
│  │  └─ Check for Updates                                     │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  GeminiSettingsDialog                                     │  │
│  │  ├─ Enable/Disable Toggle                                │  │
│  │  ├─ API Key Input (show/hide)                            │  │
│  │  ├─ Help Card (How to get API key)                       │  │
│  │  └─ Save Button                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 ▲
                                 │
                          USER INTERACTION
```

---

## Data Flow Diagram

```
┌──────────────┐
│  App Opens   │
└──────┬───────┘
       │
       ▼
┌────────────────────────────┐
│ Check Gemini Configuration │
└────────┬───────────────────┘
         │
         ├─────────────────────────────┐
         │                             │
         ▼                             ▼
    ┌─────────┐                  ┌──────────┐
    │   NO    │                  │   YES    │
    └────┬────┘                  └─────┬────┘
         │                             │
         ▼                             ▼
┌──────────────────┐           ┌──────────────────┐
│ Show Configure   │           │ Check if shown   │
│ Gemini Overlay   │           │ today already    │
└─────────┬────────┘           └─────┬────────────┘
          │                          │
          │                          ├──────────────┐
          │                          │              │
          │                          ▼              ▼
          │                    ┌─────────┐    ┌─────────┐
          │                    │   YES   │    │   NO    │
          │                    └────┬────┘    └────┬────┘
          │                         │              │
          │                         ▼              ▼
          │                   ┌──────────┐   ┌─────────────┐
          │                   │   STOP   │   │   Continue  │
          │                   └──────────┘   └──────┬──────┘
          │                                         │
          │                                         ▼
          │                                  ┌──────────────┐
          │                                  │ Has Overdue? │
          │                                  └──────┬───────┘
          │                                         │
          │                           ├─────────────┴─────────────┐
          │                           │                           │
          │                           ▼                           ▼
          │                    ┌─────────────┐           ┌──────────────┐
          │                    │   YES       │           │     NO       │
          │                    └──────┬──────┘           └──────┬───────┘
          │                           │                          │
          │                           ▼                          ▼
          │                  ┌─────────────────┐      ┌──────────────────┐
          │                  │ Show Loading    │      │ Show Loading     │
          │                  │ Generate        │      │ Generate         │
          │                  │ Overdue Message │      │ Welcome Message  │
          │                  └────────┬────────┘      └────────┬─────────┘
          │                           │                         │
          │                           ▼                         ▼
          │                  ┌──────────────────┐     ┌──────────────────┐
          │                  │ Show Overdue     │     │ Show Welcome     │
          │                  │ Message Overlay  │     │ Message Overlay  │
          │                  │ (⚠️ Warning)     │     │ (😊 Happy)       │
          │                  └──────────────────┘     └──────────────────┘
          │                           │                         │
          │                           └─────────┬───────────────┘
          │                                     │
          │                                     ▼
          │                            ┌─────────────────┐
          │                            │ Auto-dismiss    │
          │                            │ after 5 seconds │
          │                            └─────────────────┘
          │                                     │
          └─────────────────────────────────────┘
                                       │
                                       ▼
                               ┌───────────────┐
                               │ Mark as shown │
                               │ today         │
                               └───────────────┘
```

---

## Component Interaction

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                          HomeScreen                            ┃
┃  ┌──────────────────────────────────────────────────────────┐  ┃
┃  │  State Management                                        │  ┃
┃  │  • showGeminiMessage                                     │  ┃
┃  │  • showGeminiLoading                                     │  ┃
┃  │  • showConfigureGemini                                   │  ┃
┃  │  • geminiMessage (generated text)                        │  ┃
┃  │  • isGeminiOverdue (message type)                        │  ┃
┃  └──────────────────────────────────────────────────────────┘  ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
           │                      │                      │
           │                      │                      │
           ▼                      ▼                      ▼
    ┌─────────────┐      ┌──────────────┐      ┌──────────────┐
    │  Configure  │      │   Loading    │      │   Message    │
    │   Overlay   │      │   Overlay    │      │   Overlay    │
    └─────────────┘      └──────────────┘      └──────────────┘
           │
           │ Navigate to Settings
           ▼
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                        ProfileScreen                           ┃
┃  ┌──────────────────────────────────────────────────────────┐  ┃
┃  │  Gemini AI Settings Row                                  │  ┃
┃  │  Click → Opens GeminiSettingsDialog                      │  ┃
┃  └──────────────────────────────────────────────────────────┘  ┃
┃  ┌──────────────────────────────────────────────────────────┐  ┃
┃  │  GeminiSettingsDialog                                    │  ┃
┃  │  • Enable/Disable                                        │  ┃
┃  │  • API Key Input                                         │  ┃
┃  │  • Help Instructions                                     │  ┃
┃  │  • Save → GeminiPreferences                              │  ┃
┃  └──────────────────────────────────────────────────────────┘  ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                                  │
                                  ▼
                    ┏━━━━━━━━━━━━━━━━━━━━━━━━┓
                    ┃   GeminiPreferences    ┃
                    ┃  (Encrypted Storage)   ┃
                    ┗━━━━━━━━━━━━━━━━━━━━━━━━┛
                                  ▲
                                  │ Read API Key
                                  │
                    ┏━━━━━━━━━━━━━━━━━━━━━━━━┓
                    ┃   GeminiApiService     ┃
                    ┃   (Network Layer)      ┃
                    ┗━━━━━━━━━━━━━━━━━━━━━━━━┛
                                  │
                                  ▼
                    ┏━━━━━━━━━━━━━━━━━━━━━━━━┓
                    ┃  Google Gemini API     ┃
                    ┃  (External Service)    ┃
                    ┗━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## Message Generation Flow

```
┌────────────┐
│   Start    │
└─────┬──────┘
      │
      ▼
┌──────────────────────────┐
│ getUserName()            │
│ getOverdueCount()        │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ Build Prompt             │
│ • Context: Habit Tracker │
│ • Tone: Based on type    │
│ • Length: 2-3 sentences  │
│ • Personalization: Name  │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ Create Request Body      │
│ {                        │
│   "contents": [{         │
│     "parts": [{          │
│       "text": "{prompt}" │
│     }]                   │
│   }]                     │
│ }                        │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ Send HTTP POST           │
│ • URL: Gemini API        │
│ • Header: API Key        │
│ • Timeout: 30s           │
└────────┬─────────────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌──────────┐
│Success │ │  Error   │
└────┬───┘ └────┬─────┘
     │          │
     ▼          ▼
┌─────────┐ ┌────────────┐
│ Parse   │ │ Log Error  │
│Response │ │ Fail       │
│         │ │ Gracefully │
└────┬────┘ └────────────┘
     │
     ▼
┌──────────────────────────┐
│ Extract Text             │
│ candidates[0]            │
│   .content.parts[0].text │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ Return Result<String>    │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ Display in Overlay       │
└──────────────────────────┘
```

---

**This architecture ensures:**
- ✅ Clean separation of concerns
- ✅ Secure data handling
- ✅ Scalable design
- ✅ User-friendly experience
- ✅ Robust error handling

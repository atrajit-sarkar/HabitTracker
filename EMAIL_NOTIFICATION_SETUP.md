# 📧 Email Notification Setup Guide

This guide will help you set up Gmail SMTP email notifications for the Habit Tracker app.

## 🔐 Security Features

- ✅ **Encrypted Storage**: App password is stored using Android's EncryptedSharedPreferences
- ✅ **Build-time Injection**: Credentials are injected at build time from `keystore.properties`
- ✅ **Git Ignored**: `keystore.properties` is automatically excluded from version control
- ✅ **No Hardcoding**: No credentials are hardcoded in source code

## 📋 Prerequisites

1. A Gmail account
2. Gmail App Password (not your regular Gmail password)

## 🔑 Getting a Gmail App Password

1. **Enable 2-Factor Authentication** on your Gmail account:
   - Go to [Google Account Security](https://myaccount.google.com/security)
   - Enable "2-Step Verification"

2. **Generate an App Password**:
   - Go to [App Passwords](https://myaccount.google.com/apppasswords)
   - Select "Mail" and "Other (Custom name)"
   - Enter "Habit Tracker" as the name
   - Click "Generate"
   - Copy the 16-character password (format: `xxxx xxxx xxxx xxxx`)

## ⚙️ Configuration

### 1. Create/Update `keystore.properties`

Create or edit the file `keystore.properties` in your project root (same level as `build.gradle.kts`):

```properties
# Email Configuration
# If using an alias email (send-as), use your actual Gmail for authentication:
SMTP_AUTH_EMAIL=your_actual_gmail@gmail.com
EMAIL_APP_PASSWORD=your_gmail_app_password_here
EMAIL_FROM_ADDRESS=your_alias@domain.com  # Optional: Your alias email

# GitHub Token (existing)
GITHUB_TOKEN=your_github_token_here

# Release Signing (existing, if you have one)
RELEASE_STORE_FILE=path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

**Important Notes:**
- ✅ Include spaces in the app password (e.g., `xxxx xxxx xxxx xxxx`)
- ✅ This file is already in `.gitignore` and won't be committed
- ✅ Keep this file secure and never share it

### 2. Verify `.gitignore`

Make sure your `.gitignore` includes:

```gitignore
# Keystore properties (SECURITY: Contains passwords!)
keystore.properties
```

This is already configured in your project! ✅

## 🎨 User Setup in App

Users need to configure their email address in the app to receive notifications:

### Settings Screen (To Be Implemented)

1. Navigate to Settings/Profile
2. Find "Email Notifications" section
3. Enter their email address
4. Toggle "Enable Email Notifications" ON
5. Emails will be sent to this address when habits are due

## 🧪 Testing

1. **Build the app**:
   ```bash
   ./gradlew assembleDebug
   ```

2. **Install and run** on your device

3. **Configure user email** in app settings

4. **Create a test habit** with a reminder in the next few minutes

5. **Wait for the reminder time** - you should receive:
   - ✅ In-app notification
   - ✅ Email notification (if configured)

## 📧 Email Features

- **Beautiful HTML Design**: Gradient headers, responsive layout
- **Mobile Optimized**: Works perfectly on Gmail mobile app
- **Deep Links**: Click "Open Habit Tracker" button to go directly to the habit
- **Plain Text Fallback**: For email clients that don't support HTML
- **Personalized**: Includes user's display name
- **Motivational**: Includes inspirational quotes

## 🔧 Technical Details

### Components Created

1. **SecureEmailStorage.kt**: Encrypted storage for email settings
2. **EmailTemplate.kt**: Beautiful HTML email templates
3. **EmailNotificationService.kt**: SMTP email sending service
4. **EmailReminderWorker.kt**: Background worker for reliable delivery
5. **Deep Link Handling**: `habittracker://habit/{habitId}` scheme

### How It Works

1. When a habit reminder triggers (`HabitReminderReceiver`)
2. System shows in-app notification
3. System schedules `EmailReminderWorker` in background
4. Worker sends email via Gmail SMTP (TLS encrypted)
5. Email contains deep link to open habit in app
6. User clicks link → app opens to habit details

### SMTP Configuration

- **Host**: `smtp.gmail.com`
- **Port**: `587`
- **Security**: TLS/STARTTLS
- **Authentication**: App Password
- **Timeout**: 10 seconds

## 🚨 Troubleshooting

### Email Not Sending

1. **Check user configured email**:
   - User must enable email notifications in app
   - User must provide their email address

2. **Check Gmail credentials**:
   - Verify `EMAIL_APP_PASSWORD` in `keystore.properties`
   - Ensure 2FA is enabled on Gmail account
   - Regenerate app password if needed

3. **Check logs**:
   ```bash
   adb logcat | grep EmailNotificationService
   ```

4. **Network issues**:
   - Ensure device has internet connection
   - Check firewall settings
   - Verify SMTP port 587 is not blocked

### Deep Link Not Working

1. **Check manifest registration**:
   - `habittracker://habit` scheme should be registered
   - Already configured in `AndroidManifest.xml` ✅

2. **Test deep link manually**:
   ```bash
   adb shell am start -a android.intent.action.VIEW -d "habittracker://habit/1"
   ```

## 📱 Example Email Preview

```
┌─────────────────────────────────────┐
│       🎯 Habit Reminder            │  (Purple gradient)
├─────────────────────────────────────┤
│  Hi Atrajit! 👋                     │
│  It's time for your habit:          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Morning Meditation          │   │
│  │ Start your day mindfully    │   │
│  │ ⏰ 7:00 AM                  │   │
│  └─────────────────────────────┘   │
│                                     │
│  Ready to complete this habit?      │
│  ┌─────────────────────────────┐   │
│  │   OPEN HABIT TRACKER       │   │  (Purple button)
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

## 🔒 Security Best Practices

✅ **DO**:
- Keep `keystore.properties` private
- Use Gmail App Passwords (not regular passwords)
- Enable 2FA on Gmail account
- Store only what's necessary in BuildConfig

❌ **DON'T**:
- Commit `keystore.properties` to Git
- Share app passwords with anyone
- Use your regular Gmail password
- Hardcode credentials in source code

## 📝 Notes

- Email sending happens in background (doesn't block app)
- Emails are sent only if user enables email notifications
- Deep links work from any email client
- Falls back gracefully if email is not configured
- No emails sent for already-completed habits

## 🎉 You're All Set!

Your Habit Tracker app now has secure, beautiful email notifications! 📧✨

---

**For Issues**: Check logs or contact support
**Version**: 5.0.2+
**Last Updated**: 2025-01-09


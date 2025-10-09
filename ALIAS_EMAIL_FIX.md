# 🔧 Quick Fix: Alias Email Configuration

## Problem
You're getting "Could not convert socket to TLS" error because your alias email is configured as a "send-as" address in your actual Gmail account.

## Solution
Gmail SMTP requires authentication with the **actual Gmail account**, not the alias.

## Update Your `keystore.properties`

**Current (WRONG):**
```properties
EMAIL_ADDRESS=your_alias@domain.com
EMAIL_APP_PASSWORD=your_app_password_here
```

**New (CORRECT):**
```properties
# Your actual Gmail account for SMTP authentication
SMTP_AUTH_EMAIL=your_actual_gmail@gmail.com

# App password for your Gmail account
EMAIL_APP_PASSWORD=your_app_password_here

# The alias that will appear as "From" in emails
EMAIL_FROM_ADDRESS=your_alias@domain.com
```

## Steps to Fix:

1. **Open `keystore.properties`** in your project root
2. **Replace the email section** with the config above
3. **Save the file**
4. **Rebuild the app**:
   ```bash
   .\gradlew.bat clean :app:assembleDebug
   ```
5. **Install**:
   ```bash
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
6. **Test again!**

## How It Works Now:

1. **SMTP Authentication**: Uses your actual Gmail account + app password
2. **From Address**: Emails show as from your alias
3. **Recipients see**: Your alias email address
4. **Gmail knows**: You're authenticated as your actual Gmail account

## Verify Your Alias Setup

Make sure in Gmail (your actual account):
1. Settings → Accounts and Import
2. "Send mail as" should list your alias email
3. If not configured, Gmail might reject the From address

## Need the App Password?

If you don't have the app password for your Gmail account:
1. Go to https://myaccount.google.com/apppasswords
2. Sign in to your actual Gmail account
3. Generate new app password for "Mail" / "Habit Tracker"
4. Copy the password (with spaces)
5. Update `EMAIL_APP_PASSWORD` in keystore.properties

---

After updating keystore.properties and rebuilding, the email should send successfully! 🚀


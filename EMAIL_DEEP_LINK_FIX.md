# Email Deep Link Fix - January 2025

## Issues Fixed

### 1. **Avatar Showing URL Instead of Emoji**
**Problem**: The test email was showing a GitHub URL instead of the habit emoji.

**Solution**: 
- Updated `EmailSettingsViewModel.createTestHabit()` to explicitly set an emoji avatar (✨)
- Ensured the test habit uses `HabitAvatarType.EMOJI` with proper emoji value

### 2. **Button Not Clickable in Gmail**
**Problem**: Gmail blocks custom URL schemes (`habittracker://`) in emails for security reasons.

**Solution**:
- Changed deep links from `habittracker://habit/{id}` to `https://atraj.it/habittracker/habit/{id}`
- Added HTTPS intent filter to AndroidManifest.xml with `android:autoVerify="true"`
- Updated MainActivity to handle both custom scheme and HTTPS deep links
- Gmail now treats the link as a regular HTTPS link and makes it clickable

## Changes Made

### 1. EmailNotificationService.kt
```kotlin
// OLD: return "habittracker://habit/$habitId"
// NEW:
return "https://atraj.it/habittracker/habit/$habitId"
```

### 2. AndroidManifest.xml
Added HTTPS deep link support:
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <data
        android:scheme="https"
        android:host="atraj.it"
        android:pathPrefix="/habittracker" />
</intent-filter>
```

### 3. MainActivity.kt
Added HTTPS deep link handling:
```kotlin
// Supports both:
// - habittracker://habit/{habitId}
// - https://atraj.it/habittracker/habit/{habitId}

if (deepLinkUri.scheme == "https" && deepLinkUri.host == "atraj.it") {
    val pathSegments = deepLinkUri.pathSegments
    if (pathSegments.size >= 3 && pathSegments[0] == "habittracker" && pathSegments[1] == "habit") {
        habitId = pathSegments[2].toLongOrNull()
    }
}
```

### 4. EmailSettingsViewModel.kt
Fixed test habit to use emoji avatar:
```kotlin
avatar = HabitAvatar(
    type = HabitAvatarType.EMOJI,
    value = "✨",
    backgroundColor = "#667eea"
)
```

### 5. EmailTemplate.kt
Improved button styling for better email client compatibility:
```html
<!-- Outlook-compatible button with VML and proper bgcolor -->
<td align="center" style="..." bgcolor="#667eea">
    <a href="$deepLink" style="display: block; ...">
        ✅ OPEN HABIT TRACKER
    </a>
</td>
```

## How It Works

### Deep Link Flow:
1. User receives email with HTTPS link: `https://atraj.it/habittracker/habit/123`
2. Gmail renders it as a clickable link (no security warning)
3. User clicks the link
4. Android opens the link
5. Since your app has an intent filter for `https://atraj.it/habittracker/*`, Android shows your app as an option
6. MainActivity parses the URL and extracts habit ID (123)
7. App navigates directly to habit details screen

### Avatar Display:
- All emails now show the habit's emoji/avatar in:
  - Email header (large 48px emoji)
  - Habit title in card
  - Email subject line
- Test emails show ✨ emoji by default

## Testing

### 1. Test Email with New Avatar
1. Open app → Profile → Email Settings
2. Enable notifications and enter your email
3. Click "Send Test Email"
4. Check your inbox - you should see:
   - ✨ emoji in subject: "✨ Time for: Hello Test"
   - Large ✨ at the top of the email
   - ✨ next to "Hello Test" in the habit card

### 2. Test Clickable Button
1. Open the test email in Gmail app
2. Scroll to the purple button that says "✅ OPEN HABIT TRACKER"
3. The button should be clearly visible and tappable
4. Tap the button
5. Android may show a dialog asking which app to open the link with
6. Select "Habit Tracker" (you can set as default)
7. App should open directly to the habit details

### 3. Test with Real Habit
1. Create a habit with a specific emoji (e.g., 💪 for workout)
2. Set a reminder for 2 minutes from now
3. Wait for the reminder
4. Check your email - it should show 💪 emoji
5. Click the button to open the app

## Important Notes

### Domain Verification (Optional)
For seamless deep linking without the app selection dialog, you need to verify your domain:

1. Create `assetlinks.json` on your web server at:
   ```
   https://atraj.it/.well-known/assetlinks.json
   ```

2. Content should be:
   ```json
   [{
     "relation": ["delegate_permission/common.handle_all_urls"],
     "target": {
       "namespace": "android_app",
       "package_name": "it.atraj.habittracker",
       "sha256_cert_fingerprints": ["YOUR_SHA256_FINGERPRINT"]
     }
   }]
   ```

3. Get your SHA256 fingerprint:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

Without domain verification, users will see an app selection dialog, but the link will still work.

### Fallback Support
The app still supports the old `habittracker://` scheme for backward compatibility, but emails will now use HTTPS links.

## Troubleshooting

### Button Still Not Clickable
- Clear Gmail app cache
- Try opening in Gmail web instead of app
- Check that you have the latest version of the app installed

### App Doesn't Open
- Make sure the app is installed
- Check logcat for "Deep link received:" messages
- Verify the intent filter is properly registered in AndroidManifest

### Wrong Emoji Shows
- Check the habit's avatar settings in the database
- Ensure the habit has an emoji type avatar, not a URL
- For test emails, the emoji is hardcoded to ✨

## Security Considerations

- HTTPS links are more secure than custom schemes
- Gmail trusts HTTPS links from known domains
- Android App Links verification prevents other apps from hijacking your links (with proper domain verification)
- Deep links only expose habit IDs, which are not sensitive

## Future Improvements

1. Set up proper domain verification for seamless opening
2. Add analytics to track email open rates and button clicks
3. Support universal links for iOS when app is ported
4. Add unsubscribe link in email footer


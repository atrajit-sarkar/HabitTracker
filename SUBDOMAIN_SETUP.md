# Subdomain Setup for Deep Links

## Problem Solved

Previously, deep links used `https://atraj.it/habittracker/habit/{id}` which conflicted with your main website (mathematics portfolio). Now we use a subdomain: `https://habittracker.atraj.it/habit/{id}`

## Two Options to Make Deep Links Work:

### Option 1: Set up Subdomain (Recommended)

#### Step 1: Add DNS Record
In your domain provider (where you manage atraj.it), add a DNS record:

```
Type: A or CNAME
Name: habittracker
Value: (same as your main domain, or point to a server)
TTL: 3600
```

#### Step 2: Create `.well-known/assetlinks.json`
On the subdomain `habittracker.atraj.it`, create this file at:
```
https://habittracker.atraj.it/.well-known/assetlinks.json
```

With this content:
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "it.atraj.habittracker",
    "sha256_cert_fingerprints": [
      "YOUR_SHA256_FINGERPRINT_HERE"
    ]
  }
}]
```

#### Step 3: Get Your App's SHA256 Fingerprint

For **debug** build:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256
```

For **release** build (when you publish):
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-key-alias
```

#### Step 4: Test
1. Deploy the `assetlinks.json` file
2. Verify it's accessible: `curl https://habittracker.atraj.it/.well-known/assetlinks.json`
3. Send test email and click the link - it should open your app directly!

---

### Option 2: Use Custom Scheme Only (Simpler, but Button Might Not Work in Gmail)

If you don't want to set up a subdomain, we can revert to using only `habittracker://habit/{id}`. However, Gmail blocks custom schemes in emails, so the button won't be clickable.

To revert, change:
1. `EmailNotificationService.kt`: `return "habittracker://habit/$habitId"`
2. Remove the HTTPS intent filter from `AndroidManifest.xml`
3. Use in-app notifications primarily instead of email buttons

---

## Current Implementation

### Deep Link URLs:
- **Format**: `https://habittracker.atraj.it/habit/{habitId}`
- **Example**: `https://habittracker.atraj.it/habit/123`

### App Handles:
1. Custom scheme: `habittracker://habit/{id}` (fallback)
2. HTTPS: `https://habittracker.atraj.it/habit/{id}` (primary for emails)

### Avatar Handling:
- **Emoji avatars**: Shows emoji in email (e.g., 💪, ✨, 🎯)
- **GitHub URL avatars**: Converts to 🎯 emoji (URLs don't work in plain text emails)
- **Custom image avatars**: Converts to 🎯 emoji

---

## Testing Without Subdomain Setup

Until you set up the subdomain, clicking the button in emails will:
1. Open your browser
2. Try to load `habittracker.atraj.it`
3. Show "Site can't be reached" or redirect to atraj.it
4. Android will NOT show your app as an option

**Workaround for testing**:
1. Use the in-app notification instead (tapping it works perfectly)
2. Or manually open your app and navigate to the habit
3. Once subdomain is set up, the email button will work

---

## Simple Testing Before Full Setup

If you want to test the deep link without setting up DNS:

### Option: Use localhost tunneling (for testing only)
1. Install `ngrok` or similar: `npm install -g localtunnel`
2. Create a simple server:
   ```bash
   mkdir habittracker-server
   cd habittracker-server
   mkdir -p .well-known
   # Create assetlinks.json file
   echo '[{...}]' > .well-known/assetlinks.json
   python -m http.server 8080
   ```
3. Tunnel it: `lt --port 8080 --subdomain habittracker-atraj`
4. Update the code to use the tunnel URL temporarily

---

## Recommended Approach

**For now (development)**:
- Keep the current setup
- Test using in-app notifications (they work perfectly)
- Email reminders will still send, but button will open browser

**For production**:
- Set up `habittracker.atraj.it` subdomain properly
- Add the `assetlinks.json` file with your release keystore fingerprint
- Test thoroughly before publishing to Play Store

---

## Alternative: Use Different Domain

If you don't want to modify atraj.it at all, you could:
1. Buy a cheap domain like `habittracker.app` or `habittracker.me`
2. Point it to a simple static page with the `assetlinks.json`
3. Update the code to use that domain

This keeps your mathematics portfolio completely separate.

---

## Summary

**What Changed**:
- ✅ Avatar URLs (GitHub) now show as 🎯 emoji instead of broken image
- ✅ Deep link changed from `atraj.it/habittracker` to `habittracker.atraj.it`
- ✅ Prevents conflict with your main website

**What You Need to Do**:
1. **(Optional)** Set up `habittracker.atraj.it` subdomain
2. **(Optional)** Add `assetlinks.json` for seamless app opening
3. **For now**: Test with in-app notifications (they work perfectly)

**Current Status**:
- ✅ Emails send successfully
- ✅ Emojis show correctly
- ⚠️ Email button opens browser (until subdomain is set up)
- ✅ In-app notifications open app directly


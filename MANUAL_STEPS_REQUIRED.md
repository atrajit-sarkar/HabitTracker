# 📋 MANUAL STEPS CHECKLIST - Play Store Publication

## 🎯 YOUR IMMEDIATE ACTION ITEMS

### ⚠️ CRITICAL - Must Do Before Building

#### 1. Change Application ID (REQUIRED)
**Why**: Google Play Store rejects apps with `com.example.*` package names

**What to do**:
1. Open `app/build.gradle.kts`
2. Find line 22: `namespace = "com.example.habittracker"`
3. Find line 26: `applicationId = "com.example.habittracker"`
4. Change both to your unique identifier, for example:
   - If you own domain: `com.yourdomain.habittracker`
   - Or use: `dev.yourname.habittracker`
   - Or use: `io.github.yourusername.habittracker`

**Example**:
```kotlin
namespace = "com.atrajitsarkar.habittracker"
applicationId = "com.atrajitsarkar.habittracker"
```

5. Save the file

#### 2. Update Firebase Configuration
**Why**: Firebase needs to know your new package name

**What to do**:
1. Go to https://console.firebase.google.com/
2. Select your Habit Tracker project
3. Click gear icon → Project Settings
4. Scroll to "Your apps" section
5. Click on your Android app
6. Update package name to match your new applicationId
7. Download the new `google-services.json`
8. Replace `app/google-services.json` with the new file
9. Add SHA-1 fingerprint for release build (see step 3)

#### 3. Add Release SHA-1 to Firebase
**Why**: Google Sign-In won't work in production without this

**What to do**:
1. Run this command in your terminal:
```powershell
.\gradlew signingReport
```
2. Look for the "release" variant SHA-1 fingerprint
3. Copy the SHA-1 (looks like: `AB:CD:EF:12:34:...`)
4. Go to Firebase Console → Project Settings → Your App
5. Click "Add fingerprint"
6. Paste the SHA-1
7. Save

---

## 🏗️ BUILD YOUR RELEASE

### Step 1: Clean Build
```powershell
.\gradlew clean
```

### Step 2: Build Release AAB (Android App Bundle)
```powershell
.\gradlew bundleRelease
```

**Output location**: `app/build/outputs/bundle/release/app-release.aab`

### Step 3: Test Release Build (Optional but Recommended)
Build an APK to test locally:
```powershell
.\gradlew assembleRelease
```
**Output location**: `app/build/outputs/apk/release/app-release.apk`

Install on device:
```powershell
adb install app/build/outputs/apk/release/app-release.apk
```

**Test checklist**:
- [ ] App launches without crashes
- [ ] Google Sign-In works
- [ ] Habits can be created and edited
- [ ] Notifications appear at set times
- [ ] Data syncs to Firebase
- [ ] Profile features work
- [ ] Social/chat features work (if enabled)

---

## 🌐 CREATE PRIVACY POLICY

### Option 1: Use Free Generator
1. Go to: https://app-privacy-policy-generator.firebaseapp.com/
2. Fill in your app details:
   - App name: Habit Tracker
   - Your email: [your email]
   - Data collected: Email, Name, Profile Picture, Habit Data, Chat Messages
   - Third-party services: Firebase, Google Sign-In
3. Generate the policy
4. Copy the HTML
5. Host it somewhere (GitHub Pages, your website, etc.)
6. Save the URL (you'll need it for Play Console)

### Option 2: Use the Template
1. Open `PRIVACY_POLICY_TEMPLATE.md` in this folder
2. Replace all placeholders:
   - `[YOUR_EMAIL_ADDRESS]` → your support email
   - `[YOUR_WEBSITE_URL]` → your website (or remove)
   - Update any other details
3. Convert to HTML or publish as-is on a website
4. Get the public URL

**Where to host for free**:
- GitHub Pages (create a repo, enable Pages)
- Firebase Hosting (if you're using Firebase)
- Google Sites
- Notion (make page public)

---

## 📸 CREATE STORE ASSETS

### Assets You Need:

#### 1. App Icon (You Already Have ✓)
- Location: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
- Need: Extract as 512x512 PNG for Play Console

#### 2. Feature Graphic (REQUIRED)
**Size**: 1024 x 500 pixels
**Format**: PNG or JPEG

**What to include**:
- App name: "Habit Tracker"
- App icon
- Key feature icons or screenshot
- Tagline: "Build Better Habits Daily"

**Tools to create**:
- Canva (free templates)
- Figma
- Photoshop
- GIMP (free)

**Save as**: `feature-graphic.png`

#### 3. Screenshots (MINIMUM 2, RECOMMENDED 4-8)
**Size**: 1080 x 2340 pixels (or your device resolution)

**How to capture**:
1. Install your app on a phone
2. Open the app
3. Take screenshots of:
   - Home screen with habits
   - Add/Edit habit screen
   - Habit details with chart
   - Profile screen
   - Settings screen
   - Any other interesting features

4. Transfer screenshots to PC
5. Place in a folder: `screenshots/`

**Tips**:
- Use a clean device with good data
- Show actual habit names (not "Test 1, Test 2")
- Make it look appealing
- Show variety of features

#### 4. Promotional Video (OPTIONAL)
- 30-90 seconds
- Upload to YouTube
- Copy the link

---

## 🎮 GOOGLE PLAY CONSOLE SETUP

### PART 1: Create Developer Account

1. **Go to**: https://play.google.com/console/signup

2. **Sign in** with your Google account

3. **Pay $25 fee** (one-time, never expires)
   - Use credit/debit card
   - Keep receipt

4. **Fill account details**:
   - Developer name (public): Your name or company
   - Email: Your contact email
   - Website: (optional)
   - Phone: Your number

5. **Choose account type**:
   - **Personal**: For individuals (easier)
   - **Organization**: For companies (requires verification)

6. **Accept agreements** and submit

7. **Wait for verification** (usually instant, can take 48 hours)

### PART 2: Create Your App

1. **Click "Create app"** in Play Console

2. **Fill basic info**:
   - **App name**: Habit Tracker
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free

3. **Declarations**:
   - "Is this app targeted at children?" → **No** (unless it is)

4. **Click "Create app"**

### PART 3: Complete Store Listing

Navigate to **"Store presence" → "Main store listing"**

Fill in ALL required fields:

1. **App name**: Habit Tracker

2. **Short description** (80 chars):
```
Build better habits with reminders, tracking, and progress analytics
```

3. **Full description**:
   - Copy from `PLAY_STORE_SETUP_GUIDE.md` (search for "Full Description")
   - Customize as needed

4. **App icon**: Upload 512x512 PNG

5. **Feature graphic**: Upload your 1024x500 graphic

6. **Phone screenshots**: Upload at least 2 (recommended 4-8)

7. **Category**: Choose **Productivity** or **Lifestyle**

8. **Tags**: Add relevant keywords:
   - habit tracker
   - productivity
   - goals
   - routines
   - self improvement

9. **Contact email**: Your support email

10. **Privacy policy URL**: Paste your hosted privacy policy URL

11. **Click "Save"**

### PART 4: Set Up App Content

#### A. Privacy Policy
- Already done in store listing ✓

#### B. Content Rating
1. Go to **"Policy" → "App content" → "Content rating"**
2. Click **"Start questionnaire"**
3. Enter your email
4. Select app category: **Utility, Productivity, Communication, or Other**
5. Answer all questions honestly:
   - Violence: No
   - Sexual content: No
   - Language: No
   - Drugs: No
   - Gambling: No
   - User-generated content: **Yes** (you have chat)
6. Submit
7. You'll likely get: **Everyone** or **Everyone 10+**

#### C. Data Safety
1. Go to **"Policy" → "App content" → "Data safety"**
2. Click **"Start"**

**Data collected**:
- ✅ **Personal info**:
  - Name
  - Email address
  - User IDs
  
- ✅ **App activity**:
  - In-app actions (habit completions)
  - Other user-generated content (habits, chat)

- ✅ **App info and performance**:
  - Crash logs
  - Diagnostics

**Data usage**:
- App functionality
- Account management
- Analytics

**Data sharing**:
- Shared with Firebase (Google)
- Not sold to third parties

**Security practices**:
- ✅ Data encrypted in transit
- ✅ Users can request data deletion
- ✅ Committed to Play Families Policy (if applicable)

3. Fill all sections
4. Click **"Save"** and then **"Submit"**

#### D. Target Audience
1. Go to **"Policy" → "App content" → "Target audience"**
2. Select age groups: **13 and older** (recommended)
3. Submit

#### E. News App
1. Go to **"Policy" → "App content" → "News app"**
2. Select **"No"**
3. Submit

#### F. COVID-19 Contact Tracing
1. Go to **"Policy" → "App content" → "COVID-19 contact tracing"**
2. Select **"No"**
3. Submit

#### G. Data Deletion
1. Go to **"Policy" → "App content" → "Data deletion"**
2. Provide instructions or link
3. Example: "Users can delete their account and data in the app Settings → Profile → Delete Account"

#### H. Government App (if applicable)
- Select **"No"** (unless you're a government entity)

#### I. Ads Declaration
1. Go to **"Policy" → "App content" → "Ads"**
2. Select: **"No, my app does not contain ads"**
3. Submit

### PART 5: Set Countries and Pricing

1. Go to **"Grow" → "Countries/regions"**
2. Choose countries:
   - **Option 1**: Select all countries
   - **Option 2**: Start with your country, expand later
3. Confirm: **Free** (your app is free)
4. Click **"Save"**

### PART 6: Upload Your App

#### A. Set Up App Signing

1. Go to **"Release" → "Setup" → "App signing"**
2. Choose: **"Use Google-generated key"** (RECOMMENDED)
   - Google manages signing
   - More secure
   - Can't lose your key
3. Accept terms
4. Continue

#### B. Create Production Release

1. Go to **"Release" → "Production"**
2. Click **"Create new release"**

3. **Upload app bundle**:
   - Click **"Upload"**
   - Select: `app/build/outputs/bundle/release/app-release.aab`
   - Wait for upload and processing

4. **Release name**: Auto-filled with version (4.0.0)

5. **Release notes**:
```
🎉 Initial release of Habit Tracker!

✨ Features:
• Create habits with daily, weekly, monthly, or yearly schedules
• Set custom reminders with notification sounds
• Track your progress with beautiful statistics and charts
• Build streaks to stay motivated
• Personalize habits with avatars, emojis, and colors
• Cloud sync with Google Sign-In (optional)
• Social features to connect with friends
• Real-time chat for accountability
• Light and dark mode support
• Multi-language support

Start building better habits today! 🚀
```

6. **Click "Next"**

7. **Review rollout**:
   - Can choose staged rollout (20% → 50% → 100%)
   - Or full rollout immediately

8. **Click "Start rollout to production"**

### PART 7: Final Review

1. **Check for warnings/errors**:
   - All must be resolved before publishing
   - Green checkmarks everywhere

2. **Submit for review**:
   - Click final confirmation
   - Google will review (2-7 days typically)

3. **Wait for approval**:
   - You'll get email notifications
   - Check Play Console for status

---

## 📱 POST-SUBMISSION

### What Happens Next?

1. **Google reviews your app** (2-7 days)
   - Automated scans
   - Manual policy review
   - Security checks

2. **Possible outcomes**:
   - ✅ **Approved**: App goes live!
   - ⚠️ **Needs changes**: Fix and resubmit
   - ❌ **Rejected**: Address violations and resubmit

3. **If approved**:
   - App appears in Play Store
   - Users can search and download
   - You get a Play Store link

### Monitor Your App

**Daily (first week)**:
- Check for crashes in Firebase Crashlytics
- Monitor reviews and ratings
- Respond to user feedback
- Check Play Console for issues

**Weekly**:
- Review install statistics
- Check retention rates
- Analyze user feedback trends
- Plan updates

**Monthly**:
- Release updates and improvements
- Update store listing if needed
- Review analytics
- Engage with community

---

## 🎉 YOUR APP IS LIVE!

### Share Your App

**Play Store Link**:
```
https://play.google.com/store/apps/details?id=YOUR_PACKAGE_NAME
```

**Share on**:
- Social media
- Reddit (r/androidapps)
- Friends and family
- Tech communities
- Your website

### Update Your App

When releasing updates:

1. Update `versionCode` and `versionName` in `build.gradle.kts`
2. Build new AAB: `.\gradlew bundleRelease`
3. Go to Play Console → Production
4. Create new release
5. Upload new AAB
6. Add release notes (what's new)
7. Submit

---

## 📞 SUPPORT RESOURCES

**Google Play Console Help**:
https://support.google.com/googleplay/android-developer

**Developer Policies**:
https://play.google.com/about/developer-content-policy/

**App Bundle Guide**:
https://developer.android.com/guide/app-bundle

**If You Get Rejected**:
- Read the rejection reason carefully
- Fix the specific issues mentioned
- Respond to Google if clarification needed
- Resubmit

---

## ✅ FINAL CHECKLIST

Before clicking "Submit for Review":

- [ ] Application ID changed from com.example.*
- [ ] Firebase updated with new package name
- [ ] Release SHA-1 added to Firebase
- [ ] Release AAB built and tested
- [ ] Privacy policy created and hosted
- [ ] Feature graphic created (1024x500)
- [ ] At least 4 screenshots captured
- [ ] Developer account created and paid
- [ ] Store listing completed
- [ ] Content rating submitted
- [ ] Data safety completed
- [ ] Target audience selected
- [ ] Countries selected
- [ ] App bundle uploaded
- [ ] Release notes written
- [ ] All sections show green checkmarks
- [ ] Ready to launch!

---

**Good luck! You've got this! 🚀**

Need help? Check the detailed guide in `PLAY_STORE_SETUP_GUIDE.md`

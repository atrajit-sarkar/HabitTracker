# Custom Avatar Images in Emails - Issue & Solution

## Problem
Custom uploaded habit avatars display correctly in the app but show as "Image not found" or fallback emojis in emails.

## Root Cause
Custom avatar images are uploaded to GitHub repository and stored as URLs. However:

1. **Public Repository**: Images are accessible via `https://raw.githubusercontent.com/...` URLs - **These WORK in emails**
2. **Private Repository**: Images are accessible via `https://raw.githubusercontent.com/...` BUT require GitHub authentication token - **These DON'T WORK in emails**

Email clients (Gmail, Outlook, etc.) cannot access images that require authentication headers.

## Current Behavior

### In the App ✅
- Custom images load perfectly
- Coil image loader adds authentication headers automatically
- Code in `HabitAvatarPickerDialog.kt` (line 354):
  ```kotlin
  if (token != null) {
      requestBuilder.addHeader("Authorization", "token $token")
  }
  ```

### In Emails ❌
- Email clients don't send authentication headers
- Private repo images fail to load
- Public repo images work fine

## Solution Implemented

**Smart Fallback Emoji System** in `EmailTemplate.kt`:

When a custom image cannot be displayed in email (local file or private URL), the system:

1. **Tries to extract emoji from habit title**
   - Example: "🏃 Morning Run" → Shows 🏃

2. **Uses keyword-based intelligent fallback**
   - "exercise", "workout", "gym" → 💪
   - "read", "book" → 📚  
   - "meditate", "yoga" → 🧘
   - "water", "drink" → 💧
   - 20+ keyword mappings

3. **Default fallback** → 🎯

### Code Example:
```kotlin
private fun getSmartFallbackEmoji(habit: Habit): String {
    // Try to extract emoji from title first
    val titleEmoji = habit.title.firstOrNull { 
        it.toString().matches(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")) 
    }
    if (titleEmoji != null) {
        return titleEmoji.toString()
    }
    
    // Use keyword-based fallback
    val lowerTitle = habit.title.lowercase()
    return when {
        lowerTitle.contains("exercise") -> "💪"
        lowerTitle.contains("read") -> "📚"
        // ... more mappings
        else -> "🎯"
    }
}
```

## Complete Fix Options

### Option 1: Make Repository Public ✅ **BEST FOR PUBLIC APPS**
**Pros:**
- Images work everywhere (app + email)
- No authentication needed
- Simpler architecture

**Cons:**
- Images are publicly accessible
- Not suitable if users upload sensitive content

**How to do it:**
1. Go to GitHub repo: `gongobongofounder/habit-tracker-avatar-repo`
2. Settings → Danger Zone → Change visibility to Public
3. That's it! All existing URLs will work

### Option 2: Use Public CDN (Imgur, Cloudinary) ✅ **BEST FOR SCALING**
**Pros:**
- Designed for public image hosting
- Better performance and caching
- Free tiers available

**Cons:**
- Requires API integration
- Third-party dependency

**Services:**
- Imgur API
- Cloudinary
- AWS S3 with public buckets
- Azure Blob Storage

### Option 3: Keep Smart Fallback (Current) ✅ **BEST FOR NOW**
**Pros:**
- Works immediately
- No infrastructure changes
- Intelligent emoji selection

**Cons:**
- Emails show emoji instead of custom image
- Not as personalized

## Repository Check

To verify if your repository is public or private:

```bash
curl https://raw.githubusercontent.com/gongobongofounder/habit-tracker-avatar-repo/main/README.md
```

- **If it works**: Repository is PUBLIC → Custom images should work in emails
- **If it fails (404)**: Repository is PRIVATE → Smart fallback will be used

## Testing

### Test Custom Image in Email:
1. Upload a custom habit avatar in the app
2. Set a reminder for that habit
3. Wait for the email
4. Check if image displays or fallback emoji is used

### Check Avatar URL Format:
Look at the Firestore habit document:
```json
{
  "avatar": {
    "type": "CUSTOM_IMAGE",
    "value": "https://raw.githubusercontent.com/gongobongofounder/habit-tracker-avatar-repo/main/avatars/habits/{userId}/avatar_123456.png"
  }
}
```

If the URL is publicly accessible, it will work in emails!

## Recommendation

**For your app**, I recommend **Option 1: Make Repository Public**

Why?
- Habit avatars are generally not sensitive
- Users see them in social features anyway
- Simplest solution
- No code changes needed
- Works immediately

Just make the GitHub repository public, and all custom avatars will display perfectly in emails! 🎉

---

**Current Status**: Smart fallback implemented and working  
**Version**: 5.0.3+  
**Date**: 2025-01-09


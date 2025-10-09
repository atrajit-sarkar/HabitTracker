# Email Avatar Images Support

## Feature Overview

The email notification system now fully supports displaying custom habit avatars, including GitHub-hosted images, in beautiful HTML emails.

## How It Works

### Avatar Types Supported:

1. **Emoji Avatars** (🎯, 💪, ✨, etc.)
   - Displayed as large emoji text in emails
   - Works in both HTML and plain text emails
   - Emoji size: 64px in header, 32px in title

2. **GitHub URL Images**
   - If avatar.value contains a URL (e.g., `https://raw.githubusercontent.com/...`)
   - Rendered as `<img>` tag in HTML emails
   - Circular with 64px size in header, 32px in title
   - Falls back to 🎯 emoji in plain text emails

3. **Custom Image Avatars** (CUSTOM_IMAGE type)
   - Similar to GitHub URLs
   - Shows as `<img>` tag if value is a valid URL
   - Circular, responsive, with object-fit: cover

## Implementation Details

### Functions Created:

```kotlin
// Returns emoji text or null for images
private fun getHabitEmojiText(habit: Habit): String?

// Returns HTML (emoji text or <img> tag)
private fun getHabitAvatarHtml(habit: Habit, size: String = "48px"): String

// Returns emoji for plain text (no images)
private fun getHabitAvatarPlainText(habit: Habit): String
```

### HTML Email Structure:

#### Header (Large Avatar - 64px):
```html
<div style="font-size: 64px; margin-bottom: 10px;">
    <!-- Either emoji or <img> tag -->
    $habitAvatarHeader
</div>
```

#### Title Card (Small Avatar - 32px):
```html
<h2 style="...">
    <span>$habitAvatarSmall</span>
    <span>${habit.title}</span>
</h2>
```

### Image Styling:
```css
width: [size]; 
height: [size]; 
border-radius: 50%; 
object-fit: cover;
```

## Examples

### Example 1: Emoji Avatar (💪)
```
Header: 💪 (64px emoji)
Title: 💪 Morning Workout
Subject: 💪 Time for: Morning Workout
```

### Example 2: GitHub Image URL
```
Header: [Circular image 64px] 
Title: [Small circular image 32px] Morning Workout
Subject: 🎯 Time for: Morning Workout (emoji fallback)
```

**HTML:**
```html
<img src="https://raw.githubusercontent.com/user/repo/main/avatar.png" 
     alt="Habit Avatar" 
     style="width: 64px; height: 64px; border-radius: 50%; object-fit: cover;" />
```

## Email Client Compatibility

### ✅ Supported:
- **Gmail** (Web & Mobile) - Images load perfectly
- **Outlook** (Web & Desktop) - Images display correctly
- **Apple Mail** (iOS & macOS) - Full support
- **Yahoo Mail** - Works well
- **ProtonMail** - Supports external images

### ⚠️ Notes:
- Some email clients block external images by default
- Users need to "Show Images" or trust the sender
- Plain text emails always show emoji fallback (🎯)

## Testing

### Test with Emoji Avatar:
1. Create habit with emoji (e.g., ✨)
2. Send test email
3. Email shows: ✨ in header and title

### Test with GitHub Image:
1. Create habit with custom image
2. Set avatar.value to GitHub raw URL:
   ```
   https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_3_creative.png
   ```
3. Set avatar.type to CUSTOM_IMAGE or EMOJI (works for both)
4. Send test/scheduled email
5. Email shows: Circular image in header and title

## GitHub Image URLs

### Format for GitHub Raw Content:
```
https://raw.githubusercontent.com/[username]/[repo]/[branch]/[path]/[filename]
```

### Example:
```
https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_1_reading.png
```

### Requirements:
- Repository must be **public** (or use authenticated URLs)
- File must be accessible via HTTPS
- Image formats: PNG, JPG, GIF, SVG
- Recommended size: 256x256px minimum

## Image Best Practices

### For Best Email Display:

1. **Image Size**: 
   - Upload at least 256x256px
   - Emails will display at 64px (header) and 32px (title)
   - High DPI displays will use full resolution

2. **File Format**:
   - **PNG** with transparency - Best choice
   - **JPG** - Good for photos
   - **SVG** - Scalable but some email clients don't support

3. **File Size**:
   - Keep under 100KB for fast loading
   - Optimize images before uploading

4. **Repository**:
   - Use public GitHub repo or CDN
   - Ensure stable URLs (don't delete/rename files)

## Fallback Strategy

The system uses a smart fallback approach:

```
1. Check avatar type
   ├─ EMOJI → Check if URL
   │   ├─ Is URL → Show as <img>
   │   └─ Not URL → Show emoji text
   ├─ CUSTOM_IMAGE → Show as <img>
   └─ Other → Show 🎯
   
2. Plain text email
   └─ Always use emoji (🎯 for images)
   
3. Subject line
   └─ Always use emoji (🎯 for images)
```

## Security Considerations

### Image URLs:
- ✅ Only loads images from HTTPS URLs
- ✅ No JavaScript in image URLs
- ✅ Uses standard `<img>` tags
- ✅ Alt text for accessibility

### Privacy:
- Image URLs may leak to email providers
- GitHub tracks image requests (analytics)
- Consider using CDN for privacy

## Troubleshooting

### Images Not Showing:

1. **Check URL accessibility**:
   ```bash
   curl -I https://raw.githubusercontent.com/user/repo/main/avatar.png
   ```
   Should return `200 OK`

2. **Email client blocking images**:
   - User needs to "Show Images"
   - Add sender to trusted contacts

3. **GitHub repository is private**:
   - Make repository public, or
   - Use public CDN like Imgur, Cloudinary

4. **URL formatting**:
   - Must start with `https://`
   - No spaces in URL
   - File extension required

### Email Shows 🎯 Instead of Image:

- Check `avatar.type` is EMOJI or CUSTOM_IMAGE
- Verify `avatar.value` starts with `https://`
- Test URL in browser first

## Future Enhancements

Potential improvements:

1. **Image Caching**: Cache GitHub images on your server
2. **Image Proxy**: Proxy images through your domain for privacy
3. **Multiple Sizes**: Generate thumbnails for faster loading
4. **GIF Support**: Add animation support for GIF avatars
5. **Video Thumbnails**: Support video avatar thumbnails

## Summary

✅ **What Works Now**:
- Emoji avatars display beautifully
- GitHub-hosted images show as circular avatars
- Custom image URLs work in HTML emails
- Smart fallback to emoji in plain text
- Responsive sizing (64px header, 32px title)

🎯 **Default Behavior**:
- Unknown/broken images → 🎯 emoji
- Plain text emails → Always emoji
- Subject lines → Always emoji

📧 **Email Client Support**:
- Full support in modern email clients
- Graceful degradation in old clients
- Accessibility with alt text


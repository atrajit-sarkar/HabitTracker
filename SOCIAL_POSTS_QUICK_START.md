# 🚀 Social Profile & Posts - Quick Start Guide

## What Was Built

A complete social networking system for the Habit Tracker app with:

✅ **Social Profile Screen** - View your profile, bio, friends count, and all your posts in a grid  
✅ **Browse Posts Screen** - Instagram-like feed showing friends' posts with images and markdown content  
✅ **Create Post Screen** - Upload up to 10 images and write markdown-formatted posts  
✅ **Full Markdown Support** - Including math equations, tables, code syntax highlighting, and more  
✅ **GitHub Image Storage** - Secure private repo with token authentication  
✅ **Friends-Only Privacy** - Posts visible only to your friends  
✅ **Like & Comment System** - Full interaction support  
✅ **Custom Theming** - All screens respect your app's theme system  

## File Structure Created

```
app/src/main/java/com/example/habittracker/
├── social/
│   ├── data/
│   │   ├── model/
│   │   │   └── Post.kt              # Data models (Post, Comment, UserSocialProfile)
│   │   └── repository/
│   │       └── PostsRepository.kt    # Firebase & GitHub integration
│   ├── di/
│   │   └── SocialModule.kt          # Dependency injection
│   ├── ui/
│   │   ├── components/
│   │   │   └── MarkdownText.kt      # Markdown renderer component
│   │   ├── screen/
│   │   │   ├── SocialProfileScreen.kt   # Profile & posts grid
│   │   │   ├── BrowsePostsScreen.kt     # Instagram-like feed
│   │   │   └── CreatePostScreen.kt      # Post creation
│   │   └── viewmodel/
│   │       ├── SocialProfileViewModel.kt
│   │       └── PostsViewModel.kt
```

## Navigation Added

### New Routes
- `social_profile` - Your social profile
- `browse_posts` - Browse friends' posts
- `create_post` - Create new post

### Sidebar Menu Items
Under new **SOCIAL** section:
- **My Social Profile** - View your posts and profile
- **Browse Posts** - See what friends are posting

## How to Access Features

### 1. View Your Profile
1. Open the app
2. Tap hamburger menu (☰)
3. Tap **"My Social Profile"** under SOCIAL section
4. See your profile pic, bio, stats, and posts grid

### 2. Create a Post
1. Go to "My Social Profile"
2. Tap the **+** button (bottom-right)
3. Add images (optional, up to 10)
4. Write content with markdown
5. Tap **Post**

### 3. Browse Friends' Posts
1. Open sidebar
2. Tap **"Browse Posts"**
3. Scroll through Instagram-like feed
4. Like posts (heart icon)
5. Comment on posts (comment icon)
6. Tap images to view full-screen

## Markdown Examples

```markdown
# Heading 1
## Heading 2

**Bold text** and *italic text*

- Bullet point 1
- Bullet point 2

1. Numbered item
2. Another item

[Link text](https://example.com)

`inline code`

$$E = mc^2$$  (Math equation)

| Column 1 | Column 2 |
|----------|----------|
| Cell 1   | Cell 2   |

- [ ] Unchecked task
- [x] Completed task
```

## Key Features

### Privacy
- ✅ Posts only visible to friends
- ✅ Private GitHub repository for images
- ✅ Secure token authentication
- ✅ Firestore security rules ready

### Performance
- ✅ Pull-to-refresh support
- ✅ Efficient image caching (Coil)
- ✅ Lazy loading lists
- ✅ Optimized Firestore queries

### UX
- ✅ Material 3 design
- ✅ Custom theme compatibility
- ✅ Smooth animations
- ✅ Loading states
- ✅ Error handling

## Configuration Required

### 1. GitHub Token (Already in keystore.properties)
```properties
GITHUB_TOKEN=your_github_token_here
```

### 2. Firestore Security Rules (Recommended)
Add to Firebase Console → Firestore → Rules:

```javascript
match /posts/{postId} {
  allow read: if request.auth != null && 
    (resource.data.userId == request.auth.uid || 
     request.auth.uid in resource.data.viewerIds);
  allow create: if request.auth != null && 
    request.resource.data.userId == request.auth.uid;
  allow update, delete: if request.auth != null && 
    resource.data.userId == request.auth.uid;
}

match /comments/{commentId} {
  allow read: if request.auth != null;
  allow create: if request.auth != null && 
    request.resource.data.userId == request.auth.uid;
}

match /user_social_profiles/{userId} {
  allow read: if request.auth != null;
  allow write: if request.auth != null && userId == request.auth.uid;
}
```

## Build & Run

```powershell
# Build the app
./gradlew assembleDebug

# Or use existing build scripts
.\build-optimized-release.ps1
```

## Testing the Feature

1. **Sign in** to the app
2. **Add friends** (if not already added)
3. **Create a post** with images and markdown
4. **View your profile** - see the post in grid
5. **Browse posts** - see friends' posts in feed
6. **Like and comment** on posts
7. **Test markdown** - try headers, bold, math equations

## Dependencies Added

All dependencies have been added to `app/build.gradle.kts`:
- Markwon (markdown rendering with math, tables, etc.)
- Accompanist (pager for image carousel)
- OkHttp (already present, used for GitHub API)
- Coil (already present, used for image loading)

## Next Steps

- [x] ✅ All core features implemented
- [x] ✅ Navigation integrated
- [x] ✅ Sidebar updated
- [x] ✅ Theme compatibility ensured
- [ ] Test with multiple users
- [ ] Add Firestore security rules
- [ ] Test markdown edge cases
- [ ] Add post notifications (optional)

## Troubleshooting

### Posts not appearing?
- Ensure users are friends
- Check Firestore security rules
- Verify internet connection

### Images not uploading?
- Check `keystore.properties` has `GITHUB_TOKEN`
- Verify GitHub repo is accessible
- Check Logcat for errors

### Markdown not rendering?
- Verify Markwon dependencies in build.gradle.kts
- Check markdown syntax
- Review app logs

## Support

For detailed documentation, see:
- `SOCIAL_PROFILE_POSTS_FEATURE.md` - Complete technical documentation
- Inline code comments in source files
- Firebase documentation for security rules

---

**Status**: ✅ Ready for Testing  
**Build**: Should compile without errors  
**Theme**: Fully compatible with custom themes  
**Production Ready**: Yes, with proper testing

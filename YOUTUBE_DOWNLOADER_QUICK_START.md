# YouTube Downloader - Quick Start Guide

## 🚀 Quick Access

### From the App
1. Open **Habit Tracker** app
2. Go to **Profile** (bottom navigation)
3. Scroll down to **Settings** section
4. Tap **"YouTube Downloader"** button
5. Start downloading! 🎉

## 📱 How to Download a Video

### Step-by-Step Process

#### 1. Get YouTube URL
- Open YouTube app or website
- Find the video you want to download
- Tap **Share** button
- Copy the link

#### 2. Paste URL in App
- Open YouTube Downloader screen
- Paste URL in the text field
- Tap **"Fetch Video Info"** button
- Wait for metadata to load (~2-3 seconds)

#### 3. Preview Video Info
You'll see:
- 📸 **Thumbnail** - Video preview image
- 🎬 **Title** - Full video title
- 👤 **Channel** - Creator name
- ⏱️ **Duration** - Video length
- 👁️ **Views** - View count
- 🎵 **Streams** - Available quality options

#### 4. Choose Format
Select your preferred format:
- **MP3** (🎵 Audio Only)
  - Best for music, podcasts, audiobooks
  - Smaller file size
  - Quick downloads
  
- **MP4** (🎥 Video + Audio)
  - Full video experience
  - Larger file size
  - Includes visuals

#### 5. Start Download
- Tap **"Download as MP3"** or **"Download as MP4"**
- Watch the progress bar
- Download speed and size are shown in real-time

#### 6. Download Complete!
- Success message appears
- File is saved to your device
- Location: `/Android/data/it.atraj.habittracker/files/YouTubeDownloads/`

## 💡 Pro Tips

### URL Formats Supported
All these work perfectly:
```
✅ https://www.youtube.com/watch?v=dQw4w9WgXcQ
✅ https://youtu.be/dQw4w9WgXcQ
✅ https://www.youtube.com/shorts/ABC123
✅ https://m.youtube.com/watch?v=dQw4w9WgXcQ
✅ https://www.youtube.com/embed/dQw4w9WgXcQ
```

### Best Practices
- ✅ Use WiFi for large downloads (video files can be 50MB+)
- ✅ MP3 format recommended for music (5-10MB average)
- ✅ Check video preview before downloading
- ✅ Cancel anytime using the ❌ button
- ✅ Wait for "Success" message before closing

### Common Issues & Solutions

#### ❌ "Invalid YouTube URL"
- **Fix:** Make sure you copied the complete URL
- Check for extra spaces or characters

#### ❌ "Failed to extract video info"
- **Fix:** Check your internet connection
- Try a different video
- Some videos may be restricted

#### ❌ "Download failed"
- **Fix:** Ensure stable internet connection
- Free up storage space
- Try again later

#### ⚠️ Download stuck at 0%
- **Fix:** Cancel and restart download
- Check internet speed
- Switch between WiFi and mobile data

## 🎯 Example Use Cases

### 1. Download Music
```
1. Find song on YouTube
2. Copy link
3. Open downloader
4. Choose MP3
5. Enjoy offline! 🎵
```

### 2. Download Educational Content
```
1. Copy tutorial/lecture link
2. Open downloader
3. Choose MP4 (to see visuals)
4. Study offline! 📚
```

### 3. Save Podcast Episode
```
1. Find podcast on YouTube
2. Copy link
3. Open downloader
4. Choose MP3
5. Listen anywhere! 🎙️
```

## 📊 Feature Comparison

| Feature | MP3 (Audio) | MP4 (Video) |
|---------|-------------|-------------|
| File Size | Small (5-15MB) | Large (20-200MB) |
| Quality | Best audio | Full video |
| Use Case | Music, podcasts | Tutorials, movies |
| Speed | Fast ⚡ | Slower 🐢 |
| Bandwidth | Low 📶 | High 📶📶📶 |

## 🔄 Download Progress Explained

### What You'll See
```
Downloading... [Cancel ❌]
━━━━━━━━━━━━━━━━━━━━ 45%

2.1 MB / 4.7 MB
Speed: 1.2 MB/s
```

### What It Means
- **Percentage**: How much is downloaded
- **Size**: Current / Total file size
- **Speed**: How fast it's downloading
- **Time**: Estimated time remaining (calculated)

### Cancel Anytime
- Tap the **❌** button
- Download stops immediately
- Partial files are deleted
- No charge to storage

## 🎨 UI Guide

### Color Coding
- 🔵 **Blue/Primary**: Main actions (Download button)
- 🟢 **Green**: Success messages
- 🔴 **Red**: Error messages, Cancel button
- 🟣 **Purple/Secondary**: Format selection
- ⚫ **Gray**: Info cards, thumbnails

### Icons Meaning
- 🔗 **Link**: URL input
- 🔍 **Search**: Fetch video info
- 📥 **Download**: Start download
- ⏸️ **Pause**: Not yet supported (coming soon!)
- ❌ **Close/Cancel**: Stop download
- ✅ **Check**: Success

## 📂 Finding Your Downloads

### Default Location
```
Internal Storage
└── Android
    └── data
        └── it.atraj.habittracker
            └── files
                └── YouTubeDownloads
                    ├── Song_Title_1729612345678.m4a
                    ├── Video_Title_1729612456789.mp4
                    └── ...
```

### How to Access
1. Open **Files** app on your device
2. Go to **Internal Storage**
3. Navigate to path above
4. Find your downloaded files

### File Manager Access
- Use any file manager app (Google Files, etc.)
- Search for file name
- Files are in standard audio/video formats
- Play with any media player

## ⚠️ Important Disclaimers

### Legal Notice
- ⚠️ Downloading copyrighted content may be illegal in your region
- ✅ Only download content you have rights to
- ✅ Respect creators and copyright laws
- ✅ Use for personal, educational purposes only

### YouTube Terms
- This feature may violate YouTube's Terms of Service
- Use responsibly and at your own risk
- Consider supporting creators through official channels
- Recommend YouTube Premium for official offline viewing

## 🆘 Need Help?

### Troubleshooting Steps
1. **Check internet connection**
2. **Try a different video**
3. **Restart the app**
4. **Clear app cache** (Settings → Apps → Habit Tracker → Clear Cache)
5. **Update to latest version**

### Still Having Issues?
- Check the main documentation: `YOUTUBE_DOWNLOADER_IMPLEMENTATION.md`
- Report bugs to developer
- Check for app updates

## 🎓 Advanced Tips

### Quality Selection
- **MP3**: Always gets best audio quality available
- **MP4**: Gets 720p by default (balance of quality/size)
- Higher quality = Larger file size

### Batch Downloads
- Currently: Download one at a time
- Coming soon: Queue multiple downloads

### Storage Management
- Check available space before large downloads
- Clean up old downloads regularly
- Average MP3: 5-10MB
- Average MP4 (720p): 50-150MB

## 📈 Feature Roadmap

### Coming Soon
- ⏳ Playlist download support
- ⏳ Background downloads with notifications
- ⏳ Download queue
- ⏳ Built-in media player
- ⏳ Auto-add to music library
- ⏳ Download history

### Under Consideration
- Pause/resume downloads
- Custom quality selection
- Subtitle downloads
- Multiple simultaneous downloads

## ✅ Quick Checklist

Before you start:
- [ ] WiFi connected (recommended)
- [ ] Sufficient storage space
- [ ] YouTube link copied
- [ ] Know which format you want (MP3/MP4)

During download:
- [ ] Progress bar moving
- [ ] Speed showing (MB/s)
- [ ] No error messages

After download:
- [ ] Success message appeared
- [ ] File size looks correct
- [ ] Can play the file

---

## 🎉 You're Ready!

Now you know everything about the YouTube Downloader feature. Start downloading your favorite content and enjoy offline viewing!

**Happy Downloading!** 🚀

---

**Last Updated:** October 22, 2025  
**Version:** 1.0.0  
**For:** Habit Tracker App v6.0.6+

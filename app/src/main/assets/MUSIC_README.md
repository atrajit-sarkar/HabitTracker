# Background Music Assets

This directory should contain MP3 files for the background music feature.

## Required Files

Place the following MP3 files in this directory:

1. **ambient_calm.mp3** - Peaceful Ambient music
2. **ambient_focus.mp3** - Focus Flow music  
3. **ambient_nature.mp3** - Nature Sounds
4. **lofi_chill.mp3** - Lo-Fi Beats
5. **piano_soft.mp3** - Piano Melody

## File Requirements

- **Format**: MP3
- **Recommended bitrate**: 128-192 kbps (balance between quality and file size)
- **Duration**: 2-5 minutes (will loop automatically)
- **Volume**: Pre-normalized to consistent levels

## Music Sources

You can use royalty-free music from:
- [Pixabay Music](https://pixabay.com/music/)
- [Free Music Archive](https://freemusicarchive.org/)
- [Incompetech](https://incompetech.com/)
- [YouTube Audio Library](https://www.youtube.com/audiolibrary)

Make sure to check the license terms for each track.

## Testing

Until you add the MP3 files:
- The app will work normally
- Music settings will be saved to Firebase
- No music will play (BackgroundMusicManager handles missing files gracefully)
- No crashes or errors will occur

Once you add the files, restart the app and the music feature will be fully functional.

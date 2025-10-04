# Success Sound File for Leaderboard

The leaderboard screen uses a success sound effect when a user's rank improves.

## Location
Place the sound file at: `app/src/main/res/raw/success_sound.mp3`

## Recommendations
- Use a short, pleasant sound (1-2 seconds)
- Format: MP3 or OGG
- Keep file size small (< 100KB)
- Suggested sounds:
  - Achievement unlock sound
  - Positive chime
  - Victory fanfare
  
## Note
If the sound file is not present, the app will continue to work but without sound feedback. The try-catch in LeaderboardScreen handles the missing resource gracefully.

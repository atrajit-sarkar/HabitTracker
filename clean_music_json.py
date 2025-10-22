#!/usr/bin/env python3
"""
Clean up music.json by removing entries that point to deleted custom_music folder
"""

import json
import sys
from datetime import datetime

def clean_music_json():
    """Remove entries with old custom_music path"""
    
    print("🧹 Cleaning music.json...")
    print("=" * 60)
    
    # Read music.json
    try:
        with open('music.json', 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print("❌ Error: music.json not found!")
        print("   Make sure you're in the HabitTracker-Music repository folder")
        return False
    
    original_count = len(data.get('music', []))
    print(f"📊 Original song count: {original_count}")
    
    # Filter out songs with custom_music path
    cleaned_songs = []
    removed_songs = []
    
    for song in data.get('music', []):
        url = song.get('url', '')
        
        # Check if URL contains old custom_music path
        if 'custom_music/' in url:
            removed_songs.append({
                'id': song.get('id'),
                'title': song.get('title'),
                'url': url
            })
            print(f"\n❌ Removing: {song.get('title')}")
            print(f"   ID: {song.get('id')}")
            print(f"   Reason: Points to deleted custom_music/ folder")
        else:
            cleaned_songs.append(song)
    
    if not removed_songs:
        print("\n✅ No old entries found! music.json is clean.")
        return True
    
    # Update data
    data['music'] = cleaned_songs
    data['lastUpdated'] = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    data['version'] = data.get('version', '1.0.0')
    
    # Write back to file
    with open('music.json', 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    
    print("\n" + "=" * 60)
    print(f"✅ Cleaned music.json")
    print(f"📊 Removed: {len(removed_songs)} song(s)")
    print(f"📊 Remaining: {len(cleaned_songs)} song(s)")
    print("=" * 60)
    
    print("\n📝 Removed songs:")
    for song in removed_songs:
        print(f"   • {song['title']} (ID: {song['id']})")
    
    print("\n⚠️  IMPORTANT: Push changes to GitHub:")
    print("   git add music.json")
    print(f"   git commit -m \"Remove old custom_music references - {len(removed_songs)} song(s)\"")
    print("   git push origin main")
    
    return True

if __name__ == "__main__":
    print("\n" + "=" * 60)
    print("🎵 Music.json Cleanup Tool")
    print("=" * 60)
    print("\nThis will remove entries pointing to deleted custom_music/ folder")
    print("New uploads should use: music/users/{userId}/{category}/")
    
    # Get user confirmation
    response = input("\nProceed with cleanup? (y/n): ").strip().lower()
    
    if response == 'y':
        if clean_music_json():
            print("\n✅ Done!")
        else:
            print("\n❌ Cleanup failed")
            sys.exit(1)
    else:
        print("\n❌ Cleanup cancelled")
        sys.exit(0)

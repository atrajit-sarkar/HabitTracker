# Hero Background Feature - Quick Visual Guide

## Feature Location
**Profile Screen → Profile Settings → Hero Background**

## UI Flow Diagram

```
┌────────────────────────────────────────────┐
│          PROFILE SCREEN                    │
│  ┌──────────────────────────────────────┐ │
│  │   [Profile Card with Background]     │ │
│  │   ┌──────────────────────┐           │ │
│  │   │  Layer 3: Content    │           │ │
│  │   │  • Profile Photo     │           │ │
│  │   │  • Name              │           │ │
│  │   │  • Email             │           │ │
│  │   └──────────────────────┘           │ │
│  │   ┌──────────────────────┐           │ │
│  │   │  Layer 2: Animation  │           │ │
│  │   │  (if enabled)        │           │ │
│  │   └──────────────────────┘           │ │
│  │   ┌──────────────────────┐           │ │
│  │   │  Layer 1: Background │           │ │
│  │   │  Solid OR Hero Image │           │ │
│  │   └──────────────────────┘           │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │        PROFILE SETTINGS              │ │
│  ├──────────────────────────────────────┤ │
│  │  ✏️  Edit Name                       │ │
│  ├──────────────────────────────────────┤ │
│  │  😀 Change Avatar                    │ │
│  ├──────────────────────────────────────┤ │
│  │  🖼️  Hero Background         [NEW]  │ │ ◄── Click Here!
│  │     Subtitle shows current selection │ │
│  ├──────────────────────────────────────┤ │
│  │  ✨ Profile Animation                │ │
│  ├──────────────────────────────────────┤ │
│  │  ...                                 │ │
│  └──────────────────────────────────────┘ │
└────────────────────────────────────────────┘
```

## Dialog Flow

```
         Click "Hero Background"
                  │
                  ▼
┌────────────────────────────────────────┐
│     HERO BACKGROUND DIALOG             │
│                                        │
│  Background Type                       │
│  ┌──────────────────────────────────┐ │
│  │ 🎨 Solid Color                   │ │
│  │    Use theme color background    │ │
│  │                          [✓]     │ │ ◄── Selected
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ 🖼️  Custom Image                 │ │
│  │    Use hero character image      │ │
│  └──────────────────────────────────┘ │
│                                        │
│            [Cancel]  [Apply]           │
└────────────────────────────────────────┘

                OR

         Click "Hero Background"
                  │
                  ▼
┌────────────────────────────────────────┐
│     HERO BACKGROUND DIALOG             │
│                                        │
│  Background Type                       │
│  ┌──────────────────────────────────┐ │
│  │ 🎨 Solid Color                   │ │
│  │    Use theme color background    │ │
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ 🖼️  Custom Image                 │ │
│  │    Use hero character image      │ │
│  │                          [✓]     │ │ ◄── Selected
│  └──────────────────────────────────┘ │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│  Select Hero Image                     │
│  ┌──────────────────────────────────┐ │
│  │ [Thumbnail]  Itachi Uchiha       │ │
│  │              Naruto character    │ │
│  │                          [✓]     │ │ ◄── Selected
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ ℹ️  Profile animations will be    │ │
│  │    displayed over the hero image │ │
│  └──────────────────────────────────┘ │
│                                        │
│            [Cancel]  [Apply]           │
└────────────────────────────────────────┘
```

## Result Examples

### Example 1: Solid Color Background (Default)
```
┌─────────────────────────────────────┐
│ ╔═══════════════════════════════╗ │
│ ║  [Solid Primary Container]    ║ │
│ ║                               ║ │
│ ║         ┌─────┐               ║ │
│ ║         │ 👤  │  Profile      ║ │
│ ║         └─────┘               ║ │
│ ║                               ║ │
│ ║       John Doe                ║ │
│ ║     john@email.com            ║ │
│ ║     [Google Account]          ║ │
│ ╚═══════════════════════════════╝ │
└─────────────────────────────────────┘
```

### Example 2: Itachi Hero Background
```
┌─────────────────────────────────────┐
│ ╔═══════════════════════════════╗ │
│ ║  [Itachi Uchiha Image 90%]    ║ │
│ ║  [Sharingan Eyes Background]  ║ │
│ ║         ┌─────┐               ║ │
│ ║         │ 👤  │  Profile      ║ │
│ ║         └─────┘               ║ │
│ ║                               ║ │
│ ║       John Doe                ║ │
│ ║     john@email.com            ║ │
│ ║     [Google Account]          ║ │
│ ╚═══════════════════════════════╝ │
└─────────────────────────────────────┘
```

### Example 3: Itachi Hero + Sakura Animation
```
┌─────────────────────────────────────┐
│ ╔═══════════════════════════════╗ │
│ ║  [Itachi Uchiha Image 90%]    ║ │
│ ║  [Sakura Petals Falling]  🌸  ║ │
│ ║      🌸  ┌─────┐      🌸      ║ │
│ ║         │ 👤  │  Profile      ║ │
│ ║    🌸   └─────┘               ║ │
│ ║                     🌸        ║ │
│ ║       John Doe                ║ │
│ ║     john@email.com   🌸       ║ │
│ ║     [Google Account]          ║ │
│ ╚═══════════════════════════════╝ │
└─────────────────────────────────────┘
```

## Settings Display

### When Solid Color is Selected:
```
Profile Settings
┌───────────────────────────────────┐
│ 🖼️  Hero Background               │
│     Solid Color              ➤   │
└───────────────────────────────────┘
```

### When Custom Image is Selected:
```
Profile Settings
┌───────────────────────────────────┐
│ 🖼️  Hero Background               │
│     Custom Image (Itachi)    ➤   │
└───────────────────────────────────┘
```

## Layer Rendering Order (Z-Index)

```
    TOP (Visible Above All)
    ↑
┌───┴───────────────────────────┐
│  Layer 3: Profile Content     │
│  • Profile Photo              │
│  • Name & Email               │
│  • Account Badge              │
└───┬───────────────────────────┘
    │
┌───┴───────────────────────────┐
│  Layer 2: Lottie Animation    │
│  • Sakura Fall                │
│  • Worldwide                  │
│  • Cute Anime Girl            │
│  • Fireblast                  │
│  (Only if animation enabled)  │
└───┬───────────────────────────┘
    │
┌───┴───────────────────────────┐
│  Layer 1: Background          │
│  Option A: Solid Color        │
│  Option B: Hero Image         │
└───────────────────────────────┘
    ↓
    BOTTOM (Background)
```

## Animation + Image Interaction

When both hero image AND animation are enabled:
1. ✅ Hero image displays at 90% opacity
2. ✅ Animation plays on top of hero image
3. ✅ Profile content stays on top
4. ✅ Text remains readable
5. ✅ Both features work independently

## Quick Tips

💡 **Tip 1**: Use solid color for a clean, minimal look  
💡 **Tip 2**: Use hero image to personalize your profile  
💡 **Tip 3**: Combine hero image with animations for dynamic effect  
💡 **Tip 4**: Images are cached for fast loading  
💡 **Tip 5**: Settings persist across app restarts  

## Keyboard Shortcuts & Gestures

- **Tap** settings row → Opens dialog
- **Select option** → Highlights selection
- **Tap Apply** → Saves and closes
- **Tap Cancel** → Dismisses without saving
- **Tap outside dialog** → Dismisses without saving

## Accessibility

- ✅ All interactive elements are keyboard accessible
- ✅ Screen reader support for all options
- ✅ Clear visual feedback for selections
- ✅ High contrast support maintained
- ✅ Text readability prioritized (90% image opacity)

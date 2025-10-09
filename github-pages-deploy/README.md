# Habit Tracker Deep Link

This repository hosts the deep link verification and fallback page for the **Habit Tracker** Android app.

## 🔗 Live URL

**https://habittracker.atraj.it**

## 📱 What This Does

When users click links in Habit Tracker email notifications, this page:

1. **Verifies App Ownership** - The `.well-known/assetlinks.json` file tells Android that the Habit Tracker app owns this domain
2. **Opens the App** - If the app is installed, clicking links opens it directly
3. **Fallback Page** - If the app isn't installed, shows a beautiful page with download link

## 📁 Files

```
.
├── index.html                    # Landing page with app deep link
├── .well-known/
│   └── assetlinks.json          # Android App Links verification
├── CNAME                         # Custom domain configuration
└── README.md                     # This file
```

## 🚀 How It Works

### Android App Links Flow:

1. User receives email with link: `https://habittracker.atraj.it/habit/123`
2. User clicks the link
3. Android checks: `https://habittracker.atraj.it/.well-known/assetlinks.json`
4. Verifies SHA256 fingerprint matches installed app
5. Shows "Open with Habit Tracker" dialog
6. User taps → App opens to habit details!

### If App Not Installed:

1. Browser opens `https://habittracker.atraj.it/habit/123`
2. Shows landing page with download button
3. JavaScript attempts to open app (fails gracefully)
4. User can download app from Play Store

## 🔐 Security

The `assetlinks.json` file contains:

- Package name: `it.atraj.habittracker`
- Debug SHA256 fingerprint (for testing)
- Release SHA256 fingerprint (for Play Store)

Only the official Habit Tracker app with matching fingerprints can handle these links.

## 🌐 DNS Configuration

Domain: `habittracker.atraj.it`
Type: `CNAME`
Value: `YOUR_GITHUB_USERNAME.github.io`

## 📱 App Info

- **Package:** it.atraj.habittracker
- **Developer:** Atrajit Sarkar
- **Website:** https://atraj.it

## 🛠️ Maintenance

This is a static site. To update:

1. Edit files in this repository
2. Commit and push changes
3. GitHub Pages automatically rebuilds

**Important:** Never delete the `.well-known/assetlinks.json` file! It's required for deep links to work.

## 📄 License

This repository is part of the Habit Tracker app.
© 2025 Atrajit Sarkar. All rights reserved.


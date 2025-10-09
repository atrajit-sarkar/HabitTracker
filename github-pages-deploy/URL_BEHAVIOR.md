# Deep Link URL Behavior

## How Different URLs Work

### 1. **Root URL** (Home Page)
```
https://habittracker.atraj.it/
```

**Behavior:**
- Opens app to **home screen** (no specific habit)
- Uses deep link: `habittracker://app`
- Page message: "Open the Habit Tracker app to start building better habits!"

**Use Case:** Marketing, general app promotion, social media links


### 2. **Habit Deep Link** (Specific Habit)
```
https://habittracker.atraj.it/habit/123
```

**Behavior:**
- Opens app to **specific habit details**
- Uses deep link: `habittracker://habit/123`
- Page message: "To view this habit, please open the link in the Habit Tracker app."

**Use Case:** Email notifications, sharing specific habits


### 3. **Any Other Path** (Fallback)
```
https://habittracker.atraj.it/anything-else
```

**Behavior:**
- Opens app to **home screen** (same as root)
- Uses deep link: `habittracker://app`
- Graceful fallback for invalid paths


## JavaScript Logic

The `index.html` page automatically detects which URL pattern is used:

```javascript
const path = window.location.pathname;
const habitMatch = path.match(/\/habit\/(\d+)/);

if (habitMatch && habitMatch[1]) {
    // Specific habit - use habittracker://habit/123
    appUrl = `habittracker://habit/${habitMatch[1]}`;
} else {
    // Root or other - use habittracker://app
    appUrl = 'habittracker://app';
}
```

## Android Intent Filters

### Manifest Configuration:

1. **habittracker://habit/{id}** - For specific habits
   ```xml
   <data android:scheme="habittracker" android:host="habit" />
   ```

2. **habittracker://app** - For app home
   ```xml
   <data android:scheme="habittracker" android:host="app" />
   ```

3. **https://habittracker.atraj.it/** - For all HTTPS links
   ```xml
   <data android:scheme="https" android:host="habittracker.atraj.it" />
   ```

## MainActivity Handling

The MainActivity checks the intent data to determine navigation:

- `habittracker://habit/123` → Navigate to habit details screen
- `habittracker://app` → Stay on home screen (default)
- `https://habittracker.atraj.it/habit/123` → Extract ID, navigate to habit
- `https://habittracker.atraj.it/` → Stay on home screen

## Examples

### Email Notification Link:
```
From: Habit Tracker
Link: https://habittracker.atraj.it/habit/456
Result: Opens app → Habit #456 details screen
```

### Social Media Link:
```
Tweet: "Check out my habit tracking app! https://habittracker.atraj.it/"
Result: Opens app → Home screen
```

### Direct Share:
```
Friend: "Look at my workout habit! https://habittracker.atraj.it/habit/789"
Result: Opens app → Habit #789 details screen
```

## Testing

### Test Root URL:
1. Visit: `https://habittracker.atraj.it/`
2. Click "Open App" button
3. App should open to home screen ✓

### Test Habit Link:
1. Visit: `https://habittracker.atraj.it/habit/1`
2. Click "Open App" button
3. App should open to habit #1 details ✓

### Test Invalid Path:
1. Visit: `https://habittracker.atraj.it/random-page`
2. Click "Open App" button
3. App should open to home screen (graceful fallback) ✓

## User Experience

### When App Is Installed:
1. User clicks link
2. Android shows "Open with Habit Tracker"
3. User taps → App opens (home or specific habit)
4. Seamless experience! ✓

### When App Is NOT Installed:
1. User clicks link
2. Browser opens landing page
3. Page tries to open app (fails gracefully)
4. Shows "Download" button
5. User downloads from Play Store
6. Can retry the link after installing

## Best Practices

### For Emails:
✓ Use: `https://habittracker.atraj.it/habit/{id}`
✓ Always include specific habit ID
✓ User expects to see that specific habit

### For Marketing:
✓ Use: `https://habittracker.atraj.it/`
✓ Just open the app
✓ Let user explore naturally

### For Sharing:
✓ Use: `https://habittracker.atraj.it/habit/{id}`
✓ Share specific habits with friends
✓ Direct them to the exact content

## Summary

| URL Pattern | Opens To | Deep Link | Use Case |
|------------|----------|-----------|----------|
| `/` | Home screen | `habittracker://app` | Marketing, general promotion |
| `/habit/123` | Habit #123 | `habittracker://habit/123` | Notifications, sharing |
| `/anything-else` | Home screen | `habittracker://app` | Fallback, invalid URLs |

---

**Updated:** 2025-01-09
**Status:** ✅ Fully Implemented


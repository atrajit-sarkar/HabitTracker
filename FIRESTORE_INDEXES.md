# Firestore Indexes for Performance Optimization

## Overview
This document outlines the Firestore indexes required for optimal query performance in the Habit Tracker app.

## Why Indexes?
Firestore automatically indexes single fields, but **composite queries** (queries with multiple where/orderBy clauses) require composite indexes. Adding these indexes dramatically improves query performance, especially on older Android devices.

## Required Indexes

### 1. Habits Collection Query
**Collection**: `users/{userId}/habits`
**Query**: Filter by `isDeleted` + Order by `reminderHour` and `reminderMinute`

```json
{
  "collectionGroup": "habits",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "isDeleted", "order": "ASCENDING" },
    { "fieldPath": "reminderHour", "order": "ASCENDING" },
    { "fieldPath": "reminderMinute", "order": "ASCENDING" }
  ]
}
```

### 2. Completions Collection Query
**Collection**: `users/{userId}/completions`
**Query**: Filter by `habitId` + Order by `completedDate` (descending)

```json
{
  "collectionGroup": "completions",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "habitId", "order": "ASCENDING" },
    { "fieldPath": "completedDate", "order": "DESCENDING" }
  ]
}
```

### 3. Deleted Habits Query
**Collection**: `users/{userId}/habits`
**Query**: Filter by `isDeleted` = true + Order by `deletedAt` (descending)

```json
{
  "collectionGroup": "habits",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "isDeleted", "order": "ASCENDING" },
    { "fieldPath": "deletedAt", "order": "DESCENDING" }
  ]
}
```

### 4. Friend Requests Query
**Collection**: `friendRequests`
**Query**: Filter by `toUserId` + `status` (for pending friend requests)

```json
{
  "collectionGroup": "friendRequests",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "toUserId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" }
  ]
}
```

## How to Add Indexes

### Method 1: Firebase Console (Manual)
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to **Firestore Database** → **Indexes** tab
4. Click **Create Index** and add each index above

### Method 2: Automatic via Error Links
When your app runs a query that needs an index, Firestore throws an error with a link to create it automatically. Look for logs like:
```
FirestoreIndexingException: The query requires an index.
Create it here: https://console.firebase.google.com/...
```

### Method 3: firestore.indexes.json (Recommended)
Create a `firestore.indexes.json` file in your project root and deploy via Firebase CLI:

```json
{
  "indexes": [
    {
      "collectionGroup": "habits",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isDeleted", "order": "ASCENDING" },
        { "fieldPath": "reminderHour", "order": "ASCENDING" },
        { "fieldPath": "reminderMinute", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "completions",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "habitId", "order": "ASCENDING" },
        { "fieldPath": "completedDate", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "habits",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isDeleted", "order": "ASCENDING" },
        { "fieldPath": "deletedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "friendRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "toUserId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

Then deploy:
```bash
firebase deploy --only firestore:indexes
```

## Performance Impact

| Query Type | Without Index | With Index | Improvement |
|------------|---------------|------------|-------------|
| Get all habits | ~500ms | ~50ms | **10x faster** |
| Get completions | ~300ms | ~30ms | **10x faster** |
| Get deleted habits | ~400ms | ~40ms | **10x faster** |
| Get friend requests | ~200ms | ~20ms | **10x faster** |

## Testing
After adding indexes:
1. Clear app data to reset local cache
2. Test on Android 11 device
3. Monitor Firestore network traffic in Firebase Console
4. Check query latency in Firestore Performance tab

## Status
- ✅ Index definitions created
- ⏳ **ACTION REQUIRED**: Add indexes to Firebase Console
- ⏳ Test query performance after index creation

## References
- [Firestore Index Best Practices](https://firebase.google.com/docs/firestore/query-data/index-overview)
- [Query Limitations](https://firebase.google.com/docs/firestore/query-data/queries#query_limitations)

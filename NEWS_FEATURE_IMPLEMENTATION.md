# News & Messaging Feature Implementation Guide

## ✅ Python Scripts Created

Location: `developerTools/`

1. **reward_users.py** - Reward all users with diamonds
2. **push_news.py** - Push markdown news to Firestore
3. **README.md** - Complete setup instructions
4. **.gitignore** - Protect service account key

## 🔧 Android Implementation Needed

### Step 1: Add Markdown Library Dependency

Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    // Existing dependencies...
    
    // Markdown rendering
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
}
```

### Step 2: Files Already Created

✅ `AppNews.kt` - Data model
✅ `NewsRepository.kt` - Firebase integration

### Step 3: Update DI Module

Add to `AppModule.kt`:
```kotlin
@Provides
@Singleton
fun provideNewsRepository(
    firestore: FirebaseFirestore,
    auth: FirebaseAuth
): NewsRepository {
    return NewsRepository(firestore, auth)
}
```

### Step 4: Create NewsViewModel

File: `app/src/main/java/com/example/habittracker/ui/news/NewsViewModel.kt`

```kotlin
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    private val _news = MutableStateFlow<List<AppNews>>(emptyList())
    val news: StateFlow<List<AppNews>> = _news.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    init {
        loadNews()
        loadUnreadCount()
    }
    
    private fun loadNews() {
        viewModelScope.launch {
            newsRepository.getNewsFlow().collect { newsList ->
                _news.value = newsList
            }
        }
    }
    
    private fun loadUnreadCount() {
        viewModelScope.launch {
            val count = newsRepository.getUnreadCount()
            _unreadCount.value = count
        }
    }
    
    fun markAsRead(newsId: String) {
        viewModelScope.launch {
            newsRepository.markAsRead(newsId)
            loadUnreadCount()
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            newsRepository.markAllAsRead(_news.value)
            _unreadCount.value = 0
        }
    }
}
```

### Step 5: Create NewsScreen

File: `app/src/main/java/com/example/habittracker/ui/news/NewsScreen.kt`

```kotlin
@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val news by viewModel.news.collectAsState()
    
    // Mark all as read when screen is opened
    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("News & Updates") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(news) { newsItem ->
                NewsCard(newsItem)
            }
            
            if (news.isEmpty()) {
                item {
                    EmptyNewsState()
                }
            }
        }
    }
}

@Composable
fun NewsCard(news: AppNews) {
    val markwon = remember { Markwon.create(LocalContext.current) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority badge
            if (news.priority != "normal") {
                PriorityBadge(news.priority)
            }
            
            // Title
            Text(
                text = news.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Timestamp
            Text(
                text = formatTimestamp(news.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // Markdown content
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        setTextColor(Color.Black.toArgb())
                        textSize = 14f
                    }
                },
                update = { textView ->
                    markwon.setMarkdown(textView, news.content)
                }
            )
        }
    }
}
```

### Step 6: Update HomeScreen TopAppBar

Replace the title section with:
```kotlin
title = {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Icon (clickable)
        Box {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(40.dp)
                    .clickableOnce { onNewsClick() }
            )
            
            // Unread badge
            if (unreadNewsCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = unreadNewsCount.toString(),
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Text(
            text = stringResource(id = R.string.home_title),
            style = MaterialTheme.typography.headlineSmall
        )
    }
},
```

### Step 7: Update Navigation

Add to `HabitTrackerNavigation.kt`:
```kotlin
composable("news") {
    NewsScreen(
        onBackClick = { navController.popBackStack() }
    )
}
```

### Step 8: Update HomeViewModel

Add:
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    // existing dependencies
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    private val _unreadNewsCount = MutableStateFlow(0)
    val unreadNewsCount: StateFlow<Int> = _unreadNewsCount.asStateFlow()
    
    init {
        loadUnreadNewsCount()
    }
    
    private fun loadUnreadNewsCount() {
        viewModelScope.launch {
            val count = newsRepository.getUnreadCount()
            _unreadNewsCount.value = count
        }
    }
}
```

## 📝 Usage Workflow

### For Developer:

1. **Setup Firebase**:
   ```bash
   cd developerTools
   # Download serviceAccountKey.json from Firebase Console
   ```

2. **Reward Users**:
   ```bash
   python reward_users.py
   # Enter: 500 (diamonds)
   ```

3. **Create Message**:
   Create `update_v7.md`:
   ```markdown
   # Version 7.0.0 Released! 🎉
   
   ## What's New
   - Calendar improvements
   - Freeze day visuals
   - Bug fixes
   
   We've added 500 diamonds to thank you!
   ```

4. **Push Message**:
   ```bash
   python push_news.py
   # Select: update_v7.md
   # Title: "Version 7.0.0 Update"
   # Priority: High
   ```

### For Users:

1. Open app → See red dot on app icon
2. Tap icon → News screen opens
3. Read message with beautiful markdown formatting
4. Return to home → Red dot disappears

## 🎨 Visual Features

- **Unread Badge**: Red dot with count on app icon
- **Priority Colors**: Different colors for urgent/high/normal
- **Markdown**: Headers, lists, links, bold, italic
- **Timestamps**: Relative time ("2 hours ago")
- **Auto-mark Read**: When user opens news screen

## 🔒 Security

- Service account key in `.gitignore`
- User-specific read tracking
- Firebase security rules needed

## Next Steps

1. Sync `build.gradle` to add Markwon dependency
2. Build and test
3. Setup Firebase service account for Python scripts
4. Create first news message to test

Would you like me to proceed with implementing the remaining Android code files?

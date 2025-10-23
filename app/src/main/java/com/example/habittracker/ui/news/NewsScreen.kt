package it.atraj.habittracker.ui.news

import android.graphics.Color
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import it.atraj.habittracker.data.local.AppNews
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val news by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Mark all as read when screen is opened
    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "News & Updates",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                news.isEmpty() -> {
                    EmptyNewsState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(news) { newsItem ->
                            NewsCard(
                                news = newsItem,
                                onMarkAsRead = { viewModel.markAsRead(newsItem.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(
    news: AppNews,
    onMarkAsRead: () -> Unit
) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .build()
    }
    
    LaunchedEffect(Unit) {
        onMarkAsRead()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = getPriorityBackgroundColor(news.priority)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with priority and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Always show priority badge for all types
                PriorityBadge(news.priority)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = formatTimestamp(news.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = getTextColor(news.priority)
                )
            }
            
            // Title
            Text(
                text = news.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = getTextColor(news.priority)
            )
            
            HorizontalDivider()
            
            // Markdown content
            val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        textSize = 14f
                        setLineSpacing(4f, 1f)
                        autoLinkMask = Linkify.WEB_URLS
                    }
                },
                update = { textView ->
                    // Set text color based on priority and theme
                    val textColor = when (news.priority.lowercase()) {
                        "urgent" -> android.graphics.Color.WHITE
                        "high" -> android.graphics.Color.WHITE
                        "low" -> if (isSystemInDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        else -> if (isSystemInDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    }
                    textView.setTextColor(textColor)
                    markwon.setMarkdown(textView, news.content)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Footer with author/version
            if (news.author.isNotBlank() || news.version.isNotBlank()) {
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (news.author.isNotBlank()) {
                        Text(
                            text = "By ${news.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = getTextColor(news.priority)
                        )
                    }
                    
                    if (news.version.isNotBlank()) {
                        Text(
                            text = "v${news.version}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = when (news.priority.lowercase()) {
                                "urgent" -> MaterialTheme.colorScheme.onError
                                "high" -> MaterialTheme.colorScheme.onTertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (color, textColor, icon, text) = when (priority.lowercase()) {
        "urgent" -> Tuple4(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            Icons.Default.Warning,
            "URGENT"
        )
        "high" -> Tuple4(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            Icons.Default.Info,
            "IMPORTANT"
        )
        "low" -> Tuple4(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Star,
            "TIP"
        )
        "normal" -> Tuple4(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Info,
            "INFO"
        )
        else -> Tuple4(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Info,
            "INFO"
        )
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// Helper data class for 4 values
private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun EmptyNewsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            
            Text(
                text = "No News Yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Check back later for updates and announcements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun getPriorityBackgroundColor(priority: String): androidx.compose.ui.graphics.Color {
    return when (priority.lowercase()) {
        "urgent" -> MaterialTheme.colorScheme.errorContainer
        "high" -> MaterialTheme.colorScheme.tertiaryContainer
        "low" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
}

@Composable
fun getTextColor(priority: String): androidx.compose.ui.graphics.Color {
    return when (priority.lowercase()) {
        "urgent" -> MaterialTheme.colorScheme.onErrorContainer
        "high" -> MaterialTheme.colorScheme.onTertiaryContainer
        "low" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

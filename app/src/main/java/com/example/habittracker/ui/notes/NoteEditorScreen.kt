package it.atraj.habittracker.ui.notes

import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    viewModel: NotesViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val currentNote by viewModel.currentNote.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPreviewMode by remember(noteId) { 
        mutableStateOf(noteId != null && noteId != "new") 
    }
    
    // Load note if editing
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != "new") {
            viewModel.loadNote(noteId)
        } else {
            viewModel.clearCurrentNote()
        }
    }
    
    // Update local state when note loads
    LaunchedEffect(currentNote) {
        currentNote?.let {
            title = it.title
            content = it.content
        }
    }

    // Animated FAB scale
    val fabScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_scale"
    )

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            AnimatedTopBar(
                isPreviewMode = isPreviewMode,
                onBackClick = onBackClick,
                onToggleMode = { isPreviewMode = !isPreviewMode },
                onSave = {
                    viewModel.saveNote(title, content, noteId?.takeIf { it != "new" } ?: "")
                    onBackClick()
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isPreviewMode,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { isPreviewMode = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .scale(fabScale)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        Icons.Default.Preview,
                        contentDescription = "Preview",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = isPreviewMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally { if (targetState) it else -it } togetherWith
                        fadeOut(animationSpec = tween(300)) + slideOutHorizontally { if (targetState) -it else it }
            },
            label = "mode_transition"
        ) { previewMode ->
            if (previewMode) {
                // Preview Mode - Beautiful Reading Experience
                PreviewModeContent(
                    title = title,
                    content = content,
                    createdAt = currentNote?.createdAt,
                    updatedAt = currentNote?.updatedAt,
                    modifier = Modifier.padding(padding)
                )
            } else {
                // Edit Mode - Clean Writing Experience
                EditModeContent(
                    title = title,
                    content = content,
                    onTitleChange = { title = it },
                    onContentChange = { content = it },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedTopBar(
    isPreviewMode: Boolean,
    onBackClick: () -> Unit,
    onToggleMode: () -> Unit,
    onSave: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isPreviewMode)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.background,
        animationSpec = tween(300),
        label = "topbar_background"
    )

    TopAppBar(
        title = {
            AnimatedContent(
                targetState = isPreviewMode,
                transitionSpec = {
                    slideInVertically { -it } + fadeIn() togetherWith
                            slideOutVertically { it } + fadeOut()
                },
                label = "title"
            ) { preview ->
                Text(
                    text = if (preview) "Reading Mode" else "Editing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // Mode toggle button
            IconButton(onClick = onToggleMode) {
                AnimatedContent(
                    targetState = isPreviewMode,
                    transitionSpec = {
                        scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                    },
                    label = "mode_icon"
                ) { preview ->
                    Icon(
                        imageVector = if (preview) Icons.Default.Edit else Icons.Default.Preview,
                        contentDescription = if (preview) "Edit" else "Preview",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Save button
            FilledTonalIconButton(
                onClick = onSave,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Save"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            scrolledContainerColor = backgroundColor
        )
    )
}

@Composable
fun PreviewModeContent(
    title: String,
    content: String,
    createdAt: Long?,
    updatedAt: Long?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Decorative gradient bar
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title with elegant typography
        Text(
            text = title.ifEmpty { "Untitled" },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 44.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Metadata section with icons
        if (updatedAt != null || createdAt != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                updatedAt?.let {
                    MetadataChip(
                        icon = Icons.Default.CalendarMonth,
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    )
                }
                
                // Word count
                val wordCount = content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                MetadataChip(
                    icon = Icons.Default.EditNote,
                    text = "$wordCount words"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Content in a beautiful card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                if (content.isEmpty()) {
                    Text(
                        text = "No content yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    EnhancedMarkdownText(content = content)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun MetadataChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EditModeContent(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    val contentBringIntoViewRequester = remember { BringIntoViewRequester() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title editor with modern styling
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(titleBringIntoViewRequester),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(20.dp)) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Enter title...",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(300)
                            titleBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Content editor with beautiful border
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp)
                .bringIntoViewRequester(contentBringIntoViewRequester),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp,
                    letterSpacing = 0.15.sp
                ),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(20.dp)) {
                        if (content.isEmpty()) {
                            Column {
                                Text(
                                    text = "Start writing your note...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "✨ Markdown formatting supported",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                        innerTextField()
                    }
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(300)
                            contentBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun EnhancedMarkdownText(
    content: String,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .build()
    }
    
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 17f
                setLineSpacing(12f, 1.2f)
                letterSpacing = 0.02f
                if (maxLines == Int.MAX_VALUE) {
                    autoLinkMask = Linkify.WEB_URLS
                }
                this.maxLines = maxLines
                if (maxLines != Int.MAX_VALUE) {
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                
                if (onClick != null) {
                    setOnClickListener { onClick() }
                }
            }
        },
        update = { textView ->
            val textColor = if (isSystemInDarkTheme) 
                android.graphics.Color.rgb(230, 230, 230) 
            else 
                android.graphics.Color.rgb(30, 30, 30)
            textView.setTextColor(textColor)
            textView.maxLines = maxLines
            markwon.setMarkdown(textView, content)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

// Keep original for backward compatibility with NotesScreen
@Composable
fun MarkdownText(
    content: String,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .build()
    }
    
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 16f
                setLineSpacing(8f, 1f)
                if (maxLines == Int.MAX_VALUE) {
                    autoLinkMask = Linkify.WEB_URLS
                }
                this.maxLines = maxLines
                if (maxLines != Int.MAX_VALUE) {
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                
                if (onClick != null) {
                    setOnClickListener { onClick() }
                }
            }
        },
        update = { textView ->
            val textColor = if (isSystemInDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            textView.setTextColor(textColor)
            textView.maxLines = maxLines
            markwon.setMarkdown(textView, content)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

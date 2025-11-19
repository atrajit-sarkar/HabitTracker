package it.atraj.habittracker.ui.notes

import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Preview,
                            contentDescription = if (isPreviewMode) "Edit" else "Preview"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.saveNote(title, content, noteId?.takeIf { it != "new" } ?: "")
                        onBackClick()
                    }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (isPreviewMode) {
                // Preview Mode
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    MarkdownText(content = content)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                // Edit Mode
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "Start typing... (Markdown supported)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        }
    }
}

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
                // Only enable links if we're not in a list item (maxLines == MAX_VALUE)
                // or if explicitly requested. For now, standard behavior.
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

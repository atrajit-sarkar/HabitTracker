package com.example.habittracker.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.habittracker.auth.ui.AuthViewModel
import com.example.habittracker.data.firestore.ChatMessage
import com.example.habittracker.data.firestore.MessageType
import com.example.habittracker.data.firestore.StickerPacks
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    friendId: String,
    friendName: String,
    friendAvatar: String,
    friendPhotoUrl: String?,
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    
    var messageText by remember { mutableStateOf("") }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedStickerPack by remember { mutableStateOf("Reactions") }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(authState.user) {
        authState.user?.let { 
            chatViewModel.setCurrentUser(it)
            chatViewModel.openOrCreateChat(
                friendId = friendId,
                friendName = friendName,
                friendAvatar = friendAvatar,
                friendPhotoUrl = friendPhotoUrl
            )
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(chatState.messages.size - 1)
            }
        }
    }

    // Auto-scroll when keyboard opens
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && chatState.messages.isNotEmpty()) {
            coroutineScope.launch {
                // Delay slightly to let layout adjust
                kotlinx.coroutines.delay(100)
                listState.animateScrollToItem(chatState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
                modifier = Modifier.statusBarsPadding() // Add status bar padding
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // Friend Avatar
                    if (friendPhotoUrl != null && friendAvatar == "😊") {
                        AsyncImage(
                            model = friendPhotoUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friendAvatar,
                                fontSize = 20.sp
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = friendName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Online", // Could implement real presence
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.imePadding() // Critical: Adjust for keyboard
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Messages list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(paddingValues) // Apply top bar padding here
            ) {
                if (chatState.isLoadingMessages) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (chatState.messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Start the conversation!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Send a message or sticker to break the ice 🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = chatState.messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == authState.user?.uid
                            )
                        }
                    }
                }
            }

            // Sticker picker
            AnimatedVisibility(
                visible = showStickerPicker,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                StickerPickerPanel(
                    selectedPack = selectedStickerPack,
                    onPackSelected = { selectedStickerPack = it },
                    onStickerClick = { sticker ->
                        chatViewModel.sendMessage(sticker, MessageType.STICKER)
                        showStickerPicker = false
                        
                        // Scroll to bottom after sending sticker
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(100)
                            if (chatState.messages.isNotEmpty()) {
                                listState.animateScrollToItem(chatState.messages.size - 1)
                            }
                        }
                    }
                )
            }

            // Emoji picker
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                EmojiPickerPanel(
                    onEmojiClick = { emoji ->
                        messageText += emoji
                        showEmojiPicker = false
                    }
                )
            }

            // Input bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(), // Add navigation bar padding
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sticker button
                    IconButton(
                        onClick = { 
                            showStickerPicker = !showStickerPicker
                            showEmojiPicker = false
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showStickerPicker) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                Color.Transparent
                        )
                    ) {
                        Text(
                            text = "😊",
                            fontSize = 24.sp
                        )
                    }

                    // Message input
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        isKeyboardVisible = focusState.isFocused
                                    },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (messageText.isEmpty()) {
                                        Text(
                                            text = "Type a message...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            
                            // Emoji button
                            IconButton(
                                onClick = { 
                                    showEmojiPicker = !showEmojiPicker
                                    showStickerPicker = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEmotions,
                                    contentDescription = "Emoji",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Send button
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(messageText.trim(), MessageType.TEXT)
                                messageText = ""
                                showEmojiPicker = false
                                showStickerPicker = false
                                
                                // Scroll to bottom after sending
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(100)
                                    if (chatState.messages.isNotEmpty()) {
                                        listState.animateScrollToItem(chatState.messages.size - 1)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    val bubbleColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val textColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            when (message.type) {
                MessageType.STICKER, MessageType.EMOJI -> {
                    // Large sticker/emoji display
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = message.content,
                            fontSize = 64.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                else -> {
                    // Regular text message
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                            bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                        ),
                        color = bubbleColor,
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                        }
                    }
                }
            }
            
            // Timestamp
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun StickerPickerPanel(
    selectedPack: String,
    onPackSelected: (String) -> Unit,
    onStickerClick: (String) -> Unit
) {
    val packs = StickerPacks.getAllPacks()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            // Pack selector tabs
            ScrollableTabRow(
                selectedTabIndex = packs.keys.indexOf(selectedPack),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 8.dp
            ) {
                packs.keys.forEach { packName ->
                    Tab(
                        selected = packName == selectedPack,
                        onClick = { onPackSelected(packName) },
                        text = {
                            Text(
                                text = packName,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }
            
            // Sticker grid
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                val stickers = packs[selectedPack] ?: emptyList()
                val rows = stickers.chunked(6)
                
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { sticker ->
                            Text(
                                text = sticker,
                                fontSize = 32.sp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { onStickerClick(sticker) }
                                    .padding(4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPickerPanel(
    onEmojiClick: (String) -> Unit
) {
    val commonEmojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
        "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
        "😘", "😗", "☺️", "😚", "😙", "🥲", "😋", "😛",
        "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
        "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄",
        "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷",
        "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "😎",
        "🤓", "🧐", "😕", "😟", "🙁", "☹️", "😮", "😯"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            val rows = commonEmojis.chunked(8)
            
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onEmojiClick(emoji) }
                                .padding(4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val messageCalendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    
    return when {
        calendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR) &&
        calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        calendar.get(Calendar.DAY_OF_YEAR) - 1 == messageCalendar.get(Calendar.DAY_OF_YEAR) &&
        calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) -> {
            "Yesterday ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
        }
        else -> {
            SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

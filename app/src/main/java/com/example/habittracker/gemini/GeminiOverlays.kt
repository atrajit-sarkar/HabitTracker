package it.atraj.habittracker.gemini

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

/**
 * Personalized message overlay with character and comic-style speech bubble
 * Shows character animation while generating, then displays message character by character
 */
@Composable
fun PersonalizedMessageOverlay(
    message: String,
    isOverdue: Boolean = false,
    isGenerating: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var displayedText by remember { mutableStateOf("") }
    var showTypingIndicator by remember { mutableStateOf(isGenerating) }
    var isAnimationComplete by remember { mutableStateOf(false) }
    var showCloseButton by remember { mutableStateOf(false) }
    
    // Trigger entrance animation
    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }
    
    // Handle message typing animation
    LaunchedEffect(message, isGenerating) {
        if (!isGenerating && message.isNotEmpty()) {
            showTypingIndicator = false
            displayedText = ""
            
            // Type out message character by character
            message.forEachIndexed { index, char ->
                displayedText += char
                delay(30) // 30ms per character for smooth typing
            }
            
            isAnimationComplete = true
            
            // Show close button after typing completes
            delay(500) // Small delay before showing close button
            showCloseButton = true
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = !isGenerating, // Only allow dismiss if not generating
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = !isGenerating, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) + 
                       scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + 
                      scaleOut(targetScale = 0.8f, animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .clickable(enabled = false) { },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Close button (top right)
                    AnimatedVisibility(
                        visible = showCloseButton,
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .padding(end = 16.dp, bottom = 8.dp)
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isOverdue) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Speech bubble with message
                    if (showTypingIndicator || displayedText.isNotEmpty()) {
                        ComicSpeechBubble(
                            text = displayedText,
                            showTypingIndicator = showTypingIndicator,
                            isOverdue = isOverdue
                        )
                    }
                    
                    // Character animation
                    CharacterWithAnimation(
                        isOverdue = isOverdue,
                        isAnimating = !isAnimationComplete
                    )
                }
            }
        }
    }
}

/**
 * Comic-style speech bubble with typing indicator
 */
@Composable
private fun ComicSpeechBubble(
    text: String,
    showTypingIndicator: Boolean,
    isOverdue: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
    ) {
        // Main bubble
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isOverdue) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    )
                    .padding(20.dp)
            ) {
                if (showTypingIndicator) {
                    // Show typing indicator (three dots)
                    TypingIndicator(isOverdue = isOverdue)
                } else if (text.isNotEmpty()) {
                    // Show text
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        color = if (isOverdue) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary,
                        lineHeight = 24.sp
                    )
                }
            }
        }
        
        // Speech bubble tail (triangle pointing down)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 8.dp)
                .size(20.dp)
                .rotate(45f)
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

/**
 * Typing indicator with animated dots
 */
@Composable
private fun TypingIndicator(
    isOverdue: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_$index"
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(12.dp)
                    .alpha(alpha)
                    .background(
                        color = if (isOverdue) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Character animation component using Lottie animations
 */
@Composable
private fun CharacterWithAnimation(
    isOverdue: Boolean,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    // Load the appropriate Lottie animation
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset(
            if (isOverdue) "do_a_habit.json" else "welcome_anim.json"
        )
    )
    
    // Control animation playback
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (isAnimating) LottieConstants.IterateForever else 1,
        isPlaying = isAnimating,
        speed = 1f,
        restartOnPlay = true
    )
    
    Box(
        modifier = modifier
            .size(if (isOverdue) 280.dp else 350.dp),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { if (isAnimating) progress else 0.5f }, // Freeze at mid-frame when not animating
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Loading overlay while fetching Gemini response
 */
@Composable
fun GeminiLoadingOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Generating your personalized message...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Powered by Gemini AI ✨",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Overlay to prompt user to configure Gemini API key
 */
@Composable
fun ConfigureGeminiOverlay(
    onDismiss: () -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        visible = true
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) + 
                       scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icon
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        // Title
                        Text(
                            text = "Configure Gemini AI",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Description
                        Text(
                            text = "To see personalized welcome messages and friendly reminders, please configure your Gemini API key in Profile Settings.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Configure button
                        Button(
                            onClick = {
                                onDismiss()
                                onConfigureClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Go to Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Dismiss button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Maybe Later",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

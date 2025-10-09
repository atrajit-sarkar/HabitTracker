package it.atraj.habittracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import it.atraj.habittracker.auth.GoogleSignInHelper
import it.atraj.habittracker.auth.User
import it.atraj.habittracker.performance.PerformanceManager
import javax.inject.Inject
import it.atraj.habittracker.auth.ui.AuthScreen
import it.atraj.habittracker.auth.ui.AuthViewModel
import it.atraj.habittracker.auth.ui.ProfileScreen
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.util.rememberNavigationHandler
import it.atraj.habittracker.ui.statistics.StatisticsScreen
import it.atraj.habittracker.ui.social.SearchUsersScreen
import it.atraj.habittracker.ui.social.FriendsListScreen
import it.atraj.habittracker.ui.social.LeaderboardScreen
import it.atraj.habittracker.ui.social.FriendProfileScreen
import it.atraj.habittracker.ui.chat.ChatListScreen
import it.atraj.habittracker.ui.chat.ChatScreen
import it.atraj.habittracker.ui.settings.NotificationSetupGuideScreen
import it.atraj.habittracker.ui.settings.LanguageSelectorScreen
import it.atraj.habittracker.email.ui.EmailSettingsScreen
import it.atraj.habittracker.data.firestore.FriendRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HabitTrackerNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "loading",
    googleSignInHelper: GoogleSignInHelper,
    onCheckForUpdates: () -> Unit = {}
) {
    // Prevent navigation black screen by ensuring stable content
    DisposableEffect(navController) {
        // Keep the navigation controller stable
        onDispose { }
    }
    
    // Safe navigation wrapper to prevent black screens and duplicate navigations
    fun safeNavigate(route: String, popUpToRoute: String? = null) {
        try {
            navController.navigate(route) {
                launchSingleTop = true
                popUpToRoute?.let {
                    popUpTo(it) {
                        inclusive = false
                    }
                }
            }
        } catch (e: Exception) {
            // Catch any navigation exceptions to prevent crashes
            android.util.Log.e("Navigation", "Navigation error: ${e.message}")
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Optimized transitions for better performance (reduced from 200ms to 100ms)
        enterTransition = { 
            fadeIn(animationSpec = tween(100)) + 
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(100)
            )
        },
        exitTransition = { 
            fadeOut(animationSpec = tween(100)) + 
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(100)
            )
        },
        popEnterTransition = { 
            fadeIn(animationSpec = tween(100)) + 
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(100)
            )
        },
        popExitTransition = { 
            fadeOut(animationSpec = tween(100)) + 
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(100)
            )
        }
    ) {
        composable("loading") {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            
            // Get PerformanceManager from context
            val context = androidx.compose.ui.platform.LocalContext.current
            val performanceManager = remember { PerformanceManager(context.applicationContext) }
            
            // Log performance info once
            LaunchedEffect(Unit) {
                performanceManager.logPerformanceInfo()
            }
            
            // Navigate based on auth state once it's loaded
            LaunchedEffect(authState.isLoading, authState.user) {
                if (!authState.isLoading) {
                    // Auth state has been checked, now navigate
                    if (authState.user != null) {
                        // User is authenticated, go directly to home
                        navController.navigate("home") {
                            popUpTo("loading") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        // User is not authenticated, go to auth screen
                        navController.navigate("auth") {
                            popUpTo("loading") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
            
            // Loading screen with Trail Loading animation (adaptive performance)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.Asset("trail_loading.json")
                )
                
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true,
                    speed = performanceManager.getAnimationSpeed(),
                    restartOnPlay = true
                )
                
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(performanceManager.getAnimationSize().dp)
                )
            }
        }
        
        composable("auth") {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            
            // If user becomes authenticated, navigate to home
            LaunchedEffect(authState.user) {
                if (authState.user != null) {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }
            
            AuthScreen(
                viewModel = authViewModel,
                googleSignInHelper = googleSignInHelper,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            // Get AuthViewModel for user data
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            
            // Refresh habits when screen becomes visible to recalculate completion states
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshHabitsUI()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            // Track last navigation time for debouncing
            var lastNavigationTime by remember { mutableLongStateOf(0L) }
            
            // Debounced navigation handlers to prevent rapid clicks
            val onAddHabitClick = rememberNavigationHandler { 
                safeNavigate("add_habit")
            }
            val onEditHabitClick: (Long) -> Unit = { habitId ->
                viewModel.exitSelectionMode()
                safeNavigate("edit_habit/$habitId")
            }
            val onTrashClick = rememberNavigationHandler { 
                safeNavigate("trash")
            }
            val onProfileClick = rememberNavigationHandler { 
                safeNavigate("profile")
            }
            val onNotificationGuideClick = rememberNavigationHandler {
                safeNavigate("notification_setup_guide")
            }
            
            HabitHomeRoute(
                state = state,
                user = authState.user,
                onAddHabitClick = onAddHabitClick,
                onToggleReminder = viewModel::toggleReminder,
                onMarkHabitCompleted = viewModel::markHabitCompleted,
                onDeleteHabit = viewModel::deleteHabit,
                onHabitDetailsClick = { habitId -> 
                    // Check debounce before navigating
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime >= 500L) {
                        lastNavigationTime = currentTime
                        safeNavigate("habit_details/$habitId")
                    }
                },
                onTrashClick = onTrashClick,
                onProfileClick = onProfileClick,
                onNotificationGuideClick = onNotificationGuideClick,
                onEditHabitClick = onEditHabitClick,
                onToggleHabitSelection = viewModel::toggleHabitSelection,
                onStartSelectionMode = viewModel::startSelectionMode,
                onExitSelectionMode = viewModel::exitSelectionMode,
                onDeleteSelectedHabits = viewModel::deleteSelectedHabits
            )
        }
        
        composable("add_habit") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val coroutineScope = rememberCoroutineScope()
            var saveTriggered by remember { mutableStateOf(false) }
            
            // Navigate back when save completes
            LaunchedEffect(state.addHabitState.isSaving, saveTriggered) {
                if (saveTriggered && !state.addHabitState.isSaving) {
                    // Small delay to ensure UI updates are visible
                    kotlinx.coroutines.delay(200)
                    navController.popBackStack()
                }
            }
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                viewModel.resetAddHabitState()
                navController.popBackStack()
            }
            
            // Trigger save - navigation happens automatically via LaunchedEffect above
            val onSaveHabit = rememberNavigationHandler {
                coroutineScope.launch {
                    saveTriggered = true
                    viewModel.saveHabit()
                }
            }
            
            AddHabitScreen(
                state = state.addHabitState,
                onHabitNameChange = viewModel::onHabitNameChange,
                onHabitDescriptionChange = viewModel::onHabitDescriptionChange,
                onHabitReminderToggleChange = viewModel::onHabitReminderToggle,
                onHabitTimeChange = viewModel::onHabitTimeChange,
                onHabitFrequencyChange = viewModel::onHabitFrequencyChange,
                onHabitDayOfWeekChange = viewModel::onHabitDayOfWeekChange,
                onHabitDayOfMonthChange = viewModel::onHabitDayOfMonthChange,
                onHabitMonthOfYearChange = viewModel::onHabitMonthOfYearChange,
                onAvatarChange = viewModel::onAvatarChange,
                onNotificationSoundChange = viewModel::onNotificationSoundChange,
                onBackClick = onBackClick,
                onSaveHabit = onSaveHabit
            )
        }
        
        composable("edit_habit/{habitId}") { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId")?.toLongOrNull() ?: return@composable
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val coroutineScope = rememberCoroutineScope()
            var saveTriggered by remember { mutableStateOf(false) }
            
            // Load habit data for editing
            LaunchedEffect(habitId) {
                viewModel.loadHabitForEdit(habitId)
            }
            
            // Navigate back when save completes
            LaunchedEffect(state.addHabitState.isSaving, saveTriggered) {
                if (saveTriggered && !state.addHabitState.isSaving) {
                    // Small delay to ensure UI updates are visible
                    kotlinx.coroutines.delay(200)
                    navController.popBackStack()
                }
            }
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                viewModel.resetAddHabitState()
                navController.popBackStack()
            }
            
            // Trigger save - navigation happens automatically via LaunchedEffect above
            val onSaveHabit = rememberNavigationHandler {
                coroutineScope.launch {
                    saveTriggered = true
                    viewModel.saveHabit()
                }
            }
            
            AddHabitScreen(
                state = state.addHabitState,
                onHabitNameChange = viewModel::onHabitNameChange,
                onHabitDescriptionChange = viewModel::onHabitDescriptionChange,
                onHabitReminderToggleChange = viewModel::onHabitReminderToggle,
                onHabitTimeChange = viewModel::onHabitTimeChange,
                onHabitFrequencyChange = viewModel::onHabitFrequencyChange,
                onHabitDayOfWeekChange = viewModel::onHabitDayOfWeekChange,
                onHabitDayOfMonthChange = viewModel::onHabitDayOfMonthChange,
                onHabitMonthOfYearChange = viewModel::onHabitMonthOfYearChange,
                onAvatarChange = viewModel::onAvatarChange,
                onNotificationSoundChange = viewModel::onNotificationSoundChange,
                onBackClick = onBackClick,
                onSaveHabit = onSaveHabit
            )
        }
        
        composable("habit_details/{habitId}") { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId")?.toLongOrNull() ?: return@composable
            val viewModel: HabitViewModel = hiltViewModel()
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            HabitDetailsRoute(
                habitId = habitId,
                viewModel = viewModel,
                onBackClick = onBackClick
            )
        }
        
        composable("trash") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            TrashScreen(
                deletedHabits = state.deletedHabits,
                onRestoreHabit = viewModel::restoreHabit,
                onPermanentlyDeleteHabit = viewModel::permanentlyDeleteHabit,
                onEmptyTrash = viewModel::emptyTrash,
                onBackClick = onBackClick
            )
        }
        
        composable("profile") {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            // Debounced sign out navigation
            val onSignedOut = rememberNavigationHandler {
                navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                }
            }
            
            // Debounced statistics navigation
            val onStatisticsClick = rememberNavigationHandler {
                safeNavigate("statistics")
            }
            
            // Social features navigation
            val onSearchUsersClick = rememberNavigationHandler {
                safeNavigate("searchUsers")
            }
            
            val onFriendsListClick = rememberNavigationHandler {
                safeNavigate("friendsList")
            }
            
            val onLeaderboardClick = rememberNavigationHandler {
                safeNavigate("leaderboard")
            }
            
            val onNotificationGuideClick = rememberNavigationHandler {
                safeNavigate("notification_setup_guide")
            }
            
            val onLanguageSettingsClick = rememberNavigationHandler {
                safeNavigate("language_selector")
            }
            
            val onEmailSettingsClick = rememberNavigationHandler {
                safeNavigate("email_settings")
            }
            
            ProfileScreen(
                viewModel = authViewModel,
                onBackClick = onBackClick,
                onSignedOut = onSignedOut,
                onStatisticsClick = onStatisticsClick,
                onSearchUsersClick = onSearchUsersClick,
                onFriendsListClick = onFriendsListClick,
                onLeaderboardClick = onLeaderboardClick,
                onNotificationGuideClick = onNotificationGuideClick,
                onCheckForUpdates = onCheckForUpdates,
                onLanguageSettingsClick = onLanguageSettingsClick,
                onEmailSettingsClick = onEmailSettingsClick
            )
        }
        
        composable("statistics") {
            val habitViewModel: HabitViewModel = hiltViewModel()
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            StatisticsScreen(
                viewModel = habitViewModel,
                onBackClick = onBackClick
            )
        }
        
        composable("notification_setup_guide") {
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            NotificationSetupGuideScreen(
                onNavigateBack = onBackClick
            )
        }
        
        composable("language_selector") {
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            LanguageSelectorScreen(
                onBackClick = onBackClick
            )
        }
        
        composable("email_settings") {
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            EmailSettingsScreen(
                onNavigateBack = onBackClick
            )
        }
        
        composable("searchUsers") {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            SearchUsersScreen(
                authViewModel = authViewModel,
                onBackClick = onBackClick
            )
        }
        
        composable("friendsList") {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            FriendsListScreen(
                authViewModel = authViewModel,
                onBackClick = onBackClick,
                onFriendClick = { friendId ->
                    safeNavigate("friendProfile/$friendId")
                }
            )
        }
        
        composable("leaderboard") {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            LeaderboardScreen(
                authViewModel = authViewModel,
                onBackClick = onBackClick
            )
        }
        
        composable("friendProfile/{friendId}") { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            val habitViewModel: HabitViewModel = hiltViewModel()
            
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            val onMessageClick: (String, String, String?, String?) -> Unit = { id, name, avatar, photoUrl ->
                // Navigate to chat screen
                val avatarEncoded = avatar?.let {
                    java.net.URLEncoder.encode(it, "UTF-8")
                } ?: "null"
                val photoUrlEncoded = photoUrl?.let { 
                    java.net.URLEncoder.encode(it, "UTF-8") 
                } ?: "null"
                safeNavigate("chat/$id/$name/$avatarEncoded/$photoUrlEncoded")
            }
            
            // Get FriendRepository from DI
            val context = androidx.compose.ui.platform.LocalContext.current
            val repository = remember {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().let { firestore ->
                    FriendRepository(firestore)
                }
            }
            
            FriendProfileScreen(
                friendId = friendId,
                friendRepository = repository,
                habitViewModel = habitViewModel,
                onBackClick = onBackClick,
                onMessageClick = onMessageClick
            )
        }

        // Chat List Screen
        composable("chatList") {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            val onChatClick: (it.atraj.habittracker.data.firestore.Chat) -> Unit = { chat ->
                // Get the other participant's info
                val currentUserId = authViewModel.uiState.value.user?.uid ?: ""
                val friendId = chat.participants.firstOrNull { it != currentUserId } ?: ""
                val friendName = chat.participantNames[friendId] ?: "Unknown"
                val friendAvatar = chat.participantAvatars[friendId] ?: "😊"
                val friendPhotoUrl = chat.participantPhotoUrls[friendId]
                
                val photoUrlEncoded = friendPhotoUrl?.let { 
                    java.net.URLEncoder.encode(it, "UTF-8") 
                } ?: "null"
                
                safeNavigate("chat/$friendId/$friendName/$friendAvatar/$photoUrlEncoded")
            }
            
            ChatListScreen(
                onBackClick = onBackClick,
                onChatClick = onChatClick
            )
        }

        // Individual Chat Screen
        composable("chat/{friendId}/{friendName}/{friendAvatar}/{friendPhotoUrl}") { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            val friendName = backStackEntry.arguments?.getString("friendName") ?: ""
            val friendAvatar = backStackEntry.arguments?.getString("friendAvatar") ?: "😊"
            val friendPhotoUrlEncoded = backStackEntry.arguments?.getString("friendPhotoUrl")
            val friendPhotoUrl = if (friendPhotoUrlEncoded != null && friendPhotoUrlEncoded != "null") {
                java.net.URLDecoder.decode(friendPhotoUrlEncoded, "UTF-8")
            } else {
                null
            }
            
            val onBackClick = rememberNavigationHandler {
                navController.popBackStack()
            }
            
            ChatScreen(
                friendId = friendId,
                friendName = friendName,
                friendAvatar = friendAvatar,
                friendPhotoUrl = friendPhotoUrl,
                onBackClick = onBackClick
            )
        }
    }
}

@Composable
fun HabitDetailsRoute(
    habitId: Long,
    viewModel: HabitViewModel,
    onBackClick: () -> Unit
) {
    var habit by remember { mutableStateOf<Habit?>(null) }
    var progress by remember { mutableStateOf<HabitProgress?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Function to refresh progress data
    suspend fun refreshProgress() {
        try {
            android.util.Log.d("HabitDetails", "Loading habit with ID: $habitId")
            val loadedHabit = viewModel.getHabitById(habitId)
            android.util.Log.d("HabitDetails", "Habit loaded: ${loadedHabit.title}")
            val loadedProgress = viewModel.getHabitProgress(habitId)
            habit = loadedHabit
            progress = loadedProgress
            error = null

            val creationDate = loadedHabit.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
            val today = LocalDate.now()
            val adjustedDate = when {
                selectedDate.isBefore(creationDate) -> creationDate
                selectedDate.isAfter(today) -> today
                else -> selectedDate
            }
            if (adjustedDate != selectedDate) {
                selectedDate = adjustedDate
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitDetails", "Error loading habit: ${e.message}", e)
            error = "Failed to load habit: ${e.message}"
            habit = null
            progress = null
        }
    }

    // Initial load only shows full screen loader
    LaunchedEffect(habitId) {
        isLoading = true
        refreshProgress()
        isLoading = false
    }

    // Subsequent refreshes (e.g. after marking completion) should not blank the screen
    LaunchedEffect(refreshTrigger) {
        if (!isLoading) {
            refreshProgress()
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.Asset("loading_sand_clock.json")
            )
            
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                isPlaying = true,
                speed = 1f,
                restartOnPlay = true
            )
            
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(120.dp)
            )
        }
    } else {
        val currentHabit = habit
        val currentProgress = progress
        
        if (error != null) {
            // Error state
            val coroutineScope = rememberCoroutineScope()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Error loading habit",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            refreshProgress()
                            isLoading = false
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
        } else if (currentHabit != null && currentProgress != null) {
            val isSelectedDateCompleted = currentProgress.completedDates.contains(selectedDate)
            HabitDetailsScreen(
                habit = currentHabit,
                progress = currentProgress,
                selectedDate = selectedDate,
                isSelectedDateCompleted = isSelectedDateCompleted,
                onBackClick = onBackClick,
                onSelectDate = { date ->
                    selectedDate = date
                },
                onMarkCompleted = {
                    viewModel.markHabitCompletedForDate(habitId, selectedDate)
                    // Trigger a lightweight refresh without showing the full-screen loader
                    refreshTrigger++
                }
            )
        } else {
            // Error state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Habit not found")
            }
        }
    }
}

package com.example.habittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.habittracker.auth.GoogleSignInHelper
import com.example.habittracker.auth.User
import com.example.habittracker.auth.ui.AuthScreen
import com.example.habittracker.auth.ui.AuthViewModel
import com.example.habittracker.auth.ui.ProfileScreen
import com.example.habittracker.data.local.Habit
import com.example.habittracker.util.rememberNavigationHandler
import com.example.habittracker.ui.statistics.StatisticsScreen
import com.example.habittracker.ui.social.SearchUsersScreen
import com.example.habittracker.ui.social.FriendsListScreen
import com.example.habittracker.ui.social.LeaderboardScreen
import com.example.habittracker.ui.social.FriendProfileScreen
import com.example.habittracker.ui.chat.ChatListScreen
import com.example.habittracker.ui.chat.ChatScreen
import com.example.habittracker.ui.settings.NotificationSetupGuideScreen
import com.example.habittracker.data.firestore.FriendRepository
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
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("loading") {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            
            // Navigate based on auth state once it's loaded
            LaunchedEffect(authState.isLoading, authState.user) {
                if (!authState.isLoading) {
                    // Auth state has been checked, now navigate
                    if (authState.user != null) {
                        // User is authenticated, go directly to home
                        navController.navigate("home") {
                            popUpTo("loading") { inclusive = true }
                        }
                    } else {
                        // User is not authenticated, go to auth screen
                        navController.navigate("auth") {
                            popUpTo("loading") { inclusive = true }
                        }
                    }
                }
            }
            
            // Loading screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                navController.navigate("add_habit") 
            }
            val onTrashClick = rememberNavigationHandler { 
                navController.navigate("trash") 
            }
            val onProfileClick = rememberNavigationHandler { 
                navController.navigate("profile") 
            }
            val onNotificationGuideClick = rememberNavigationHandler {
                navController.navigate("notification_setup_guide")
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
                        navController.navigate("habit_details/$habitId")
                    }
                },
                onTrashClick = onTrashClick,
                onProfileClick = onProfileClick,
                onNotificationGuideClick = onNotificationGuideClick
            )
        }
        
        composable("add_habit") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            // Debounced back navigation
            val onBackClick = rememberNavigationHandler {
                viewModel.resetAddHabitState()
                navController.popBackStack()
            }
            
            // Debounced save action
            val onSaveHabit = rememberNavigationHandler {
                viewModel.saveHabit()
                navController.popBackStack()
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
                navController.navigate("statistics")
            }
            
            // Social features navigation
            val onSearchUsersClick = rememberNavigationHandler {
                navController.navigate("searchUsers")
            }
            
            val onFriendsListClick = rememberNavigationHandler {
                navController.navigate("friendsList")
            }
            
            val onLeaderboardClick = rememberNavigationHandler {
                navController.navigate("leaderboard")
            }
            
            val onNotificationGuideClick = rememberNavigationHandler {
                navController.navigate("notification_setup_guide")
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
                onCheckForUpdates = onCheckForUpdates
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
                    navController.navigate("friendProfile/$friendId")
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
            
            val onMessageClick: (String, String, String, String?) -> Unit = { id, name, avatar, photoUrl ->
                // Navigate to chat screen
                val photoUrlEncoded = photoUrl?.let { 
                    java.net.URLEncoder.encode(it, "UTF-8") 
                } ?: "null"
                navController.navigate("chat/$id/$name/$avatar/$photoUrlEncoded")
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
            
            val onChatClick: (com.example.habittracker.data.firestore.Chat) -> Unit = { chat ->
                // Get the other participant's info
                val currentUserId = authViewModel.uiState.value.user?.uid ?: ""
                val friendId = chat.participants.firstOrNull { it != currentUserId } ?: ""
                val friendName = chat.participantNames[friendId] ?: "Unknown"
                val friendAvatar = chat.participantAvatars[friendId] ?: "😊"
                val friendPhotoUrl = chat.participantPhotoUrls[friendId]
                
                val photoUrlEncoded = friendPhotoUrl?.let { 
                    java.net.URLEncoder.encode(it, "UTF-8") 
                } ?: "null"
                
                navController.navigate("chat/$friendId/$friendName/$friendAvatar/$photoUrlEncoded")
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
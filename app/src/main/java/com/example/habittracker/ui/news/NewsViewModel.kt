package it.atraj.habittracker.ui.news

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.atraj.habittracker.data.local.AppNews
import it.atraj.habittracker.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    private val TAG = "NewsViewModel"
    
    private val _news = MutableStateFlow<List<AppNews>>(emptyList())
    val news: StateFlow<List<AppNews>> = _news.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        Log.d(TAG, "NewsViewModel initialized")
        loadNews()
        loadUnreadCountRealtime()
    }
    
    private fun loadNews() {
        viewModelScope.launch {
            try {
                newsRepository.getNewsFlow().collect { newsList ->
                    _news.value = newsList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadUnreadCountRealtime() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting to collect unread count flow")
                newsRepository.getUnreadCountFlow().collect { count ->
                    Log.d(TAG, "Unread count updated in ViewModel: $count")
                    _unreadCount.value = count
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting unread count", e)
                _unreadCount.value = 0
            }
        }
    }
    
    fun markAsRead(newsId: String) {
        viewModelScope.launch {
            try {
                newsRepository.markAsRead(newsId)
                // No need to reload - real-time listener will update automatically
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                newsRepository.markAllAsRead(_news.value)
                // No need to reload - real-time listener will update automatically
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun refreshNews() {
        // News is already real-time, but this can force a refresh if needed
        loadNews()
    }
}

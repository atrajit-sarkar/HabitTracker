package it.atraj.habittracker.ui.news

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
    
    private val _news = MutableStateFlow<List<AppNews>>(emptyList())
    val news: StateFlow<List<AppNews>> = _news.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadNews()
        loadUnreadCount()
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
    
    private fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val count = newsRepository.getUnreadCount()
                _unreadCount.value = count
            } catch (e: Exception) {
                _unreadCount.value = 0
            }
        }
    }
    
    fun markAsRead(newsId: String) {
        viewModelScope.launch {
            try {
                newsRepository.markAsRead(newsId)
                loadUnreadCount()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                newsRepository.markAllAsRead(_news.value)
                _unreadCount.value = 0
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun refreshNews() {
        loadNews()
        loadUnreadCount()
    }
}

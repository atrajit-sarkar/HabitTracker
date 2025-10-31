package it.atraj.habittracker.gemini

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Service for making requests to Google's Gemini 2.5 Flash API
 */
class GeminiApiService(private val apiKey: String) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    companion object {
        private const val TAG = "GeminiApiService"
        // Using gemini-2.0-flash-exp as it's the latest experimental version with improved performance
        // Alternative: Use "gemini-1.5-flash" for stable version
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    }
    
    /**
     * Generate a personalized welcome message
     */
    suspend fun generateWelcomeMessage(userName: String): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate a warm, friendly, and personalized welcome message for $userName who just opened their habit tracking app.
            The user has no overdue tasks - they're on track!
            
            Requirements:
            - Be enthusiastic and encouraging
            - Keep it short (2-3 sentences maximum)
            - Use a friendly, casual tone
            - Motivate them to maintain their good habits
            - Don't use emojis
            - Address them by name
            
            Generate ONLY the welcome message text, nothing else.
        """.trimIndent()
        
        generateContent(prompt)
    }
    
    /**
     * Generate an overdue task message with a slightly angry but caring tone
     */
    suspend fun generateOverdueMessage(userName: String, overdueCount: Int): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate a message for $userName who has $overdueCount overdue habit(s) in their habit tracking app.
            
            Requirements:
            - Use a slightly frustrated/disappointed tone (like a caring friend who's a bit annoyed)
            - Be firm but still supportive and caring
            - Mention the specific number of overdue tasks ($overdueCount)
            - Encourage them to complete the tasks immediately
            - Keep it short (2-3 sentences maximum)
            - Don't be too harsh - remember you're trying to help them
            - Don't use emojis
            - Address them by name
            
            Generate ONLY the message text, nothing else.
        """.trimIndent()
        
        generateContent(prompt)
    }
    
    /**
     * Generate a congratulatory goodnight message for completing all habits
     */
    suspend fun generateCompletionGoodnightMessage(userName: String, completedCount: Int): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate a warm, congratulatory goodnight message for $userName who has successfully completed ALL $completedCount habit(s) today in their habit tracking app.
            
            Requirements:
            - Congratulate them on their achievement
            - Express pride and encouragement
            - Mention they completed all $completedCount habits
            - Wish them a good night's rest
            - Encourage them to come back tomorrow and continue their streak
            - Use a warm, proud, and caring tone (like a supportive friend or mentor)
            - Keep it short (2-3 sentences maximum)
            - Don't use emojis
            - Address them by name
            - Make it feel special and rewarding
            
            Generate ONLY the message text, nothing else.
        """.trimIndent()
        
        generateContent(prompt)
    }
    
    /**
     * Generate an aggressive motivational message for severely overdue habits (6+ hours)
     */
    suspend fun generateAggressiveMotivationalMessage(
        userName: String,
        habitTitle: String,
        habitDescription: String,
        hoursOverdue: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate a VERY firm, aggressive, and motivational message for $userName who has NOT completed their habit "$habitTitle" for $hoursOverdue hours.
            
            Habit description: "$habitDescription"
            
            Requirements:
            - Use an AGGRESSIVE and URGENT tone (this is serious!)
            - Be VERY direct and firm - no more gentle reminders
            - Include a powerful motivational quote or statement related to the habit's purpose
            - Reference the specific habit ("$habitTitle") and its description to make it personal
            - Emphasize the consequences of continued delay
            - Push them to take immediate action NOW
            - Use commanding language (but not insulting)
            - Keep it 2-3 sentences maximum
            - Don't use emojis
            - Address them by name
            - Make them feel the urgency and importance
            
            Generate ONLY the message text, nothing else.
        """.trimIndent()
        
        generateContent(prompt)
    }
    
    /**
     * Generate a custom message with a specific prompt
     * Public method for custom use cases like overdue notifications
     */
    suspend fun generateCustomMessage(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        generateContent(prompt)
    }
    
    /**
     * Generate content using Gemini API
     */
    private suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                )
            )
            
            val jsonBody = json.encodeToString(GeminiRequest.serializer(), requestBody)
            
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            Log.d(TAG, "Making Gemini API request...")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} - $responseBody")
                return@withContext Result.failure(
                    Exception("API request failed: ${response.code} - ${response.message}")
                )
            }
            
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Empty response from Gemini API")
                return@withContext Result.failure(Exception("Empty response from API"))
            }
            
            Log.d(TAG, "Gemini API response received: ${responseBody.take(200)}")
            
            val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), responseBody)
            
            val generatedText = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
            
            if (generatedText.isNullOrEmpty()) {
                Log.e(TAG, "No text generated in response")
                return@withContext Result.failure(Exception("No text generated"))
            }
            
            Log.d(TAG, "Successfully generated text: $generatedText")
            Result.success(generatedText)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content", e)
            Result.failure(e)
        }
    }
    
    // Data classes for Gemini API request/response
    @Serializable
    data class GeminiRequest(
        val contents: List<Content>
    )
    
    @Serializable
    data class Content(
        val parts: List<Part>
    )
    
    @Serializable
    data class Part(
        val text: String
    )
    
    @Serializable
    data class GeminiResponse(
        val candidates: List<Candidate>? = null
    )
    
    @Serializable
    data class Candidate(
        val content: Content? = null
    )
}

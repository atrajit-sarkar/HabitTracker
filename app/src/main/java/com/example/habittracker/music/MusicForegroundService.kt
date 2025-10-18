package it.atraj.habittracker.music

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R

class MusicForegroundService : Service() {
    
    private val binder = MusicBinder()
    private var musicManager: BackgroundMusicManager? = null
    private lateinit var mediaSession: MediaSessionCompat
    private var currentTrackName: String = ""
    private var isPlayingState: Boolean = false
    
    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "it.atraj.habittracker.ACTION_PLAY"
        const val ACTION_PAUSE = "it.atraj.habittracker.ACTION_PAUSE"
        const val ACTION_STOP = "it.atraj.habittracker.ACTION_STOP"
        
        private var serviceInstance: MusicForegroundService? = null
        
        fun isRunning(): Boolean = serviceInstance != null
    }
    
    inner class MusicBinder : Binder() {
        fun getService(): MusicForegroundService = this@MusicForegroundService
    }
    
    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        createNotificationChannel()
        initializeMediaSession()
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                isPlayingState = true
                musicManager?.resumeMusic()
                updateNotification()
            }
            ACTION_PAUSE -> {
                isPlayingState = false
                musicManager?.pauseMusic()
                updateNotification()
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
            else -> {
                // Start foreground with initial notification
                // Don't interrupt music if it's already playing
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }
        return START_STICKY
    }
    
    fun setMusicManager(manager: BackgroundMusicManager) {
        this.musicManager = manager
        Log.d("MusicService", "Music manager set")
    }
    
    fun updateTrackInfo(trackName: String, isPlaying: Boolean) {
        currentTrackName = trackName
        isPlayingState = isPlaying
        updateNotification()
        Log.d("MusicService", "Track updated: $trackName, playing: $isPlaying")
    }
    
    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicForegroundService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
                    )
                    .build()
            )
            isActive = true
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Intent to open app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Play/Pause action
        val playPauseAction = if (isPlayingState) {
            val pauseIntent = Intent(this, MusicForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                R.drawable.ic_pause,
                "Pause",
                pausePendingIntent
            ).build()
        } else {
            val playIntent = Intent(this, MusicForegroundService::class.java).apply {
                action = ACTION_PLAY
            }
            val playPendingIntent = PendingIntent.getService(
                this, 2, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                R.drawable.ic_play_arrow,
                "Play",
                playPendingIntent
            ).build()
        }
        
        // Stop action
        val stopIntent = Intent(this, MusicForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.ic_close_24,
            "Stop",
            stopPendingIntent
        ).build()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTrackName.ifEmpty { "Music Playing" })
            .setContentText("Habit Tracker Music")
            .setSmallIcon(R.drawable.ic_music_note)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setContentIntent(openPendingIntent)
            .setOngoing(isPlayingState)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
    
    fun stopForegroundService() {
        isPlayingState = false
        musicManager?.stopMusic()
        mediaSession.isActive = false
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        serviceInstance = null
    }
    
    override fun onDestroy() {
        serviceInstance = null
        super.onDestroy()
    }
}

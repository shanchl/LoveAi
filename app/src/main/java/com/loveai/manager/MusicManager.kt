package com.loveai.manager

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import com.loveai.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object MusicManager {

    data class Song(
        val id: Int,
        val key: String,
        val name: String,
        val resourceId: Int = 0,
        val filePath: String? = null,
        val uri: Uri? = null
    )

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var isPlaying = false
    private var currentContext: Context? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongIndex: Int = 0
    private var playMode: PlayMode = PlayMode.LOOP
    private const val PREFS_NAME = "loveai_music"
    private const val KEY_EXTERNAL_SONGS = "external_songs"

    enum class PlayMode {
        LOOP,
        SINGLE,
        RANDOM
    }

    fun init(context: Context, resourceId: Int = 0) {
        currentContext = context.applicationContext

        try {
            release()

            if (resourceId != 0) {
                mediaPlayer = MediaPlayer.create(context, resourceId)
            } else {
                val defaultResourceId = context.resources.getIdentifier(
                    "bg_music", "raw", context.packageName
                )
                if (defaultResourceId != 0) {
                    mediaPlayer = MediaPlayer.create(context, defaultResourceId)
                }
            }

            setupMediaPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
            isPrepared = false
        }
    }

    fun initPlaylist(context: Context, songs: List<Song>) {
        currentContext = context.applicationContext
        playlist = songs
        currentSongIndex = 0

        if (songs.isNotEmpty()) {
            playSong(0)
        }
    }

    fun initAutoPlaylist(context: Context) {
        val songs = scanRawMusicResources(context) + loadExternalSongs(context)
        if (songs.isNotEmpty()) {
            initPlaylist(context, songs.distinctBy { it.key })
        } else {
            init(context)
        }
    }

    private fun scanRawMusicResources(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val packageName = context.packageName
        val excludeNames = setOf(
            "ic_launcher",
            "ic_launcher_background",
            "ic_launcher_foreground",
            "mipmap_anydpi_v26",
            "mipmap_hdpi",
            "mipmap_mdpi",
            "mipmap_xhdpi",
            "mipmap_xxhdpi",
            "mipmap_xxxhdpi"
        )

        try {
            val rawClass = R.raw::class.java
            val fields = rawClass.declaredFields

            for ((index, field) in fields.withIndex()) {
                try {
                    val rawName = field.name
                    if (excludeNames.contains(rawName.lowercase(Locale.ROOT))) {
                        continue
                    }

                    field.isAccessible = true
                    val resourceId = field.getInt(null)
                    val stringResName = "music_$rawName"
                    val stringResId = context.resources.getIdentifier(
                        stringResName,
                        "string",
                        packageName
                    )

                    val songName = if (stringResId != 0) {
                        context.getString(stringResId)
                    } else {
                        rawName.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                    }

                    songs.add(
                        Song(
                            id = index,
                            key = rawName,
                            name = songName,
                            resourceId = resourceId
                        )
                    )
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songs.sortedBy { it.name }
    }

    private fun playSong(index: Int) {
        if (playlist.isEmpty()) return
        if (index !in playlist.indices) return

        currentSongIndex = index
        val song = playlist[index]

        try {
            release()

            mediaPlayer = when {
                song.resourceId != 0 && currentContext != null ->
                    MediaPlayer.create(currentContext, song.resourceId)
                song.filePath != null ->
                    MediaPlayer().apply { setDataSource(song.filePath) }
                song.uri != null ->
                    MediaPlayer.create(currentContext, song.uri)
                else -> null
            }

            if (mediaPlayer != null) {
                setupMediaPlayer()
                mediaPlayer?.setOnCompletionListener { onSongComplete() }
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPrepared = false
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayer?.apply {
            isLooping = false
            setVolume(0.8f, 0.8f)
            isPrepared = true
        }
    }

    private fun onSongComplete() {
        when (playMode) {
            PlayMode.LOOP -> next()
            PlayMode.SINGLE -> mediaPlayer?.start()
            PlayMode.RANDOM -> playSong(playlist.indices.random())
        }
    }

    fun next() {
        if (playlist.isEmpty()) return
        val nextIndex = when (playMode) {
            PlayMode.RANDOM -> playlist.indices.random()
            else -> (currentSongIndex + 1) % playlist.size
        }
        playSong(nextIndex)
    }

    fun previous() {
        if (playlist.isEmpty()) return
        val prevIndex = when (playMode) {
            PlayMode.RANDOM -> playlist.indices.random()
            else -> if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
        }
        playSong(prevIndex)
    }

    fun setPlayMode(mode: PlayMode) {
        playMode = mode
        mediaPlayer?.isLooping = false
    }

    fun getPlayMode(): PlayMode = playMode

    fun getCurrentSongName(): String {
        return if (playlist.isNotEmpty() && currentSongIndex < playlist.size) {
            playlist[currentSongIndex].name
        } else {
            "\u672a\u77e5\u6b4c\u66f2"
        }
    }

    fun getCurrentSongKey(): String? {
        return playlist.getOrNull(currentSongIndex)?.key
    }

    fun getCurrentSongIndex(): Int = currentSongIndex

    fun getPlaylist(): List<Song> = playlist

    fun playByIndex(index: Int) {
        playSong(index)
    }

    fun playByKey(key: String): Boolean {
        val index = playlist.indexOfFirst { it.key == key }
        if (index < 0) return false
        playSong(index)
        return true
    }

    fun ensurePlaylist(context: Context) {
        if (playlist.isEmpty()) {
            initAutoPlaylist(context)
        }
    }

    fun registerExternalSong(context: Context, uri: Uri, name: String): String {
        val songKey = "local:$uri"
        val previousSongKey = getCurrentSongKey()
        val wasPlaying = isMusicPlaying()
        val songs = loadExternalSongs(context).toMutableList()
        val existingIndex = songs.indexOfFirst { it.key == songKey }
        val song = Song(
            id = songs.size + 10_000,
            key = songKey,
            name = name,
            uri = uri
        )
        if (existingIndex >= 0) {
            songs[existingIndex] = song
        } else {
            songs += song
        }
        persistExternalSongs(context, songs)
        initAutoPlaylist(context)
        if (!previousSongKey.isNullOrBlank()) {
            playByKey(previousSongKey)
        }
        if (!wasPlaying) {
            pause()
        }
        return songKey
    }

    fun play() {
        if (isPrepared && mediaPlayer != null && !isPlaying) {
            try {
                mediaPlayer?.start()
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        if (isPlaying && mediaPlayer != null) {
            try {
                mediaPlayer?.pause()
                isPlaying = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.stop()
                isPlaying = false
                isPrepared = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            isPrepared = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggle(): Boolean {
        return if (isPlaying) {
            pause()
            false
        } else {
            play()
            true
        }
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(v, v)
    }

    fun isMusicPlaying(): Boolean = isPlaying

    fun isReady(): Boolean = isPrepared

    fun hasInitializedPlayback(): Boolean {
        return mediaPlayer != null || playlist.isNotEmpty()
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initFromFile(context: Context, filePath: String) {
        init(context)
    }

    fun initFromUri(context: Context, uri: Uri) {
        init(context)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadExternalSongs(context: Context): List<Song> {
        val raw = getPrefs(context).getString(KEY_EXTERNAL_SONGS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val songs = mutableListOf<Song>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val uriValue = item.optString("uri")
            if (uriValue.isBlank()) continue
            val key = item.optString("key").ifBlank { "local:$uriValue" }
            val name = item.optString("name").ifBlank { "\u672c\u5730\u97f3\u4e50" }
            songs += Song(
                id = 10_000 + index,
                key = key,
                name = name,
                uri = Uri.parse(uriValue)
            )
        }
        return songs
    }

    private fun persistExternalSongs(context: Context, songs: List<Song>) {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(
                JSONObject().apply {
                    put("key", song.key)
                    put("name", song.name)
                    put("uri", song.uri?.toString())
                }
            )
        }
        getPrefs(context).edit().putString(KEY_EXTERNAL_SONGS, array.toString()).apply()
    }
}

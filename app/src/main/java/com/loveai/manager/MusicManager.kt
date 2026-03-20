package com.loveai.manager

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.loveai.R
import java.util.Locale

/**
 * 背景音乐管理器
 * 支持播放列表、循环播放、上一首/下一首
 */
object MusicManager {

    // 歌曲数据结构
    data class Song(
        val id: Int,
        val name: String,
        val resourceId: Int = 0,
        val filePath: String? = null,
        val uri: Uri? = null
    )

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var isPlaying = false
    private var currentContext: Context? = null

    // 播放列表相关
    private var playlist: List<Song> = emptyList()
    private var currentSongIndex: Int = 0
    private var playMode: PlayMode = PlayMode.LOOP  // 默认列表循环

    // 播放模式
    enum class PlayMode {
        LOOP,      // 列表循环
        SINGLE,    // 单曲循环
        RANDOM     // 随机播放
    }

    /**
     * 初始化音乐播放器（单首）
     * @param context 上下文
     * @param resourceId 音乐资源ID
     */
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

    /**
     * 初始化播放列表
     * @param context 上下文
     * @param songs 歌曲列表
     */
    fun initPlaylist(context: Context, songs: List<Song>) {
        currentContext = context.applicationContext
        playlist = songs
        currentSongIndex = 0

        if (songs.isNotEmpty()) {
            playSong(0)
        }
    }

    /**
     * 自动扫描 res/raw 目录下的所有音乐文件，生成播放列表
     * 文件名即为歌曲名（不含扩展名）
     * @param context 上下文
     */
    fun initAutoPlaylist(context: Context) {
        val songs = scanRawMusicResources(context)
        if (songs.isNotEmpty()) {
            initPlaylist(context, songs)
        } else {
            // 如果没有扫描到音乐，回退到默认方式
            init(context)
        }
    }

    /**
     * 扫描 res/raw 目录下的所有音乐资源
     * 文件名使用英文，歌名通过 strings.xml 映射（格式：music_文件名）
     */
    private fun scanRawMusicResources(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val packageName = context.packageName
        
        // 排除已知的非音乐资源
        val excludeNames = setOf(
            "ic_launcher", "ic_launcher_background", "ic_launcher_foreground",
            "mipmap_anydpi_v26", "mipmap_hdpi", "mipmap_mdpi", "mipmap_xhdpi", 
            "mipmap_xxhdpi", "mipmap_xxxhdpi"
        )
        
        try {
            // 通过反射获取 R.raw 类的所有字段
            val rawClass = R.raw::class.java
            val fields = rawClass.declaredFields
            
            for ((index, field) in fields.withIndex()) {
                try {
                    val rawName = field.name
                    
                    // 跳过非音乐资源
                    if (excludeNames.contains(rawName.lowercase(Locale.ROOT))) {
                        continue
                    }
                    
                    // 获取资源 ID
                    field.isAccessible = true
                    val resourceId = field.getInt(null)
                    
                    // 优先从 strings.xml 获取中文歌名，格式：music_文件名
                    val stringResName = "music_$rawName"
                    val stringResId = context.resources.getIdentifier(
                        stringResName, "string", packageName
                    )
                    
                    val songName = if (stringResId != 0) {
                        // 从 strings.xml 获取中文名
                        context.getString(stringResId)
                    } else {
                        // 没有配置则使用资源名
                        rawName.replace("_", " ")
                            .replaceFirstChar { it.uppercaseChar() }
                    }
                    
                    songs.add(Song(
                        id = index,
                        name = songName,
                        resourceId = resourceId
                    ))
                } catch (e: Exception) {
                    // 跳过无效字段
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 按歌曲名排序
        return songs.sortedBy { it.name }
    }

    /**
     * 播放指定歌曲
     */
    private fun playSong(index: Int) {
        if (playlist.isEmpty()) return
        if (index < 0 || index >= playlist.size) return

        currentSongIndex = index
        val song = playlist[index]

        try {
            release()

            mediaPlayer = when {
                song.resourceId != 0 && currentContext != null -> 
                    MediaPlayer.create(currentContext, song.resourceId)
                song.filePath != null -> {
                    MediaPlayer().apply { setDataSource(song.filePath) }
                }
                song.uri != null -> MediaPlayer.create(currentContext, song.uri)
                else -> null
            }

            if (mediaPlayer != null) {
                setupMediaPlayer()

                // 设置播放完成监听
                mediaPlayer?.setOnCompletionListener {
                    onSongComplete()
                }

                // 自动播放
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPrepared = false
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayer?.apply {
            isLooping = playMode == PlayMode.LOOP  // 只有列表循环模式下才循环
            setVolume(0.8f, 0.8f)
            isPrepared = true
        }
    }

    /**
     * 歌曲播放完成时的处理
     */
    private fun onSongComplete() {
        when (playMode) {
            PlayMode.LOOP -> {
                // 播放下一首
                next()
            }
            PlayMode.SINGLE -> {
                // 重新播放当前歌曲
                mediaPlayer?.start()
            }
            PlayMode.RANDOM -> {
                // 随机播放下一首
                val randomIndex = (playlist.indices).random()
                playSong(randomIndex)
            }
        }
    }

    /**
     * 播放下一首
     */
    fun next() {
        if (playlist.isEmpty()) return

        val nextIndex = when (playMode) {
            PlayMode.RANDOM -> (playlist.indices).random()
            else -> (currentSongIndex + 1) % playlist.size
        }
        playSong(nextIndex)
    }

    /**
     * 播放上一首
     */
    fun previous() {
        if (playlist.isEmpty()) return

        val prevIndex = when (playMode) {
            PlayMode.RANDOM -> (playlist.indices).random()
            else -> if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
        }
        playSong(prevIndex)
    }

    /**
     * 设置播放模式
     */
    fun setPlayMode(mode: PlayMode) {
        playMode = mode
        mediaPlayer?.isLooping = (mode == PlayMode.LOOP)
    }

    /**
     * 获取当前播放模式
     */
    fun getPlayMode(): PlayMode = playMode

    /**
     * 获取当前歌曲名称
     */
    fun getCurrentSongName(): String {
        return if (playlist.isNotEmpty() && currentSongIndex < playlist.size) {
            playlist[currentSongIndex].name
        } else {
            "未知歌曲"
        }
    }

    /**
     * 获取当前歌曲索引
     */
    fun getCurrentSongIndex(): Int = currentSongIndex

    /**
     * 获取播放列表
     */
    fun getPlaylist(): List<Song> = playlist

    /**
     * 播放指定索引的歌曲
     */
    fun playByIndex(index: Int) {
        playSong(index)
    }

    /**
     * 开始播放音乐
     */
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

    /**
     * 暂停音乐
     */
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

    /**
     * 停止音乐
     */
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

    /**
     * 释放资源
     */
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

    /**
     * 切换播放/暂停
     * @return 切换后的播放状态
     */
    fun toggle(): Boolean {
        return if (isPlaying) {
            pause()
            false
        } else {
            play()
            true
        }
    }

    /**
     * 设置音量 (0.0 - 1.0)
     */
    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(v, v)
    }

    /**
     * 获取当前播放状态
     */
    fun isMusicPlaying(): Boolean = isPlaying

    /**
     * 是否已准备就绪
     */
    fun isReady(): Boolean = isPrepared

    fun hasInitializedPlayback(): Boolean {
        return mediaPlayer != null || playlist.isNotEmpty()
    }

    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取音乐总时长（毫秒）
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============ 兼容旧版本 ============

    /**
     * 从文件路径加载音乐（兼容旧版本）
     */
    fun initFromFile(context: Context, filePath: String) {
        init(context)
    }

    /**
     * 从Uri加载音乐（兼容旧版本）
     */
    fun initFromUri(context: Context, uri: Uri) {
        init(context)
    }
}

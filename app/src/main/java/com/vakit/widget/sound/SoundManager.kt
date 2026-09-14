package com.vakit.widget.sound

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.xentros.vakitwidget.R

/**
 * Plays built-in and custom alarm sounds. Uses a single MediaPlayer instance,
 * so playing a new sound stops the previous one.
 */
object SoundManager {

    const val DEFAULT_SOUND_KEY = "builtin_azan_v1"
    const val BUILTIN_PREFIX = "builtin_"

    data class BuiltInSound(
        val key: String,
        val labelRes: Int,
        val rawRes: Int,
    )

    fun builtInSounds(): List<BuiltInSound> = listOf(
        BuiltInSound("builtin_azan_v2", R.string.alarm_sound_chime, R.raw.azan_v2),
        BuiltInSound("builtin_azan_v3", R.string.alarm_sound_call, R.raw.azan_v3),
        BuiltInSound("builtin_azan_v4", R.string.alarm_sound_dawn, R.raw.azan_v4),
    )

    private var player: MediaPlayer? = null

    /** Resolves the raw resource for a built-in key, or null for custom URIs. */
    private fun builtInRawFor(soundUri: String?): Int? {
        if (soundUri.isNullOrBlank()) return R.raw.azan_v1
        if (soundUri.startsWith(BUILTIN_PREFIX)) {
            return builtInSounds().firstOrNull { it.key == soundUri }?.rawRes
        }
        return null
    }

    /**
     * Plays the selected sound. [soundUri] is either a built-in key (or null for
     * the default chime) or a content URI. If the custom sound cannot be loaded
     * it falls back to the default chime instead of failing.
     */
    fun play(context: Context, soundUri: String?, onCompletion: (() -> Unit)? = null) {
        stop()
        val mp = MediaPlayer()
        try {
            val raw = builtInRawFor(soundUri)
            if (raw != null) {
                mp.setDataSource(context, Uri.parse("android.resource://${context.packageName}/$raw"))
            } else {
                mp.setDataSource(context, Uri.parse(soundUri))
            }
            mp.isLooping = false
            mp.setOnCompletionListener {
                stop()
                onCompletion?.invoke()
            }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            runCatching {
                mp.reset()
                mp.setDataSource(context, Uri.parse("android.resource://${context.packageName}/${R.raw.azan_v1}"))
                mp.isLooping = false
                mp.setOnCompletionListener {
                    stop()
                    onCompletion?.invoke()
                }
                mp.prepare()
                mp.start()
                player = mp
            }.onFailure { mp.release() }
        }
    }

    /** Plays one of the built-in sounds (used for previews). */
    fun playBuiltIn(context: Context, rawRes: Int) {
        stop()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(context, Uri.parse("android.resource://${context.packageName}/$rawRes"))
            mp.isLooping = false
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            mp.release()
        }
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }
}
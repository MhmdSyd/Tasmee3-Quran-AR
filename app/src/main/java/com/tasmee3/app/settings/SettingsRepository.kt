package com.tasmee3.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.tasmee3.app.match.RevealMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings stored on the device (SharedPreferences). One shared instance so screens stay in sync. */
class SettingsRepository private constructor(private val prefs: SharedPreferences) {
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<RecitationSettings> = _settings.asStateFlow()

    fun update(change: (RecitationSettings) -> RecitationSettings) {
        val next = change(_settings.value)
        prefs.edit()
            .putString(K_MODE, next.revealMode.name)
            .putBoolean(K_AUTO_HINT, next.autoHint)
            .putBoolean(K_SECOND_TRY, next.revealAfterSecondTry)
            .putBoolean(K_SOUND, next.errorSound)
            .apply()
        _settings.value = next
    }

    private fun load() = RecitationSettings(
        revealMode = runCatching { RevealMode.valueOf(prefs.getString(K_MODE, null) ?: "") }.getOrDefault(RevealMode.WORD),
        autoHint = prefs.getBoolean(K_AUTO_HINT, true),
        revealAfterSecondTry = prefs.getBoolean(K_SECOND_TRY, true),
        errorSound = prefs.getBoolean(K_SOUND, false),
    )

    companion object {
        private const val K_MODE = "reveal_mode"
        private const val K_AUTO_HINT = "auto_hint"
        private const val K_SECOND_TRY = "reveal_after_second_try"
        private const val K_SOUND = "error_sound"
        @Volatile private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
            instance ?: SettingsRepository(
                context.applicationContext.getSharedPreferences("tasmee3_settings", Context.MODE_PRIVATE)
            ).also { instance = it }
        }
    }
}

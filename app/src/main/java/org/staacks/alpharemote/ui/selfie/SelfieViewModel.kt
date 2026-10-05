package org.staacks.alpharemote.ui.selfie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.staacks.alpharemote.SettingsStore
import org.staacks.alpharemote.selfie.SelfieSettings

/** Activity-owned preferences survive navigation and serialize rapid setting changes. */
class SelfieViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val _settings = MutableStateFlow<SelfieSettings?>(null)
    val settings = _settings.asStateFlow()
    private val saves = Channel<SelfieSettings>(Channel.CONFLATED)
    private var requested: SelfieSettings? = null

    init {
        viewModelScope.launch {
            store.selfieSettings.collect { saved ->
                if (requested == null || requested == saved) {
                    requested = null
                    _settings.value = saved
                }
            }
        }
        viewModelScope.launch {
            for (value in saves) store.setSelfieSettings(value)
        }
    }

    fun update(value: SelfieSettings) {
        if (_settings.value == null) return
        requested = value
        _settings.value = value
        saves.trySend(value)
    }
}

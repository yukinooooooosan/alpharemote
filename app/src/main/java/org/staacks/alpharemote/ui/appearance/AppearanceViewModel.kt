package org.staacks.alpharemote.ui.appearance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.staacks.alpharemote.SettingsStore

/** Activity-owned appearance. Skin changes never update shooting or pairing settings. */
class AppearanceViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val _skin = MutableStateFlow<AppSkin?>(null)
    val skin = _skin.asStateFlow()
    private val saves = Channel<AppSkin>(Channel.CONFLATED)
    private var requested: AppSkin? = null

    init {
        viewModelScope.launch {
            store.skin.collect { saved ->
                if (requested == null || requested == saved) {
                    requested = null
                    _skin.value = saved
                }
            }
        }
        viewModelScope.launch { for (value in saves) store.setSkin(value) }
    }

    fun update(value: AppSkin) {
        if (_skin.value == null) return
        requested = value
        _skin.value = value
        saves.trySend(value)
    }
}

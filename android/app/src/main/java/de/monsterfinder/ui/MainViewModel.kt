package de.monsterfinder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.monsterfinder.data.MonsterApi
import de.monsterfinder.data.Prefs
import de.monsterfinder.data.SearchResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val plz: String = "",
    val radiusKm: Int = 5,
    val serverUrl: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val response: SearchResponse? = null,
    val serverOk: Boolean? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val api = MonsterApi()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val s = prefs.settings.first()
            _state.update { it.copy(plz = s.plz, radiusKm = s.radiusKm, serverUrl = s.serverUrl) }
            if (s.plz.length == 5) search()
        }
    }

    fun onPlzChange(value: String) {
        val plz = value.filter(Char::isDigit).take(5)
        _state.update { it.copy(plz = plz, error = null) }
        if (plz.length == 5) search()
    }

    fun onRadiusChange(km: Int) {
        _state.update { it.copy(radiusKm = km) }
        viewModelScope.launch { prefs.setRadius(km) }
        if (_state.value.plz.length == 5) search()
    }

    fun onServerChange(url: String) {
        _state.update { it.copy(serverUrl = url, serverOk = null) }
        viewModelScope.launch { prefs.setServer(url) }
    }

    fun testServer() = viewModelScope.launch {
        _state.update { it.copy(serverOk = null) }
        val ok = api.ping(_state.value.serverUrl)
        _state.update { it.copy(serverOk = ok) }
    }

    fun search() {
        val s = _state.value
        if (s.plz.length != 5) {
            _state.update { it.copy(error = "Bitte 5-stellige Postleitzahl eingeben") }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            prefs.setPlz(s.plz)
            try {
                val r = api.search(s.serverUrl, s.plz, s.radiusKm)
                _state.update { it.copy(loading = false, response = r) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unbekannter Fehler") }
            }
        }
    }
}

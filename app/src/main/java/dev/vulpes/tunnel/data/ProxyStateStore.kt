package dev.vulpes.tunnel.data

import android.content.Context
import dev.vulpes.tunnel.data.model.ProxyCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProxyStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("foxyvpn_proxy_state", Context.MODE_PRIVATE)

    private val _selectedProxyFlow = MutableStateFlow<ProxyCandidate?>(load())
    val selectedProxyFlow: StateFlow<ProxyCandidate?> = _selectedProxyFlow

    fun load(): ProxyCandidate? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val port = prefs.getInt(KEY_PORT, 0)
        if (port == 0) return null
        return ProxyCandidate(
            host = host,
            port = port,
            countryCode = prefs.getString(KEY_COUNTRY_CODE, "").orEmpty(),
            countryName = prefs.getString(KEY_COUNTRY_NAME, "").orEmpty(),
            cityCode = prefs.getString(KEY_CITY_CODE, "").orEmpty(),
        )
    }

    fun save(candidate: ProxyCandidate) {
        prefs.edit()
            .putString(KEY_HOST, candidate.host)
            .putInt(KEY_PORT, candidate.port)
            .putString(KEY_COUNTRY_CODE, candidate.countryCode)
            .putString(KEY_COUNTRY_NAME, candidate.countryName)
            .putString(KEY_CITY_CODE, candidate.cityCode)
            .putInt(KEY_FAILURES, 0)
            .apply()
        _selectedProxyFlow.value = candidate
    }

    fun recordFailure(): Pair<Int, Boolean> {
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        if (failures >= FAILURE_THRESHOLD) {
            clear()
            return failures to true
        }
        prefs.edit().putInt(KEY_FAILURES, failures).apply()
        return failures to false
    }

    // --- favorites -----------------------------------------------------------
    // Stored as "COUNTRY:CITY" so a favorite survives a server-list refresh. A bare country
    // entry is stored as "COUNTRY:" and means "any city in this country".

    private val _favoritesFlow = MutableStateFlow<Set<String>>(loadFavorites())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow

    private fun loadFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun isFavorite(countryCode: String, cityCode: String): Boolean =
        favoriteKey(countryCode, cityCode) in _favoritesFlow.value

    fun toggleFavorite(countryCode: String, cityCode: String): Boolean {
        val key = favoriteKey(countryCode, cityCode)
        val next = _favoritesFlow.value.toMutableSet()
        val added = if (key in next) {
            next.remove(key); false
        } else {
            next.add(key); true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, HashSet(next)).apply()
        _favoritesFlow.value = next
        return added
    }

    fun clear() {
        prefs.edit().clear().apply()
        _selectedProxyFlow.value = null
    }

    companion object {
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_COUNTRY_CODE = "country_code"
        private const val KEY_COUNTRY_NAME = "country_name"
        private const val KEY_CITY_CODE = "city_code"
        private const val KEY_FAILURES = "failures"
        private const val KEY_FAVORITES = "favorites"

        fun favoriteKey(countryCode: String, cityCode: String): String = "$countryCode:$cityCode"
        private const val FAILURE_THRESHOLD = 3
    }
}

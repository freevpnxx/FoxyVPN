package dev.vulpes.tunnel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.vulpes.tunnel.R
import dev.vulpes.tunnel.data.ProxyStateStore
import dev.vulpes.tunnel.data.RECOMMENDED_COUNTRY_CODE
import dev.vulpes.tunnel.data.ServerListClient
import dev.vulpes.tunnel.data.model.VpnCountry
import dev.vulpes.tunnel.vpn.PingUtil

private sealed class LocationRow {
    data object Recommended : LocationRow()
    data class Section(val title: String, val key: String) : LocationRow()
    data class Country(val country: VpnCountry) : LocationRow()
    data class City(
        val country: VpnCountry,
        val cityIndex: Int,
        val showCountry: Boolean,
    ) : LocationRow()
}

private enum class PingState { PENDING, DONE, FAILED }

private enum class SortMode { NAME, PING }

private const val FASTEST_LIMIT = 5

/** ISO 3166-1 alpha-2 to a flag emoji via regional indicator symbols. No image assets needed. */
private fun flagFor(code: String): String {
    val normalized = code.trim().uppercase()
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return ""
    val builder = StringBuilder()
    for (char in normalized) {
        builder.append(String(Character.toChars(0x1F1E6 + (char - 'A'))))
    }
    return builder.toString()
}

@Composable
fun ServerListScreen(
    serverListClient: ServerListClient,
    proxyStateStore: ProxyStateStore,
    onServerSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var countries by remember { mutableStateOf<List<VpnCountry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    val selected by proxyStateStore.selectedProxyFlow.collectAsState()
    val favorites by proxyStateStore.favoritesFlow.collectAsState()

    val pingResults = remember { mutableStateMapOf<String, PingUtil.Sample>() }
    val pingStates = remember { mutableStateMapOf<String, PingState>() }
    // Bumping this re-runs every probe, which is what the refresh button does.
    var pingEpoch by remember { mutableStateOf(0) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    val isRefreshing = pingStates.values.any { it == PingState.PENDING }

    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        runCatching { serverListClient.fetchCountries() }
            .onSuccess { countries = it }
            .onFailure { errorMessage = it.message }
        isLoading = false
    }

    fun pickCity(countryCode: String, cityCode: String): Boolean {
        val chosen = ServerListClient.candidatesForCity(countries, countryCode, cityCode).randomOrNull()
        if (chosen == null) return false
        proxyStateStore.save(chosen)
        onServerSelected()
        return true
    }

    fun pickCountry(country: VpnCountry): Boolean {
        val chosen = ServerListClient.candidatesForCountry(countries, country.code).randomOrNull()
        if (chosen == null) return false
        proxyStateStore.save(chosen)
        onServerSelected()
        return true
    }

    val recommendedCountry = remember(countries) {
        countries.firstOrNull { it.code == RECOMMENDED_COUNTRY_CODE }
    }

    val searchActive = query.isNotBlank()
    val needle = query.trim().lowercase()

    val filtered = remember(countries, needle) {
        if (needle.isEmpty()) {
            countries.filter { it.code != RECOMMENDED_COUNTRY_CODE }
                .sortedBy { it.name.lowercase() }
        } else {
            countries.filter { it.code != RECOMMENDED_COUNTRY_CODE }.mapNotNull { country ->
                val countryMatches = country.name.lowercase().contains(needle) ||
                    country.code.lowercase().contains(needle)
                val cities = country.cities.filter { city ->
                    countryMatches ||
                        city.code.lowercase().contains(needle) ||
                        city.name.lowercase().contains(needle)
                }
                if (cities.isEmpty()) null else country.copy(cities = cities)
            }.sortedBy { it.name.lowercase() }
        }
    }

    val orderedCountries = remember(filtered, sortMode, pingResults.toMap()) {
        if (sortMode != SortMode.PING) {
            filtered
        } else {
            filtered.map { country ->
                country.copy(
                    cities = country.cities.sortedBy { city ->
                        pingResults["${country.code}:${city.code}"]?.latencyMs ?: Int.MAX_VALUE
                    },
                )
            }
        }
    }

    val favoriteKeys = favorites
    val favoriteCities = remember(countries, favoriteKeys) {
        favoriteKeys.mapNotNull { key ->
            val parts = key.split(":", limit = 2)
            val countryCode = parts.getOrNull(0) ?: return@mapNotNull null
            val cityCode = parts.getOrNull(1).orEmpty()
            val country = countries.firstOrNull { it.code == countryCode } ?: return@mapNotNull null
            val cityIndex = country.cities.indexOfFirst { it.code == cityCode }
            if (cityIndex < 0) null else country to cityIndex
        }
    }

    val fastest = remember(pingResults.toMap(), countries) {
        pingResults.entries
            .sortedBy { it.value.latencyMs }
            .take(FASTEST_LIMIT)
            .mapNotNull { (key, sample) ->
                val parts = key.split(":", limit = 2)
                val country = countries.firstOrNull { it.code == parts.getOrNull(0) }
                    ?: return@mapNotNull null
                val cityIndex = country.cities.indexOfFirst { it.code == parts.getOrNull(1) }
                if (cityIndex < 0) null else Triple(country, cityIndex, sample.latencyMs)
            }
    }

    val rows = remember(orderedCountries, searchActive, favoriteCities, fastest, recommendedCountry) {
        buildList {
            if (!searchActive) {
                if (recommendedCountry != null) add(LocationRow.Recommended)

                if (fastest.isNotEmpty()) {
                    add(LocationRow.Section(title = "fastest", key = "section-fastest"))
                    fastest.forEach { (country, cityIndex, _) ->
                        add(LocationRow.City(country, cityIndex, showCountry = true))
                    }
                }

                if (favoriteCities.isNotEmpty()) {
                    add(LocationRow.Section(title = "favorites", key = "section-favorites"))
                    favoriteCities.forEach { (country, cityIndex) ->
                        add(LocationRow.City(country, cityIndex, showCountry = true))
                    }
                }

                if (orderedCountries.isNotEmpty()) {
                    add(LocationRow.Section(title = "all", key = "section-all"))
                }
            }

            for (country in orderedCountries) {
                add(LocationRow.Country(country))
                country.cities.indices.forEach { index ->
                    add(LocationRow.City(country, index, showCountry = false))
                }
            }
        }
    }

    val totalLocations = remember(orderedCountries) { orderedCountries.sumOf { it.cities.size } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.locations_title))
                        if (!isLoading && totalLocations > 0) {
                            Text(
                                stringResource(R.string.locations_count, totalLocations),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sortMode = if (sortMode == SortMode.NAME) SortMode.PING else SortMode.NAME
                    }) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = stringResource(
                                if (sortMode == SortMode.PING) {
                                    R.string.locations_sort_name
                                } else {
                                    R.string.locations_sort_ping
                                },
                            ),
                            tint = if (sortMode == SortMode.PING) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(
                        onClick = { pingEpoch++ },
                        enabled = !isRefreshing,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.locations_refresh),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onClear = {
                    query = ""
                    keyboard?.hide()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            val shownError = errorMessage
            if (shownError != null) {
                Text(
                    stringResource(R.string.locations_none_for_city, shownError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    !isLoading && rows.isEmpty() -> EmptyState(
                        query = query,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(
                            rows,
                            key = { row ->
                                when (row) {
                                    is LocationRow.Recommended -> "recommended"
                                    is LocationRow.Section -> row.key
                                    is LocationRow.Country -> "country:${row.country.code}"
                                    is LocationRow.City -> {
                                        val city = row.country.cities[row.cityIndex]
                                        val prefix = when {
                                            !row.showCountry -> "all"
                                            else -> "pinned"
                                        }
                                        "$prefix:${row.country.code}:${city.code}"
                                    }
                                }
                            },
                        ) { row ->
                            when (row) {
                                is LocationRow.Recommended -> RecommendedCard(
                                    selected = selected?.countryCode == RECOMMENDED_COUNTRY_CODE,
                                    onClick = {
                                        recommendedCountry?.let { country ->
                                            if (!pickCountry(country)) {
                                                errorMessage = country.name
                                            }
                                        }
                                    },
                                )

                                is LocationRow.Section -> SectionHeader(
                                    title = when (row.title) {
                                        "fastest" -> stringResource(R.string.locations_fastest)
                                        "favorites" -> stringResource(R.string.locations_favorites)
                                        else -> stringResource(R.string.locations_all)
                                    },
                                )

                                is LocationRow.Country -> CountryHeader(country = row.country)

                                is LocationRow.City -> {
                                    val city = row.country.cities[row.cityIndex]
                                    val pingKey = "${row.country.code}:${city.code}"
                                    val pingSample = pingResults[pingKey]
                                    val pingState = pingStates[pingKey] ?: PingState.PENDING

                                    LaunchedEffect(pingKey, pingEpoch) {
                                        if (pingEpoch == 0 && pingStates.containsKey(pingKey)) {
                                            return@LaunchedEffect
                                        }
                                        pingStates[pingKey] = PingState.PENDING
                                        val target = city.servers.firstOrNull { !it.quarantined }
                                            ?.let { ServerListClient.defaultConnectTarget(it) }
                                        if (target == null) {
                                            pingStates[pingKey] = PingState.FAILED
                                            return@LaunchedEffect
                                        }
                                        val result = PingUtil.sample(target.first, target.second)
                                        if (result != null) {
                                            pingResults[pingKey] = result
                                            pingStates[pingKey] = PingState.DONE
                                        } else {
                                            pingResults.remove(pingKey)
                                            pingStates[pingKey] = PingState.FAILED
                                        }
                                    }

                                    val isFavorite = pingKey in favoriteKeys
                                    val isSelected = selected?.countryCode == row.country.code &&
                                        selected?.cityCode == city.code

                                    CityRow(
                                        cityCode = city.code,
                                        cityName = city.name,
                                        countryLabel = if (row.showCountry) {
                                            "${flagFor(row.country.code)} ${row.country.name}"
                                        } else {
                                            null
                                        },
                                        serverCount = city.servers.count { !it.quarantined },
                                        quarantinedCount = city.servers.count { it.quarantined },
                                        pingMs = pingSample?.latencyMs,
                                        jitterMs = pingSample?.jitterMs,
                                        pingState = pingState,
                                        isFavorite = isFavorite,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (!pickCity(row.country.code, city.code)) {
                                                errorMessage = city.code
                                            }
                                        },
                                        onToggleFavorite = {
                                            proxyStateStore.toggleFavorite(row.country.code, city.code)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.locations_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun CountryHeader(country: VpnCountry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val flag = flagFor(country.code)
        if (flag.isNotEmpty()) {
            Text(flag, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
        }
        Text(country.name, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                if (country.cities.size == 1) {
                    stringResource(R.string.locations_one_server)
                } else {
                    stringResource(R.string.locations_servers_count, country.cities.size)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecommendedCard(selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.06f)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.locations_recommended),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.locations_recommended_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.locations_selected),
                    tint = accent,
                )
            }
        }
    }
}

@Composable
private fun CityRow(
    cityCode: String,
    cityName: String,
    countryLabel: String?,
    serverCount: Int,
    quarantinedCount: Int,
    pingMs: Int?,
    jitterMs: Int?,
    pingState: PingState,
    isFavorite: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val statusColors = dev.vulpes.tunnel.ui.theme.LocalFoxyStatusColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    accent.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f)
                },
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cityName.ifBlank { cityCode },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.locations_selected),
                            tint = accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                val serverLabel = if (serverCount == 1) {
                    stringResource(R.string.locations_one_server)
                } else {
                    stringResource(R.string.locations_servers_count, serverCount)
                }
                val jitterNote = if (jitterMs != null && jitterMs > 0) {
                    "  \u2022  " + stringResource(R.string.locations_jitter, jitterMs)
                } else {
                    ""
                }
                val limitedNote = if (quarantinedCount > 0) {
                    "  \u2022  " + stringResource(R.string.locations_limited, quarantinedCount)
                } else {
                    ""
                }
                val base = if (countryLabel != null) {
                    "$countryLabel  \u2022  $serverLabel"
                } else {
                    serverLabel
                }
                val subtitle = base + jitterNote + limitedNote
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LatencyPill(pingMs = pingMs, pingState = pingState, good = statusColors.connected)

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isFavorite) {
                        stringResource(R.string.locations_favorite_remove)
                    } else {
                        stringResource(R.string.locations_favorite_add)
                    },
                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun LatencyPill(pingMs: Int?, pingState: PingState, good: androidx.compose.ui.graphics.Color) {
    val warning = MaterialTheme.colorScheme.error
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant

    val (label, color) = when {
        pingMs != null && pingMs < 120 ->
            stringResource(R.string.locations_latency_value, pingMs) to good
        pingMs != null && pingMs < 260 ->
            stringResource(R.string.locations_latency_value, pingMs) to neutral
        pingMs != null ->
            stringResource(R.string.locations_latency_value, pingMs) to warning
        pingState == PingState.FAILED ->
            stringResource(R.string.locations_latency_unavailable) to neutral
        else -> stringResource(R.string.locations_latency_measuring) to neutral
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun EmptyState(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.locations_search_empty, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

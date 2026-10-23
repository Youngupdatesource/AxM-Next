package frb.axeron.manager.ui.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlin.math.abs
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.ui.util.LocalCapsuleInset
import frb.axeron.manager.R
import frb.axeron.manager.data.SettingsRepository
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.theme.cardContainerColor
import frb.axeron.manager.ui.theme.insetTextFieldColors
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsEditorScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val scope = rememberCoroutineScope()

    /* ================= SOURCE OF TRUTH ================= */

    val settingTypes = remember {
        SettingsRepository.SettingType.entries.toList()
    }

    // null = the "All" page; page N+1 is settingTypes[N].
    var selectedType by remember {
        mutableStateOf<SettingsRepository.SettingType?>(null)
    }

    var query by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchScope.ALL) }
    var showFab by remember { mutableStateOf(true) }

    // The one pending chip-driven page change; a newer tap always cancels it.
    var navJob by remember { mutableStateOf<Job?>(null) }

    // Prefix filter per page, so every source remembers its own.
    val groupByPage = remember { mutableStateMapOf<Int, String>() }

    val settingsRepository = remember(contentResolver) {
        SettingsRepository(contentResolver)
    }

    fun pageOf(type: SettingsRepository.SettingType?): Int =
        type?.let { settingTypes.indexOf(it) + 1 } ?: 0

    /* ================= PAGER ================= */

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { settingTypes.size + 1 }
    )

    /* ================= STATE CACHES ================= */

    val dataCache = remember {
        mutableStateMapOf<SettingsRepository.SettingType, Map<String, String?>>()
    }

    val loadingStates = remember {
        mutableStateMapOf<SettingsRepository.SettingType, Boolean>()
    }

    val scrollStates = remember {
        mutableStateMapOf<SettingsRepository.SettingType, LazyListState>()
    }

    /* ================= SYNC PAGER → CHIPS ================= */

    // targetPage, not currentPage: a chip tap that animates over several pages must not make
    // the selection flicker through the pages in between.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }
            .distinctUntilChanged()
            .collect { page ->
                selectedType = settingTypes.getOrNull(page - 1)
                showFab = true
            }
    }

    // After a search the pager is composed again: bring it to the source picked meanwhile.
    val isSearching = query.isNotBlank()
    LaunchedEffect(isSearching) {
        if (!isSearching) {
            val target = pageOf(selectedType)
            if (pagerState.currentPage != target) pagerState.scrollToPage(target)
        }
    }

    /* ================= REFRESH EVENT ================= */

    // The repository spawns processes and reads thousands of lines: never on the main thread.
    suspend fun load(type: SettingsRepository.SettingType) {
        loadingStates[type] = true
        try {
            dataCache[type] = withContext(Dispatchers.IO) { settingsRepository.getAll(type) }
        } finally {
            loadingStates[type] = false
        }
    }

    fun refresh(type: SettingsRepository.SettingType) {
        scope.launch { load(type) }
    }

    fun refreshAll() {
        settingTypes.forEach { refresh(it) }
    }

    /* ================= SEARCH + FILTER ================= */

    // The source chip also scopes a search; "All" searches every source.
    val scopedTypes = selectedType?.let { listOf(it) } ?: settingTypes

    // "All" and search need their sources in memory (only the ones in scope).
    LaunchedEffect(isSearching, selectedType) {
        if (!isSearching && selectedType != null) return@LaunchedEffect
        val types = selectedType?.let { listOf(it) } ?: settingTypes
        types.forEach { type ->
            if (!dataCache.containsKey(type) && loadingStates[type] != true) {
                launch { load(type) }
            }
        }
    }

    val allHits by remember {
        derivedStateOf {
            settingTypes.flatMap { type ->
                dataCache[type].orEmpty().entries
                    .sortedBy { it.key }
                    .map { SearchHit(type, it.key, it.value ?: "") }
            }
        }
    }

    // Every word must match (key and/or value, per the scope); key hits rank first.
    val tokens = remember(query) { query.trim().split(WHITESPACE).filter { it.isNotEmpty() } }
    val searchResults by remember(tokens, searchScope, selectedType) {
        derivedStateOf {
            if (tokens.isEmpty()) {
                emptyList()
            } else {
                val types = selectedType?.let { listOf(it) } ?: settingTypes
                types.flatMap { type ->
                    dataCache[type].orEmpty().entries.mapNotNull { entry ->
                        val rank = matchRank(entry.key, entry.value, tokens, searchScope)
                        if (rank < 0) null
                        else RankedHit(SearchHit(type, entry.key, entry.value ?: ""), rank)
                    }
                }
                    .sortedWith(compareBy<RankedHit> { it.rank }.thenBy { it.hit.key })
                    .map { it.hit }
            }
        }
    }

    val searchLoading = isSearching && scopedTypes.any {
        loadingStates[it] == true || !dataCache.containsKey(it)
    }

    // Prefix chips for the current source (count of entries is deliberately not shown).
    val groups by remember(selectedType) {
        derivedStateOf {
            val types = selectedType?.let { listOf(it) } ?: settingTypes
            topGroups(types.flatMap { dataCache[it]?.keys.orEmpty() })
        }
    }
    val currentGroup = groupByPage[pageOf(selectedType)]

    /* ================= UI ================= */

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = "Settings Editor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                searchLabel = stringResource(R.string.settings_editor_search_all),
                searchText = query,
                onSearchTextChange = { query = it },
                onClearClick = {
                    query = ""
                    searchScope = SearchScope.ALL
                },
                action = {
                    IconButton(onClick = { selectedType?.let { refresh(it) } ?: refreshAll() }) {
                        Icon(Icons.Outlined.Refresh, null)
                    }
                }
            )
        },
        floatingActionButton = {
            var isAdding by remember { mutableStateOf(false) }
            // Adding needs a concrete source, so the FAB is hidden on the "All" page.
            val addType = selectedType ?: SettingsRepository.SettingType.GLOBAL

            TableEditor(
                addTable = true,
                context = context,
                showDialog = isAdding,
                selectedType = addType,
                settingsRepository = settingsRepository,
                onDismissRequest = { isAdding = false },
                onRefresh = { refresh(addType) }
            )

            AnimatedVisibility(
                visible = showFab && !isSearching && selectedType != null,
                modifier = Modifier.padding(bottom = LocalCapsuleInset.current),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                FloatingActionButton(onClick = { isAdding = true }) {
                    Icon(Icons.Filled.Add, null)
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterBar(
                selectedType = selectedType,
                onSelectType = { target ->
                    if (isSearching) {
                        // No pager on screen while searching: the chip just rescopes the search.
                        selectedType = target
                    } else {
                        // Rapid taps: cancel the running animation first and always scroll to the
                        // *latest* tap. (Comparing with currentPage skipped the scroll when the
                        // previous animation had already passed halfway, so that older animation
                        // finished and the wrong tab stayed on screen.) Far jumps snap instead of
                        // composing every page in between.
                        navJob?.cancel()
                        val page = pageOf(target)
                        selectedType = target
                        navJob = scope.launch {
                            if (abs(pagerState.currentPage - page) > 1) {
                                pagerState.scrollToPage(page)
                            } else {
                                pagerState.animateScrollToPage(page)
                            }
                            selectedType = target
                            showFab = true
                        }
                    }
                },
                searching = isSearching,
                resultCount = searchResults.size,
                loading = searchLoading,
                searchScope = searchScope,
                onScopeChange = { searchScope = it },
                groups = groups,
                group = currentGroup,
                onGroupChange = { picked ->
                    val page = pageOf(selectedType)
                    if (picked == null) groupByPage.remove(page) else groupByPage[page] = picked
                }
            )

            if (isSearching) {
                SearchResults(
                    hits = searchResults,
                    loading = searchLoading,
                    context = context,
                    settingsRepository = settingsRepository,
                    onRefresh = { refresh(it) }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> settingTypes.getOrNull(page - 1)?.name ?: "all" }
                ) { page ->

                    val type = settingTypes.getOrNull(page - 1)
                    val pageGroup = groupByPage[page]

                    if (type == null) {
                        val visibleHits = remember(allHits, pageGroup) {
                            if (pageGroup == null) allHits
                            else allHits.filter { groupOf(it.key) == pageGroup }
                        }
                        AllPage(
                            hits = visibleHits,
                            loading = settingTypes.any { loadingStates[it] == true },
                            context = context,
                            settingsRepository = settingsRepository,
                            onRefreshAll = { refreshAll() },
                            onRefresh = { refresh(it) }
                        )
                        return@HorizontalPager
                    }

                    /* ========== INITIAL LOAD (ONCE PER PAGE) ========== */

                    LaunchedEffect(type) {
                        if (!dataCache.containsKey(type) && loadingStates[type] != true) {
                            load(type)
                            Log.d("SettingsEditor", "loaded $type")
                        }
                    }

                    val data = dataCache[type] ?: emptyMap()
                    val loading = loadingStates[type] == true

                    val filtered by remember(data, pageGroup) {
                        derivedStateOf {
                            if (pageGroup == null) data
                            else data.filter { groupOf(it.key) == pageGroup }
                        }
                    }

                    val listState = scrollStates.getOrPut(type) {
                        LazyListState()
                    }

                    /* ========== FAB AUTO HIDE ========== */

                    LaunchedEffect(type) {
                        var lastIndex = listState.firstVisibleItemIndex
                        var lastOffset = listState.firstVisibleItemScrollOffset

                        snapshotFlow {
                            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                        }.collect { (i, o) ->
                            val down = i > lastIndex || (i == lastIndex && o > lastOffset + 4)
                            val up = i < lastIndex || (i == lastIndex && o < lastOffset - 4)

                            when {
                                down && showFab -> showFab = false
                                up && !showFab -> showFab = true
                            }

                            lastIndex = i
                            lastOffset = o
                        }
                    }

                    /* ========== CONTENT ========== */

                    PullToRefreshBox(
                        isRefreshing = loading,
                        onRefresh = { refresh(type) }
                    ) {
                        if (loading && data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (filtered.isEmpty()) {
                            EmptyHint()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + LocalCapsuleInset.current)
                            ) {
                                items(
                                    items = filtered.entries.toList(),
                                    key = { it.key }
                                ) { entry ->
                                    TableItem(
                                        context = context,
                                        key = entry.key,
                                        value = entry.value ?: "",
                                        selectedType = type,
                                        settingsRepository = settingsRepository
                                    ) {
                                        refresh(type)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SearchHit(
    val type: SettingsRepository.SettingType,
    val key: String,
    val value: String
)

private class RankedHit(val hit: SearchHit, val rank: Int)

/** Where a search looks. */
private enum class SearchScope(val labelId: Int) {
    ALL(R.string.settings_editor_all),
    KEY(R.string.settings_editor_scope_key),
    VALUE(R.string.settings_editor_scope_value)
}

private val WHITESPACE = Regex("\\s+")

/**
 * -1 when [key]/[value] do not satisfy every token; otherwise 0 (exact key) .. 3 (value only),
 * the worst over all tokens, so entries whose key matches everything sort first.
 */
private fun matchRank(
    key: String,
    value: String?,
    tokens: List<String>,
    scope: SearchScope
): Int {
    var rank = 0
    for (token in tokens) {
        val inKey = scope != SearchScope.VALUE && key.contains(token, ignoreCase = true)
        val inValue = scope != SearchScope.KEY && value?.contains(token, ignoreCase = true) == true
        if (!inKey && !inValue) return -1
        val r = when {
            inKey && key.equals(token, ignoreCase = true) -> 0
            inKey && key.startsWith(token, ignoreCase = true) -> 1
            inKey -> 2
            else -> 3
        }
        if (r > rank) rank = r
    }
    return rank
}

private const val MIN_GROUP_SIZE = 4
private const val MAX_GROUPS = 16

/** "ro.build.version.sdk" -> "ro.build", "sys.boot" -> "sys", "wifi_on" -> "wifi". */
private fun groupOf(key: String): String {
    val first = key.indexOf('.')
    if (first < 0) return key.substringBefore('_')
    val second = key.indexOf('.', first + 1)
    return if (second < 0) key.substring(0, first) else key.substring(0, second)
}

/** The most common key prefixes, biggest first; small ones are left to search. */
private fun topGroups(keys: List<String>): List<String> {
    val counts = HashMap<String, Int>()
    for (key in keys) {
        val g = groupOf(key)
        counts[g] = (counts[g] ?: 0) + 1
    }
    return counts.entries
        .filter { it.value >= MIN_GROUP_SIZE }
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(MAX_GROUPS)
        .map { it.key }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.settings_editor_no_results),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SearchResults(
    hits: List<SearchHit>,
    loading: Boolean,
    context: Context,
    settingsRepository: SettingsRepository,
    onRefresh: (SettingsRepository.SettingType) -> Unit
) {
    if (hits.isEmpty()) {
        if (!loading) EmptyHint()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + LocalCapsuleInset.current)
    ) {
        items(
            items = hits,
            key = { "${it.type.name}/${it.key}" }
        ) { hit ->
            TableItem(
                context = context,
                key = hit.key,
                value = hit.value,
                selectedType = hit.type,
                settingsRepository = settingsRepository,
                location = stringResource(hit.type.stringId)
            ) {
                onRefresh(hit.type)
            }
        }
    }
}

/**
 * Everything that narrows the list, in one compact block:
 *  - a capsule chip per source ("All" + the five sources), always visible and scrolled so the
 *    selected one is never cut off;
 *  - below it, either prefix chips of the current source (browsing), or the result count and
 *    where to look (searching). The count only exists while searching.
 */
@Composable
private fun FilterBar(
    selectedType: SettingsRepository.SettingType?,
    onSelectType: (SettingsRepository.SettingType?) -> Unit,
    searching: Boolean,
    resultCount: Int,
    loading: Boolean,
    searchScope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    groups: List<String>,
    group: String?,
    onGroupChange: (String?) -> Unit
) {
    val types = remember { SettingsRepository.SettingType.entries.toList() }
    val selectedIndex = selectedType?.let { types.indexOf(it) + 1 } ?: 0
    val sourceState = rememberLazyListState()
    val groupState = rememberLazyListState()
    val chipShape = RoundedCornerShape(50)

    LaunchedEffect(selectedIndex) {
        sourceState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }
    LaunchedEffect(selectedType) { groupState.scrollToItem(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .padding(bottom = 4.dp)
    ) {
        LazyRow(
            state = sourceState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(types.size + 1, key = { it }) { index ->
                val type = types.getOrNull(index - 1)
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onSelectType(type) },
                    shape = chipShape,
                    label = {
                        Text(stringResource(type?.shortLabelId() ?: R.string.settings_editor_all))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = type?.icon() ?: Icons.Outlined.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = !searching && groups.size >= 2,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                state = groupState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                item(key = "__all") {
                    FilterChip(
                        selected = group == null,
                        onClick = { onGroupChange(null) },
                        shape = chipShape,
                        label = { Text(stringResource(R.string.settings_editor_all)) }
                    )
                }
                items(groups, key = { it }) { name ->
                    FilterChip(
                        selected = group == name,
                        onClick = { onGroupChange(if (group == name) null else name) },
                        shape = chipShape,
                        label = {
                            Text(
                                text = name,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = searching,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 4.dp)
                ) {
                    Text(
                        text = if (loading && resultCount == 0) "…"
                        else stringResource(R.string.settings_editor_entries, resultCount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SearchScope.entries.forEach { option ->
                            FilterChip(
                                selected = searchScope == option,
                                onClick = { onScopeChange(option) },
                                shape = chipShape,
                                label = { Text(stringResource(option.labelId)) }
                            )
                        }
                    }
                }
                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun SettingsRepository.SettingType.shortLabelId(): Int = when (this) {
    SettingsRepository.SettingType.GLOBAL -> R.string.settings_editor_short_global
    SettingsRepository.SettingType.SECURE -> R.string.settings_editor_short_secure
    SettingsRepository.SettingType.SYSTEM -> R.string.settings_editor_short_system
    SettingsRepository.SettingType.ANDROID_PROP -> R.string.settings_editor_short_prop
    SettingsRepository.SettingType.AXERON_ENV -> R.string.settings_editor_short_env
}

private fun SettingsRepository.SettingType.icon(): ImageVector = when (this) {
    SettingsRepository.SettingType.GLOBAL -> Icons.Outlined.Public
    SettingsRepository.SettingType.SECURE -> Icons.Outlined.Shield
    SettingsRepository.SettingType.SYSTEM -> Icons.Outlined.Settings
    SettingsRepository.SettingType.ANDROID_PROP -> Icons.Outlined.Android
    SettingsRepository.SettingType.AXERON_ENV -> Icons.Outlined.Terminal
}

/** "All" page: every entry, tagged with its source. The chips above do the narrowing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllPage(
    hits: List<SearchHit>,
    loading: Boolean,
    context: Context,
    settingsRepository: SettingsRepository,
    onRefreshAll: () -> Unit,
    onRefresh: (SettingsRepository.SettingType) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefreshAll
    ) {
        if (loading && hits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + LocalCapsuleInset.current)
            ) {
                items(
                    items = hits,
                    key = { "${it.type.name}/${it.key}" }
                ) { hit ->
                    TableItem(
                        context = context,
                        key = hit.key,
                        value = hit.value,
                        selectedType = hit.type,
                        settingsRepository = settingsRepository,
                        location = stringResource(hit.type.stringId)
                    ) {
                        onRefresh(hit.type)
                    }
                }
            }
        }
    }
}


@Composable
fun TableItem(
    context: Context,
    key: String,
    value: String,
    selectedType: SettingsRepository.SettingType,
    settingsRepository: SettingsRepository,
    location: String? = null,
    onRefresh: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    TableEditor(
        context = context,
        showDialog = isEditing,
        key = key,
        value = value,
        selectedType = selectedType,
        settingsRepository = settingsRepository,
        onDismissRequest = {
            isEditing = false
        },
        onRefresh = onRefresh
    )

    ElevatedCard(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = {
            isEditing = true
        },
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardContainerColor()
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (location != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = location,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            SmartWrappedText(
                text = key,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
            )
            Spacer(Modifier.height(4.dp))
            SmartWrappedText(
                text = value.ifBlank { "(null)" },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditor(
    addTable: Boolean = false,
    context: Context,
    showDialog: Boolean,
    key: String = "",
    value: String = "",
    selectedType: SettingsRepository.SettingType,
    settingsRepository: SettingsRepository,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    var newKey by remember { mutableStateOf(key) }
    var newValue by remember { mutableStateOf(value) }

    LaunchedEffect(key, value, showDialog) {
        newKey = key
        newValue = value
    }

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardContainerColor(),
            onDismissRequest = {
                onDismissRequest()
            },
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (!addTable) {
                        val confirmDialog = rememberConfirmDialog()
                        val scope = rememberCoroutineScope()

                        val title = stringResource(R.string.ask_remove_settings)
                        val content = stringResource(R.string.ask_remove_settings_msg)
                        val confirm = stringResource(R.string.remove)
                        val dismiss = stringResource(R.string.cancel)
                        val failed = stringResource(R.string.failed_to_remove_settings)

                        FilledTonalButton(
                            modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                            onClick = {
                                scope.launch {
                                    val confirmResult = confirmDialog.awaitConfirm(
                                        title,
                                        content = content,
                                        confirm = confirm,
                                        dismiss = dismiss
                                    )
                                    if (confirmResult == ConfirmResult.Confirmed) {
                                        if (settingsRepository.deleteValue(selectedType, newKey)) {
                                            onRefresh()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                failed,
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                        onDismissRequest()
                                    }
                                }

                            },
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null
                            )
//                            Text(
//                                modifier = Modifier.padding(start = 7.dp),
//                                text = "Remove",
//                                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
//                                fontSize = MaterialTheme.typography.labelMedium.fontSize
//                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    val copied = stringResource(R.string.key_copied)

                    FilledTonalButton(
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = newKey.isNotEmpty(),
                        onClick = {
                            if (ClipboardUtil.put(context, newKey)) {
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                        VerticalDivider(
                            modifier = Modifier
                                .height(12.dp)
                                .padding(horizontal = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null
                        )
//                        Text(
//                            modifier = Modifier.padding(start = 7.dp),
//                            text = "Key",
//                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
//                            fontSize = MaterialTheme.typography.labelMedium.fontSize
//                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    val valCopied = stringResource(R.string.value_copied)

                    FilledTonalButton(
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = newValue.isNotEmpty(),
                        onClick = {
                            if (ClipboardUtil.put(context, newValue)) {
                                Toast.makeText(context, valCopied, Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                        VerticalDivider(
                            modifier = Modifier
                                .height(12.dp)
                                .padding(horizontal = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.AutoMirrored.Outlined.TextSnippet,
                            contentDescription = null
                        )
//                        Text(
//                            modifier = Modifier.padding(start = 7.dp),
//                            text = "Value",
//                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
//                            fontSize = MaterialTheme.typography.labelMedium.fontSize
//                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    val failed = stringResource(R.string.failed_to_save_settings)

                    FilledTonalButton(
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = newValue != value && newKey.isNotEmpty(),
                        onClick = {
                            if (settingsRepository.putValue(selectedType, newKey, newValue)) {
                                onRefresh()
                            } else {
                                Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                            }
                            onDismissRequest()
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 7.dp),
                            text = stringResource(R.string.save),
                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }
                }

                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    DividerDefaults.Thickness,
                    MaterialTheme.colorScheme.outlineVariant
                )

                if (addTable) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newKey.ifBlank { "" },
                        onValueChange = {
                            newKey = it
                        },
                        label = {
                            Text(stringResource(R.string.add_key_settings))
                        },
                        textStyle = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        singleLine = true,
                        colors = insetTextFieldColors().copy(
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    )
                } else {
                    SmartWrappedText(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = newKey,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newValue.ifBlank { "" },
                    onValueChange = {
                        newValue = it
                    },
                    label = {
                        if (addTable) {
                            Text(stringResource(R.string.add_value_settings))
                        } else {
                            Text(stringResource(R.string.edit_value_settings))
                        }
                    },
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 20,
                    singleLine = false,
                    colors = insetTextFieldColors().copy(
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SmartWrappedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    wrapSymbols: String = ".,=_()[]{}<>:;+-*/|\\"
) {
    // Buat regex untuk simbol wrap
    val regex = remember(wrapSymbols) {
        Regex("(?<=[${Regex.escape(wrapSymbols)}])")
    }

    // Sisipkan zero-width space setelah simbol
    val formatted = remember(text) {
        text.replace(regex, "\u200B")
    }

    Text(
        text = formatted,
        color = color,
        style = style.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier,
        softWrap = true,
        overflow = TextOverflow.Clip
    )
}


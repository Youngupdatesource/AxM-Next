package frb.axeron.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExecutePluginActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import frb.axeron.api.AxeronInfo
import frb.axeron.manager.R
import frb.axeron.manager.ui.navigation.BottomBarDestination
import frb.axeron.manager.ui.screen.FlashIt
import frb.axeron.manager.ui.theme.AxManagerTheme
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.LocaleHelper
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.PluginInstaller

class AxActivity : ComponentActivity() {

    companion object {
        const val OPEN_QUICK_SHELL = "AxManager.QUICK_SHELL"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    var zipUri by mutableStateOf<List<PluginInstaller>?>(emptyList())

    val shortcut by lazy {
        ShortcutInfoCompat.Builder(this, "quick-shell")
            .setShortLabel(getString(R.string.quick_shell_short))
            .setLongLabel(getString(R.string.quick_shell))
            .setDisabledMessage(getString(R.string.quick_shell_not_supported))
            .setIcon(IconCompat.createWithResource(this, R.drawable.terminal))
            .setIntent(
                Intent(this, AxActivity::class.java).apply {
                    action = OPEN_QUICK_SHELL
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            .build()
    }

    private var intentState: Intent? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ShortcutManagerCompat.pushDynamicShortcut(this@AxActivity, shortcut)

        intentState = intent

        setContent {
            AxManagerTheme {
                MainScreen(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState = intent
        Log.i("AxActivity", "onNewIntent")
    }

    @Composable
    fun MainScreen(settingsViewModel: SettingsViewModel) {
        val snackBarHostState = remember { SnackbarHostState() }
        val navController = rememberNavController()
        val navigator = navController.rememberDestinationsNavigator()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination

        val activateViewModel: ActivateViewModel = viewModel<ActivateViewModel>()

        val appsViewModel: AppsViewModel = viewModel<AppsViewModel>()
        val privilegeViewModel: PrivilegeViewModel = viewModel<PrivilegeViewModel>()
        val pluginViewModel: PluginViewModel = viewModel<PluginViewModel>()

        val axeronInfo = activateViewModel.axeronInfo

        LaunchedEffect(intentState) {
            if (intentState == null) return@LaunchedEffect
            Log.i("AxActivity", "intent: $intentState")
            when (intentState!!.action) {
                OPEN_QUICK_SHELL -> {
                    if (axeronInfo.isRunning()) {
                        navigator.navigate(HomeScreenDestination()) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                        }
                        if (navController.currentDestination?.route == HomeScreenDestination.route) {
                            navigator.navigate(QuickShellScreenDestination())
                        }
                    }
                }

                Intent.ACTION_VIEW -> {
                    if (intentState!!.data?.scheme == "content") {
                        zipUri =
                            intent.data?.let { uri ->
                                arrayListOf(PluginInstaller(uri))
                            } ?: run {
                                val uris: ArrayList<Uri>? =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        intent.getParcelableArrayListExtra("uris", Uri::class.java)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        intent.getParcelableArrayListExtra("uris")
                                    }

                                uris
                                    ?.map { PluginInstaller(it) }
                                    ?.toCollection(arrayListOf())
                                    ?: arrayListOf()
                            }
                        if (!zipUri.isNullOrEmpty()) {
                            navigator.navigate(
                                FlashScreenDestination(
                                    flashIt = FlashIt.FlashPlugins(zipUri!!)
                                )
                            )
                            zipUri = null
                        }
                    }
                }
            }

            intentState = null
        }

        LaunchedEffect(axeronInfo) {
            if (axeronInfo.isRunning()) {
                pluginViewModel.fetchModuleList()
                appsViewModel.loadInstalledApps()
                privilegeViewModel.loadInstalledApps()
            }
        }

        val viewModelGlobal = remember {
            ViewModelGlobal(
                settingsViewModel = settingsViewModel,
                appsViewModel = appsViewModel,
                activateViewModel = activateViewModel,
                pluginViewModel = pluginViewModel,
                privilegeViewModel = privilegeViewModel
            )
        }

        val bottomBarRoutes = remember {
            BottomBarDestination.entries.map { it.direction.route }.toSet()
        }
        val navTransitions = remember(bottomBarRoutes) { createNavTransitions(bottomBarRoutes) }

        // Urutan tab yang sedang tampil di bottom bar (tab needAxeron disembunyikan saat server mati).
        val swipeTabs = BottomBarDestination.entries.filter { axeronInfo.isRunning() || !it.needAxeron }
        val currentTabIndex = swipeTabs.indexOfFirst { it.direction.route == currentDestination?.route }

        val showBottomBar = when (currentDestination?.route) {
            ActivateScreenDestination.route -> false // Hide for Activate
            FlashScreenDestination.route -> false // Hide for Flash
            ExecutePluginActionScreenDestination.route -> false // Hide for ExecutePluginAction
            else -> true
        }

        // --- floating nav capsule ---
        // Icons never move or resize; only opacity/scale change, so a tap always lands where it
        // looks like it will. Active: right after a tap / tab change. Idle (2.5 s) or while the
        // content scrolls underneath: slightly smaller and dimmer.
        val capsule = remember { CapsuleState() }
        val capsuleScroll = remember(capsule) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && abs(available.y) > 1f) capsule.fold()
                    return Offset.Zero
                }
            }
        }
        LaunchedEffect(capsule.tick) {
            delay(2500)
            capsule.fold()
        }
        LaunchedEffect(currentDestination?.route) { capsule.poke() }
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box {
            Scaffold(
                contentWindowInsets = WindowInsets()
            ) { innerPadding ->
                CompositionLocalProvider(
                    LocalSnackbarHost provides snackBarHostState,
                ) {
                    DestinationsNavHost(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(bottom = if (showBottomBar) CapsuleReserve + navInset else 0.dp)
                            .nestedScroll(capsuleScroll)
                            // Geser kiri/kanan = pindah ke tab sebelah. Dipasang di parent, jadi
                            // komponen anak yang sudah memakai drag horizontal (slider, dll) tetap menang.
                            .pointerInput(showBottomBar, currentTabIndex, swipeTabs) {
                                if (!showBottomBar || currentTabIndex < 0) return@pointerInput
                                val threshold = 72.dp.toPx()
                                var total = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { total = 0f },
                                    onDragCancel = { total = 0f },
                                    onDragEnd = {
                                        val target = when {
                                            total <= -threshold -> swipeTabs.getOrNull(currentTabIndex + 1)
                                            total >= threshold -> swipeTabs.getOrNull(currentTabIndex - 1)
                                            else -> null
                                        }
                                        total = 0f
                                        target?.let {
                                            navigator.navigate(it.direction) {
                                                popUpTo(NavGraphs.root) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount -> total += dragAmount }
                                )
                            },
                        navGraph = NavGraphs.root,
                        navController = navController,
                        dependenciesContainerBuilder = {
                            dependency(viewModelGlobal)
                        },
                        defaultTransitions = navTransitions
                    )
                }
            }

            AnimatedVisibility(
                visible = showBottomBar && !imeVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomBar(
                    navController,
                    navigator,
                    activateViewModel.axeronInfo,
                    pluginViewModel.pluginUpdateCount,
                    capsule
                )
            }
        }
    }

    /**
     * Transisi navigasi ala Axora:
     * - pindah antar tab bottom bar: slide horizontal (spring) searah indeks tab
     * - masuk sub-screen: slide dari kanan; kembali: scale-out + fade
     */
    private fun createNavTransitions(bottomBarRoutes: Set<String>): NavHostAnimatedDestinationStyle {
        fun tabIndex(route: String?): Int =
            BottomBarDestination.entries.indexOfFirst { it.direction.route == route }

        return object : NavHostAnimatedDestinationStyle() {
            override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                val targetRoute = targetState.destination.route
                if (targetRoute !in bottomBarRoutes) {
                    slideInHorizontally(initialOffsetX = { it })
                } else {
                    val from = tabIndex(initialState.destination.route)
                    val to = tabIndex(targetRoute)
                    if (from != -1 && to != -1) {
                        val spec = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 300f)
                        if (to > from) slideInHorizontally(spec) { width -> width }
                        else slideInHorizontally(spec) { width -> -width }
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                }
            }

            override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (initialRoute in bottomBarRoutes && targetRoute !in bottomBarRoutes) {
                    slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                } else if (initialRoute in bottomBarRoutes && targetRoute in bottomBarRoutes) {
                    val from = tabIndex(initialRoute)
                    val to = tabIndex(targetRoute)
                    if (from != -1 && to != -1) {
                        val spec = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 300f)
                        if (to > from) slideOutHorizontally(spec) { width -> -width }
                        else slideOutHorizontally(spec) { width -> width }
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                } else {
                    fadeOut(animationSpec = tween(340))
                }
            }

            override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                if (targetState.destination.route in bottomBarRoutes) {
                    slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                } else {
                    fadeIn(animationSpec = tween(340))
                }
            }

            override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                if (initialState.destination.route !in bottomBarRoutes) {
                    scaleOut(targetScale = 0.9f) + fadeOut()
                } else {
                    fadeOut(animationSpec = tween(340))
                }
            }
        }
    }

    @Composable
    private fun BottomBar(
        navController: NavHostController,
        navigator: DestinationsNavigator,
        axeronServerInfo: AxeronInfo,
        moduleUpdateCount: Int,
        capsule: CapsuleState
    ) {
        val active = capsule.active
        val alpha by animateFloatAsState(
            targetValue = if (active) 1f else 0.85f,
            animationSpec = tween(durationMillis = 350),
            label = "capsuleAlpha"
        )
        val scale by animateFloatAsState(
            targetValue = if (active) 1f else 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "capsuleScale"
        )
        val shadow by animateDpAsState(
            targetValue = if (active) 10.dp else 4.dp,
            animationSpec = tween(durationMillis = 300),
            label = "capsuleShadow"
        )

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = CapsuleGap)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = shadow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
            ) {
                val tabs = BottomBarDestination.entries.filter {
                    axeronServerInfo.isRunning() || !it.needAxeron
                }
                // key(): state tiap tab tetap stabil saat tab muncul/hilang (server hidup/mati).
                val selectedStates = tabs.map { tab ->
                    key(tab) { navController.isRouteOnBackStackAsState(tab.direction) }
                }
                val selectedIndex = selectedStates.indexOfFirst { it.value }

                // Posisi pill = indeks tab (0f..n-1f). Spring: meluncur ke tab baru lalu
                // overshoot sedikit. Dibaca hanya di fase layout/draw, tanpa recomposition.
                val pillPosition by animateFloatAsState(
                    targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "capsulePill"
                )
                val pillAlpha by animateFloatAsState(
                    targetValue = if (selectedIndex >= 0) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "capsulePillAlpha"
                )
                val stepPx = with(LocalDensity.current) {
                    (CapsuleItemWidth + CapsuleItemGap).toPx()
                }
                val pillColor = MaterialTheme.colorScheme.primaryContainer

                Box(modifier = Modifier.padding(6.dp)) {
                    // Indikator terpilih: satu pill yang bergeser di belakang ikon.
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((pillPosition * stepPx).roundToInt(), 0) }
                            .size(width = CapsuleItemWidth, height = CapsuleItemHeight)
                            .graphicsLayer {
                                // Melar sedikit saat bergerak, kembali normal saat sampai.
                                val travel = abs(selectedIndex.coerceAtLeast(0) - pillPosition)
                                scaleX = 1f + 0.16f * travel.coerceAtMost(1f)
                                this.alpha = pillAlpha
                            }
                            .background(pillColor, CircleShape)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CapsuleItemGap)
                    ) {
                        tabs.forEachIndexed { index, destination ->
                            CapsuleItem(
                                destination = destination,
                                selected = index == selectedIndex,
                                badgeCount = if (destination == BottomBarDestination.Plugin) moduleUpdateCount else 0,
                                onClick = {
                                    capsule.poke()
                                    if (index == selectedIndex) {
                                        navigator.popBackStack(destination.direction, false)
                                    }
                                    navigator.navigate(destination.direction) {
                                        popUpTo(NavGraphs.root) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Slot ukuran tetap: ikon tidak pernah berpindah tempat, jadi tap selalu kena sasaran.
     * Latar terpilih digambar oleh pill yang bergeser di BottomBar, bukan di sini.
     */
    @Composable
    private fun CapsuleItem(
        destination: BottomBarDestination,
        selected: Boolean,
        badgeCount: Int,
        onClick: () -> Unit
    ) {
        val label = stringResource(id = destination.labelId)
        val tint by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 250),
            label = "capsuleItemTint"
        )
        // "Pop" kecil pada ikon yang baru dipilih; area sentuh tidak berubah.
        val pop by animateFloatAsState(
            targetValue = if (selected) 1.12f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "capsuleItemPop"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = CapsuleItemWidth, height = CapsuleItemHeight)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            val iconContent: @Composable () -> Unit = {
                Crossfade(
                    targetState = selected,
                    animationSpec = tween(durationMillis = 180),
                    label = "capsuleItemIcon"
                ) { isSelected ->
                    Icon(
                        imageVector = if (isSelected) destination.iconSelected else destination.iconNotSelected,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                        }
                    )
                }
            }
            if (badgeCount > 0) {
                BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) { iconContent() }
            } else {
                iconContent()
            }
        }
    }
}

/** Shared state of the floating nav capsule: [active] (full) vs idle (dimmer, a bit smaller). */
@Stable
private class CapsuleState {
    var active by mutableStateOf(true)
        private set
    var tick by mutableIntStateOf(0)
        private set

    fun poke() {
        active = true
        tick++
    }

    fun fold() {
        active = false
    }
}

// Ukuran & posisi kapsul nav. CapsuleGap = jarak kapsul dari tepi bawah (di atas nav bar sistem)
// supaya benar-benar melayang; CapsuleReserve = ruang yang disisakan untuk konten di bawahnya.
private val CapsuleItemWidth = 60.dp
private val CapsuleItemHeight = 48.dp
private val CapsuleItemGap = 4.dp
private val CapsuleGap = 24.dp
private val CapsuleReserve = CapsuleItemHeight + 12.dp + CapsuleGap

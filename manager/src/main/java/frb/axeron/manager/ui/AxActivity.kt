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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.min
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import frb.axeron.manager.ui.screen.HomeScreen
import frb.axeron.manager.ui.screen.PrivilegeScreen
import frb.axeron.manager.ui.screen.SettingsScreen
import frb.axeron.manager.ui.screen.plugin.PluginScreen
import frb.axeron.manager.ui.util.LocalCapsuleInset
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
        val swipe = remember { TabSwipeState() }
        val navTransitions = remember(bottomBarRoutes) {
            createNavTransitions(bottomBarRoutes) { swipe.skipNavTransition }
        }

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
        // Active: right after a tap / tab change. Idle (2.5 s) or while the content scrolls
        // underneath: the capsule dims (size and shadow stay as they are).
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

        // Semua layar yang menampilkan kapsul: konten tembus sampai tepi bawah layar dan kapsul
        // melayang di atasnya. Ruang kapsul dikirim lewat LocalCapsuleInset; tiap layar memakainya
        // sebagai padding bawah konten scroll / FAB / snackbar.
        val capsuleInset = if (showBottomBar) CapsuleReserve + navInset else 0.dp

        // Geser kiri/kanan antar tab: konten mengikuti jari, tab tetangga ikut terlihat (preview),
        // lepas jari -> pindah kalau cukup jauh atau cukup cepat, kalau tidak balik ke semula.
        val swipeScope = rememberCoroutineScope()
        val density = LocalDensity.current
        val flingThresholdPx = with(density) { 600.dp.toPx() }
        val currentRoute = rememberUpdatedState(currentDestination?.route)
        val nextTab = swipeTabs.getOrNull(currentTabIndex + 1)?.takeIf { currentTabIndex >= 0 }
        val prevTab = swipeTabs.getOrNull(currentTabIndex - 1)?.takeIf { currentTabIndex >= 0 }
        val dragNeighbor by remember(swipeTabs, currentTabIndex) {
            derivedStateOf {
                val o = swipe.offsetPx
                when {
                    currentTabIndex < 0 -> null
                    o < 0f -> swipeTabs.getOrNull(currentTabIndex + 1)
                    o > 0f -> swipeTabs.getOrNull(currentTabIndex - 1)
                    else -> null
                }
            }
        }
        val previewTab = swipe.hold ?: dragNeighbor
        val dragState = rememberDraggableState { delta ->
            swipe.onDrag(delta, hasNext = nextTab != null, hasPrev = prevTab != null)
        }

        Box {
            Scaffold(
                contentWindowInsets = WindowInsets()
            ) { innerPadding ->
                CompositionLocalProvider(
                    LocalSnackbarHost provides snackBarHostState,
                    LocalCapsuleInset provides capsuleInset
                ) {
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .then(
                                if (showBottomBar) {
                                    // Nav bar sudah ditangani kapsul + LocalCapsuleInset, jadi
                                    // Scaffold di tiap layar tidak perlu menambah padding bawah lagi.
                                    Modifier.consumeWindowInsets(
                                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                                    )
                                } else Modifier
                            )
                            .onSizeChanged { swipe.width = it.width.toFloat() }
                            .nestedScroll(capsuleScroll)
                            // Dipasang di parent, jadi komponen anak yang sudah memakai drag
                            // horizontal (slider, dll) tetap menang.
                            .draggable(
                                state = dragState,
                                orientation = Orientation.Horizontal,
                                enabled = showBottomBar && currentTabIndex >= 0,
                                onDragStarted = {
                                    swipe.beginDrag()
                                    capsule.poke()
                                },
                                onDragStopped = { velocity ->
                                    if (!swipe.handingOff && swipe.offsetPx != 0f) {
                                        val goingNext = swipe.offsetPx < 0f
                                        val target = if (goingNext) nextTab else prevTab
                                        val targetIndex = if (goingNext) currentTabIndex + 1 else currentTabIndex - 1
                                        swipe.job = swipeScope.launch {
                                            swipe.settle(
                                                velocity = velocity,
                                                flingThresholdPx = flingThresholdPx,
                                                target = target,
                                                targetIndex = targetIndex,
                                                routeNow = { currentRoute.value },
                                                navigateTo = { tab ->
                                                    navigator.navigate(tab.direction) {
                                                        popUpTo(NavGraphs.root) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationX = swipe.offsetPx }
                        ) {
                            DestinationsNavHost(
                                navGraph = NavGraphs.root,
                                navController = navController,
                                dependenciesContainerBuilder = {
                                    dependency(viewModelGlobal)
                                },
                                defaultTransitions = navTransitions
                            )
                        }

                        // Preview tab tetangga: ikut bergeser bersama jari, menempel di sisinya.
                        previewTab?.let { tab ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = when {
                                            swipe.hold != null -> 0f
                                            swipe.offsetPx < 0f -> swipe.offsetPx + size.width
                                            else -> swipe.offsetPx - size.width
                                        }
                                    }
                            ) {
                                TabPreview(tab, navigator, viewModelGlobal)
                            }
                        }
                    }
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
                    capsule,
                    swipe
                )
            }
        }
    }

    /** Salinan tab tetangga yang hanya hidup selama jari menggeser (atau sesaat saat serah-terima). */
    @Composable
    private fun TabPreview(
        tab: BottomBarDestination,
        navigator: DestinationsNavigator,
        viewModelGlobal: ViewModelGlobal
    ) {
        when (tab) {
            BottomBarDestination.Home -> HomeScreen(navigator, viewModelGlobal)
            BottomBarDestination.Privilege -> PrivilegeScreen(navigator, viewModelGlobal)
            BottomBarDestination.Plugin -> PluginScreen(navigator, viewModelGlobal)
            BottomBarDestination.Settings -> SettingsScreen(navigator, viewModelGlobal)
        }
    }

    /**
     * Transisi navigasi ala Axora:
     * - pindah antar tab bottom bar: slide horizontal (spring) searah indeks tab
     * - masuk sub-screen: slide dari kanan; kembali: scale-out + fade
     */
    private fun createNavTransitions(
        bottomBarRoutes: Set<String>,
        skip: () -> Boolean
    ): NavHostAnimatedDestinationStyle {
        fun tabIndex(route: String?): Int =
            BottomBarDestination.entries.indexOfFirst { it.direction.route == route }

        return object : NavHostAnimatedDestinationStyle() {
            override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = enter@{
                if (skip()) return@enter EnterTransition.None
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

            override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = exit@{
                if (skip()) return@exit ExitTransition.None
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

            override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = popEnter@{
                if (skip()) return@popEnter EnterTransition.None
                if (targetState.destination.route in bottomBarRoutes) {
                    slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                } else {
                    fadeIn(animationSpec = tween(340))
                }
            }

            override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = popExit@{
                if (skip()) return@popExit ExitTransition.None
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
        capsule: CapsuleState,
        swipe: TabSwipeState
    ) {
        // Saat idle (tidak ada interaksi / konten sedang di-scroll) kapsul meredup: badan dan isinya
        // memudar ke 0,85. Ukuran tidak berubah dan shadow dibiarkan penuh (alpha-nya sengaja
        // tidak dipasang di layer Surface, supaya shadow tidak ikut memudar).
        val fade by animateFloatAsState(
            targetValue = if (capsule.active) 1f else 0.85f,
            animationSpec = tween(durationMillis = 350),
            label = "capsuleFade"
        )

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = CapsuleGap)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = fade),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = CapsuleShadow
            ) {
                val tabs = BottomBarDestination.entries.filter {
                    axeronServerInfo.isRunning() || !it.needAxeron
                }
                // key(): state tiap tab tetap stabil saat tab muncul/hilang (server hidup/mati).
                val selectedStates = tabs.map { tab ->
                    key(tab) { navController.isRouteOnBackStackAsState(tab.direction) }
                }
                val selectedIndex = selectedStates.indexOfFirst { it.value }
                val maxIdx = (tabs.size - 1).coerceAtLeast(0).toFloat()

                // Posisi indikator = indeks tab (0f..n-1f) + progres geser jari. Spring dipakai saat
                // tab berganti lewat tap; saat digeser/di-hold indikator langsung mengikuti jari.
                // Dibaca di fase layout/draw, tanpa recomposition per frame.
                LaunchedEffect(selectedIndex) {
                    if (selectedIndex >= 0) {
                        swipe.pill.animateTo(
                            targetValue = selectedIndex.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
                // Ikon yang sedang "di bawah" indikator ikut disorot, jadi preview terasa nyambung.
                val highlighted by remember(selectedIndex, maxIdx) {
                    derivedStateOf {
                        if (selectedIndex < 0) -1 else swipe.position(maxIdx).roundToInt()
                    }
                }
                val goTo: (BottomBarDestination) -> Unit = { destination ->
                    navigator.navigate(destination.direction) {
                        popUpTo(NavGraphs.root) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current
                val tabsNow = rememberUpdatedState(tabs)
                val selectedNow = rememberUpdatedState(selectedIndex)
                val grabScale by animateFloatAsState(
                    targetValue = if (swipe.scrubbing) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "capsuleGrab"
                )
                val pillAlpha by animateFloatAsState(
                    targetValue = if (selectedIndex >= 0) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "capsulePillAlpha"
                )

                // Lebar tiap item = lebar ikon + (label * seberapa "terpilih" item itu).
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()
                val labelStyle = MaterialTheme.typography.labelLarge
                val labels = tabs.map { stringResource(id = it.labelId) }
                val metrics = remember(labels, labelStyle, density) {
                    with(density) {
                        val maxLabel = CapsuleLabelMax.toPx().toInt()
                        CapsuleMetrics(
                            basePx = CapsuleItemWidth.toPx(),
                            gapPx = CapsuleItemGap.toPx(),
                            extras = FloatArray(labels.size) { i ->
                                val w = textMeasurer.measure(
                                    text = labels[i],
                                    style = labelStyle,
                                    softWrap = false,
                                    maxLines = 1
                                ).size.width
                                CapsuleLabelGap.toPx() + w.coerceAtMost(maxLabel)
                            }
                        )
                    }
                }
                val pillColor = MaterialTheme.colorScheme.secondaryContainer
                // Posisi jari -> indeks indikator kontinu (indikator berpusat di bawah jari).
                val scrubIndexAt: (Float) -> Float = { x ->
                    metrics.pAtX(x, swipe.position(maxIdx))
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer { this.alpha = fade }
                        .padding(6.dp)
                        // Tahan lalu geser: indikator ikut jari, lepas = pindah ke tab di bawahnya.
                        // Tap biasa tetap ditangani CapsuleItem (long-press membatalkan klik).
                        .pointerInput(metrics) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    swipe.job?.cancel()
                                    swipe.scrubPos = scrubIndexAt(offset.x)
                                    swipe.scrubbing = true
                                    capsule.poke()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    swipe.scrubPos = scrubIndexAt(change.position.x)
                                },
                                onDragEnd = {
                                    val list = tabsNow.value
                                    val from = swipe.scrubPos
                                    val idx = from.roundToInt().coerceIn(0, (list.size - 1).coerceAtLeast(0))
                                    scope.launch {
                                        // Lanjut dari posisi jari tanpa lompat, lalu mendarat di tab tujuan.
                                        swipe.pill.snapTo(from)
                                        swipe.scrubbing = false
                                        list.getOrNull(idx)?.let { dest ->
                                            if (idx != selectedNow.value) goTo(dest)
                                            swipe.pill.animateTo(
                                                targetValue = idx.toFloat(),
                                                animationSpec = spring(
                                                    dampingRatio = 0.72f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    val back = selectedNow.value.coerceAtLeast(0).toFloat()
                                    scope.launch {
                                        swipe.pill.snapTo(swipe.scrubPos)
                                        swipe.scrubbing = false
                                        swipe.pill.animateTo(
                                            targetValue = back,
                                            animationSpec = spring(
                                                dampingRatio = 0.72f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CapsuleItemGap),
                        modifier = Modifier.drawBehind {
                            // Indikator terpilih: satu pill yang meluncur dan melebar mengikuti
                            // item yang sedang "terpilih" (ikon + label).
                            if (metrics.count == 0) return@drawBehind
                            val p = swipe.position(maxIdx)
                            val l = metrics.pillLeft(p)
                            val r = metrics.pillRight(p)
                            val h = size.height * grabScale
                            val w = (r - l) * grabScale
                            val cx = (l + r) / 2f
                            drawRoundRect(
                                color = pillColor,
                                topLeft = Offset(cx - w / 2f, (size.height - h) / 2f),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(h / 2f, h / 2f),
                                alpha = pillAlpha
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, destination ->
                            CapsuleItem(
                                destination = destination,
                                label = labels[index],
                                selected = index == highlighted,
                                badgeCount = if (destination == BottomBarDestination.Plugin) moduleUpdateCount else 0,
                                widthPx = {
                                    val p = if (selectedIndex < 0) -10f else swipe.position(maxIdx)
                                    metrics.width(index, p).roundToInt()
                                },
                                expansion = {
                                    if (selectedIndex < 0) 0f
                                    else metrics.expansion(index, swipe.position(maxIdx))
                                },
                                onClick = {
                                    // Klik yang menyusul setelah hold-geser tidak boleh ikut dieksekusi.
                                    if (!swipe.scrubbing) {
                                        capsule.poke()
                                        if (index == selectedIndex) {
                                            navigator.popBackStack(destination.direction, false)
                                        }
                                        goTo(destination)
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
     * Satu item kapsul: ikon di kiri (posisinya tetap), label muncul di kanan ikon saat item
     * terpilih. Lebar item mengikuti [widthPx]; label bergeser dari ikon ke kanan sambil
     * memudar masuk (dipotong oleh batas item yang ikut melebar).
     */
    @Composable
    private fun CapsuleItem(
        destination: BottomBarDestination,
        label: String,
        selected: Boolean,
        badgeCount: Int,
        widthPx: () -> Int,
        expansion: () -> Float,
        onClick: () -> Unit
    ) {
        val tint by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 250),
            label = "capsuleItemTint"
        )
        // "Pop" kecil pada ikon yang baru dipilih.
        val pop by animateFloatAsState(
            targetValue = if (selected) 1.12f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "capsuleItemPop"
        )
        val labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        val slidePx = with(LocalDensity.current) { 14.dp.toPx() }

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .layout { measurable, _ ->
                    val w = widthPx()
                    val h = CapsuleItemHeight.roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) { placeable.place(0, 0) }
                }
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.padding(start = CapsuleIconStart)) {
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .padding(start = CapsuleIconStart + CapsuleIconSize + CapsuleLabelGap)
                    // Ukuran asli teks dipertahankan; yang terlihat dibatasi lebar item.
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        val e = expansion()
                        alpha = e
                        translationX = -(1f - e) * slidePx
                    }
            )
        }
    }
}

/**
 * State geser antar tab. [offsetPx] = pergeseran konten (negatif = menuju tab berikutnya),
 * [pill] = posisi indikator kapsul (indeks tab), [hold] = tab tujuan yang dipertahankan
 * sebagai preview sesaat sampai NavHost benar-benar menampilkannya.
 */
@Stable
private class TabSwipeState {
    var offsetPx by mutableFloatStateOf(0f)
    var width by mutableFloatStateOf(0f)
    var hold by mutableStateOf<BottomBarDestination?>(null)
    val pill = Animatable(0f)

    /** Animasi lepas-jari sedang jalan; boleh disela jari berikutnya kapan saja. */
    var settling by mutableStateOf(false)

    /** Serah-terima ke tab asli (beberapa frame saja); satu-satunya fase yang tidak bisa disela. */
    var handingOff = false
    var job: Job? = null
    private var generation = 0

    /** Dibaca di transisi NavHost; true = pindah tab tanpa animasi (animasinya sudah dari jari). */
    var skipNavTransition = false

    /** Hold-lalu-geser pada kapsul: pill mengikuti jari, lepas = buka tab yang dituju. */
    var scrubbing by mutableStateOf(false)
    var scrubPos by mutableFloatStateOf(0f)

    /** Posisi indikator kapsul sebagai indeks kontinu: hold-geser > tap/spring + progres geser konten. */
    fun position(maxIndex: Float): Float =
        (if (scrubbing) scrubPos else pill.value + progress).coerceIn(0f, maxIndex)

    /** Progres geser -1f..1f (positif = menuju tab berikutnya), dipakai pill. */
    val progress: Float
        get() = if (width > 0f) (-offsetPx / width).coerceIn(-1f, 1f) else 0f

    /** Jari baru menyentuh saat animasi sebelumnya belum selesai: batalkan, lanjut dari posisi sekarang. */
    fun beginDrag() {
        if (!handingOff) job?.cancel()
    }

    fun onDrag(delta: Float, hasNext: Boolean, hasPrev: Boolean) {
        if (handingOff || width <= 0f) return
        val raw = offsetPx + delta
        val allowed = if (raw < 0f) hasNext else hasPrev
        offsetPx = if (allowed) {
            raw.coerceIn(-width, width)
        } else {
            // Tidak ada tab di sisi itu: tahan, hanya menggeser sedikit (efek karet).
            (offsetPx + delta * 0.25f).coerceIn(-width * 0.12f, width * 0.12f)
        }
    }

    suspend fun settle(
        velocity: Float,
        flingThresholdPx: Float,
        target: BottomBarDestination?,
        targetIndex: Int,
        routeNow: () -> String?,
        navigateTo: (BottomBarDestination) -> Unit
    ) {
        val mine = ++generation
        settling = true
        try {
            val w = width
            if (w <= 0f) {
                offsetPx = 0f
                return
            }
            val toLeft = offsetPx < 0f
            val farEnough = abs(offsetPx) > w * 0.33f
            val flung = abs(offsetPx) > 8f && abs(velocity) > flingThresholdPx &&
                (velocity < 0f) == toLeft
            // visibilityThreshold dalam px: tanpa ini spring Float baru "selesai" di 0,01 px,
            // ekor animasi yang tak terlihat itu yang bikin pindah tab terasa menunggu.
            val spec = spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 800f,
                visibilityThreshold = 1f
            )

            if (target == null || !(farEnough || flung)) {
                animate(offsetPx, 0f, velocity, spec) { v, _ -> offsetPx = v }
                offsetPx = 0f
                return
            }

            animate(offsetPx, if (toLeft) -w else w, velocity, spec) { v, _ -> offsetPx = v }

            // Serah-terima (tidak bisa dibatalkan): preview tujuan menutupi layar di posisi 0,
            // NavHost pindah tab tanpa animasi, preview dilepas setelah tab asli tampil.
            withContext(NonCancellable) {
                handingOff = true
                try {
                    settling = false
                    hold = target
                    skipNavTransition = true
                    pill.snapTo(targetIndex.toFloat())
                    offsetPx = 0f
                    navigateTo(target)
                    withTimeoutOrNull(400) {
                        snapshotFlow { routeNow() }.first { it == target.direction.route }
                    }
                    repeat(2) { withFrameNanos { } }
                } finally {
                    hold = null
                    skipNavTransition = false
                    handingOff = false
                }
            }
        } finally {
            if (generation == mine) settling = false
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
// Ukuran kapsul nav. CapsuleItemWidth = lebar item saat hanya ikon; item terpilih melebar
// untuk label. CapsuleGap = jarak kapsul dari tepi bawah (di atas nav bar sistem) supaya
// benar-benar melayang; CapsuleReserve = ruang yang dipakai konten sebagai padding bawah.
private val CapsuleItemWidth = 58.dp
private val CapsuleItemHeight = 56.dp
private val CapsuleItemGap = 4.dp
private val CapsuleIconSize = 24.dp
private val CapsuleIconStart = (CapsuleItemWidth - CapsuleIconSize) / 2
private val CapsuleLabelGap = 8.dp
private val CapsuleLabelMax = 96.dp
private val CapsuleGap = 24.dp
private val CapsuleShadow = 10.dp
private val CapsuleReserve = CapsuleItemHeight + 12.dp + CapsuleGap

/**
 * Geometri kapsul sebagai fungsi posisi indikator [p] (indeks kontinu). Item ke-i melebar
 * sebesar extras[i] * expansion(i, p); indikator = interpolasi tepi dua item terdekat.
 */
private class CapsuleMetrics(
    val basePx: Float,
    val gapPx: Float,
    val extras: FloatArray
) {
    val count: Int get() = extras.size

    fun expansion(index: Int, p: Float): Float = max(0f, 1f - abs(p - index))

    fun width(index: Int, p: Float): Float = basePx + expansion(index, p) * extras[index]

    fun left(index: Int, p: Float): Float {
        var x = 0f
        for (j in 0 until index) x += width(j, p) + gapPx
        return x
    }

    private fun right(index: Int, p: Float): Float = left(index, p) + width(index, p)

    fun pillLeft(p: Float): Float {
        val i0 = floor(p).toInt().coerceIn(0, count - 1)
        val i1 = min(i0 + 1, count - 1)
        return lerp(left(i0, p), left(i1, p), (p - i0).coerceIn(0f, 1f))
    }

    fun pillRight(p: Float): Float {
        val i0 = floor(p).toInt().coerceIn(0, count - 1)
        val i1 = min(i0 + 1, count - 1)
        return lerp(right(i0, p), right(i1, p), (p - i0).coerceIn(0f, 1f))
    }

    /** Kebalikan dari pusat indikator: posisi x (px, relatif ke tepi kiri baris item) -> indeks kontinu. */
    fun pAtX(x: Float, current: Float): Float {
        if (count <= 1) return 0f
        val c = FloatArray(count) { left(it, current) + width(it, current) / 2f }
        if (x <= c[0]) return 0f
        if (x >= c[count - 1]) return (count - 1).toFloat()
        var i = 0
        while (i < count - 2 && x >= c[i + 1]) i++
        return i + (x - c[i]) / (c[i + 1] - c[i])
    }
}

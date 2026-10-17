package frb.axeron.manager.ui.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarController not present")
}

/**
 * Ruang di bawah layar tab yang ditutupi kapsul nav floating (tinggi kapsul + jarak + nav bar).
 * Layar tab memakainya sebagai padding bawah konten scroll / FAB / snackbar supaya konten
 * bisa tembus sampai ke tepi bawah tapi item terakhir tetap bisa di-scroll di atas kapsul.
 * Bernilai 0.dp di luar layar tab.
 */
val LocalCapsuleInset = compositionLocalOf<Dp> { 0.dp }

/**
 * Posisi scroll empat layar tab, dipegang di AxActivity (bukan di tiap layar). Layar asli dan
 * salinan preview saat geser antar tab memakai state yang sama, jadi preview tidak pernah
 * menampilkan daftar di posisi paling atas lalu "melompat" saat layar asli muncul.
 */
@Stable
class TabScrollStates(
    val home: ScrollState,
    val privilege: LazyListState,
    val plugin: LazyListState,
    val settings: ScrollState
)

val LocalTabScrollStates = compositionLocalOf<TabScrollStates?> { null }

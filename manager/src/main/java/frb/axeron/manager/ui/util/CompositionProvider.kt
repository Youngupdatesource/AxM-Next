package frb.axeron.manager.ui.util

import androidx.compose.material3.SnackbarHostState
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

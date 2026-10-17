package frb.axeron.manager.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import frb.axeron.manager.ui.util.AppearancePrefs

/**
 * Switch standar aplikasi. Mengikuti opsi Appearance > Switch icon: aktif = thumb berisi
 * ikon ✓ (hidup) / ✕ (mati), nonaktif = thumb bundar polos.
 */
@Composable
fun AxSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
) {
    val thumb: (@Composable () -> Unit)? = if (AppearancePrefs.switchIcon) {
        {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        }
    } else null

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumb,
        enabled = enabled,
        interactionSource = interactionSource
    )
}

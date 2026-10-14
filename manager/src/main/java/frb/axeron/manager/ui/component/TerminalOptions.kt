package frb.axeron.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.TerminalPrefs
import kotlin.math.roundToInt

private const val SCALE_STEP = 0.1f

@Composable
fun TerminalOptionsCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsItem(
            label = stringResource(R.string.terminal_line_numbers),
            description = stringResource(R.string.terminal_line_numbers_desc),
            iconVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
            checked = TerminalPrefs.lineNumbers,
            onSwitchChange = { TerminalPrefs.lineNumbers = it }
        )

        val scale = TerminalPrefs.textScale
        SettingsItem(
            label = stringResource(R.string.terminal_text_size),
            description = "${(scale * 100).roundToInt()}%",
            iconVector = Icons.Outlined.FormatSize,
        ) { _, _ ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 52.dp, end = 4.dp, bottom = 4.dp)
            ) {
                IconButton(
                    onClick = { TerminalPrefs.textScale = scale.snap() - SCALE_STEP },
                    enabled = scale > TerminalPrefs.MIN_SCALE
                ) { Icon(Icons.Outlined.TextDecrease, contentDescription = null) }
                Slider(
                    value = scale,
                    onValueChange = { TerminalPrefs.textScale = it.snap() },
                    valueRange = TerminalPrefs.MIN_SCALE..TerminalPrefs.MAX_SCALE,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { TerminalPrefs.textScale = scale.snap() + SCALE_STEP },
                    enabled = scale < TerminalPrefs.MAX_SCALE
                ) { Icon(Icons.Outlined.TextIncrease, contentDescription = null) }
            }
        }

        SettingsItem(
            label = stringResource(R.string.terminal_unlimited),
            description = stringResource(R.string.terminal_unlimited_desc),
            iconVector = Icons.Outlined.AllInclusive,
            checked = TerminalPrefs.unlimited,
            onSwitchChange = { TerminalPrefs.unlimited = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalOptionsSheet(onDismissRequest: () -> Unit) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.terminal_view_options),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            TerminalOptionsCard()
        }
    }
}

private fun Float.snap(): Float = (this / SCALE_STEP).roundToInt() * SCALE_STEP

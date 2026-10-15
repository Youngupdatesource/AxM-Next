package frb.axeron.manager.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RoundedCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.36f)
    val fill by animateColorAsState(if (checked) colors.primary else Color.Transparent, label = "checkFill")
    val stroke by animateColorAsState(if (checked) colors.primary else colors.outline, label = "checkStroke")

    Box(
        modifier = modifier
            .size(size)
            .background(fill, shape)
            .border(2.dp, stroke, shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}

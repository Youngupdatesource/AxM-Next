package frb.axeron.manager.ui.component

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.TerminalBuffer
import frb.axeron.manager.ui.util.TerminalPrefs
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlinx.coroutines.withContext

/** Default text style for command output, scaled by [TerminalPrefs.textScale]. */
@Composable
fun terminalTextStyle(scale: Float = TerminalPrefs.textScale): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp * scale,
    lineHeight = 16.sp * scale,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * Renders a [TerminalBuffer]: line numbers, follow-the-bottom, a fast-scroll thumb for huge
 * output and a side pill (top / go to line[:column] / bottom).
 *
 * Index based: only the visible lines are read from the buffer, so a million spooled lines
 * scroll like twenty. View options (line numbers, text scale, unlimited lines) come from
 * [TerminalPrefs].
 *
 * @param wrap soft-wrap long lines; when false the text pans horizontally while the line
 * numbers stay put.
 * @param filter hides lines by kind (e.g. stderr outside developer mode). Remember it.
 * @param color per-kind base colour; ANSI colours in the text still win.
 * @param textViewerOpen shows the full-screen text viewer (every line, "select all" works).
 * Long-press on the output itself selects text in place; the viewer is only opened explicitly.
 */
@Composable
fun <K : Any> TerminalOutput(
    buffer: TerminalBuffer<K>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    wrap: Boolean = true,
    filter: ((K) -> Boolean)? = null,
    color: @Composable (K) -> Color = { MaterialTheme.colorScheme.onSurfaceVariant },
    jumpButtonPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
    textViewerOpen: Boolean = false,
    onTextViewerDismiss: () -> Unit = {},
) {
    val showLineNumbers = TerminalPrefs.lineNumbers
    val style = terminalTextStyle()

    // The storage mode follows the shared preference, also for output already on screen.
    val unlimited = TerminalPrefs.unlimited
    LaunchedEffect(buffer, unlimited) { buffer.setUnlimited(unlimited) }

    val index = remember(buffer, filter) { filter?.let { FilterIndex(it) } }
    val view by remember(buffer, index) {
        derivedStateOf {
            val snap = buffer.snapshot
            index?.view(snap, buffer) ?: LineView(snap.size, snap.head, null, 0)
        }
    }
    val version = buffer.snapshot.version

    // ---- follow mode ----
    var follow by remember { mutableStateOf(true) }
    val atBottom by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index == info.totalItemsCount - 1 &&
                    last.offset + last.size <= info.viewportEndOffset + 8
        }
    }
    val dragged by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged, atBottom) {
        if (atBottom) follow = true
        else if (dragged) follow = false
    }
    // Once per published snapshot (throttled by the buffer), not per chunk. Jump instantly:
    // rapid output would keep cancelling an animated scroll.
    LaunchedEffect(state, buffer) {
        snapshotFlow { view to follow }.collect { (shown, following) ->
            if (following && shown.count > 0) state.scrollToItem(shown.count - 1, Int.MAX_VALUE)
        }
    }

    // ---- horizontal panning (no-wrap) ----
    val pan = remember { HorizontalPan() }
    LaunchedEffect(wrap) { pan.reset() }
    LaunchedEffect(buffer) {
        snapshotFlow { buffer.snapshot.isEmpty }.collect { if (it) pan.reset() }
    }
    val panState = rememberScrollableState { delta ->
        val old = pan.offset
        pan.scrollTo(old - delta)
        old - pan.offset
    }

    // ---- gutter ----
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val charWidth = remember(style, density) { measurer.measure("0", style).size.width }
    val digits = maxOf(3, buffer.snapshot.tail.toString().length)
    val gutterWidth = with(density) { (charWidth * digits).toDp() }
    val gutterStyle = style.copy(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        textAlign = TextAlign.End
    )

    // ---- go to line ----
    var highlight by remember { mutableStateOf<Mark?>(null) }
    LaunchedEffect(highlight) {
        if (highlight != null) {
            delay(2500)
            highlight = null
        }
    }
    val rowHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val columnHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    var showGoTo by remember { mutableStateOf(false) }
    var pillActive by remember { mutableStateOf(false) }
    var pillTick by remember { mutableIntStateOf(0) }
    var thumbDragging by remember { mutableStateOf(false) }
    // Long-press selects text right in the output (SelectionContainer around the list). Lines
    // that were never composed can't be part of such a selection, so the full-text viewer stays
    // available through [textViewerOpen] for "copy everything".
    val scope = rememberCoroutineScope()

    val canScroll by remember {
        derivedStateOf { state.canScrollBackward || state.canScrollForward }
    }
    val showScrollbar = canScroll && view.count > SCROLLBAR_MIN_LINES

    Box(modifier) {
        Box(Modifier.fillMaxSize()) {
            SelectionContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (wrap) Modifier
                        else Modifier.scrollable(panState, Orientation.Horizontal)
                    )
            ) {
                val shown = view
                items(
                    count = shown.count,
                    key = { shown.positionAt(it) },
                    contentType = { 0 }
                ) { i ->
                    val position = shown.positionAt(i)
                    // Re-read on every publish: an open line (progress bar) changes in place.
                    val line = remember(position, version) { buffer.lineAt(position) }
                        ?: return@items
                    val mark = highlight?.takeIf { it.line == line.number }
                    val parsed = remember(line.text) { line.text.toAnsiString() }
                    val text = remember(parsed, mark) {
                        if (mark?.column == null) parsed
                        else parsed.withColumnMark(mark.column - 1, columnHighlight)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (mark != null) Modifier.background(rowHighlight) else Modifier)
                    ) {
                        if (showLineNumbers) {
                            // Line numbers are not part of the output: keep them out of a selection.
                            DisableSelection {
                            BasicText(
                                text = line.number.toString(),
                                style = gutterStyle,
                                maxLines = 1,
                                modifier = Modifier.width(gutterWidth)
                            )
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        BasicText(
                            text = text,
                            style = style.copy(color = color(line.kind)),
                            softWrap = wrap,
                            modifier = Modifier
                                .weight(1f)
                                .then(if (wrap) Modifier else Modifier.horizontalPan(pan))
                        )
                    }
                }
            }
            }
        }

        if (showScrollbar) {
            FastScrollbar(
                state = state,
                count = view.count,
                onDragStart = {
                    follow = false
                    thumbDragging = true
                    pillTick++
                },
                onDragEnd = {
                    thumbDragging = false
                    pillTick++
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(contentPadding.let {
                        PaddingValues(top = it.calculateTopPadding(), bottom = it.calculateBottomPadding())
                    })
            )
        }

        // The nav pill only shows while the user is scrolling, then slides away to uncover the output.
        LaunchedEffect(state) {
            state.interactionSource.interactions.collect {
                if (it is DragInteraction.Start) pillTick++
            }
        }
        LaunchedEffect(pillTick) {
            if (pillTick == 0) return@LaunchedEffect
            pillActive = true
            delay(300)
            snapshotFlow { state.isScrollInProgress }.first { !it }
            delay(PILL_IDLE_MS)
            pillActive = false
        }

        val currentLine by remember {
            derivedStateOf {
                val v = view
                if (v.count == 0) null
                else v.positionAt(state.firstVisibleItemIndex.coerceIn(0, v.count - 1)) + 1
            }
        }
        AnimatedVisibility(
            visible = canScroll && view.count > 0 && (pillActive || thumbDragging),
            enter = fadeIn() + scaleIn(initialScale = 0.6f, transformOrigin = TransformOrigin(1f, 0.5f)) +
                    slideInHorizontally { it / 2 },
            exit = fadeOut() + scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 0.5f)) +
                    slideOutHorizontally { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(jumpButtonPadding)
                .padding(12.dp)
                // Stay clear of the scrollbar thumb.
                .padding(end = if (showScrollbar) 14.dp else 0.dp)
        ) {
            TerminalNavPill(
                currentLine = currentLine,
                following = follow,
                canScrollUp = state.canScrollBackward,
                onTop = {
                    pillTick++
                    follow = false
                    scope.launch { state.scrollToItem(0) }
                },
                onGoTo = {
                    pillTick++
                    showGoTo = true
                },
                onBottom = {
                    pillTick++
                    follow = true
                    scope.launch { state.scrollToItem((view.count - 1).coerceAtLeast(0), Int.MAX_VALUE) }
                }
            )
        }
    }

    // One request per opening: TextSelectDialog reloads whenever the request instance changes.
    val viewerRequest = remember(textViewerOpen) {
        val v = view
        if (textViewerOpen && v.count > 0) {
            TextViewerRequest(v, v.positionAt(state.firstVisibleItemIndex.coerceIn(0, v.count - 1)))
        } else null
    }
    viewerRequest?.let { request ->
        TextSelectDialog(
            buffer = buffer,
            request = request,
            fontSizeSp = style.fontSize.value,
            onDismiss = onTextViewerDismiss
        )
    }

    if (showGoTo && view.count > 0) {
        val v = view
        GoToLineDialog(
            first = v.positionAt(0) + 1,
            last = v.positionAt(v.count - 1) + 1,
            onDismiss = { showGoTo = false },
            onGo = { target, column ->
                showGoTo = false
                val i = v.indexOf(target - 1)
                follow = false
                scope.launch {
                    // A few lines of context above the target.
                    state.scrollToItem((i - 3).coerceAtLeast(0))
                    highlight = Mark(v.positionAt(i) + 1, column)
                    if (!wrap) {
                        withFrameNanos { } // let the target line measure its width first
                        val x = column?.let { (it - 1) * charWidth - pan.viewport / 3f } ?: 0f
                        pan.scrollTo(x)
                    }
                }
            }
        )
    }
}

private const val SCROLLBAR_MIN_LINES = 200
private const val PILL_IDLE_MS = 1800L

/** Lines handed to the native text view; keeps a runaway buffer from exhausting memory. */
private const val VIEWER_MAX_LINES = 30_000

/** Which buffer positions are on screen, in order. */
@Immutable
private class LineView(
    val count: Int,
    private val head: Long,
    private val positions: LongArray?,
    private val start: Int,
) {
    fun positionAt(i: Int): Long = if (positions == null) head + i else positions[start + i]

    /** Index of the first shown line at or after [position], clamped to the view. */
    fun indexOf(position: Long): Int {
        if (count == 0) return 0
        if (positions == null) return (position - head).coerceIn(0, count - 1L).toInt()
        var lo = 0
        var hi = count - 1
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (positions[start + mid] < position) lo = mid + 1 else hi = mid
        }
        return lo
    }
}

/**
 * Positions of the lines passing a filter, extended incrementally per snapshot: only new lines
 * are scanned, so filtering stays cheap with a million lines. Rebuilt when positions were
 * dropped or reused (snapshot generation).
 */
private class FilterIndex<K : Any>(private val filter: (K) -> Boolean) {
    private var positions = LongArray(1024)
    private var size = 0
    private var start = 0
    private var scanned = 0L
    private var generation = -1

    fun view(snap: TerminalBuffer.Snapshot, buffer: TerminalBuffer<K>): LineView {
        if (generation != snap.generation) {
            // New array: views handed out earlier keep reading the old one.
            positions = LongArray(1024)
            size = 0
            start = 0
            scanned = snap.head
            generation = snap.generation
        }
        if (scanned < snap.head) scanned = snap.head
        if (scanned < snap.tail) {
            buffer.forEachKind(scanned, snap.tail) { position, kind ->
                if (filter(kind)) {
                    if (size == positions.size) positions = positions.copyOf(size * 2)
                    positions[size++] = position
                }
            }
            scanned = snap.tail
        }
        while (start < size && positions[start] < snap.head) start++
        return LineView(size - start, snap.head, positions, start)
    }
}

/** Draggable thumb on the right edge; maps the touch position straight to a line. */
@Composable
private fun FastScrollbar(
    state: LazyListState,
    count: Int,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeight = 44.dp
    val thumbPx = with(density) { thumbHeight.toPx() }
    var trackPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val scrollFraction by remember(count) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo.size
            val max = (count - visible).coerceAtLeast(1)
            (state.firstVisibleItemIndex.toFloat() / max).coerceIn(0f, 1f)
        }
    }
    val fraction = if (dragging) dragFraction else scrollFraction
    val active = dragging || state.isScrollInProgress
    val alpha by animateFloatAsState(if (active) 0.9f else 0.35f, label = "thumbAlpha")
    val width by animateDpAsState(if (dragging) 8.dp else 4.dp, label = "thumbWidth")
    val thumbColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .onSizeChanged { trackPx = it.height }
            .pointerInput(count) {
                fun jump(y: Float) {
                    val range = (trackPx - thumbPx).coerceAtLeast(1f)
                    dragFraction = ((y - thumbPx / 2) / range).coerceIn(0f, 1f)
                    val target = (dragFraction * (count - 1)).roundToInt()
                    scope.launch { state.scrollToItem(target) }
                }
                detectVerticalDragGestures(
                    onDragStart = {
                        dragging = true
                        onDragStart()
                        jump(it.y)
                    },
                    onDragEnd = {
                        dragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragging = false
                        onDragEnd()
                    }
                ) { change, _ ->
                    change.consume()
                    jump(change.position.y)
                }
            }
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, (fraction * (trackPx - thumbPx).coerceAtLeast(0f)).roundToInt()) }
                .padding(end = 4.dp)
                .width(width)
                .height(thumbHeight)
                .clip(CircleShape)
                .background(thumbColor.copy(alpha = alpha))
        )
    }
}

private class TextViewerRequest(val view: LineView, val target: Long)

private class ViewerText(val text: String, val targetOffset: Int, val truncated: Boolean)

/**
 * Full-screen, natively selectable copy of the output. A single [TextView] owns the whole text,
 * so the system toolbar's "Select all" / "Copy" cover every line, not just the rendered ones.
 */
@Composable
private fun <K : Any> TextSelectDialog(
    buffer: TerminalBuffer<K>,
    request: TextViewerRequest,
    fontSizeSp: Float,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val loaded by produceState<ViewerText?>(null, buffer, request) {
        value = withContext(Dispatchers.Default) {
            val view = request.view
            val first = (view.count - VIEWER_MAX_LINES).coerceAtLeast(0)
            val targetIndex = view.indexOf(request.target).coerceAtLeast(first)
            val sb = StringBuilder()
            var targetOffset = 0
            for (i in first until view.count) {
                if (i == targetIndex) targetOffset = sb.length
                val line = buffer.lineAt(view.positionAt(i)) ?: continue
                sb.append(AnsiFilter.stripAnsi(line.text)).append('\n')
            }
            ViewerText(sb.toString(), targetOffset, first > 0)
        }
    }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.systemBarsPadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.terminal_select_text),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (loaded?.truncated == true)
                                stringResource(R.string.terminal_select_text_truncated, VIEWER_MAX_LINES)
                            else stringResource(R.string.terminal_select_text_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        enabled = loaded != null,
                        onClick = {
                            val ok = loaded?.let { ClipboardUtil.put(context, it.text) } == true
                            Toast.makeText(
                                context,
                                if (ok) R.string.copied else R.string.failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    val data = loaded
                    if (data == null) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val pad = (12 * ctx.resources.displayMetrics.density).toInt()
                                val tv = TextView(ctx).apply {
                                    typeface = Typeface.MONOSPACE
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                                    setTextColor(textColor)
                                    setPadding(pad, pad, pad, pad)
                                    setTextIsSelectable(true)
                                    text = data.text
                                }
                                tv.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                                    override fun onLayoutChange(
                                        v: View, left: Int, top: Int, right: Int, bottom: Int,
                                        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                                    ) {
                                        val layout = tv.layout ?: return
                                        tv.removeOnLayoutChangeListener(this)
                                        val offset = data.targetOffset.coerceIn(0, tv.text.length)
                                        (tv.parent as? View)?.scrollTo(
                                            0, layout.getLineTop(layout.getLineForOffset(offset))
                                        )
                                    }
                                })
                                ScrollView(ctx).apply {
                                    isVerticalScrollBarEnabled = true
                                    clipChildren = true
                                    clipToPadding = true
                                    outlineProvider = ViewOutlineProvider.BOUNDS
                                    clipToOutline = true
                                    addView(
                                        tv,
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalNavPill(
    currentLine: Long?,
    following: Boolean,
    canScrollUp: Boolean,
    onTop: () -> Unit,
    onGoTo: () -> Unit,
    onBottom: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 3.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onTop, enabled = canScrollUp) {
                Icon(
                    Icons.Filled.KeyboardDoubleArrowUp,
                    contentDescription = stringResource(R.string.terminal_scroll_top)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGoTo)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.terminal_line_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = currentLine?.toString() ?: "–",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            IconButton(onClick = onBottom) {
                Icon(
                    Icons.Filled.KeyboardDoubleArrowDown,
                    contentDescription = stringResource(R.string.terminal_scroll_bottom),
                    // Highlighted while new output is not being followed.
                    tint = if (following) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val GOTO_REGEX = Regex("""^(\d+)(?:\s*[:.,\s]\s*(\d+))?$""")

@Composable
private fun GoToLineDialog(
    first: Long,
    last: Long,
    onDismiss: () -> Unit,
    onGo: (line: Long, column: Int?) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val parsed = remember(input) {
        GOTO_REGEX.matchEntire(input.trim())?.let { m ->
            val line = m.groupValues[1].toLongOrNull() ?: return@let null
            val column = m.groupValues[2].toIntOrNull()?.takeIf { it > 0 }
            line to column
        }
    }
    val submit = { parsed?.let { onGo(it.first, it.second) } }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_goto_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                placeholder = { Text("100:20") },
                isError = input.isNotBlank() && parsed == null,
                supportingText = { Text(stringResource(R.string.terminal_goto_range, first, last)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                modifier = Modifier.focusRequester(focus)
            )
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = parsed != null) {
                Text(stringResource(R.string.terminal_goto))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private data class Mark(val line: Long, val column: Int?)

/**
 * Shared horizontal offset for every line. Lines read [offset] only while placing, so panning
 * re-places the visible lines without recomposing or re-measuring the list.
 */
@Stable
private class HorizontalPan {
    var offset by mutableFloatStateOf(0f)
        private set

    /** Widest line measured so far; plain fields, written during layout. */
    var contentWidth = 0
    var viewport = 0

    fun scrollTo(x: Float) {
        offset = x.coerceIn(0f, (contentWidth - viewport).coerceAtLeast(0).toFloat())
    }

    fun reset() {
        contentWidth = 0
        offset = 0f
    }
}

private fun Modifier.horizontalPan(pan: HorizontalPan) = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
    if (placeable.width > pan.contentWidth) pan.contentWidth = placeable.width
    pan.viewport = width
    layout(width, placeable.height) {
        placeable.placeRelative(-pan.offset.roundToInt(), 0)
    }
}

private const val ESC = '\u001B'

/** Most lines carry no ANSI codes; skip the parser for those. */
private fun String.toAnsiString(): AnnotatedString =
    if (indexOf(ESC) < 0) AnnotatedString(this) else parseAsAnsiAnnotatedString()

private fun AnnotatedString.withColumnMark(index: Int, color: Color): AnnotatedString {
    if (index !in indices) return this
    val source = this
    return buildAnnotatedString {
        append(source)
        addStyle(SpanStyle(background = color), index, index + 1)
    }
}

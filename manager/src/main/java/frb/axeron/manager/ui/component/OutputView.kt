package frb.axeron.manager.ui.component

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import frb.axeron.manager.ui.viewmodel.QuickShellViewModel

private const val CLIPBOARD_MAX_CHARS = 240_000

class OutputView(context: Context) : ScrollView(context) {

    private val textView = OutputTextView(context)
    private var committed = 0
    private var tailStart = 0
    private val entryLengths = ArrayDeque<Int>()

    var onOversizedCopy: ((String) -> Unit)?
        get() = textView.onOversizedCopy
        set(value) {
            textView.onOversizedCopy = value
        }

    var onScrollDelta: ((Int) -> Unit)? = null

    init {
        isVerticalScrollBarEnabled = true
        addView(
            textView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            onScrollDelta?.invoke(scrollY - oldScrollY)
        }
    }

    fun style(sizeSp: Float, color: Int, topPaddingPx: Int, bottomPaddingPx: Int) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        textView.setTextColor(color)
        textView.setPadding(0, topPaddingPx, 0, bottomPaddingPx)
    }

    fun render(
        logs: List<QuickShellViewModel.Output>,
        isVisible: (QuickShellViewModel.Output) -> Boolean,
        full: Boolean,
        dropped: Int,
        maxLines: Int
    ): Boolean {
        val stickToBottom = isNearBottom()
        val buffer = ensureEditable()

        if (full || logs.size < committed) {
            buffer.clear()
            committed = 0
            entryLengths.clear()
        } else {
            if (tailStart < buffer.length) buffer.delete(tailStart, buffer.length)
            if (dropped > 0) dropHead(buffer, dropped)
        }
        tailStart = buffer.length

        val lastLine = logs.lastOrNull()
        val hasTail = lastLine != null && !lastLine.completed
        val commitEnd = if (hasTail) logs.lastIndex else logs.size
        val limit = minOf(commitEnd, committed + maxLines)

        for (index in committed until limit) {
            val line = logs[index]
            val before = buffer.length
            if (isVisible(line)) appendLine(buffer, line.output, true)
            entryLengths.addLast(buffer.length - before)
        }
        committed = limit
        tailStart = buffer.length

        val pending = committed < commitEnd
        if (!pending && lastLine != null && hasTail && isVisible(lastLine)) {
            appendLine(buffer, lastLine.output, false)
        }

        if (stickToBottom) scrollToBottomAfterLayout()
        return pending
    }

    fun reset() {
        val buffer = ensureEditable()
        buffer.clear()
        committed = 0
        tailStart = 0
        entryLengths.clear()
    }

    private fun dropHead(buffer: Editable, count: Int) {
        var chars = 0
        var removed = 0
        while (removed < count && entryLengths.isNotEmpty()) {
            chars += entryLengths.removeFirst()
            removed++
        }
        if (chars > 0) buffer.delete(0, chars)
        committed = (committed - count).coerceAtLeast(0)
    }

    private fun ensureEditable(): Editable {
        val current = textView.text
        if (current is Editable) return current
        textView.append("")
        return textView.text as Editable
    }

    private fun appendLine(buffer: Editable, raw: String, newline: Boolean) {
        val safe = if (raw.indexOf('\u0000') >= 0) raw.replace("\u0000", "") else raw
        val styled = try {
            safe.parseAsAnsiAnnotatedString()
        } catch (e: Exception) {
            null
        }
        val start = buffer.length
        if (styled == null) {
            buffer.append(safe)
            if (newline) buffer.append('\n')
            return
        }
        buffer.append(styled.text)
        for (range in styled.spanStyles) {
            val from = start + range.start
            val to = start + range.end
            if (to <= from) continue
            val style = range.item
            if (style.color != Color.Unspecified) {
                buffer.setSpan(
                    ForegroundColorSpan(style.color.toArgb()), from, to,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (style.background != Color.Unspecified) {
                buffer.setSpan(
                    BackgroundColorSpan(style.background.toArgb()), from, to,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            val weight = style.fontWeight
            if (weight != null && weight >= FontWeight.Bold) {
                buffer.setSpan(
                    StyleSpan(Typeface.BOLD), from, to,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (style.fontStyle == FontStyle.Italic) {
                buffer.setSpan(
                    StyleSpan(Typeface.ITALIC), from, to,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            val decoration = style.textDecoration
            if (decoration != null) {
                if (decoration.contains(TextDecoration.Underline)) {
                    buffer.setSpan(UnderlineSpan(), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (decoration.contains(TextDecoration.LineThrough)) {
                    buffer.setSpan(StrikethroughSpan(), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        if (newline) buffer.append('\n')
    }

    private fun isNearBottom(): Boolean {
        val child = getChildAt(0) ?: return true
        val remaining = child.height - (height + scrollY)
        return remaining <= textView.lineHeight * 3
    }

    private fun scrollToBottomAfterLayout() {
        val observer = viewTreeObserver
        if (!observer.isAlive) return
        observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(this)
                val child = getChildAt(0)
                if (child != null) scrollTo(0, (child.height - height).coerceAtLeast(0))
                return true
            }
        })
    }
}

private class OutputTextView(context: Context) : TextView(context) {
    var onOversizedCopy: ((String) -> Unit)? = null

    init {
        typeface = Typeface.MONOSPACE
        setIncludeFontPadding(false)
        setHorizontallyScrolling(true)
        setTextIsSelectable(true)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.copy) {
            val from = minOf(selectionStart, selectionEnd)
            val to = maxOf(selectionStart, selectionEnd)
            if (to - from > CLIPBOARD_MAX_CHARS) {
                onOversizedCopy?.invoke(text.subSequence(from, to).toString())
                return true
            }
        }
        return super.onTextContextMenuItem(id)
    }
}

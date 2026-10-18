package frb.axeron.manager.ui.component

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.util.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * Process-wide memory of the last frame drawn for a banner, plus the size it was drawn at.
 *
 * Banners live inside tabs that leave the composition on every tab switch/swipe. On return the
 * image used to be re-requested from scratch (GIFs are not kept by Coil's memory cache, and
 * the area size is only known after the first layout), so the banner was blank for a frame or
 * more and then popped in. With the last frame kept here it is drawn on the very first frame.
 */
internal object BannerFrameCache {
    private const val MAX_BYTES = 16 * 1024 * 1024

    private val frames = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val sizes = HashMap<String, IntSize>()

    @Synchronized
    fun frame(key: String): Bitmap? = frames.get(key)

    @Synchronized
    fun size(key: String): IntSize = sizes[key] ?: IntSize.Zero

    @Synchronized
    fun putSize(key: String, size: IntSize) {
        if (sizes.size > 64) sizes.clear()
        sizes[key] = size
    }

    @Synchronized
    fun putFrame(key: String, bitmap: Bitmap, replace: Boolean = false) {
        if (bitmap.allocationByteCount > MAX_BYTES / 2) return
        if (replace || frames.get(key) == null) frames.put(key, bitmap)
    }
}

/**
 * Screens drawn only as a preview (the swipe-between-tabs peek) switch banner animation off:
 * a GIF decoding and redrawing every frame under a moving page is what made transitions lag.
 */
val LocalBannerAnimationAllowed = compositionLocalOf { true }

/**
 * Banners animate only while their own screen is fully RESUMED. A navigation entry is not
 * resumed while it slides in or out, so during a transition the banner shows its still frame
 * and starts moving once the screen has settled.
 */
@Composable
fun rememberBannerAnimate(): Boolean {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    return state.isAtLeast(Lifecycle.State.RESUMED) && LocalBannerAnimationAllowed.current
}

/**
 * Banner (image/GIF/WebP) that fills its parent without flicker when it re-enters the
 * composition.
 *
 * - Drawn through Modifier.paint(sizeToIntrinsics = false) so the image never influences the
 *   height of the card around it (the parent decides the size).
 * - The cached last frame is shown until the live painter has loaded, then swapped in the same
 *   composition, so the two never blend.
 * - [animate] = false keeps only the cached frame (no live painter, so a GIF stops animating).
 */
@Composable
fun CachedBannerImage(
    cacheKey: String,
    model: Any?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    alignment: Alignment = Alignment.Center,
    animate: Boolean = rememberBannerAnimate(),
) {
    val context = LocalContext.current
    var areaSize by remember(cacheKey) { mutableStateOf(BannerFrameCache.size(cacheKey)) }
    var cached by remember(cacheKey) { mutableStateOf(BannerFrameCache.frame(cacheKey)?.asImageBitmap()) }

    Box(
        modifier = modifier.onSizeChanged {
            if (it.width > 0 && it.height > 0 && it != areaSize) {
                areaSize = it
                BannerFrameCache.putSize(cacheKey, it)
            }
        }
    ) {
        val live = animate && model != null && areaSize.width > 0 && areaSize.height > 0
        val painter = if (live) {
            rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .memoryCacheKey("$cacheKey:${areaSize.width}x${areaSize.height}")
                    .size(areaSize.width, areaSize.height)
                    .build(),
                contentScale = ContentScale.Crop
            )
        } else null
        // Coil 2.x: painter.state is a Compose-observable value (not a StateFlow).
        val success = painter?.state as? AsyncImagePainter.State.Success

        LaunchedEffect(success) {
            if (success != null && BannerFrameCache.frame(cacheKey) == null) {
                runCatching { success.result.drawable.toBitmap() }
                    .getOrNull()
                    ?.let {
                        BannerFrameCache.putFrame(cacheKey, it)
                        cached = it.asImageBitmap()
                    }
            }
        }

        // When the live painter leaves (transition started, tab left) keep the frame it was
        // showing, so the banner freezes where it was instead of jumping back to frame 0.
        DisposableEffect(success) {
            onDispose {
                val drawable = success?.result?.drawable
                if (drawable is Animatable) {
                    runCatching { drawable.toBitmap() }.getOrNull()?.let {
                        BannerFrameCache.putFrame(cacheKey, it, replace = true)
                        cached = it.asImageBitmap()
                    }
                }
            }
        }

        // The cached frame stays until the live painter has an image, then the two swap in the
        // same composition (never both drawn, so no double alpha and no blank frame).
        val frame = cached
        if (frame != null && success == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .paint(
                        BitmapPainter(frame),
                        sizeToIntrinsics = false,
                        alignment = alignment,
                        contentScale = ContentScale.Crop,
                        alpha = alpha
                    )
            )
        }
        if (painter != null && (success != null || frame == null)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .paint(
                        painter,
                        sizeToIntrinsics = false,
                        alignment = alignment,
                        contentScale = ContentScale.Crop,
                        alpha = alpha
                    )
            )
        }
    }
}

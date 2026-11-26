package org.hayya.salah

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    @Test fun adaptiveMasksAndThemedLayerKeepTheGapsAtLauncherSize() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcher = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        val activity = checkNotNull(launcher.resolveActivityInfo(context.packageManager, 0))
        val icon = context.getDrawable(activity.iconResource)?.mutate() as AdaptiveIconDrawable
        val background = context.getColor(R.color.launcher_background)
        val size = 48
        icon.setBounds(0, 0, size, size)
        val layerBounds = Rect(icon.foreground.bounds)
        val silhouette = render(size) { icon.foreground.draw(it) }.solidPixels()
        assertEquals("The arch and the two mat sections stay separate at 48px", 3, components(silhouette, size))
        assertEquals(background, render(size) { icon.draw(it) }.getPixel(size / 2, size / 2))

        val masks = listOf("Circle" to ::circle, "Rounded square" to ::roundedSquare, "Squircle" to ::squircle)
        val previews = mutableListOf<Pair<String, (Int) -> Bitmap>>()
        for ((label, mask) in masks) {
            val clipped = render(size) { canvas ->
                canvas.clipPath(mask(size))
                icon.foreground.draw(canvas)
            }
            assertArrayEquals("$label clips the mark", silhouette, clipped.solidPixels())
            previews += label to { extent: Int ->
                icon.setBounds(0, 0, extent, extent)
                masked(extent, mask(extent), background, icon.foreground)
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            for ((label, tint, surface) in listOf(
                Triple("Themed light", 0xff284831.toInt(), 0xffd4edda.toInt()),
                Triple("Themed dark", 0xffb7d5bd.toInt(), 0xff293d30.toInt())
            )) {
                icon.setBounds(0, 0, size, size)
                val source = checkNotNull(icon.monochrome?.constantState)
                val monochrome = source.newDrawable(context.resources).mutate()
                monochrome.bounds = layerBounds
                monochrome.setTint(tint)
                assertArrayEquals("$label must use the same open mark", silhouette, render(size) { monochrome.draw(it) }.solidPixels())
                previews += label to { extent: Int ->
                    icon.setBounds(0, 0, extent, extent)
                    monochrome.bounds = icon.foreground.bounds
                    masked(extent, circle(extent), surface, monochrome)
                }
            }
        }

        previews += "Emulator mask" to { extent: Int ->
            icon.setBounds(0, 0, extent, extent)
            render(extent) { icon.draw(it) }
        }
        writePreview(previews)
    }

    private fun writePreview(previews: List<Pair<String, (Int) -> Bitmap>>) {
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: return
        val directory = File(output, "launcher-icon").apply { mkdirs() }
        val columnWidth = 176
        val sheet = Bitmap.createBitmap(previews.size * columnWidth, 250, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xff202721.toInt()
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        previews.forEachIndexed { column, (label, draw) ->
            val center = columnWidth * (column + 0.5f)
            canvas.drawText(label, center, 24f, labelPaint)
            canvas.drawBitmap(draw(144), center - 72f, 36f, null)
            canvas.drawBitmap(draw(48), center - 24f, 192f, null)
        }
        File(directory, "option-05-api-${Build.VERSION.SDK_INT}.png").outputStream().use {
            check(sheet.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}

private fun render(size: Int, draw: (Canvas) -> Unit): Bitmap =
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { draw(Canvas(it)) }

private fun masked(size: Int, mask: Path, background: Int, mark: Drawable): Bitmap = render(size) { canvas ->
    canvas.drawPath(mask, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background })
    canvas.clipPath(mask)
    mark.draw(canvas)
}

private fun circle(size: Int) = Path().apply {
    val half = size / 2f
    addCircle(half, half, half, Path.Direction.CW)
}

private fun roundedSquare(size: Int) = Path().apply {
    val radius = size * 0.23f
    addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, Path.Direction.CW)
}

private fun squircle(size: Int) = Path().apply {
    val radius = size / 2f
    for (step in 0 until 96) {
        val angle = 2 * PI * step / 96
        val x = cos(angle)
        val y = sin(angle)
        val dx = Math.copySign(sqrt(abs(x)), x)
        val dy = Math.copySign(sqrt(abs(y)), y)
        val px = (radius * (1 + dx)).toFloat()
        val py = (radius * (1 + dy)).toFloat()
        if (step == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

private fun Bitmap.solidPixels(): BooleanArray {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return BooleanArray(pixels.size) { Color.alpha(pixels[it]) > 127 }
}

private fun components(pixels: BooleanArray, width: Int): Int {
    val remaining = pixels.copyOf()
    val queue = ArrayDeque<Int>()
    var count = 0
    fun visit(index: Int) {
        if (!remaining[index]) return
        remaining[index] = false
        queue.addLast(index)
    }
    for (origin in remaining.indices) {
        if (!remaining[origin]) continue
        count++
        visit(origin)
        while (queue.isNotEmpty()) {
            val pixel = queue.removeFirst()
            val column = pixel % width
            if (column > 0) visit(pixel - 1)
            if (column < width - 1) visit(pixel + 1)
            if (pixel >= width) visit(pixel - width)
            if (pixel + width < remaining.size) visit(pixel + width)
        }
    }
    return count
}

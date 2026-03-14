package app.otakureader.feature.reader.components

import android.graphics.Bitmap
import app.otakureader.feature.reader.model.CropConfig
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Coil [Transformation] that removes white and/or black borders from manga page images.
 *
 * The algorithm scans rows and columns from the edges inward. A row or column is
 * considered a border if the fraction of "border-colored" pixels meets the [CropConfig.threshold].
 * A pixel is "white-border" when all RGB channels are >= 240, and "black-border" when all are <= 15.
 *
 * The final crop is clamped between [CropConfig.minCropPercent] and [CropConfig.maxCropPercent]
 * of the respective dimension, ensuring meaningful content is never removed.
 */
class CropBorderTransformation(private val config: CropConfig = CropConfig()) : Transformation() {

    override val cacheKey: String =
        "CropBorderTransformation-${config.threshold}-${config.minCropPercent}" +
            "-${config.maxCropPercent}-${config.detectWhiteBorders}-${config.detectBlackBorders}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (!config.enabled) return input

        val width = input.width
        val height = input.height
        if (width == 0 || height == 0) return input

        // If neither white nor black border detection is enabled, there is nothing to do.
        if (!config.detectWhiteBorders && !config.detectBlackBorders) return input

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val top = findTopCrop(pixels, width, height)
        val bottom = findBottomCrop(pixels, width, height)
        val left = findLeftCrop(pixels, width, height)
        val right = findRightCrop(pixels, width, height)

        // Clamp total crop amounts per dimension to configured limits
        val maxHeightCrop = (height * config.maxCropPercent).toInt()
        val maxWidthCrop = (width * config.maxCropPercent).toInt()

        val rawVertical = top + bottom
        val rawHorizontal = left + right

        val verticalScale = when {
            rawVertical <= 0 -> 0f
            rawVertical > maxHeightCrop -> maxHeightCrop.toFloat() / rawVertical.toFloat()
            else -> 1f
        }
        val horizontalScale = when {
            rawHorizontal <= 0 -> 0f
            rawHorizontal > maxWidthCrop -> maxWidthCrop.toFloat() / rawHorizontal.toFloat()
            else -> 1f
        }

        val clampedTop = (top * verticalScale).toInt()
        val clampedBottom = (bottom * verticalScale).toInt()
        val clampedLeft = (left * horizontalScale).toInt()
        val clampedRight = (right * horizontalScale).toInt()

        // Skip trivial crops (smaller than minCropPercent of the full dimension)
        val minHeightCrop = (height * config.minCropPercent).toInt()
        val minWidthCrop = (width * config.minCropPercent).toInt()
        val effectiveTop = if (clampedTop + clampedBottom >= minHeightCrop) clampedTop else 0
        val effectiveBottom = if (clampedTop + clampedBottom >= minHeightCrop) clampedBottom else 0
        val effectiveLeft = if (clampedLeft + clampedRight >= minWidthCrop) clampedLeft else 0
        val effectiveRight = if (clampedLeft + clampedRight >= minWidthCrop) clampedRight else 0

        // If nothing to crop, return original
        if (effectiveTop == 0 && effectiveBottom == 0 &&
            effectiveLeft == 0 && effectiveRight == 0
        ) {
            return input
        }

        val newWidth = width - effectiveLeft - effectiveRight
        val newHeight = height - effectiveTop - effectiveBottom

        // Validate crop rectangle is within bitmap bounds and non-empty.
        if (effectiveLeft < 0 || effectiveTop < 0 ||
            effectiveLeft >= width || effectiveTop >= height ||
            newWidth <= 0 || newHeight <= 0
        ) {
            return input
        }

        // Return the same bitmap if nothing actually changed
        if (newWidth == width && newHeight == height) return input

        return Bitmap.createBitmap(input, effectiveLeft, effectiveTop, newWidth, newHeight)
    }

    private fun isBorderPixel(argb: Int): Boolean {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        if (config.detectWhiteBorders && r >= 240 && g >= 240 && b >= 240) return true
        if (config.detectBlackBorders && r <= 15 && g <= 15 && b <= 15) return true
        return false
    }

    /** Returns the number of rows to crop from the top. */
    private fun findTopCrop(pixels: IntArray, width: Int, height: Int): Int {
        var crop = 0
        for (row in 0 until height) {
            if (isRowBorder(pixels, row, width)) crop++ else break
        }
        return crop
    }

    /** Returns the number of rows to crop from the bottom. */
    private fun findBottomCrop(pixels: IntArray, width: Int, height: Int): Int {
        var crop = 0
        for (row in height - 1 downTo 0) {
            if (isRowBorder(pixels, row, width)) crop++ else break
        }
        return crop
    }

    /** Returns the number of columns to crop from the left. */
    private fun findLeftCrop(pixels: IntArray, width: Int, height: Int): Int {
        var crop = 0
        for (col in 0 until width) {
            if (isColBorder(pixels, col, width, height)) crop++ else break
        }
        return crop
    }

    /** Returns the number of columns to crop from the right. */
    private fun findRightCrop(pixels: IntArray, width: Int, height: Int): Int {
        var crop = 0
        for (col in width - 1 downTo 0) {
            if (isColBorder(pixels, col, width, height)) crop++ else break
        }
        return crop
    }

    private fun isRowBorder(pixels: IntArray, row: Int, width: Int): Boolean {
        var borderCount = 0
        val start = row * width
        for (col in 0 until width) {
            if (isBorderPixel(pixels[start + col])) borderCount++
        }
        return (borderCount.toFloat() / width) >= config.threshold
    }

    private fun isColBorder(pixels: IntArray, col: Int, width: Int, height: Int): Boolean {
        var borderCount = 0
        for (row in 0 until height) {
            if (isBorderPixel(pixels[row * width + col])) borderCount++
        }
        return (borderCount.toFloat() / height) >= config.threshold
    }
}

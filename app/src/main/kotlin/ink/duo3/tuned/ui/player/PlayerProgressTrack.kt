package ink.duo3.tuned.ui.player

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import ink.duo3.tuned.domain.model.Chapter

internal data class ProgressTrackInput(
    val positionMs: Long,
    val durationMs: Long,
    val chapters: List<Chapter>,
)

internal data class ProgressTrackColors(
    val inactive: Color,
    val played: Color,
    val activeChapterDivider: Color,
    val remainingChapterDivider: Color,
    val activeStopIndicator: Color,
    val remainingStopIndicator: Color,
)

internal data class ProgressTrackMetrics(
    val height: Dp,
    val cornerRadius: Dp,
    val progressGap: Dp,
    val chapterDividerWidth: Dp,
    val chapterDividerInset: Dp,
    val stopIndicatorSize: Dp,
)

internal fun DrawScope.drawSegmentedProgressTrack(
    input: ProgressTrackInput,
    colors: ProgressTrackColors,
    metrics: ProgressTrackMetrics,
) {
    val duration = input.durationMs.takeIf { it > 0L } ?: 1L
    val trackHeightPx = metrics.height.toPx()
    val centerY = size.height / 2f
    val style =
        TrackLayerStyle(
            trackHeightPx = trackHeightPx,
            centerY = centerY,
            radiusPx = metrics.cornerRadius.toPx(),
        )
    val gapPx = metrics.progressGap.toPx()
    val chapterDividers = chapterDividerCenters(input.chapters, duration, size.width, style.radiusPx, gapPx)
    val stopCenter = size.width - style.radiusPx
    val markers = TrackMarkers(chapterCenters = chapterDividers, stopCenters = listOf(stopCenter))
    val split =
        progressSplit(
            seamCenterPx = input.positionMs.toSeamX(duration, size.width, style.radiusPx, gapPx),
            gapPx = gapPx,
            widthPx = size.width,
            radiusPx = style.radiusPx,
        )
    val remainingSegment =
        split
            .remainingStartPx
            .takeIf { it < size.width - style.radiusPx }
            ?.let { start -> trackSegment(startPx = start, endPx = size.width, color = colors.inactive) }
    remainingSegment?.let { segment ->
        drawTrackSegment(segment = segment, style = style)
        drawMarkersInTrackSegment(
            segment = segment,
            markers = markers,
            markerColors =
                TrackMarkerColors(
                    chapterDivider = colors.remainingChapterDivider,
                    stopIndicator = colors.remainingStopIndicator,
                ),
            metrics = metrics,
            style = style,
        )
    }

    trackSegment(startPx = 0f, endPx = split.playedEndPx, color = colors.played)?.let { segment ->
        drawTrackSegment(segment = segment, style = style)
        drawMarkersInTrackSegment(
            segment = segment,
            markers = markers,
            markerColors =
                TrackMarkerColors(
                    chapterDivider = colors.activeChapterDivider,
                    stopIndicator = colors.activeStopIndicator,
                ),
            metrics = metrics,
            style = style,
        )
    }
}

private fun DrawScope.trackSegment(
    startPx: Float,
    endPx: Float,
    color: Color,
): VisibleTrackSegment? {
    val start = startPx.coerceIn(0f, size.width)
    val end = endPx.coerceIn(0f, size.width)
    return VisibleTrackSegment(
        color = color,
        startPx = start,
        endPx = end,
    ).takeIf { it.endPx > it.startPx }
}

private fun DrawScope.drawMarkersInTrackSegment(
    segment: VisibleTrackSegment,
    markers: TrackMarkers,
    markerColors: TrackMarkerColors,
    metrics: ProgressTrackMetrics,
    style: TrackLayerStyle,
) {
    clipPath(trackSegmentPath(segment, style)) {
        drawChapterDividers(
            centers = markers.chapterCenters,
            color = markerColors.chapterDivider,
            metrics = metrics,
            style = style,
        )
        drawStopIndicators(
            centers = markers.stopCenters,
            color = markerColors.stopIndicator,
            sizePx = metrics.stopIndicatorSize.toPx(),
            style = style,
        )
    }
}

private fun DrawScope.drawTrackSegment(
    segment: VisibleTrackSegment,
    style: TrackLayerStyle,
) {
    drawPath(path = trackSegmentPath(segment, style), color = segment.color)
}

private fun DrawScope.trackSegmentPath(
    segment: VisibleTrackSegment,
    style: TrackLayerStyle,
): Path {
    val top = style.centerY - style.trackHeightPx / 2f
    val width = segment.endPx - segment.startPx
    val maxRadius = minOf(style.trackHeightPx / 2f, width / 2f)
    val radius = style.radiusPx.coerceAtMost(maxRadius)
    return Path().apply {
        addRoundRect(
            RoundRect(
                left = segment.startPx,
                top = top,
                right = segment.endPx,
                bottom = top + style.trackHeightPx,
                topLeftCornerRadius = CornerRadius(radius, radius),
                topRightCornerRadius = CornerRadius(radius, radius),
                bottomRightCornerRadius = CornerRadius(radius, radius),
                bottomLeftCornerRadius = CornerRadius(radius, radius),
            ),
        )
    }
}

private fun DrawScope.drawChapterDividers(
    centers: List<Float>,
    color: Color,
    metrics: ProgressTrackMetrics,
    style: TrackLayerStyle,
) {
    val width = metrics.chapterDividerWidth.toPx()
    val inset = metrics.chapterDividerInset.toPx()
    val top = style.centerY - style.trackHeightPx / 2f + inset
    val bottom = style.centerY + style.trackHeightPx / 2f - inset
    val height = bottom - top
    if (height <= 0f || width <= 0f) return

    val radius = minOf(width, height) / 2f
    centers.forEach { centerX ->
        val left = (centerX - width / 2f).coerceIn(0f, size.width - width)
        val path =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + width,
                        bottom = bottom,
                        cornerRadius = CornerRadius(radius, radius),
                    ),
                )
            }
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawStopIndicators(
    centers: List<Float>,
    color: Color,
    sizePx: Float,
    style: TrackLayerStyle,
) {
    val radius = sizePx / 2f
    centers.forEach { centerX ->
        val safeCenterX = centerX.coerceIn(radius, size.width - radius)
        drawCircle(color = color, radius = radius, center = Offset(safeCenterX, style.centerY))
    }
}

private fun chapterDividerCenters(
    chapters: List<Chapter>,
    durationMs: Long,
    widthPx: Float,
    radiusPx: Float,
    gapPx: Float,
): List<Float> =
    chapters
        .filter { chapter -> chapter.startTimeMs > 0L && chapter.startTimeMs < durationMs }
        .map { chapter -> chapter.startTimeMs.toSeamX(durationMs, widthPx, radiusPx, gapPx) }
        .distinct()
        .sorted()

private fun Long.toSeamX(
    durationMs: Long,
    widthPx: Float,
    radiusPx: Float,
    gapPx: Float,
): Float {
    val fraction = coerceIn(0L, durationMs).toFloat() / durationMs.coerceAtLeast(1L)
    val start = radiusPx * 2f + gapPx / 2f
    val end = widthPx + gapPx / 2f
    return start + (end - start) * fraction
}

private fun progressSplit(
    seamCenterPx: Float,
    gapPx: Float,
    widthPx: Float,
    radiusPx: Float,
): ProgressSplit {
    val seamCenter = seamCenterPx.coerceIn(0f, widthPx)
    val activeDotEnd = radiusPx * 2f
    val activeEnd = (seamCenterPx - gapPx / 2f).coerceIn(activeDotEnd, widthPx)
    val remainingStart = (seamCenter + gapPx / 2f).coerceAtMost(widthPx)
    return ProgressSplit(playedEndPx = activeEnd, remainingStartPx = remainingStart)
}

private data class TrackLayerStyle(
    val trackHeightPx: Float,
    val centerY: Float,
    val radiusPx: Float,
)

private data class VisibleTrackSegment(
    val color: Color,
    val startPx: Float,
    val endPx: Float,
)

private data class ProgressSplit(
    val playedEndPx: Float,
    val remainingStartPx: Float,
)

private data class TrackMarkers(
    val chapterCenters: List<Float>,
    val stopCenters: List<Float>,
)

private data class TrackMarkerColors(
    val chapterDivider: Color,
    val stopIndicator: Color,
)

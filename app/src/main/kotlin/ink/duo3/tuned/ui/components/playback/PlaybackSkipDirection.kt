package ink.duo3.tuned.ui.components.playback

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class PlaybackSkipDirection {
    Back,
    Forward,
}

enum class PlaybackSkipSeconds {
    Five,
    Ten,
    Fifteen,
    Twenty,
    TwentyFive,
    Thirty,
}

@Composable
fun AnimatedSkipIcon(
    direction: PlaybackSkipDirection,
    seconds: PlaybackSkipSeconds,
    animationKey: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val rotation by
        animateFloatAsState(
            targetValue = animationKey * direction.rotationDegrees,
            animationSpec = tween(durationMillis = ROTATION_DURATION_MILLIS, easing = FastOutSlowInEasing),
            label = "Skip arrow rotation",
        )
    Box(
        modifier =
            modifier
                .size(24.dp)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            imageVector = direction.arrow,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = SKIP_ICON_TRANSFORM_ORIGIN
                        rotationZ = rotation
                    },
        )
        Icon(
            imageVector = seconds.label,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val PlaybackSkipDirection.rotationDegrees: Float
    get() =
        when (this) {
            PlaybackSkipDirection.Back -> -FULL_ROTATION_DEGREES
            PlaybackSkipDirection.Forward -> FULL_ROTATION_DEGREES
        }

private val PlaybackSkipDirection.arrow: ImageVector
    get() =
        when (this) {
            PlaybackSkipDirection.Back -> SkipBackArrow
            PlaybackSkipDirection.Forward -> SkipForwardArrow
        }

private val PlaybackSkipSeconds.label: ImageVector
    get() =
        when (this) {
            PlaybackSkipSeconds.Five -> SkipLabel5
            PlaybackSkipSeconds.Ten -> SkipLabel10
            PlaybackSkipSeconds.Fifteen -> SkipLabel15
            PlaybackSkipSeconds.Twenty -> SkipLabel20
            PlaybackSkipSeconds.TwentyFive -> SkipLabel25
            PlaybackSkipSeconds.Thirty -> SkipLabel30
        }

private val SkipBackArrow by lazy { skipVector("SkipBackArrow", SKIP_BACK_ARROW_PATH) }
private val SkipForwardArrow by lazy { skipVector("SkipForwardArrow", SKIP_FORWARD_ARROW_PATH) }
private val SkipLabel5 by lazy { skipVector("SkipLabel5", SKIP_LABEL_5_PATH) }
private val SkipLabel10 by lazy { skipVector("SkipLabel10", SKIP_LABEL_10_PATH) }
private val SkipLabel15 by lazy { skipVector("SkipLabel15", SKIP_LABEL_15_PATH) }
private val SkipLabel20 by lazy { skipVector("SkipLabel20", SKIP_LABEL_20_PATH, viewportSize = 24f) }
private val SkipLabel25 by lazy { skipVector("SkipLabel25", SKIP_LABEL_25_PATH, viewportSize = 24f) }
private val SkipLabel30 by lazy { skipVector("SkipLabel30", SKIP_LABEL_30_PATH) }

private fun skipVector(
    name: String,
    pathData: String,
    viewportSize: Float = 960f,
): ImageVector {
    val builder =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewportSize,
            viewportHeight = viewportSize,
        )
    builder.addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    )
    return builder.build()
}

private val SKIP_BACK_ARROW_PATH =
    """
    M478.75,40 L318.75,200 L478.75,360 L534.77,302.03 L472.73,240 L478.75,240
    C556.75,240 622.93,267.15 677.27,321.48 C731.6,375.82 758.75,442 758.75,520
    C758.75,598 731.6,664.18 677.27,718.52 C622.93,772.85 556.75,800 478.75,800
    C400.75,800 334.57,772.85 280.23,718.52 C225.9,664.18 198.75,598 198.75,520
    L118.75,520 C118.75,570 128.27,616.8 147.27,660.47 C166.27,704.14 191.89,742.2 224.22,774.53
    C256.55,806.86 294.61,832.48 338.28,851.48 C381.95,870.48 428.75,880 478.75,880
    C528.75,880 575.55,870.48 619.22,851.48 C662.89,832.48 700.95,806.86 733.28,774.53
    C765.61,742.2 791.23,704.14 810.23,660.47 C829.23,616.8 838.75,570 838.75,520
    C838.75,470 829.23,423.2 810.23,379.53 C791.23,335.86 765.61,297.8 733.28,265.47
    C700.95,233.14 662.89,207.52 619.22,188.52 C575.55,169.52 528.75,160 478.75,160
    L472.73,160 L534.77,97.97 L478.75,40 Z
    """.trimIndent()

private val SKIP_FORWARD_ARROW_PATH =
    """
    M339.5,851.5 Q274,823 225.5,774.5 T148.5,660.5 Q120,595 120,520 T148.5,379.5
    Q177,314 225.5,265.5 T339.5,188.5 Q405,160 480,160 H486 L424,98 L480,40 L640,200
    L480,360 L424,302 L486,240 H480 Q363,240 281.5,321.5 T200,520 Q200,637 281.5,718.5
    T480,800 Q597,800 678.5,718.5 T760,520 H840 Q840,595 811.5,660.5 T734.5,774.5
    Q686,823 620.5,851.5 T480,880 Q405,880 339.5,851.5 Z
    """.trimIndent()

private val SKIP_LABEL_5_PATH =
    """
    M380,640 V580 H500 V540 L380,540 V400 H560 V460 L440,460 V500 H520
    Q537,500 548.5,511.5 T560,540 V600 Q560,617 548.5,628.5 T520,640 Z
    """.trimIndent()

private val SKIP_LABEL_10_PATH =
    """
    M360,640 V460 H300 V400 H420 V640 Z
    M500,640 Q483,640 471.5,628.5 T460,600 V440 Q460,423 471.5,411.5 T500,400 H580
    Q597,400 608.5,411.5 T620,440 V600 Q620,617 608.5,628.5 T580,640 Z M520,580 H560 V460 H520 Z
    """.trimIndent()

private val SKIP_LABEL_15_PATH =
    """
    M297.5,399.84 L297.5,459.84 L357.5,459.84 L357.5,639.84 L417.5,639.84 L417.5,399.84 Z
    M459.38,399.84 L459.38,539.84 L579.38,539.84 L579.38,579.84 L459.38,579.84 L459.38,639.84
    L599.38,639.84 C610.71,639.84 620.22,636.03 627.89,628.36 C635.56,620.69 639.38,611.18 639.38,599.84
    L639.38,539.84 C639.38,528.51 635.56,519.07 627.89,511.41 C620.22,503.74 610.71,499.84 599.38,499.84
    L519.38,499.84 L519.38,459.84 L639.38,459.84 L639.38,399.84 Z
    """.trimIndent()

private val SKIP_LABEL_20_PATH =
    """
    M7.5,16 V13.5 C7.5,13.22 7.6,12.98 7.79,12.79 S8.22,12.5 8.5,12.5 H10 V11.5 H7.5 V10 H10.5
    C10.78,10 11.02,10.1 11.21,10.29 S11.5,10.72 11.5,11 V12.5 C11.5,12.78 11.4,13.02 11.21,13.21
    S10.78,13.5 10.5,13.5 H9 V14.5 H11.5 V16 Z
    M13.5,16 C13.22,16 12.98,15.9 12.79,15.71 S12.5,15.28 12.5,15 V11 C12.5,10.72 12.6,10.48 12.79,10.29
    S13.22,10 13.5,10 H15.5 C15.78,10 16.02,10.1 16.21,10.29 S16.5,10.72 16.5,11 V15
    C16.5,15.28 16.4,15.52 16.21,15.71 S15.78,16 15.5,16 Z M14,14.5 H15 V11.5 H14 Z
    """.trimIndent()

private val SKIP_LABEL_25_PATH =
    """
    M7.5,16 V13.5 C7.5,13.22 7.6,12.98 7.79,12.79 S8.22,12.5 8.5,12.5 H10 V11.5 H7.5 V10 H10.5
    C10.78,10 11.02,10.1 11.21,10.29 S11.5,10.72 11.5,11 V12.5 C11.5,12.78 11.4,13.02 11.21,13.21
    S10.78,13.5 10.5,13.5 H9 V14.5 H11.5 V16 Z
    M12.5,16 V14.5 H15 V13.5 H12.5 V10 H16.5 V11.5 H14 V12.5 H15.5
    C15.78,12.5 16.02,12.6 16.21,12.79 S16.5,13.22 16.5,13.5 V15 C16.5,15.28 16.4,15.52 16.21,15.71
    S15.78,16 15.5,16 Z
    """.trimIndent()

private val SKIP_LABEL_30_PATH =
    """
    M300,640 V580 H400 V540 H340 V500 H400 V460 L300,460 V400 H420 Q437,400 448.5,411.5 T460,440
    V600 Q460,617 448.5,628.5 T420,640 Z
    M540,640 Q523,640 511.5,628.5 T500,600 V440 Q500,423 511.5,411.5 T540,400 H620
    Q637,400 648.5,411.5 T660,440 V600 Q660,617 648.5,628.5 T620,640 Z M560,580 H600 V460 H560 Z
    """.trimIndent()

private const val FULL_ROTATION_DEGREES = 360f
private const val ROTATION_DURATION_MILLIS = 500
private val SKIP_ICON_TRANSFORM_ORIGIN = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 13f / 24f)

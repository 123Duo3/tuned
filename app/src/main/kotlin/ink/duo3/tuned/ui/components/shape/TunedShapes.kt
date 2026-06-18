package ink.duo3.tuned.ui.components.shape

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.Continuity
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.capsule.continuities.G1Continuity

@Stable
val tunedCapsuleShape: ContinuousRoundedRectangle = ContinuousCapsule

@Stable
fun tunedRoundedCornerShape(size: Dp): ContinuousRoundedRectangle = ContinuousRoundedRectangle(size)

@Stable
fun tunedRoundedCornerShape(percent: Int): ContinuousRoundedRectangle = ContinuousRoundedRectangle(percent)

@Stable
fun tunedRoundedCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        continuity = continuity,
    )

@Stable
fun tunedAnimatedRoundedCornerShape(size: Dp): ContinuousRoundedRectangle = g1(size)

private fun g1(size: Dp): ContinuousRoundedRectangle = ContinuousRoundedRectangle(size, continuity = G1Continuity)

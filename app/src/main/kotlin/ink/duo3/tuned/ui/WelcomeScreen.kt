package ink.duo3.tuned.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.ui.theme.cabinFamily

@Composable
fun WelcomeScreen(navigationNext: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
            WelcomeScreenExtremeCompact(navigationNext)
        } else if (screenWidth < 600.dp) {
            WelcomeScreenCompact(screenWidth, navigationNext)
        } else if (screenWidth < 840.dp) {
            WelcomeScreenMedium(navigationNext)
        } else {
            WelcomeScreenExpanded(navigationNext)
        }
    }
}

@Composable
fun WelcomeScreenExtremeCompact(navigationNext: () -> Unit) {
    val waveWidth = 256.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        TextLogo(modifier = Modifier.align(Alignment.TopStart))

        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            TunedWave(waveWidth)

            Text(
                text = stringResource(id = R.string.slogan_single_line),
                fontFamily = cabinFamily,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        NextButton(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(), false,
            navigationNext
        )
    }
}

@Composable
fun WelcomeScreenCompact(screenWidth: Dp, navigationNext: () -> Unit) {
    val waveWidth = if (screenWidth <= 420.dp) {
        screenWidth - 32.dp
    } else {
        420.dp - 32.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        TextLogo(modifier = Modifier.align(Alignment.TopStart))

        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            TunedWave(waveWidth)

            Text(
                text = stringResource(id = R.string.slogan_two_lines),
                fontFamily = cabinFamily,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        NextButton(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            navigationNext = navigationNext
        )
    }
}

@Composable
fun WelcomeScreenMedium(navigationNext: () -> Unit) {
    val waveWidth = 400.dp

    Row(
        modifier = Modifier
            .systemBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
        ) {
            TextLogo(modifier = Modifier.align(Alignment.TopStart))

            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                TunedWave(waveWidth)

                Text(
                    text = stringResource(id = R.string.slogan_two_lines),
                    fontFamily = cabinFamily,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        NextButton(Modifier, navigationNext = navigationNext)
    }
}

@Composable
fun WelcomeScreenExpanded(navigationNext: () -> Unit) {
    val waveWidth = 400.dp

    Row(
        modifier = Modifier
            .systemBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            TextLogo(modifier = Modifier.align(Alignment.TopStart))

            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                TunedWave(waveWidth)

                Text(
                    text = stringResource(id = R.string.slogan_two_lines),
                    fontFamily = cabinFamily,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        NextButton(Modifier, navigationNext = navigationNext)
    }
}

@Composable
fun TextLogo(modifier: Modifier) {
    val locale = Locale.current

    when (locale.toLanguageTag()) {
        "zh-Hans-CN" ->
            Icon(
                painter = painterResource(id = R.drawable.tuned_text_logo_zh_cn),
                contentDescription = stringResource(id = R.string.logo_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

        else ->
            Text(
                text = stringResource(id = R.string.app_logo_text),
                fontFamily = cabinFamily,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
            )
    }
}

@Composable
fun TunedWave(waveWidth: Dp, waveHeight: Dp = waveWidth * 0.24f) {
    Icon(
        modifier = Modifier.size(waveWidth, waveHeight),
        painter = painterResource(id = R.drawable.tuned_wave),
        tint = MaterialTheme.colorScheme.secondary,
        contentDescription = null
    )
}

@Composable
fun NextButton(modifier: Modifier, enableExtraPadding: Boolean = true, navigationNext: () -> Unit) {
    Button(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        onClick = navigationNext
    ) {
        Text(
            modifier = Modifier.padding(extraButtonTextPadding(enableExtraPadding, true)),
            text = stringResource(id = R.string.welcome_screen_next_button),
            style = MaterialTheme.typography.titleMedium
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null
        )
    }
}

fun extraButtonTextPadding(enable: Boolean, iconFollow: Boolean = false): PaddingValues {
    val textPaddingValues = if (enable) {
        PaddingValues(8.dp, 7.dp, 8.dp, 9.dp)
    } else if (iconFollow) {
        PaddingValues(bottom = 1.dp, end = 4.dp)
    } else {
        PaddingValues(bottom = 1.dp)
    }
    return textPaddingValues
}

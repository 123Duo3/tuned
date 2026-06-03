package ink.duo3.tuned.ui.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.ThemeSettings
import ink.duo3.tuned.presentation.settings.SettingsUiState
import ink.duo3.tuned.presentation.settings.SettingsViewModel
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.TunedLargeTopBarScaffold
import ink.duo3.tuned.ui.components.settings.SettingItem
import ink.duo3.tuned.ui.components.settings.SettingItemGroup
import ink.duo3.tuned.ui.components.settings.SettingItemWithSwitch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        actions =
            SettingsActions(
                onFollowSystemAppearanceChange = viewModel::setFollowSystemAppearance,
                onUseDarkModeChange = viewModel::setUseDarkMode,
                onUseMonetChange = viewModel::setUseMonet,
                onMonetSeedChange = viewModel::setMonetSeed,
            ),
        modifier = modifier,
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val canScroll by remember {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }

    TunedLargeTopBarScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
        modifier = modifier,
        enableTopBarScroll = canScroll,
    ) { hazeModifier, contentPadding ->
        LazyColumn(
            state = listState,
            userScrollEnabled = canScroll,
            modifier =
                hazeModifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentPadding =
                PaddingValues(
                    top = 16.dp,
                    bottom = LocalMiniPlayerBottomClearance.current + 16.dp,
                ),
        ) {
            val themeSettings = state.themeSettings
            if (themeSettings != null) {
                item {
                    AppearanceSettingsGroup(
                        themeSettings = themeSettings,
                        onFollowSystemAppearanceChange = actions.onFollowSystemAppearanceChange,
                        onUseDarkModeChange = actions.onUseDarkModeChange,
                        onUseMonetChange = actions.onUseMonetChange,
                        onMonetSeedChange = actions.onMonetSeedChange,
                    )
                }
            }
        }
    }
}

private data class SettingsActions(
    val onFollowSystemAppearanceChange: (Boolean) -> Unit,
    val onUseDarkModeChange: (Boolean) -> Unit,
    val onUseMonetChange: (Boolean) -> Unit,
    val onMonetSeedChange: (Int) -> Unit,
)

@Composable
private fun AppearanceSettingsGroup(
    themeSettings: ThemeSettings,
    onFollowSystemAppearanceChange: (Boolean) -> Unit,
    onUseDarkModeChange: (Boolean) -> Unit,
    onUseMonetChange: (Boolean) -> Unit,
    onMonetSeedChange: (Int) -> Unit,
) {
    SettingItemGroup(title = stringResource(R.string.settings_appearance)) {
        SettingItemWithSwitch(
            title = { Text(stringResource(R.string.settings_follow_system_appearance)) },
            description = { Text(stringResource(R.string.settings_follow_system_appearance_description)) },
            icon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) },
            checked = themeSettings.followSystemAppearance,
            onCheckedChange = onFollowSystemAppearanceChange,
        )
        AnimatedVisibility(visible = !themeSettings.followSystemAppearance) {
            SettingItemWithSwitch(
                title = { Text(stringResource(R.string.settings_dark_mode)) },
                icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                checked = themeSettings.useDarkMode,
                onCheckedChange = onUseDarkModeChange,
            )
        }
        SettingItemWithSwitch(
            title = { Text(stringResource(R.string.settings_use_dynamic_color)) },
            description = { Text(stringResource(R.string.settings_use_dynamic_color_description)) },
            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
            checked = themeSettings.useMonet,
            onCheckedChange = onUseMonetChange,
        )
        AnimatedVisibility(visible = themeSettings.useMonet) {
            SettingItem(
                title = { Text(stringResource(R.string.settings_theme_color)) },
                description = {
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            stringResource(R.string.settings_theme_color_description_modern)
                        } else {
                            stringResource(R.string.settings_theme_color_description_legacy)
                        },
                    )
                },
                bottomAction = {
                    ThemeColorSelector(
                        selectedSeed = themeSettings.monetSeed,
                        onSeedChange = onMonetSeedChange,
                    )
                },
            )
        }
    }
}

@Composable
private fun ThemeColorSelector(
    selectedSeed: Int,
    onSeedChange: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                SystemColorSeed(
                    selected = selectedSeed == ThemeSettings.MONET_SEED_SYSTEM,
                    onClick = { onSeedChange(ThemeSettings.MONET_SEED_SYSTEM) },
                )
            }
        }
        items(ThemeSeedColors) { seed ->
            ThemeSeedSwatch(
                seed = seed,
                selected = selectedSeed == seed,
                onClick = { onSeedChange(seed) },
            )
        }
    }
}

@Composable
private fun SystemColorSeed(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = tween(200),
        label = "systemColorBorderWidth",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "systemColorBorderAlpha",
    )
    Box(
        modifier =
            Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                    shape = CircleShape,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = stringResource(R.string.settings_theme_color_system),
            tint =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ThemeSeedSwatch(
    seed: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val innerSize by animateDpAsState(
        targetValue = if (selected) 24.dp else 0.dp,
        animationSpec = tween(200),
        label = "themeSeedInnerSize",
    )
    val innerAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "themeSeedInnerAlpha",
    )
    Box(
        modifier =
            Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(seed))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(innerSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = innerAlpha)),
        )
    }
}

private val ThemeSeedColors =
    listOf(
        0xFFF44336.toInt(),
        0xFFE91E63.toInt(),
        0xFF9C27B0.toInt(),
        0xFF673AB7.toInt(),
        0xFF3F51B5.toInt(),
        0xFF2196F3.toInt(),
        0xFF03A9F4.toInt(),
        0xFF00BCD4.toInt(),
        0xFF009688.toInt(),
        0xFF4CAF50.toInt(),
        0xFF8BC34A.toInt(),
        0xFFCDDC39.toInt(),
        0xFFFFEB3B.toInt(),
        0xFFFFC107.toInt(),
        0xFFFF9800.toInt(),
        0xFFFF5722.toInt(),
    )

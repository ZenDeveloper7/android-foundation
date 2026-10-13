package com.zen.foundation.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zen.foundation.core.designsystem.component.FoundationLoadingIndicator
import com.zen.foundation.core.designsystem.component.PreviewFoundation
import com.zen.foundation.core.designsystem.component.foundationContentWidth
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.designsystem.theme.supportsDynamicColor
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings

@Composable
internal fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        showDynamicColor = supportsDynamicColor,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setUseDynamicColor,
        onWriteErrorConsume = viewModel::consumeWriteError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    showDynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWriteErrorConsume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(R.string.feature_settings_error_save)
    val writeFailed = (uiState as? SettingsUiState.Success)?.writeFailed == true
    val currentOnWriteErrorConsume by rememberUpdatedState(onWriteErrorConsume)
    LaunchedEffect(writeFailed) {
        if (writeFailed) {
            snackbarHostState.showSnackbar(saveErrorMessage)
            currentOnWriteErrorConsume()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> FoundationLoadingIndicator(Modifier.padding(innerPadding))

            is SettingsUiState.Success ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .foundationContentWidth()
                ) {
                    SectionHeader(stringResource(R.string.feature_settings_theme))
                    ThemeModeOptions(
                        selected = uiState.settings.themeMode,
                        onSelect = onThemeModeChange
                    )
                    if (showDynamicColor) {
                        SectionHeader(stringResource(R.string.feature_settings_appearance))
                        DynamicColorRow(
                            checked = uiState.settings.useDynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }
                }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .padding(
                    horizontal = FoundationTheme.spacing.medium,
                    vertical = FoundationTheme.spacing.small
                )
                .semantics { heading() }
    )
}

/** Each row is one radio target (whole row clickable, announced as a radio button). */
@Composable
private fun ThemeModeOptions(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            ListItem(
                headlineContent = { Text(stringResource(mode.labelRes())) },
                leadingContent = { RadioButton(selected = mode == selected, onClick = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = mode == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(mode) }
                        )
            )
        }
    }
}

@Composable
private fun DynamicColorRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_dynamic_color)) },
        supportingContent = {
            Text(stringResource(R.string.feature_settings_dynamic_color_summary))
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    )
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.feature_settings_theme_system
    ThemeMode.Light -> R.string.feature_settings_theme_light
    ThemeMode.Dark -> R.string.feature_settings_theme_dark
}

@PreviewFoundation
@Composable
private fun SettingsScreenPreview() {
    FoundationTheme(dynamicColor = false) {
        SettingsScreen(
            uiState =
                SettingsUiState.Success(UserSettings(ThemeMode.Dark, useDynamicColor = false)),
            showDynamicColor = true,
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onWriteErrorConsume = {}
        )
    }
}

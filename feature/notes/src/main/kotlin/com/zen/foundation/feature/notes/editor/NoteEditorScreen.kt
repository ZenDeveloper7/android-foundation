package com.zen.foundation.feature.notes.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zen.foundation.core.designsystem.component.FoundationConfirmDialog
import com.zen.foundation.core.designsystem.component.FoundationLoadingIndicator
import com.zen.foundation.core.designsystem.component.FoundationMessageState
import com.zen.foundation.core.designsystem.component.PreviewFoundation
import com.zen.foundation.core.designsystem.component.foundationContentWidth
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.model.NoteLimits
import com.zen.foundation.feature.notes.R

@Composable
internal fun NoteEditorRoute(
    noteId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditorViewModel =
        hiltViewModel<NoteEditorViewModel, NoteEditorViewModel.Factory>(
            creationCallback = { factory -> factory.create(noteId) }
        )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) currentOnBack()
    }

    val hasUnsavedChanges = viewModel.hasUnsavedChanges() && !uiState.isFinished
    // Intercept Back only when there is something to lose, so predictive back works otherwise.
    BackHandler(enabled = hasUnsavedChanges) { showDiscardDialog = true }

    NoteEditorScreen(
        uiState = uiState,
        title = viewModel.title,
        body = viewModel.body,
        canSave = viewModel.canSave(),
        onSave = viewModel::save,
        onBack = { if (hasUnsavedChanges) showDiscardDialog = true else onBack() },
        onDelete = { showDeleteDialog = true },
        onRetry = viewModel::retryLoad,
        onErrorConsume = viewModel::consumeError,
        modifier = modifier
    )

    if (showDiscardDialog) {
        FoundationConfirmDialog(
            title = stringResource(R.string.feature_notes_discard_title),
            text = stringResource(R.string.feature_notes_discard_text),
            confirmLabel = stringResource(R.string.feature_notes_discard),
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false },
            destructive = true
        )
    }
    if (showDeleteDialog) {
        FoundationConfirmDialog(
            title = stringResource(R.string.feature_notes_delete_confirm_title),
            text = stringResource(R.string.feature_notes_delete_confirm_text),
            confirmLabel = stringResource(R.string.feature_notes_delete),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
            destructive = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorScreen(
    uiState: NoteEditorUiState,
    title: TextFieldState,
    body: TextFieldState,
    canSave: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onErrorConsume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage =
        when (uiState.error) {
            NoteEditorError.SaveFailed -> stringResource(R.string.feature_notes_error_save)
            NoteEditorError.DeleteFailed -> stringResource(R.string.feature_notes_error_delete)
            null -> null
        }
    val currentOnErrorShown by rememberUpdatedState(onErrorConsume)
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            currentOnErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleRes =
                        if (uiState.isNewNote) {
                            R.string.feature_notes_new_note
                        } else {
                            R.string.feature_notes_edit_note
                        }
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_notes_back)
                        )
                    }
                },
                actions = {
                    if (!uiState.isNewNote && !uiState.isMissing && !uiState.loadFailed) {
                        IconButton(onClick = onDelete, enabled = !uiState.isBusy) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription =
                                    stringResource(R.string.feature_notes_delete_note)
                            )
                        }
                    }
                    if (!uiState.isMissing && !uiState.loadFailed) {
                        TextButton(onClick = onSave, enabled = canSave) {
                            Text(stringResource(R.string.feature_notes_save))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        when {
            uiState.isLoading -> FoundationLoadingIndicator(contentModifier)

            uiState.loadFailed ->
                FoundationMessageState(
                    title = stringResource(R.string.feature_notes_error_load_title),
                    message = stringResource(R.string.feature_notes_error_load_message),
                    modifier = contentModifier,
                    action = {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.feature_notes_retry))
                        }
                    }
                )

            uiState.isMissing ->
                FoundationMessageState(
                    title = stringResource(R.string.feature_notes_missing_title),
                    message = stringResource(R.string.feature_notes_missing_message),
                    modifier = contentModifier
                )

            else -> EditorFields(title = title, body = body, modifier = contentModifier)
        }
    }
}

@Composable
private fun EditorFields(
    title: TextFieldState,
    body: TextFieldState,
    modifier: Modifier = Modifier
) {
    val spacing = FoundationTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .foundationContentWidth()
                .imePadding()
                .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        OutlinedTextField(
            state = title,
            label = { Text(stringResource(R.string.feature_notes_field_title)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = InputTransformation.maxLength(NoteLimits.MAX_TITLE_LENGTH),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            state = body,
            label = { Text(stringResource(R.string.feature_notes_field_body)) },
            inputTransformation = InputTransformation.maxLength(NoteLimits.MAX_BODY_LENGTH),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@PreviewFoundation
@Composable
private fun NoteEditorScreenPreview() {
    FoundationTheme(dynamicColor = false) {
        NoteEditorScreen(
            uiState = NoteEditorUiState(isNewNote = false),
            title = TextFieldState("Groceries"),
            body = TextFieldState("Milk, eggs, coffee"),
            canSave = true,
            onSave = {},
            onBack = {},
            onDelete = {},
            onRetry = {},
            onErrorConsume = {}
        )
    }
}

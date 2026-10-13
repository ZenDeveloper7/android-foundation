package com.zen.foundation.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zen.foundation.core.designsystem.R
import com.zen.foundation.core.designsystem.theme.FoundationTheme

/**
 * Confirmation for destructive or irreversible actions. Set [destructive] to color the confirm
 * button with the error color.
 */
@Composable
fun FoundationConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color =
                        if (destructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.core_designsystem_cancel))
            }
        }
    )
}

@PreviewFoundation
@Composable
private fun FoundationConfirmDialogPreview() {
    FoundationTheme(dynamicColor = false) {
        FoundationConfirmDialog(
            title = "Delete note?",
            text = "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
            destructive = true
        )
    }
}

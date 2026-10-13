package com.zen.foundation.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.zen.foundation.core.designsystem.R
import com.zen.foundation.core.designsystem.theme.FoundationTheme

/** Full-area progress indicator with a spoken label. */
@Composable
fun FoundationLoadingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.core_designsystem_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription = description
            }
        )
    }
}

/** Centered title, optional message and optional action: use for empty and error states. */
@Composable
fun FoundationMessageState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(FoundationTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(
            FoundationTheme.spacing.small,
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        action?.invoke()
    }
}

@PreviewFoundation
@Composable
private fun FoundationMessageStatePreview() {
    FoundationTheme(dynamicColor = false) {
        Surface {
            FoundationMessageState(
                title = "Something went wrong",
                message = "Check your connection and try again.",
                action = { TextButton(onClick = {}) { Text("Retry") } }
            )
        }
    }
}

@PreviewFoundation
@Composable
private fun FoundationLoadingIndicatorPreview() {
    FoundationTheme(dynamicColor = false) {
        Surface { FoundationLoadingIndicator() }
    }
}

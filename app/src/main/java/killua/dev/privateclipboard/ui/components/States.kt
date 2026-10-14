package killua.dev.privateclipboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.components.loadingindicator.ArsenalLoadingIndicator
import killua.dev.arsenal.material.state.ArsenalEmptyState

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ArsenalLoadingIndicator()
    }
}

@Composable
fun EmptyView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArsenalEmptyState(modifier = Modifier.fillMaxWidth(), icon = icon, title = title, message = message)
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

fun LazyListScope.stateItem(content: @Composable LazyItemScope.(Modifier) -> Unit) {
    item(key = "state") { content(Modifier.fillParentMaxHeight(STATE_HEIGHT_FRACTION)) }
}

private const val STATE_HEIGHT_FRACTION = 0.6f

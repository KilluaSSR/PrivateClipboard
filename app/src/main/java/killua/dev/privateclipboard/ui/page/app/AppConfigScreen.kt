package killua.dev.privateclipboard.ui.page.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.templates.scaffolds.plus
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition
import killua.dev.arsenal.navigation.annotation.NavDestination
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.domain.model.AccessEvent
import killua.dev.privateclipboard.domain.model.UsageCounts
import killua.dev.privateclipboard.shared.ClipShared.Category
import killua.dev.privateclipboard.ui.components.AppDetailScaffold
import killua.dev.privateclipboard.ui.components.AppHeader
import killua.dev.privateclipboard.ui.components.ClipConfigEditor
import killua.dev.privateclipboard.ui.components.ClipSurfaces
import killua.dev.privateclipboard.ui.components.ConfigPill
import killua.dev.privateclipboard.ui.components.GroupHeader
import killua.dev.privateclipboard.ui.components.Pill
import killua.dev.privateclipboard.ui.components.formatDateTime
import killua.dev.privateclipboard.ui.components.labelRes

@NavDestination(route = "app_config/{packageName}")
@Composable
fun AppConfigScreen(backStackEntry: NavBackStackEntry) {
    val viewModel: AppConfigViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppDetailScaffold(
        onBack = { viewModel.dispatch(AppConfigIntent.Back) },
        actions = { OverflowMenu(viewModel::dispatch) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding + PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header") {
                AppHeader(packageName = state.packageName, label = state.label ?: state.packageName) {
                    Pill(
                        text = stringResource(R.string.usage_denied_count, state.usage.denied),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    ConfigPill(state.config)
                }
            }
            item(key = "editor") {
                ClipConfigEditor(
                    config = state.config,
                    onChange = { key, value -> viewModel.dispatch(AppConfigIntent.SetKey(key, value)) },
                )
            }
            item(key = "usage_header") { GroupHeader(title = stringResource(R.string.usage_title)) }
            item(key = "usage_chips") {
                CategoryChips(state.usage, state.category) { viewModel.dispatch(AppConfigIntent.SelectCategory(it)) }
            }
            val events = state.visibleEvents
            when {
                events == null -> Unit
                events.isEmpty() -> item(key = "events_empty") {
                    Text(
                        stringResource(R.string.usage_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    )
                }
                else -> itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                    EventRow(event, groupedItemShape(itemPosition(index, events.size)))
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(usage: UsageCounts, selected: Category, onSelect: (Category) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        listOf(Category.AutoRead, Category.ManualRead, Category.AutoWrite, Category.ManualWrite).forEach { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(stringResource(category.labelRes()))
                        Pill(
                            text = usage.of(category).toString(),
                            container = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            content = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun EventRow(event: AccessEvent, shape: Shape) {
    val context = LocalContext.current
    Surface(shape = shape, color = ClipSurfaces.row, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm + Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = event.content.ifEmpty { stringResource(R.string.usage_content_empty) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (event.content.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    context.formatDateTime(event.occurredAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Pill(
                    text = stringResource(event.outcome.labelRes()),
                    container = if (event.outcome.isDenied) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    content = if (event.outcome.isDenied) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(onIntent: (AppConfigIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                R.string.config_menu_apply_template to AppConfigIntent.PickTemplate,
                R.string.config_menu_reset to AppConfigIntent.Reset,
            ).forEach { (label, intent) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onIntent(intent)
                    },
                )
            }
        }
    }
}

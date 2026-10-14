package killua.dev.privateclipboard.ui.page.templates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import killua.dev.arsenal.core.state.DataState
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.templates.scaffolds.plus
import killua.dev.arsenal.material.templates.settings.SettingsAccent
import killua.dev.arsenal.material.templates.settings.SettingsItem
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition
import killua.dev.arsenal.navigation.annotation.NavDestination
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.ui.components.ClipConfigEditor
import killua.dev.privateclipboard.ui.components.ClipSurfaces
import killua.dev.privateclipboard.ui.components.DetailScaffold
import killua.dev.privateclipboard.ui.components.EmptyView
import killua.dev.privateclipboard.ui.components.LoadingView
import killua.dev.privateclipboard.ui.components.configSummary
import killua.dev.privateclipboard.ui.components.stateItem

@NavDestination(route = "templates")
@Composable
fun TemplatesScreen() {
    val viewModel: TemplatesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selecting = state.selected.isNotEmpty()

    BackHandler(selecting) { viewModel.dispatch(TemplatesIntent.ClearSelection) }

    DetailScaffold(
        title = if (selecting) {
            stringResource(R.string.selected_count, state.selected.size)
        } else {
            stringResource(R.string.template_manage)
        },
        onBack = {
            viewModel.dispatch(if (selecting) TemplatesIntent.ClearSelection else TemplatesIntent.Back)
        },
        actions = {
            if (selecting) {
                IconButton(onClick = { viewModel.dispatch(TemplatesIntent.ClearSelection) }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_deselect))
                }
                IconButton(onClick = { viewModel.dispatch(TemplatesIntent.DeleteSelected) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !selecting, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.dispatch(TemplatesIntent.PromptCreate) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.template_new)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding + PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            when (val templates = state.templates) {
                is DataState.Success -> itemsIndexed(templates.data, key = { _, it -> it.id }) { index, template ->
                    val isSelected = template.id in state.selected
                    val color by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else ClipSurfaces.row,
                        label = "templateRow",
                    )
                    SettingsItem(
                        title = template.name,
                        description = configSummary(template.config),
                        leadingIcon = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Dashboard,
                        leadingAccent = if (isSelected) SettingsAccent.Primary else SettingsAccent.Secondary,
                        shape = groupedItemShape(itemPosition(index, templates.data.size)),
                        containerColor = color,
                        modifier = Modifier.animateItem(),
                        onClick = {
                            viewModel.dispatch(
                                if (selecting) TemplatesIntent.ToggleSelect(template.id) else TemplatesIntent.Open(template.id)
                            )
                        },
                        onLongClick = { viewModel.dispatch(TemplatesIntent.ToggleSelect(template.id)) },
                    )
                }
                is DataState.Empty -> stateItem { modifier ->
                    EmptyView(
                        icon = Icons.Rounded.Dashboard,
                        title = stringResource(R.string.template_none),
                        message = stringResource(R.string.template_none_message),
                        modifier = modifier,
                    )
                }
                else -> stateItem { modifier -> LoadingView(modifier) }
            }
        }
    }
}

@NavDestination(route = "template_detail/{templateId}")
@Composable
fun TemplateDetailScreen(backStackEntry: NavBackStackEntry) {
    val viewModel: TemplateDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val template = state.template

    DetailScaffold(
        title = template?.name.orEmpty(),
        subtitle = template?.let { configSummary(it.config) },
        onBack = { viewModel.dispatch(TemplateDetailIntent.Back) },
        actions = {
            IconButton(onClick = { viewModel.dispatch(TemplateDetailIntent.PromptRename) }) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.template_rename))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding + PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
        ) {
            if (template == null) {
                stateItem { modifier -> LoadingView(modifier) }
            } else {
                item(key = "editor") {
                    ClipConfigEditor(
                        config = template.config,
                        onChange = { key, value -> viewModel.dispatch(TemplateDetailIntent.SetKey(key, value)) },
                    )
                }
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.template_detail_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
            }
        }
    }
}

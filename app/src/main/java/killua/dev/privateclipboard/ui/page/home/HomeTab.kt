package killua.dev.privateclipboard.ui.page.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.components.application.AppRowContent
import killua.dev.arsenal.material.components.application.ArsenalSearchBar
import killua.dev.arsenal.material.components.application.SearchExpansionMode
import killua.dev.arsenal.material.templates.basics.EmptyView
import killua.dev.arsenal.material.templates.basics.LoadingView
import killua.dev.arsenal.material.templates.basics.stateItem
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.domain.model.AppEntry
import killua.dev.privateclipboard.domain.model.AppScope
import killua.dev.privateclipboard.ui.components.ClipSurfaces
import killua.dev.privateclipboard.ui.components.ConfigPill
import killua.dev.privateclipboard.ui.components.TabScaffold

@Composable
fun HomeTab() {
    val viewModel: HomeTabViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selecting = state.selected.isNotEmpty()

    BackHandler(selecting) { viewModel.dispatch(HomeIntent.ClearSelection) }

    TabScaffold(title = stringResource(R.string.app_name)) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Column(Modifier.fillMaxSize()) {
                SearchField(onQueryChange = { viewModel.dispatch(HomeIntent.Search(it)) })
                ScopeChips(state.filter.scope) { viewModel.dispatch(HomeIntent.SetScope(it)) }
                val apps = state.visibleApps
                if (apps == null) {
                    LoadingView()
                } else {
                    AppList(
                        apps = apps,
                        selected = state.selected,
                        onClick = { pkg ->
                            viewModel.dispatch(if (selecting) HomeIntent.ToggleSelect(pkg) else HomeIntent.Open(pkg))
                        },
                        onLongClick = { viewModel.dispatch(HomeIntent.ToggleSelect(it)) },
                    )
                }
            }

            AnimatedVisibility(
                visible = selecting,
                enter = slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) { it } +
                    fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                exit = slideOutVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) { it } +
                    fadeOut(spring(stiffness = Spring.StiffnessMedium)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                SelectionToolbar(
                    selectedCount = state.selected.size,
                    allSelected = state.allSelected,
                    onExit = { viewModel.dispatch(HomeIntent.ClearSelection) },
                    onToggleSelectAll = { viewModel.dispatch(HomeIntent.ToggleSelectAll) },
                    onOpenBatch = { viewModel.dispatch(HomeIntent.OpenBatch) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    allSelected: Boolean,
    onExit: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onOpenBatch: () -> Unit,
) {
    HorizontalFloatingToolbar(
        modifier = Modifier.padding(bottom = Spacing.md),
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.wrapContentWidth().padding(horizontal = Spacing.xs),
        ) {
            FilledIconButton(
                onClick = onExit,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_deselect)) }
            FilledIconButton(onClick = onToggleSelectAll) {
                Icon(
                    if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                    contentDescription = stringResource(R.string.action_select_all),
                )
            }
            Text(
                stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = Spacing.xs),
            )
            FilledTonalButton(
                onClick = onOpenBatch,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) { Text(stringResource(R.string.batch_title)) }
        }
    }
}

@Composable
private fun SearchField(
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState()

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { onQueryChange(it) }
    }

    ArsenalSearchBar(
        textFieldState = textFieldState,
        modifier = modifier,
        expansionMode = SearchExpansionMode.Inline,
        placeholder = stringResource(R.string.apps_search_hint),
        clearContentDescription = stringResource(R.string.action_clear_search),
        inlineContainerColor = ClipSurfaces.row,
    )
}

@Composable
private fun ScopeChips(scope: AppScope, onSelect: (AppScope) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterChip(
            selected = scope == AppScope.UserOnly,
            onClick = { onSelect(AppScope.UserOnly) },
            label = { Text(stringResource(R.string.apps_scope_user)) },
        )
        FilterChip(
            selected = scope == AppScope.All,
            onClick = { onSelect(AppScope.All) },
            label = { Text(stringResource(R.string.apps_scope_all)) },
        )
    }
}

@Composable
private fun AppList(
    apps: List<AppEntry>,
    selected: Set<String>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = LIST_BOTTOM_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (apps.isEmpty()) {
            stateItem { fill ->
                EmptyView(
                    icon = Icons.Rounded.Apps,
                    title = stringResource(R.string.apps_empty_title),
                    message = stringResource(R.string.apps_empty_message),
                    modifier = fill.animateItem(),
                )
            }
        }
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            val isSelected = app.packageName in selected
            val color by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else ClipSurfaces.row,
                label = "rowColor",
            )
            Surface(
                shape = groupedItemShape(itemPosition(index, apps.size)),
                color = color,
                modifier = Modifier.animateItem(),
            ) {
                AppRowContent(
                    packageName = app.packageName,
                    label = app.label,
                    selected = isSelected,
                    onClick = { onClick(app.packageName) },
                    onLongClick = { onLongClick(app.packageName) },
                    trailing = {
                        if (app.config.isConfigured) {
                            ConfigPill(app.config)
                        }
                    },
                )
            }
        }
    }
}

private val LIST_BOTTOM_CLEARANCE = 112.dp
package killua.dev.privateclipboard.ui.page.home

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.mvvm.BaseViewModel
import killua.dev.arsenal.core.mvvm.UiEffect
import killua.dev.arsenal.core.mvvm.UiIntent
import killua.dev.arsenal.core.mvvm.UiState
import killua.dev.arsenal.core.overlay.OverlayController
import killua.dev.arsenal.navigation.Navigator
import killua.dev.arsenal.navigation.generated.GeneratedRoutes
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.domain.AppCatalog
import killua.dev.privateclipboard.domain.ClipConfigRepository
import killua.dev.privateclipboard.domain.TemplateRepository
import killua.dev.privateclipboard.domain.model.AppEntry
import killua.dev.privateclipboard.domain.model.AppFilter
import killua.dev.privateclipboard.domain.model.AppScope
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.domain.model.ClipTemplate
import killua.dev.privateclipboard.domain.usecase.FilterApps
import killua.dev.privateclipboard.ui.components.BatchActionsSheet
import killua.dev.privateclipboard.ui.components.TemplatePickerSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

data class HomeState(
    val apps: List<AppEntry>? = null,
    val filter: AppFilter = AppFilter(),
    /** Null until the first filter pass finishes. */
    val visibleApps: List<AppEntry>? = null,
    val selected: Set<String> = emptySet(),
    val templates: List<ClipTemplate> = emptyList(),
) : UiState {
    val visiblePackages: List<String>
        get() = visibleApps.orEmpty().map { it.packageName }

    val allSelected: Boolean
        get() = visiblePackages.isNotEmpty() && selected.containsAll(visiblePackages)
}

sealed interface HomeIntent : UiIntent {
    data class Search(val query: String) : HomeIntent
    data class SetScope(val scope: AppScope) : HomeIntent
    data class Open(val packageName: String) : HomeIntent
    data class ToggleSelect(val packageName: String) : HomeIntent
    data object ToggleSelectAll : HomeIntent
    data object ClearSelection : HomeIntent
    data object OpenBatch : HomeIntent
    data class BatchSet(val key: String, val value: Boolean) : HomeIntent
    data object BatchPickTemplate : HomeIntent
    data class BatchApplyTemplate(val template: ClipTemplate, val targets: Set<String>) : HomeIntent
    data object BatchReset : HomeIntent
    data class ConfirmBatchReset(val targets: Set<String>) : HomeIntent
    data object ManageTemplates : HomeIntent
}

sealed interface HomeEffect : UiEffect

@HiltViewModel
class HomeTabViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    catalog: AppCatalog,
    private val configs: ClipConfigRepository,
    private val templates: TemplateRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    private val filterApps: FilterApps,
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    init {
        catalog.observeApps().launchInScope { setState { copy(apps = it) } }
        templates.observeAll().launchInScope { setState { copy(templates = it) } }
        observeVisibleApps()
    }

    /** Re-filters off the main thread, and only when the source list or the filter actually changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVisibleApps() {
        uiState
            .map { it.apps to it.filter }
            .distinctUntilChanged()
            .mapLatest { (apps, filter) -> apps?.let { filterApps(it, filter) } }
            .flowOn(Dispatchers.Default)
            .launchInScope { visible -> if (visible != null) setState { copy(visibleApps = visible) } }
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Search -> setState { copy(filter = filter.copy(query = intent.query)) }
            is HomeIntent.SetScope -> setState { copy(filter = filter.copy(scope = intent.scope)) }
            is HomeIntent.Open -> navigator.navigate(GeneratedRoutes.appConfigScreen(intent.packageName))
            is HomeIntent.ToggleSelect -> setState {
                copy(selected = if (intent.packageName in selected) selected - intent.packageName else selected + intent.packageName)
            }
            HomeIntent.ToggleSelectAll -> setState {
                copy(selected = if (allSelected) emptySet() else visiblePackages.toSet())
            }
            HomeIntent.ClearSelection -> setState { copy(selected = emptySet()) }
            HomeIntent.OpenBatch -> openBatchSheet()
            is HomeIntent.BatchSet -> {
                val targets = currentState.selected
                setState { copy(selected = emptySet()) }
                configs.setKey(targets, intent.key, intent.value)
                overlay.snackbar(context.getString(R.string.batch_done, targets.size))
            }
            HomeIntent.BatchPickTemplate -> {
                val targets = currentState.selected
                overlay.bottomSheet { dismiss ->
                    TemplatePickerSheet(
                        templates = currentState.templates,
                        onPick = {
                            dismiss()
                            dispatch(HomeIntent.BatchApplyTemplate(it, targets))
                        },
                        onManage = {
                            dismiss()
                            dispatch(HomeIntent.ManageTemplates)
                        },
                    )
                }
            }
            is HomeIntent.BatchApplyTemplate -> {
                configs.apply(intent.targets, intent.template.config)
                setState { copy(selected = emptySet()) }
                overlay.snackbar(context.getString(R.string.template_applied_many, intent.template.name, intent.targets.size))
            }
            HomeIntent.BatchReset -> {
                val targets = currentState.selected
                overlay.dialog(
                    title = context.getString(R.string.batch_reset_title),
                    message = context.getString(R.string.batch_reset_message, targets.size),
                    confirmLabel = context.getString(R.string.action_confirm),
                    dismissLabel = context.getString(R.string.action_cancel),
                    onConfirm = { dispatch(HomeIntent.ConfirmBatchReset(targets)) },
                )
            }
            is HomeIntent.ConfirmBatchReset -> {
                configs.apply(intent.targets, ClipConfig.Default)
                setState { copy(selected = emptySet()) }
            }
            HomeIntent.ManageTemplates -> navigator.navigate(GeneratedRoutes.TEMPLATES_SCREEN)
        }
    }

    private fun openBatchSheet() {
        val count = currentState.selected.size
        if (count == 0) return
        overlay.bottomSheet { dismiss ->
            BatchActionsSheet(
                selectedCount = count,
                onSet = { key, value ->
                    dismiss()
                    dispatch(HomeIntent.BatchSet(key, value))
                },
                // Swap the content in place: dismissing first would let the hide animation clear the picker.
                onApplyTemplate = { dispatch(HomeIntent.BatchPickTemplate) },
                onReset = {
                    dismiss()
                    dispatch(HomeIntent.BatchReset)
                },
            )
        }
    }
}

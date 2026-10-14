package killua.dev.privateclipboard.ui.page.home

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.mvvm.BaseViewModel
import killua.dev.arsenal.core.mvvm.UiEffect
import killua.dev.arsenal.core.mvvm.UiIntent
import killua.dev.arsenal.core.mvvm.UiState
import killua.dev.arsenal.core.overlay.OverlayController
import killua.dev.arsenal.core.state.DataState
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.navigation.Navigator
import killua.dev.arsenal.navigation.generated.GeneratedRoutes
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.data.AppSettings
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class HomeState(
    val apps: DataState<List<AppEntry>> = DataState.Loading(),
    val filter: AppFilter = AppFilter(),
    val selected: Set<String> = emptySet(),
    val templates: List<ClipTemplate> = emptyList(),
) : UiState {
    val visiblePackages: List<String>
        get() = (apps as? DataState.Success)?.data?.map { it.packageName }.orEmpty()

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
    private val settings: SettingsStore,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    filterApps: FilterApps,
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    private val query = MutableStateFlow("")

    init {
        val filters = combine(settings.flow(AppSettings.appsScope), query, ::AppFilter)
        combine(catalog.observeApps(), filters) { apps, filter -> filter to filterApps(apps, filter) }
            .launchInScope { (filter, apps) ->
                setState {
                    copy(filter = filter, apps = if (apps.isEmpty()) DataState.Empty() else DataState.Success(apps))
                }
            }
        templates.observeAll().launchInScope { setState { copy(templates = it) } }
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Search -> query.value = intent.query
            is HomeIntent.SetScope -> settings.set(AppSettings.appsScope, intent.scope)
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
                onApplyTemplate = {
                    dismiss()
                    dispatch(HomeIntent.BatchPickTemplate)
                },
                onReset = {
                    dismiss()
                    dispatch(HomeIntent.BatchReset)
                },
            )
        }
    }
}

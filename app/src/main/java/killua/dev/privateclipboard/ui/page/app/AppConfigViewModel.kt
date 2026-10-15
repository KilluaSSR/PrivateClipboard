package killua.dev.privateclipboard.ui.page.app

import android.content.Context
import androidx.lifecycle.SavedStateHandle
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
import killua.dev.privateclipboard.domain.UsageRepository
import killua.dev.privateclipboard.domain.model.AccessEvent
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.domain.model.ClipTemplate
import killua.dev.privateclipboard.domain.model.UsageCounts
import killua.dev.privateclipboard.shared.ClipShared.Category
import killua.dev.privateclipboard.ui.components.TemplatePickerSheet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class AppConfigState(
    val packageName: String,
    val label: String? = null,
    val config: ClipConfig = ClipConfig.Default,
    val usage: UsageCounts = UsageCounts.Empty,
    val category: Category = Category.Read,
    val eventsCategory: Category? = null,
    val events: List<AccessEvent> = emptyList(),
    val templates: List<ClipTemplate> = emptyList(),
) : UiState {
    /** Null while the list for the selected chip is still loading. */
    val visibleEvents: List<AccessEvent>? get() = events.takeIf { eventsCategory == category }
}

sealed interface AppConfigIntent : UiIntent {
    data class SetKey(val key: String, val value: Boolean) : AppConfigIntent
    data class SelectCategory(val category: Category) : AppConfigIntent
    data object PickTemplate : AppConfigIntent
    data class ApplyTemplate(val template: ClipTemplate) : AppConfigIntent
    data object Reset : AppConfigIntent
    data object ConfirmReset : AppConfigIntent
    data object Back : AppConfigIntent
}

sealed interface AppConfigEffect : UiEffect

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val configs: ClipConfigRepository,
    usage: UsageRepository,
    templates: TemplateRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    catalog: AppCatalog,
) : BaseViewModel<AppConfigState, AppConfigIntent, AppConfigEffect>(
    AppConfigState(packageName = savedStateHandle.get<String>(ARG_PACKAGE).orEmpty()),
) {
    private val packageName = currentState.packageName

    init {
        configs.observe(packageName).launchInScope { setState { copy(config = it) } }
        usage.observe(packageName).launchInScope { setState { copy(usage = it) } }
        uiState.map { it.category }
            .distinctUntilChanged()
            .flatMapLatest { category -> usage.observeEvents(packageName, category, EVENT_LIMIT).map { category to it } }
            .launchInScope { (category, events) -> setState { copy(eventsCategory = category, events = events) } }
        templates.observeAll().launchInScope { setState { copy(templates = it) } }
        catalog.observeApps().launchInScope { apps ->
            setState { copy(label = apps.firstOrNull { it.packageName == packageName }?.label) }
        }
    }

    override suspend fun handleIntent(intent: AppConfigIntent) {
        when (intent) {
            is AppConfigIntent.SetKey -> configs.update(packageName) { it.with(intent.key, intent.value) }
            is AppConfigIntent.SelectCategory -> setState { copy(category = intent.category) }
            AppConfigIntent.PickTemplate -> overlay.bottomSheet { dismiss ->
                TemplatePickerSheet(
                    templates = currentState.templates,
                    onPick = {
                        dismiss()
                        dispatch(AppConfigIntent.ApplyTemplate(it))
                    },
                    onManage = {
                        dismiss()
                        navigator.navigate(GeneratedRoutes.TEMPLATES_SCREEN)
                    },
                )
            }
            is AppConfigIntent.ApplyTemplate -> {
                configs.apply(listOf(packageName), intent.template.config)
                overlay.snackbar(context.getString(R.string.template_applied_one, intent.template.name))
            }
            AppConfigIntent.Reset -> overlay.dialog(
                title = context.getString(R.string.config_reset_title),
                message = context.getString(R.string.config_reset_message),
                confirmLabel = context.getString(R.string.action_confirm),
                dismissLabel = context.getString(R.string.action_cancel),
                onConfirm = { dispatch(AppConfigIntent.ConfirmReset) },
            )
            AppConfigIntent.ConfirmReset -> configs.apply(listOf(packageName), ClipConfig.Default)
            AppConfigIntent.Back -> navigator.navigateUp()
        }
    }

    companion object {
        const val ARG_PACKAGE = "packageName"
        private const val EVENT_LIMIT = 50
    }
}

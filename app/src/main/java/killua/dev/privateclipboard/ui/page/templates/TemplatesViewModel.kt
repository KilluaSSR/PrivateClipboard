package killua.dev.privateclipboard.ui.page.templates

import android.content.Context
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.mvvm.BaseViewModel
import killua.dev.arsenal.core.mvvm.UiEffect
import killua.dev.arsenal.core.mvvm.UiIntent
import killua.dev.arsenal.core.mvvm.UiState
import killua.dev.arsenal.core.overlay.OverlayController
import killua.dev.arsenal.core.state.DataState
import killua.dev.arsenal.navigation.Navigator
import killua.dev.arsenal.navigation.generated.GeneratedRoutes
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.domain.TemplateRepository
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.domain.model.ClipTemplate
import killua.dev.privateclipboard.ui.components.NameInputSheet
import javax.inject.Inject

data class TemplatesState(
    val templates: DataState<List<ClipTemplate>> = DataState.Loading(),
    val selected: Set<String> = emptySet(),
) : UiState

sealed interface TemplatesIntent : UiIntent {
    data class Open(val id: String) : TemplatesIntent
    data class ToggleSelect(val id: String) : TemplatesIntent
    data object ClearSelection : TemplatesIntent
    data object PromptCreate : TemplatesIntent
    data class Create(val name: String) : TemplatesIntent
    data object DeleteSelected : TemplatesIntent
    data class ConfirmDelete(val ids: Set<String>) : TemplatesIntent
    data object Back : TemplatesIntent
}

sealed interface TemplatesEffect : UiEffect

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val templates: TemplateRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
) : BaseViewModel<TemplatesState, TemplatesIntent, TemplatesEffect>(TemplatesState()) {

    init {
        templates.observeAll().launchInScope { list ->
            setState {
                copy(
                    templates = if (list.isEmpty()) DataState.Empty() else DataState.Success(list),
                    selected = selected.filterTo(HashSet()) { id -> list.any { it.id == id } },
                )
            }
        }
    }

    override suspend fun handleIntent(intent: TemplatesIntent) {
        when (intent) {
            is TemplatesIntent.Open -> navigator.navigate(GeneratedRoutes.templateDetailScreen(intent.id))
            is TemplatesIntent.ToggleSelect -> setState {
                copy(selected = if (intent.id in selected) selected - intent.id else selected + intent.id)
            }
            TemplatesIntent.ClearSelection -> setState { copy(selected = emptySet()) }
            TemplatesIntent.PromptCreate -> overlay.bottomSheet { dismiss ->
                NameInputSheet(
                    title = stringResource(R.string.template_new),
                    initial = "",
                    onConfirm = {
                        dismiss()
                        dispatch(TemplatesIntent.Create(it))
                    },
                    onCancel = dismiss,
                )
            }
            is TemplatesIntent.Create -> {
                val created = templates.create(intent.name.trim(), ClipConfig.GuardAll)
                navigator.navigate(GeneratedRoutes.templateDetailScreen(created.id))
            }
            TemplatesIntent.DeleteSelected -> {
                val ids = currentState.selected
                if (ids.isEmpty()) return
                overlay.dialog(
                    title = context.getString(R.string.template_delete_title),
                    message = context.getString(R.string.template_delete_message, ids.size),
                    confirmLabel = context.getString(R.string.action_delete),
                    dismissLabel = context.getString(R.string.action_cancel),
                    onConfirm = { dispatch(TemplatesIntent.ConfirmDelete(ids)) },
                )
            }
            is TemplatesIntent.ConfirmDelete -> {
                templates.delete(intent.ids)
                setState { copy(selected = emptySet()) }
                overlay.snackbar(context.getString(R.string.template_deleted, intent.ids.size))
            }
            TemplatesIntent.Back -> navigator.navigateUp()
        }
    }
}

data class TemplateDetailState(
    val id: String,
    val template: ClipTemplate? = null,
) : UiState

sealed interface TemplateDetailIntent : UiIntent {
    data class SetKey(val key: String, val value: Boolean) : TemplateDetailIntent
    data object PromptRename : TemplateDetailIntent
    data class Rename(val name: String) : TemplateDetailIntent
    data object Back : TemplateDetailIntent
}

sealed interface TemplateDetailEffect : UiEffect

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val templates: TemplateRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
) : BaseViewModel<TemplateDetailState, TemplateDetailIntent, TemplateDetailEffect>(
    TemplateDetailState(id = savedStateHandle.get<String>(ARG_ID).orEmpty()),
) {
    init {
        templates.observe(currentState.id).launchInScope { setState { copy(template = it) } }
    }

    override suspend fun handleIntent(intent: TemplateDetailIntent) {
        val template = currentState.template
        when (intent) {
            is TemplateDetailIntent.SetKey -> template?.let {
                templates.save(it.copy(config = it.config.with(intent.key, intent.value)))
            }
            TemplateDetailIntent.PromptRename -> template?.let { current ->
                overlay.bottomSheet { dismiss ->
                    NameInputSheet(
                        title = stringResource(R.string.template_rename),
                        initial = current.name,
                        onConfirm = {
                            dismiss()
                            dispatch(TemplateDetailIntent.Rename(it))
                        },
                        onCancel = dismiss,
                    )
                }
            }
            is TemplateDetailIntent.Rename -> template?.let { templates.save(it.copy(name = intent.name.trim())) }
            TemplateDetailIntent.Back -> navigator.navigateUp()
        }
    }

    companion object {
        const val ARG_ID = "templateId"
    }
}

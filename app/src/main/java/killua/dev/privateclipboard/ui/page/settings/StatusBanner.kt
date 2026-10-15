package killua.dev.privateclipboard.ui.page.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import killua.dev.arsenal.core.mvvm.BaseViewModel
import killua.dev.arsenal.core.mvvm.UiEffect
import killua.dev.arsenal.core.mvvm.UiIntent
import killua.dev.arsenal.core.mvvm.UiState
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.components.loadingindicator.ArsenalLoadingIndicator
import killua.dev.arsenal.material.templates.settings.ItemPosition
import killua.dev.arsenal.material.templates.settings.SettingsItemModel
import killua.dev.arsenal.material.templates.settings.SettingsItemRenderer
import killua.dev.arsenal.material.templates.settings.SettingsItemStyle
import killua.dev.arsenal.spi.annotation.SpiImpl
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.data.AppSettings
import killua.dev.privateclipboard.data.HookProbe
import killua.dev.privateclipboard.data.HookStatus
import killua.dev.privateclipboard.data.InputMethods
import killua.dev.privateclipboard.domain.UsageRepository
import killua.dev.privateclipboard.domain.model.UsageCounts
import killua.dev.privateclipboard.shared.ClipShared.Category
import killua.dev.privateclipboard.ui.components.labelRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

internal const val STATUS_BANNER_STYLE = "status_banner"

data class StatusBannerState(
    /** Null while probing. */
    val status: HookStatus? = null,
    val totals: UsageCounts = UsageCounts.Empty,
) : UiState

sealed interface StatusBannerIntent : UiIntent {
    data object Probe : StatusBannerIntent
}

sealed interface StatusBannerEffect : UiEffect

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatusBannerViewModel @Inject constructor(
    private val probe: HookProbe,
    private val inputMethods: InputMethods,
    usage: UsageRepository,
    settings: SettingsStore,
) : BaseViewModel<StatusBannerState, StatusBannerIntent, StatusBannerEffect>(StatusBannerState()) {

    private val imePackages = MutableStateFlow(inputMethods.enabledPackages())

    init {
        combine(settings.flow(AppSettings.excludeImeStats), imePackages) { exclude, imes -> if (exclude) imes else emptySet() }
            .distinctUntilChanged()
            .flatMapLatest { usage.observeTotals(it) }
            .launchInScope { setState { copy(totals = it) } }
    }

    override suspend fun handleIntent(intent: StatusBannerIntent) {
        when (intent) {
            StatusBannerIntent.Probe -> {
                imePackages.value = inputMethods.enabledPackages()
                setState { copy(status = probe.status()) }
            }
        }
    }
}

@SpiImpl
class StatusBannerRenderer @Inject constructor() : SettingsItemRenderer {
    override fun handles(model: SettingsItemModel): Boolean =
        model.style == SettingsItemStyle.Custom && (model as? SettingsItemModel.Custom)?.styleKey == STATUS_BANNER_STYLE

    @Composable
    override fun Render(model: SettingsItemModel, position: ItemPosition, enabled: Boolean, store: SettingsStore) {
        val viewModel: StatusBannerViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.dispatch(StatusBannerIntent.Probe) }
        StatusBanner(state)
    }
}

@Composable
private fun StatusBanner(state: StatusBannerState) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (state.status) {
            HookStatus.Active -> scheme.primaryContainer
            HookStatus.Outdated -> scheme.tertiaryContainer
            HookStatus.Inactive -> scheme.errorContainer
            null -> scheme.surfaceContainerHighest
        },
        label = "bannerContainer",
    )
    val content by animateColorAsState(
        when (state.status) {
            HookStatus.Active -> scheme.onPrimaryContainer
            HookStatus.Outdated -> scheme.onTertiaryContainer
            HookStatus.Inactive -> scheme.onErrorContainer
            null -> scheme.onSurface
        },
        label = "bannerContent",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content,
    ) {
        AnimatedContent(targetState = state.status, label = "bannerBody") { status ->
            Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (status == null) {
                        ArsenalLoadingIndicator(modifier = Modifier.size(32.dp), color = content)
                    } else {
                        Icon(
                            when (status) {
                                HookStatus.Active -> Icons.Rounded.Shield
                                HookStatus.Outdated -> Icons.Rounded.RestartAlt
                                HookStatus.Inactive -> Icons.Rounded.GppBad
                            },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        stringResource(
                            when (status) {
                                HookStatus.Active -> R.string.status_active
                                HookStatus.Outdated -> R.string.status_outdated
                                HookStatus.Inactive -> R.string.status_inactive
                                null -> R.string.status_checking
                            }
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                when (status) {
                    HookStatus.Active -> StatsRows(state.totals)
                    HookStatus.Outdated -> Text(stringResource(R.string.status_outdated_message), style = MaterialTheme.typography.bodyMedium)
                    HookStatus.Inactive -> Text(stringResource(R.string.status_inactive_message), style = MaterialTheme.typography.bodyMedium)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun StatsRows(totals: UsageCounts) {
    Row(Modifier.fillMaxWidth()) {
        Category.entries.forEach { Stat(it, totals.of(it), Modifier.weight(1f)) }
    }
}

@Composable
private fun Stat(category: Category, count: Long, modifier: Modifier) {
    Column(modifier) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(category.labelRes()), style = MaterialTheme.typography.labelLarge)
    }
}

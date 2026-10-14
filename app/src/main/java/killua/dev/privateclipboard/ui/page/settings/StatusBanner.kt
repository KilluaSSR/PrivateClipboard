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
import killua.dev.privateclipboard.data.HookProbe
import killua.dev.privateclipboard.domain.UsageRepository
import killua.dev.privateclipboard.domain.model.UsageCounts
import killua.dev.privateclipboard.shared.ClipShared.Category
import killua.dev.privateclipboard.ui.components.labelRes
import javax.inject.Inject

internal const val STATUS_BANNER_STYLE = "status_banner"

data class StatusBannerState(
    val active: Boolean? = null,
    val totals: UsageCounts = UsageCounts.Empty,
) : UiState

sealed interface StatusBannerIntent : UiIntent {
    data object Probe : StatusBannerIntent
}

sealed interface StatusBannerEffect : UiEffect

@HiltViewModel
class StatusBannerViewModel @Inject constructor(
    private val probe: HookProbe,
    usage: UsageRepository,
) : BaseViewModel<StatusBannerState, StatusBannerIntent, StatusBannerEffect>(StatusBannerState()) {

    init {
        usage.observeTotals().launchInScope { setState { copy(totals = it) } }
    }

    override suspend fun handleIntent(intent: StatusBannerIntent) {
        when (intent) {
            StatusBannerIntent.Probe -> setState { copy(active = probe.isHookActive()) }
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
        when (state.active) {
            true -> scheme.primaryContainer
            false -> scheme.errorContainer
            null -> scheme.surfaceContainerHighest
        },
        label = "bannerContainer",
    )
    val content by animateColorAsState(
        when (state.active) {
            true -> scheme.onPrimaryContainer
            false -> scheme.onErrorContainer
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
        AnimatedContent(targetState = state.active, label = "bannerBody") { active ->
            Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (active == null) {
                        ArsenalLoadingIndicator(modifier = Modifier.size(32.dp), color = content)
                    } else {
                        Icon(
                            if (active) Icons.Rounded.Shield else Icons.Rounded.GppBad,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        stringResource(
                            when (active) {
                                true -> R.string.status_active
                                false -> R.string.status_inactive
                                null -> R.string.status_checking
                            }
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                when (active) {
                    true -> StatsRows(state.totals)
                    false -> Text(stringResource(R.string.status_inactive_message), style = MaterialTheme.typography.bodyMedium)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun StatsRows(totals: UsageCounts) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(
            Category.AutoRead to Category.ManualRead,
            Category.AutoWrite to Category.ManualWrite,
        ).forEach { (left, right) ->
            Row(Modifier.fillMaxWidth()) {
                Stat(left, totals.of(left), Modifier.weight(1f))
                Stat(right, totals.of(right), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(category: Category, count: Long, modifier: Modifier) {
    Column(modifier) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(category.labelRes()), style = MaterialTheme.typography.labelLarge)
    }
}

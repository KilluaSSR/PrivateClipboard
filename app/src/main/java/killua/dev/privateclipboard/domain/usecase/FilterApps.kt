package killua.dev.privateclipboard.domain.usecase

import killua.dev.privateclipboard.domain.model.AppEntry
import killua.dev.privateclipboard.domain.model.AppFilter
import killua.dev.privateclipboard.domain.model.AppScope
import java.text.Collator
import javax.inject.Inject

class FilterApps @Inject constructor() {

    private val labelOrder: Collator = Collator.getInstance()

    operator fun invoke(apps: List<AppEntry>, filter: AppFilter): List<AppEntry> {
        val query = filter.query.trim()
        return apps
            .asSequence()
            .filter { filter.scope == AppScope.All || !it.isSystemApp }
            .filter {
                query.isEmpty() ||
                    it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<AppEntry> { it.config.isConfigured }
                    .thenComparator { a, b -> labelOrder.compare(a.label, b.label) },
            )
            .toList()
    }
}

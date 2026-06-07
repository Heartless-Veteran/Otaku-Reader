package app.otakureader.data.repository

import app.otakureader.domain.model.SourceHealthEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceHealthRepository @Inject constructor() {
    private val _healthMap = MutableStateFlow<Map<Long, SourceHealthEntry>>(emptyMap())
    val healthMap: StateFlow<Map<Long, SourceHealthEntry>> = _healthMap.asStateFlow()

    fun recordFailure(sourceId: Long, sourceName: String, error: String) {
        _healthMap.update { map ->
            val existing = map[sourceId]
            map + (sourceId to SourceHealthEntry(
                sourceId = sourceId,
                sourceName = sourceName,
                lastErrorMessage = error,
                lastErrorAt = System.currentTimeMillis(),
                consecutiveFailures = (existing?.consecutiveFailures ?: 0) + 1,
                isDisabled = existing?.isDisabled ?: false,
            ))
        }
    }

    fun recordSuccess(sourceId: Long) {
        _healthMap.update { map ->
            val existing = map[sourceId] ?: return@update map
            map + (sourceId to existing.copy(
                consecutiveFailures = 0,
                lastErrorMessage = null,
                lastErrorAt = null,
            ))
        }
    }

    fun setDisabled(sourceId: Long, disabled: Boolean) {
        _healthMap.update { map ->
            val existing = map[sourceId] ?: return@update map
            map + (sourceId to existing.copy(isDisabled = disabled))
        }
    }
}

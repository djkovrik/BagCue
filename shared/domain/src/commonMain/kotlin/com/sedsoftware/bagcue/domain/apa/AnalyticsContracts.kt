package com.sedsoftware.bagcue.domain.apa

import kotlinx.coroutines.flow.Flow

data class AnalyticsPreference(val enabled: Boolean = false)

enum class AnalyticsEventName(val wireName: String) {
    TemplateCreated("template_created"),
    PackingSessionCreated("packing_session_created"),
    PackingSessionCompleted("packing_session_completed"),
    PackingSessionReopened("packing_session_reopened"),
    HistoryRepeatUsed("history_repeat_used"),
    ReminderConfigured("reminder_configured"),
}

data class AnalyticsEvent(val name: AnalyticsEventName)

interface AnalyticsPreferenceRepository {
    fun observe(): Flow<AnalyticsPreference>
    suspend fun read(): Result<AnalyticsPreference>
    suspend fun update(preference: AnalyticsPreference): Result<AnalyticsPreference>
}

interface AnalyticsController {
    suspend fun setCollectionEnabled(enabled: Boolean): Result<Unit>
    suspend fun resetAnalyticsData(): Result<Unit>
    suspend fun readAppInstanceId(): Result<String?>
    suspend fun record(event: AnalyticsEvent): Result<Unit>
}

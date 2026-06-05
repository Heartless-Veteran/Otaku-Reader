package app.otakureader.domain.model

/**
 * A single data usage record aggregated by date, category, and network type.
 *
 * @param date ISO date string (yyyy-MM-dd)
 * @param category Identifies the request type (e.g., "DOWNLOAD", "IMAGE_CACHE")
 * @param network Network type at time of transfer ("WIFI" or "MOBILE")
 * @param bytes Total bytes transferred for this combination
 */
data class DataUsageRecord(
    val date: String,
    val category: String,
    val network: String,
    val bytes: Long = 0L,
)

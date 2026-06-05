package app.otakureader.core.network

/**
 * Abstraction for recording network byte counts per [RequestCategory].
 *
 * Decouples the OkHttp event listener wiring in [core/network] from the concrete
 * [DataUsageRepository] implementation in [data]. The implementation is provided
 * via a Hilt binding in the [data] module, breaking the cross-layer dependency.
 */
fun interface BytesRecorder {
    /**
     * Called for every completed HTTP response body with the resolved category and byte count.
     * Implementations must be non-blocking; any I/O should be dispatched to a background scope.
     */
    fun record(category: RequestCategory, bytes: Long)
}

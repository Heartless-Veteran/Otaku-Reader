package app.otakureader.core.network

import okhttp3.Call
import okhttp3.EventListener

enum class RequestCategory { DOWNLOAD, IMAGE_CACHE, EXTENSION_FETCH, UPDATE, OTHER }

class BytesEventListener(
    private val onBytesRecorded: (category: RequestCategory, bytes: Long) -> Unit
) : EventListener() {

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        if (byteCount <= 0) return
        val category = call.request().tag(RequestCategory::class.java) ?: RequestCategory.OTHER
        onBytesRecorded(category, byteCount)
    }

    class Factory(
        private val onBytesRecorded: (category: RequestCategory, bytes: Long) -> Unit
    ) : EventListener.Factory {
        override fun create(call: Call): EventListener = BytesEventListener(onBytesRecorded)
    }
}

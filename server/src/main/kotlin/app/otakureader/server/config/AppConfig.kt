package app.otakureader.server.config

import java.io.File

/**
 * Server configuration loaded from environment variables.
 */
data class AppConfig(
    val host: String,
    val port: Int,
    val authToken: String,
    val storagePath: String
) {
    companion object {
        fun load(): AppConfig {
            val authToken = System.getenv("AUTH_TOKEN")
            if (authToken.isNullOrBlank()) {
                System.err.println("ERROR: AUTH_TOKEN environment variable is required for security.")
                System.err.println("Please set AUTH_TOKEN to a secure random token.")
                System.err.println("Example: export AUTH_TOKEN=\"$(openssl rand -base64 32)\"")
                throw IllegalStateException("AUTH_TOKEN environment variable must be set")
            }

            return AppConfig(
                host = System.getenv("HOST") ?: "0.0.0.0",
                port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                authToken = authToken,
                storagePath = System.getenv("STORAGE_PATH") ?: "/app/data"
            )
        }
    }
    
    init {
        // Ensure storage directory exists
        File(storagePath).mkdirs()
    }
}

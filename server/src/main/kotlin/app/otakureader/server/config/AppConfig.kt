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
            return AppConfig(
                host = System.getenv("HOST") ?: "0.0.0.0",
                port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                authToken = System.getenv("AUTH_TOKEN") ?: run {
                    println("⚠️  Warning: Using default auth token. Set AUTH_TOKEN env var for security.")
                    "otaku-reader-default-token-change-me"
                },
                storagePath = System.getenv("STORAGE_PATH") ?: "/app/data"
            )
        }
    }
    
    init {
        // Ensure storage directory exists
        File(storagePath).mkdirs()
    }
}

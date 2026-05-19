package app.otakureader.domain.tracking

interface TrackManager {
    val all: List<Tracker>
    val loggedIn: List<Tracker>
    fun get(id: Int): Tracker?
    suspend fun login(trackerId: String, code: String, codeVerifier: String): Boolean
}

package pe.tecsup.main.mobile.data

import org.json.JSONObject
import java.util.UUID

internal data class CallIceServer(val urls: List<String>, val username: String, val credential: String)
internal data class DirectCallSignal(val id: UUID, val conversationId: UUID, val callerName: String,
    val kind: String, val state: String, val offer: String?, val answer: String?) {
    companion object {
        fun read(json: JSONObject) = DirectCallSignal(
            UUID.fromString(json.getString("id")), UUID.fromString(json.getString("conversationId")),
            json.getString("callerName"), json.getString("kind"), json.getString("state"),
            json.optString("offer").ifBlank { null }, json.optString("answer").ifBlank { null }
        )
    }
}

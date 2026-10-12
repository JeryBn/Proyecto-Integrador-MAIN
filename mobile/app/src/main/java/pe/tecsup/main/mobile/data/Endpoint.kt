package pe.tecsup.main.mobile.data

import java.net.URI

/** HTTP is limited to the local development bridge in a debug build. */
fun validateEndpoint(value: String, debug: Boolean): String {
    val uri = try { URI(value.trim()) } catch (_: Exception) {
        throw IllegalArgumentException("Escribe una dirección válida del servidor.")
    }
    require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null &&
        (uri.path.isNullOrEmpty() || uri.path == "/")) { "Usa la dirección del servidor sin rutas ni credenciales." }
    require(uri.port == -1 || uri.port in 1..65535) { "El puerto del servidor no es válido." }
    val local = uri.host in setOf("localhost", "127.0.0.1", "10.0.2.2")
    require(uri.scheme == "https" || (debug && local && uri.scheme == "http")) {
        "Usa HTTPS. HTTP solo está disponible para la conexión local de pruebas."
    }
    return uri.toString().trimEnd('/')
}

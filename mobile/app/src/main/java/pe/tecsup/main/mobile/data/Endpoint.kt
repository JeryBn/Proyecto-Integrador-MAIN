package pe.tecsup.main.mobile.data

import java.net.URI

internal class InvalidEndpoint(message: String) : IllegalArgumentException(message)

/** HTTP is limited to the local development bridge in a debug build. */
fun validateEndpoint(value: String, debug: Boolean): String {
    val uri = try { URI(value.trim()) } catch (_: Exception) {
        throw InvalidEndpoint("Escribe una dirección válida del servidor.")
    }
    if (uri.host == null || uri.userInfo != null || uri.query != null || uri.fragment != null ||
        (!uri.path.isNullOrEmpty() && uri.path != "/")) {
        throw InvalidEndpoint("Usa la dirección del servidor sin rutas ni credenciales.")
    }
    if (uri.port != -1 && uri.port !in 1..65535) {
        throw InvalidEndpoint("El puerto del servidor no es válido.")
    }
    val local = uri.host in setOf("localhost", "127.0.0.1", "10.0.2.2")
    if (uri.scheme != "https" && !(debug && local && uri.scheme == "http")) {
        throw InvalidEndpoint("Usa HTTPS. HTTP solo está disponible para la conexión local de pruebas.")
    }
    return uri.toString().trimEnd('/')
}

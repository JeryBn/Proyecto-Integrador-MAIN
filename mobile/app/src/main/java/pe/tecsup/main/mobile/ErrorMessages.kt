package pe.tecsup.main.mobile

import pe.tecsup.main.mobile.data.ActivityUnavailable
import pe.tecsup.main.mobile.data.ApiFailure
import pe.tecsup.main.mobile.data.InvalidCredentials
import pe.tecsup.main.mobile.data.InvalidEndpoint
import pe.tecsup.main.mobile.data.ResourceUnavailable
import pe.tecsup.main.mobile.data.SessionExpired
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Converts technical failures into short, actionable messages without exposing internals. */
internal fun userMessage(error: Exception): String = when (error) {
    is InvalidCredentials,
    is ApiFailure,
    is SessionExpired,
    is ActivityUnavailable,
    is ResourceUnavailable,
    is InvalidEndpoint -> error.message ?: "Revisa la dirección del servidor."

    is SocketTimeoutException ->
        "El servidor está tardando más de lo esperado. Intenta nuevamente en unos segundos."
    is UnknownHostException, is ConnectException ->
        "No encontramos el servidor. Revisa tu conexión y la dirección configurada."
    is SSLException ->
        "No pudimos establecer una conexión segura con el servidor."
    is IOException ->
        "La conexión se interrumpió. Revisa tu red e intenta nuevamente."
    is IllegalArgumentException ->
        "Revisa los datos ingresados e intenta nuevamente."
    else -> "Ocurrió un problema inesperado. Intenta nuevamente."
}

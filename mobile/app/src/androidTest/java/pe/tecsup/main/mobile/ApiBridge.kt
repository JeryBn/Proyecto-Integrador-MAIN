package pe.tecsup.main.mobile

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** Test-only local bridge to the REAL backend: inject one 503 or omit one cookie. */
internal class ApiBridge : AutoCloseable {
    private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
    val endpoint = "http://127.0.0.1:${server.localPort}"
    val failNextPending = AtomicBoolean(false)
    val expireNextApi = AtomicBoolean(false)
    private val worker = Thread {
        while (!server.isClosed) {
            try { server.accept().use(::forward) } catch (_: Exception) {
                if (server.isClosed) break
            }
        }
    }.apply { isDaemon = true; start() }

    private fun forward(socket: Socket) {
        socket.soTimeout = 10_000
        val input = socket.getInputStream().buffered()
        fun line(): String {
            val bytes = ArrayList<Byte>()
            while (true) {
                val b = input.read()
                if (b == -1 || b == 10) break
                if (b != 13) bytes.add(b.toByte())
            }
            return bytes.toByteArray().toString(Charsets.ISO_8859_1)
        }
        val parts = line().split(' ')
        val path = parts[1]
        val headers = linkedMapOf<String, String>()
        while (true) {
            val header = line()
            if (header.isBlank()) break
            val colon = header.indexOf(':')
            if (colon > 0) headers[header.substring(0, colon).lowercase()] = header.substring(colon + 1).trim()
        }
        val body = ByteArray(headers["content-length"]?.toInt() ?: 0)
        var offset = 0
        while (offset < body.size) {
            val count = input.read(body, offset, body.size - offset)
            if (count < 0) break
            offset += count
        }
        if (path == "/api/actividades/pendientes" && failNextPending.compareAndSet(true, false)) {
            socket.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
            return
        }
        val upstream = URL("http://127.0.0.1:8088$path").openConnection() as HttpURLConnection
        try {
            upstream.instanceFollowRedirects = false
            upstream.connectTimeout = 8_000
            upstream.readTimeout = 8_000
            upstream.requestMethod = parts[0]
            val expire = path.startsWith("/api/") && expireNextApi.compareAndSet(true, false)
            headers.filterKeys { it in setOf("cookie", "content-type", "accept") && !(expire && it == "cookie") }
                .forEach { (key, value) -> upstream.setRequestProperty(key, value) }
            if (parts[0] == "POST") {
                upstream.doOutput = true
                upstream.outputStream.use { it.write(body) }
            }
            val status = upstream.responseCode
            val bytes = (if (status >= 400) upstream.errorStream else upstream.inputStream)?.use { it.readBytes() } ?: byteArrayOf()
            val response = buildString {
                append("HTTP/1.1 $status Response\r\n")
                upstream.headerFields.filterKeys { it?.lowercase() in setOf("set-cookie", "location", "content-type") }
                    .forEach { (key, values) -> values.forEach { append("$key: $it\r\n") } }
                append("Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n")
            }
            socket.getOutputStream().apply { write(response.toByteArray(Charsets.ISO_8859_1)); write(bytes); flush() }
        } finally { upstream.disconnect() }
    }

    override fun close() { server.close(); worker.join(2_000) }
}

package pe.tecsup.main.mobile

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

private val contactDigits = Regex("[0-9]{6}")

private fun readContactQr(bitmap: Bitmap): String? {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    return try {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text.takeIf(contactDigits::matches)
    } catch (_: ReaderException) { null }
}

@Composable
internal fun ContactCodesScreen(model: MainViewModel) {
    var input by rememberSaveable { mutableStateOf("") }
    var scanError by remember { mutableStateOf("") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { image ->
        if (image != null) {
            val detected = readContactQr(image)
            if (detected != null) input = detected
            scanError = if (detected == null) "No se pudo leer el QR. Acerca la cámara o escribe el código." else ""
        }
    }
    LaunchedEffect(Unit) { model.loadContactCodes() }
    PageHeading("Agregar compañeros") { model.settingsPage = "MAIN" }
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        model.personalContactCode?.let { (code, qr) ->
            Text("Mi código: $code", style = MaterialTheme.typography.titleLarge)
            if (qr.isNotEmpty()) Canvas(Modifier.size(145.dp).background(Color.White)
                .semantics { contentDescription = "QR de mi código de contacto" }) {
                val cell = size.width / qr.size
                qr.forEachIndexed { y, row -> row.forEachIndexed { x, bit ->
                    if (bit == '1') drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell, cell))
                } }
            }
        }
        if (model.profile?.rol == "Alumno") {
            OutlinedTextField(input, { input = it.filter { char -> char in '0'..'9' }.take(6) },
                label = { Text("Código de 6 dígitos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, enabled = !model.accountBusy)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !model.accountBusy && contactDigits.matches(input), onClick = {
                    model.requestContact(input)
                }) { Text("Enviar solicitud") }
                OutlinedButton(enabled = !model.accountBusy, onClick = {
                    try { camera.launch(null) } catch (_: ActivityNotFoundException) { scanError = "Cámara no disponible." }
                }) { Text("Leer QR") }
            }
            TextButton(enabled = !model.accountBusy, onClick = model::loadContactCodes) { Text("Actualizar solicitudes") }
            model.contactInvitations.forEach { (sender, name) ->
                Text(name, style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(enabled = !model.accountBusy, onClick = { model.answerContact(sender, true) }) { Text("Aceptar") }
                    TextButton(enabled = !model.accountBusy, onClick = { model.answerContact(sender, false) }) { Text("Rechazar") }
                }
            }
        } else if (model.personalContactCode == null) {
            TextButton(enabled = !model.accountBusy, onClick = model::loadContactCodes) { Text("Actualizar código") }
        }
        if (model.accountBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (scanError.isNotBlank()) Text(scanError, color = MaterialTheme.colorScheme.error)
        if (model.accountError.isNotBlank()) Text(model.accountError, color = MaterialTheme.colorScheme.error)
        if (model.accountMessage.isNotBlank()) Text(model.accountMessage)
    }
}

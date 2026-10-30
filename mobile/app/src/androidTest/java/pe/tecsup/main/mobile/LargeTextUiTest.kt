package pe.tecsup.main.mobile

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Requires an emulator explicitly configured with font_scale=2.0 before launch. */
class LargeTextUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun readable(label: String) {
        val node = ui.onNodeWithText(label, useUnmergedTree = true)
        if (!node.isDisplayed()) node.performScrollTo()
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        val action = node.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
        assertTrue("Text layout available for $label", action?.action?.invoke(layouts) == true)
        assertTrue("Text layout is not empty for $label", layouts.isNotEmpty())
        // Compose rounds the measured IntSize; a subpixel intrinsic-width excess
        // can set didOverflowWidth even when the complete label is rendered.
        // Check actual line bounds with one raster pixel of tolerance instead.
        val clipped = layouts.any { layout ->
            (0 until layout.lineCount).any { line ->
                layout.isLineEllipsized(line) || layout.getLineLeft(line) < -1f ||
                    layout.getLineRight(line) > layout.size.width + 1f ||
                    layout.getLineBottom(line) > layout.size.height + 1f
            } || layout.getLineEnd(layout.lineCount - 1, visibleEnd = true) < label.length
        }
        assertFalse("Primary label visibly clipped at 200 percent: $label", clipped)
    }

    @Test fun primaryNavigationAndLoginRemainUsableWithDoubleFontSize() {
        assertTrue("Run with font_scale=2.0, not a simulated assertion", ui.activity.resources.configuration.fontScale >= 1.9f)
        ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
        ui.onNodeWithText("Usuario").performTextReplacement("vacio")
        ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
        ui.onNodeWithText("Ingresar").performScrollTo().assertIsDisplayed().performClick()
        ui.waitUntil(20_000) { ui.onAllNodesWithText("Tu espacio académico").fetchSemanticsNodes().isNotEmpty() }
        Evidence.capture("60_fuente_doble_inicio.png")
        for (label in listOf("Inicio", "Chats", "Estados", "IA", "Ajustes")) readable(label)
        for ((description, expected) in listOf("Mensajes" to "Chats", "Estados" to "Estados", "Asistente" to "Asistente académico", "Perfil" to "Ajustes")) {
            val destination = ui.onNodeWithContentDescription(description)
            if (!destination.isDisplayed()) destination.performScrollTo()
            destination.assertIsDisplayed().performClick()
            ui.waitUntil(15_000) { ui.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
        }
        Evidence.capture("61_fuente_doble_ajustes.png")
        ui.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        ui.waitUntil(15_000) { ui.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Ingresar").performScrollTo().assertIsDisplayed()
    }
}

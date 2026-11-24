package pe.tecsup.main.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Rule
import org.junit.Test
import pe.tecsup.main.mobile.data.MainApi

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CallPermissionUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun denyAudioBeforeVoiceCall() = verifyDenied(Manifest.permission.RECORD_AUDIO, "Llamar")

    @Test fun denyCameraBeforeVideoCall() = verifyDenied(Manifest.permission.CAMERA, "Videollamada")

    private fun verifyDenied(permission: String, action: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(permission))
        if (permission == Manifest.permission.CAMERA) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
        }
        val sender = MainApi("http://127.0.0.1:8088")
        val receiver = MainApi("http://127.0.0.1:8088")
        sender.login("alumno", "MainDemo2026!")
        receiver.login("otro", "MainDemo2026!")
        val conversation = sender.startDirect("otro")
        try {
            ui.onNodeWithText("Usuario").performTextReplacement("alumno")
            ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
            ui.onNodeWithText("Ingresar").performClick()
            ui.waitUntil(20_000) { ui.onAllNodesWithContentDescription("Mensajes").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithContentDescription("Mensajes").performClick()
            ui.waitUntil(20_000) { ui.onAllNodesWithText("Sam Demo").fetchSemanticsNodes().isNotEmpty() }
            ui.runOnIdle { ViewModelProvider(ui.activity)[MainViewModel::class.java].openConversation(conversation) }
            ui.onNodeWithContentDescription("Opciones del chat").performClick()
            ui.onNodeWithText(action).performClick()

            var denied = false
            val permissionController = "com.android.permissioncontroller:id/permission_deny_button"
            ui.waitUntil(10_000) {
                val root = instrumentation.uiAutomation.rootInActiveWindow
                denied = root?.findAccessibilityNodeInfosByViewId(permissionController)
                    ?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                denied
            }
            assertTrue(denied)
            ui.waitUntil(10_000) {
                ui.onAllNodesWithText("Permite el micrófono", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(receiver.incomingCalls().none { it.conversationId.toString() == conversation })
        } finally {
            sender.logout()
            receiver.logout()
        }
    }
}

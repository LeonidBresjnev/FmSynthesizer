package com.sputnik.fmsynthesizer

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import fmsynthesizer.shared.generated.resources.Res
import fmsynthesizer.shared.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FM Synthesizer",
        icon = painterResource(Res.drawable.compose_multiplatform)
    ) {
        App()
    }
}

package com.sputnik.fmsynthesizer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePicker(onFileSelected: (fileName: String, content: String) -> Unit): () -> Unit {
    return remember {
        {
            try {
                val dialog = FileDialog(null as Frame?, "Open MusicXML File", FileDialog.LOAD)
                dialog.file = "*.xml;*.musicxml;*.mxl"
                dialog.isVisible = true
                val fileName = dialog.file
                val directory = dialog.directory
                if (fileName != null && directory != null) {
                    val file = File(directory, fileName)
                    if (file.exists()) {
                        val content = file.readText(Charsets.UTF_8)
                        onFileSelected(fileName, content)
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }
}

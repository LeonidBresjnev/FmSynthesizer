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
                val dialog = FileDialog(null as Frame?, "Open Music File (MusicXML or MIDI)", FileDialog.LOAD)
                dialog.file = "*.xml;*.musicxml;*.mxl;*.mid;*.midi"
                dialog.isVisible = true
                val fileName = dialog.file
                val directory = dialog.directory
                if (fileName != null && directory != null) {
                    val file = File(directory, fileName)
                    if (file.exists()) {
                        val content = String(file.readBytes(), Charsets.ISO_8859_1)
                        onFileSelected(fileName, content)
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }
}

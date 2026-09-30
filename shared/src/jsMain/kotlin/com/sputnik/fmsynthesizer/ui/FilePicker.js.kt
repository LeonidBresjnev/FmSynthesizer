package com.sputnik.fmsynthesizer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberFilePicker(onFileSelected: (fileName: String, content: String) -> Unit): () -> Unit {
    return remember {
        {
            try {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = ".xml,.musicxml,.mxl"
                input.onchange = {
                    val files = input.files
                    if (files != null && files.length > 0) {
                        val file = files[0]
                        if (file != null) {
                            val reader = FileReader()
                            reader.onload = {
                                val content = reader.result as? String ?: ""
                                onFileSelected(file.name, content)
                            }
                            reader.readAsText(file)
                        }
                    }
                }
                input.click()
            } catch (_: Throwable) {
            }
        }
    }
}

package com.sputnik.fmsynthesizer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(callback) => { " +
        "  const input = document.createElement('input'); " +
        "  input.type = 'file'; " +
        "  input.accept = '.xml,.musicxml,.mxl,.mid,.midi'; " +
        "  input.onchange = (e) => { " +
        "    const files = e.target.files; " +
        "    if (files && files.length > 0) { " +
        "      const file = files[0]; " +
        "      const reader = new FileReader(); " +
        "      reader.onload = (re) => { callback(file.name, re.target.result); }; " +
        "      reader.readAsText(file, 'ISO-8859-1'); " +
        "    } " +
        "  }; " +
        "  input.click(); " +
        "}")
private external fun triggerWasmFilePickerNative(callback: (fileName: String, content: String) -> Unit)

@Composable
actual fun rememberFilePicker(onFileSelected: (fileName: String, content: String) -> Unit): () -> Unit {
    return remember {
        {
            try {
                triggerWasmFilePickerNative(onFileSelected)
            } catch (_: Throwable) {
            }
        }
    }
}

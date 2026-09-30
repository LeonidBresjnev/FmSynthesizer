package com.sputnik.fmsynthesizer.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(onFileSelected: (fileName: String, content: String) -> Unit): () -> Unit {
    return { }
}

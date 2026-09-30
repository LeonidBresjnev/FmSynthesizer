package com.sputnik.fmsynthesizer.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(onFileSelected: (fileName: String, content: String) -> Unit): () -> Unit

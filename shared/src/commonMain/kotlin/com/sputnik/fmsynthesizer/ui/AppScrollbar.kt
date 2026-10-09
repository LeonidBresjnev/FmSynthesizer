package com.sputnik.fmsynthesizer.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppVerticalScrollbar(lazyListState: LazyListState, modifier: Modifier = Modifier)

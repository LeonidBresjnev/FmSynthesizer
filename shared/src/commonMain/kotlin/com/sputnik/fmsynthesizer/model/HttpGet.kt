package com.sputnik.fmsynthesizer.model

expect suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String
expect suspend fun httpGetBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray

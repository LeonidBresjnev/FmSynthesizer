package com.sputnik.fmsynthesizer.model

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers

private val ktorJvmClient by lazy { HttpClient(CIO) }

actual suspend fun httpGetBytes(url: String, headers: Map<String, String>): ByteArray {
    val response = ktorJvmClient.get(url) {
        headers {
            headers.forEach { (key, value) ->
                append(key, value)
            }
        }
    }
    return response.body<ByteArray>()
}

actual suspend fun httpGet(url: String, headers: Map<String, String>): String {
    val bytes = httpGetBytes(url, headers)
    return String(bytes, Charsets.ISO_8859_1)
}

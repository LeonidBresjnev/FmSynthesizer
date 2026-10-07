package com.sputnik.fmsynthesizer.model

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers

private val ktorWebClient by lazy { HttpClient() }

actual suspend fun httpGetBytes(url: String, headers: Map<String, String>): ByteArray {
    val response = ktorWebClient.get(url) {
        headers {
            headers.forEach { (key, value) ->
                if (!key.equals("User-Agent", ignoreCase = true)) {
                    append(key, value)
                }
            }
        }
    }
    return response.body<ByteArray>()
}

actual suspend fun httpGet(url: String, headers: Map<String, String>): String {
    val bytes = httpGetBytes(url, headers)
    val chars = CharArray(bytes.size) { i -> (bytes[i].toInt() and 0xFF).toChar() }
    return chars.concatToString()
}

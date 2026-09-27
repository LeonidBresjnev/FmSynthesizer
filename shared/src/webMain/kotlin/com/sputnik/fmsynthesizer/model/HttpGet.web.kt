package com.sputnik.fmsynthesizer.model

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText

private val ktorWebClient by lazy { HttpClient() }

actual suspend fun httpGet(url: String, headers: Map<String, String>): String {
    val response = ktorWebClient.get(url) {
        headers {
            headers.forEach { (key, value) ->
                if (!key.equals("User-Agent", ignoreCase = true)) {
                    append(key, value)
                }
            }
        }
    }
    return response.bodyAsText()
}

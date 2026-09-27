package com.sputnik.fmsynthesizer.model

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText

private val ktorJvmClient by lazy { HttpClient(CIO) }

actual suspend fun httpGet(url: String, headers: Map<String, String>): String {
    val response = ktorJvmClient.get(url) {
        headers {
            headers.forEach { (key, value) ->
                append(key, value)
            }
        }
    }
    return response.bodyAsText()
}

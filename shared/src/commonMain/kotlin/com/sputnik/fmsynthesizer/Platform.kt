package com.sputnik.fmsynthesizer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
package com.kraskaska.minecraft.shinytech

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.net.URL

object DiscordApi {
    private val client = HttpClient(CIO)
    var token: String? = null
    var version: Short = 10
    fun doRequest(url: String, token: String = this.token!!, code: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val prefixedUrl = URL("https://discord.com/api/v$version${url}")
        val builder = HttpRequestBuilder(url = prefixedUrl)
        builder.code()
        builder.headers["Authorization"] = "Bearer $token"
        return runBlocking { return@runBlocking client.request(builder) }
    }

    fun getMe(): DiscordApiUser {
        doRequest("/")
    }
}

data class DiscordApiUser(val id: Long)

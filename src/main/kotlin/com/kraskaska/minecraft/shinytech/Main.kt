package com.kraskaska.minecraft.shinytech

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.node.ObjectNode
import com.soywiz.klock.days
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.runBlocking
import java.io.StringWriter
import java.util.UUID
import kotlin.concurrent.thread

fun main() = runBlocking<Unit> {
    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        DatabaseHandler.saveDatabase()
    })
    @Suppress("ExtractKtorModule") embeddedServer(Netty, port = 8080) {
        install(StatusPages)
        install(IgnoreTrailingSlash)
        install(Authentication) {
            oauth("oauth-discord") {
                urlProvider = { "/auth/callback" }
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings(
                        name = "discord",
                        authorizeUrl = "https://discord.com/oauth2/authorize",
                        accessTokenUrl = "https://discord.com/api/oauth2/token",
                        requestMethod = HttpMethod.Post,
                        clientId = Config.discordAppId,
                        clientSecret = Config.discordAppSecret,
                        defaultScopes = listOf("identify"),
                    )
                }
                client = HttpClient(CIO) {
                    install(ClientContentNegotiation) {
                        jackson()
                    }
                }
            }
            session<UserSession> {
                // Configure session authentication
                validate { session ->

                }
            }
        }
            install(Sessions) {
                cookie<UserSession>("user_session") {
                    cookie.path = "/"
                    cookie.maxAgeInSeconds = 30.days.seconds.toLong()
                }
            }
        routing {
            route("/") {
                get {
                    call.respond(
                        """
                        ShinyTech ${backendName.random()} v0.0.1
                        ${quotes.random()}
                    """.trimIndent()
                    )
                }
                route("/shinypassport") {
                    get {
                        val strWriter = StringWriter()
                        val generator = JsonFactory().createGenerator(strWriter)
                        generator.useDefaultPrettyPrinter()
                        generator.writeStartArray()
                        for (shinyPassport in DatabaseHandler().shinyPassport) {
                            shinyPassport.writeToGenerator(generator)
                        }
                        generator.writeEndArray()
                        generator.close()
                        call.respond(strWriter.toString())
                    }
                    post {
                        val body = call.receiveText()
                        val parser = CommonMapper().createParser(body)
                        val tree = parser.readValueAsTree<ObjectNode>()
                        val minecraftUUID = if (tree.get("minecraftUUID") != null) UUID.fromString(
                            tree.get("minecraftUUID").asText()
                        ) else null
                        val minecraftName =
                            if (tree.get("minecraftName") != null) tree.get("minecraftName").asText(null) else null
                        val discordId = tree.get("discordId")!!.asLong()
                        call.respond(
                            DatabaseHandler().shinyPassport.create(
                                ShinyPassport(
                                    minecraftUUID, minecraftName, discordId
                                )
                            ).toString()
                        )
                    }
                    route("/{id}") {
                        get {
                            val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                call.respond(
                                    HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                )
                                return@get
                            }
                            val shinyPassport = uuid to DatabaseHandler().shinyPassport[uuid]
                            val strWriter = StringWriter()
                            val generator = JsonFactory().createGenerator(strWriter)
                            generator.useDefaultPrettyPrinter()
                            shinyPassport.writeToGenerator(generator)
                            generator.close()
                            call.respond(strWriter.toString())
                        }
                        put {
                            val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                call.respond(
                                    HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                )
                                return@put
                            }
                            val body = call.receiveText()
                            val parser = CommonMapper().createParser(body)
                            val tree = parser.readValueAsTree<ObjectNode>()
                            val minecraftUUID = if (tree.get("minecraftUUID") != null) UUID.fromString(
                                tree.get("minecraftUUID").asText()
                            ) else null
                            val minecraftName =
                                if (tree.get("minecraftName") != null) tree.get("minecraftName").asText(null) else null
                            val discordId = tree.get("discordId")!!.asLong()
                            DatabaseHandler().shinyPassport.update(
                                uuid, ShinyPassport(
                                    minecraftUUID, minecraftName, discordId
                                )
                            )
                            call.respond(
                                "OK"
                            )
                        }
                        delete {
                            val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                call.respond(
                                    HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                )
                                return@delete
                            }
                            DatabaseHandler().shinyPassport.delete(uuid)
                            call.respond(
                                "OK"
                            )
                        }
                    }
                }
                route("/shinybank") {
                    route("/{id}") {
                        route("/balance") {
                            get {
                                val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                    )
                                    return@get
                                }
                                if (DatabaseHandler().shinyPassport.getOrNull(uuid) == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound, "No such user exists with this UUID"
                                    )
                                    return@get
                                }
                                call.respond(DatabaseHandler().shinyBank[uuid].toString())
                            }
                        }
                        route("/transaction/{toId}") {
                            post {
                                val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                    )
                                    return@post
                                }
                                if (DatabaseHandler().shinyPassport.getOrNull(uuid) == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound, "No such user exists with this UUID"
                                    )
                                    return@post
                                }
                                val toUuid = safe { UUID.fromString(call.parameters["toId"]) } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No recipient id supplied or not a valid UUID"
                                    )
                                    return@post
                                }
                                if (DatabaseHandler().shinyPassport.getOrNull(toUuid) == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound, "No such recipient user exists with this UUID"
                                    )
                                    return@post
                                }
                                if (toUuid == uuid) {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "You can't send money to yourself"
                                    )
                                    return@post
                                }
                                val amount = safe { call.request.queryParameters["amount"]?.toLong() } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No amount supplied or not a number"
                                    )
                                    return@post
                                }
                                if (amount <= 0) {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "Amount is less or equal to zero"
                                    )
                                    return@post
                                }
                                if (amount > DatabaseHandler().shinyBank[uuid]) {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "Amount is more than balance"
                                    )
                                    return@post
                                }
                                DatabaseHandler().shinyBank.transaction(uuid, toUuid, amount)
                                call.respond("OK")
                            }
                        }
                        route("/deposit") {
                            post {
                                val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                    )
                                    return@post
                                }
                                if (DatabaseHandler().shinyPassport.getOrNull(uuid) == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound, "No such user exists with this UUID"
                                    )
                                    return@post
                                }
                                val amount = safe { call.request.queryParameters["amount"]?.toLong() } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No amount supplied or not a number"
                                    )
                                    return@post
                                }
                                if (amount <= 0) {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "Amount is less or equal to zero"
                                    )
                                    return@post
                                }
                                DatabaseHandler().shinyBank.transaction(ShinyBankHandler.DEPOSIT_WITHDRAWAL_UUID, uuid, amount)
                                call.respond("OK")
                            }
                        }
                        route("/withdraw") {
                            post {
                                val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                    )
                                    return@post
                                }
                                if (DatabaseHandler().shinyPassport.getOrNull(uuid) == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound, "No such user exists with this UUID"
                                    )
                                    return@post
                                }
                                val amount = safe { call.request.queryParameters["amount"]?.toLong() } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No amount supplied or not a number"
                                    )
                                    return@post
                                }
                                if (amount <= 0) {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "Amount is less or equal to zero"
                                    )
                                    return@post
                                }
                                DatabaseHandler().shinyBank.transaction(uuid, ShinyBankHandler.DEPOSIT_WITHDRAWAL_UUID, amount)
                                call.respond("OK")
                            }
                        }
                        route("/transactions") {
                            get {
                                val strWriter = StringWriter()
                                val generator = JsonFactory().createGenerator(strWriter)
                                generator.useDefaultPrettyPrinter()
                                generator.writeStartArray()
                                val uuid = safe { UUID.fromString(call.parameters["id"]) } ?: run {
                                    call.respond(
                                        HttpStatusCode.fromValue(400), "No id supplied or not a valid UUID"
                                    )
                                    return@get
                                }
                                if (DatabaseHandler().shinyPassport.getOrNull(uuid) == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound, "No such user exists with this UUID"
                                    )
                                    return@get
                                }
                                for (transaction in if (DatabaseHandler().shinyBank.any { it.to == uuid || it.from == uuid }) DatabaseHandler().shinyBank.filter { it.to == uuid || it.from == uuid } else listOf()) {
                                    transaction.writeToGenerator(generator)
                                }
                                generator.writeEndArray()
                                generator.close()
                                call.respond(strWriter.toString())
                            }
                        }
                    }
                    route("/transactions") {
                        get {
                            val strWriter = StringWriter()
                            val generator = JsonFactory().createGenerator(strWriter)
                            generator.useDefaultPrettyPrinter()
                            generator.writeStartArray()
                            for (transaction in DatabaseHandler().shinyBank) {
                                transaction.writeToGenerator(generator)
                            }
                            generator.writeEndArray()
                            generator.close()
                            call.respond(strWriter.toString())
                        }
                    }
                }
            }
            authenticate("oauth-discord") {
                get("/auth/login") {
                    // Redirects to 'authorizeUrl' automatically
                }
                get("/auth/callback") {
                    val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                    call.sessions.set(UserSession(principal?.accessToken.toString()))
                    call.respondRedirect("/hello")
                }
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.error(e.toString())
            log.error(e.stackTraceToString())
        }
    }.start(true)
}

data class UserSession(val token: String)

fun ShinyBankTransaction.writeToGenerator(generator: JsonGenerator) {
    generator.writeStartObject()
    generator.writeFieldName("from")
    generator.writeString(from.toString())
    generator.writeFieldName("to")
    generator.writeString(to.toString())
    generator.writeFieldName("amount")
    generator.writeNumber(amount)
    generator.writeEndObject()
}

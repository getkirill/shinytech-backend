package com.kraskaska.minecraft.shinytech

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID
import kotlin.system.exitProcess

/**
 * Stores immutable settings
 */
object Config {
    var bannedUsers: List<Long> = listOf() // based upon discord id
    var discordAppId: String = ""
    var discordAppSecret: String = ""

    init {
        try {
            load()
        } catch(e: Exception) {
            val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
            logger.error(e.message)
            logger.error(e.stackTraceToString())
            logger.error("Invalid config, shutting down")
            exitProcess(-1)
        }
    }

    fun load() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        val mapper = CommonMapper() as TomlMapper
        val root = mapper.readTree(File("config.toml"))
        bannedUsers = root["banned_users"].map { it.longValue() }
        discordAppId = root["discord_app_id"].textValue()
        discordAppSecret = root["discord_app_secret"].textValue()
        logger.info("Config: $this")
    }

    override fun toString(): String {
        return "Config(bannedUsers=$bannedUsers, discordAppId=$discordAppId, discordAppSecret=String(length=${discordAppSecret.length}))"
    }
}
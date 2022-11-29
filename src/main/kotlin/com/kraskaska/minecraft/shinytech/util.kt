package com.kraskaska.minecraft.shinytech

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.util.*

val quotes = listOf(
    "This memorial dedicated to those who perished on the climb.",
    "Get over here!",
    "The right man in the wrong place can make all the difference in the world.",
    "…*cocks shotgun*",
    "Finish him!",
    "We both said a lot of things that you're going to regret. But I think we can put our differences behind us. For science. You monster.",
    "Grass grows, birds fly, sun shines, and brother, I hurt people.",
    "Hey! Listen!",
    "I used to be an adventurer like you, until I took an arrow to the knee.",
    "It's super effective!",
    "Space. Space. I'm in space. SPAAAAAAACE!",
)

val backendName = listOf(
    "The Great Backend",
    "Simply Backend",
    "Backend",
    "Backend and an egg",
    "…*cocks shotgun*",
)

fun CommonMapper() = ObjectMapper().apply {
    registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
    configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
}

fun Pair<UUID, ShinyPassport>.writeToGenerator(generator: JsonGenerator) {
    generator.writeStartObject()
    generator.writeFieldName("UUID")
    generator.writeString(first.toString())
    if (second.minecraftUUID != null) {
        generator.writeFieldName("minecraftUUID")
        generator.writeString(second.minecraftUUID.toString())
    }
    if (second.minecraftName != null) {
        generator.writeFieldName("minecraftName")
        generator.writeString(second.minecraftName)
    }
    generator.writeFieldName("discordId")
    generator.writeNumber(second.discordId)
    generator.writeEndObject()
}

fun <T> safe(code: () -> T?) = try {
    code()
} catch (e: Exception) {
    null
}
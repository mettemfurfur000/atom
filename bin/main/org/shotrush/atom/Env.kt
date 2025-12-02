package org.shotrush.atom

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object Env {
    val DEV_ENVIRONMENT by optional("DEV_ENVIRONMENT").asBoolean()
}

//region Env Utilities

fun optional(name: String): OptionalBuilder = OptionalBuilder(name)

fun required(name: String): RequiredBuilder = RequiredBuilder(name)

/* ---------- Optional ---------- */

class OptionalBuilder(private val name: String) {
    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadOnlyProperty<Any?, String?> = optionalNullableDelegate(name) { it }

    infix fun default(value: String): ReadOnlyProperty<Any?, String> =
        optionalWithDefaultDelegate(name, value) { it }

    infix fun default(value: Boolean): ReadOnlyProperty<Any?, Boolean> =
        optionalWithDefaultDelegate(name, value) {
            it.equals("true", ignoreCase = true) || it == "1"
        }

    infix fun default(value: Int): ReadOnlyProperty<Any?, Int> =
        optionalWithDefaultDelegate(name, value) { it.toInt() }

    infix fun default(value: Long): ReadOnlyProperty<Any?, Long> =
        optionalWithDefaultDelegate(name, value) { it.toLong() }

    infix fun default(value: Double): ReadOnlyProperty<Any?, Double> =
        optionalWithDefaultDelegate(name, value) { it.toDouble() }

    fun asBoolean(): ReadOnlyProperty<Any?, Boolean?> =
        optionalNullableDelegate(name) { parseBoolean(name, it) }

    fun asInt(): ReadOnlyProperty<Any?, Int?> =
        optionalNullableDelegate(name) { it.toInt() }

    fun asLong(): ReadOnlyProperty<Any?, Long?> =
        optionalNullableDelegate(name) { it.toLong() }

    fun asDouble(): ReadOnlyProperty<Any?, Double?> =
        optionalNullableDelegate(name) { it.toDouble() }

    infix fun <T> map(parse: (String) -> T): ReadOnlyProperty<Any?, T?> =
        optionalNullableDelegate(name, parse)

    fun <T> defaultMapped(defaultValue: T, parse: (String) -> T)
            : ReadOnlyProperty<Any?, T> =
        optionalWithDefaultDelegate(name, defaultValue, parse)
}

private fun <T> optionalWithDefaultDelegate(
    name: String,
    defaultValue: T,
    parse: (String) -> T
): ReadOnlyProperty<Any?, T> {
    return object : ReadOnlyProperty<Any?, T> {
        val cached: T by lazy {
            val raw = System.getenv(name)?.trim()
            if (raw.isNullOrEmpty()) {
                defaultValue
            } else {
                runCatching { parse(raw) }.getOrElse { defaultValue }
            }
        }
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = cached
    }
}

private fun <T> optionalNullableDelegate(
    name: String,
    parse: (String) -> T
): ReadOnlyProperty<Any?, T?> {
    return object : ReadOnlyProperty<Any?, T?> {
        val cached: T? by lazy {
            val raw = System.getenv(name)?.trim()
            if (raw.isNullOrEmpty()) {
                null
            } else {
                runCatching { parse(raw) }.getOrNull()
            }
        }
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? = cached
    }
}

private fun parseBoolean(name: String, s: String): Boolean =
    when {
        s.equals("true", ignoreCase = true) || s == "1" -> true
        s.equals("false", ignoreCase = true) || s == "0" -> false
        else -> throw IllegalArgumentException(
            "Environment $name must be boolean (true/false/1/0), was: $s"
        )
    }

/* ---------- Required ---------- */

class RequiredBuilder(private val name: String) {
    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadOnlyProperty<Any?, String> = requiredDelegate(name) { it }

    infix fun <T> map(parse: (String) -> T): ReadOnlyProperty<Any?, T> =
        requiredDelegate(name, parse)

    fun asBoolean(): ReadOnlyProperty<Any?, Boolean> =
        requiredDelegate(name) { parseBoolean(name, it) }

    fun asInt(): ReadOnlyProperty<Any?, Int> =
        requiredDelegate(name) { it.toInt() }

    fun asLong(): ReadOnlyProperty<Any?, Long> =
        requiredDelegate(name) { it.toLong() }

    fun asDouble(): ReadOnlyProperty<Any?, Double> =
        requiredDelegate(name) { it.toDouble() }
}

private fun <T> requiredDelegate(
    name: String,
    parse: (String) -> T
): ReadOnlyProperty<Any?, T> {
    return object : ReadOnlyProperty<Any?, T> {
        val cached: T by lazy {
            val raw = System.getenv(name)
                ?: throw IllegalStateException(
                    "Missing required environment variable: $name"
                )
            val value = raw.trim()
            if (value.isEmpty()) {
                throw IllegalStateException("Required environment variable $name is blank")
            }
            parse(value)
        }
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = cached
    }
}

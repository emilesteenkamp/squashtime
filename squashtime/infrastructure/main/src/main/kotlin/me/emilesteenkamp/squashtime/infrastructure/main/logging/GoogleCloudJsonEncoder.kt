package me.emilesteenkamp.squashtime.infrastructure.main.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.encoder.EncoderBase
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Renders log events as single-line JSON using the field names Cloud Logging's structured
 * logging parser recognises (`timestamp`, `severity`, `message`), so Cloud Run can parse each
 * line into `jsonPayload` instead of a single opaque text blob. Key-value pairs attached via
 * SLF4J's fluent API (e.g. kotlin-logging's `payload`) are emitted under a nested `payload` field.
 */
class GoogleCloudJsonEncoder : EncoderBase<ILoggingEvent>() {
    override fun headerBytes(): ByteArray = ByteArray(0)

    override fun footerBytes(): ByteArray = ByteArray(0)

    override fun encode(event: ILoggingEvent): ByteArray {
        val json = buildString {
            append('{')
            appendField("timestamp", Instant.ofEpochMilli(event.timeStamp).toString())
            append(',')
            appendField("severity", event.level.toGoogleCloudSeverity())
            append(',')
            appendField("message", event.formattedMessage.orEmpty())
            append(',')
            appendField("logger", event.loggerName)
            append(',')
            appendField("thread", event.threadName)

            val keyValuePairs = event.keyValuePairs
            if (!keyValuePairs.isNullOrEmpty()) {
                append(',')
                append("\"payload\":{")
                keyValuePairs.forEachIndexed { index, pair ->
                    if (index > 0) append(',')
                    appendField(pair.key, pair.value?.toString() ?: "null")
                }
                append('}')
            }

            event.throwableProxy?.let { throwableProxy ->
                append(',')
                appendField("stackTrace", ThrowableProxyUtil.asString(throwableProxy))
            }

            append('}')
            append('\n')
        }

        return json.toByteArray(StandardCharsets.UTF_8)
    }

    private fun StringBuilder.appendField(name: String, value: String) {
        append(name.toJsonString())
        append(':')
        append(value.toJsonString())
    }

    private fun Level.toGoogleCloudSeverity(): String = when (this) {
        Level.ERROR -> "ERROR"
        Level.WARN -> "WARNING"
        Level.INFO -> "INFO"
        Level.DEBUG -> "DEBUG"
        Level.TRACE -> "DEBUG"
        else -> "DEFAULT"
    }

    private fun String.toJsonString(): String =
        buildString {
            append('"')
            for (char in this@toJsonString) {
                when (char) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
                }
            }
            append('"')
        }
}

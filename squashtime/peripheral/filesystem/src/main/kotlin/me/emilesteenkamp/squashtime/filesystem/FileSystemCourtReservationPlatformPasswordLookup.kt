package me.emilesteenkamp.squashtime.filesystem

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.tatarka.inject.annotations.Inject
import java.io.InputStream
import java.nio.charset.StandardCharsets

class FileSystemCourtReservationPlatformPasswordLookup
    @Inject
    constructor(
        credentialsInputStream: CourtReservationPlatformCredentialsInputStream,
        val yaml: Yaml,
    ) : CourtReservationPlatformPasswordLookup {
        val lookup = credentialsInputStream.parseLookup()

        override suspend fun find(playerUserName: Player.UserName): CourtReservationPlatform.Password? =
            lookup[playerUserName.value]?.let { CourtReservationPlatform.Password(it) }

        fun CourtReservationPlatformCredentialsInputStream.parseLookup(): Map<String, String> =
            yaml
                .decodeFromString<SerializedPasswordLookup>(use { readBytes().toString(StandardCharsets.UTF_8) })
                .credentials
                .fold(emptyMap()) { acc, credential ->
                    acc + (credential.username to credential.password)
                }

        @Serializable
        data class SerializedPasswordLookup(
            val credentials: List<Credential>,
        ) {
            @Serializable
            data class Credential(
                val username: String,
                val password: String,
            )
        }
    }

typealias CourtReservationPlatformCredentialsInputStream = InputStream

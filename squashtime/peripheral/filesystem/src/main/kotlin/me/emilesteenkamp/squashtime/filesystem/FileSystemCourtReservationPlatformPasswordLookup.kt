package me.emilesteenkamp.squashtime.filesystem

import com.charleskorn.kaml.Yaml
import java.io.File
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup

class FileSystemCourtReservationPlatformPasswordLookup(
    file: File,
    val yaml: Yaml
) : CourtReservationPlatformPasswordLookup {
    val lookup = file.parseLookup()

    override suspend fun find(playerUserName: Player.UserName): CourtReservationPlatform.Password? =
        lookup[playerUserName.value]?.let { CourtReservationPlatform.Password(it) }

    fun File.parseLookup(): Map<String, String> = yaml
        .decodeFromString<SerializedPasswordLookup>(readText())
        .credentials
        .fold(emptyMap()) {
            acc, credential -> acc + (credential.username to credential.password)
        }

    @Serializable
    data class SerializedPasswordLookup(
        val credentials: List<Credential>
    ) {
        @Serializable
        data class Credential(
            val username: String,
            val password: String
        )
    }
}

suspend fun main() {
    val file = FileSystemCourtReservationPlatformPasswordLookup::class.java
        .getResource("/squash-city-credentials-lookup.yaml")!!
        .toFile()


    FileSystemCourtReservationPlatformPasswordLookup(
        file = file,
        yaml = Yaml.default
    ).also { println(it.find(Player.UserName("emile@steenkamps.org"))!!.value) }
}

fun URL.toFile(): File = File(this.file)
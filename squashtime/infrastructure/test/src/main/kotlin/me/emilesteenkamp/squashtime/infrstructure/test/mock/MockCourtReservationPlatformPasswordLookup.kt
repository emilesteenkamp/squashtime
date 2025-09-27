package me.emilesteenkamp.squashtime.infrstructure.test.mock

import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup

object MockCourtReservationPlatformPasswordLookup : CourtReservationPlatformPasswordLookup {
    val playerUserNameToPasswordMap = mapOf(
        Player.UserName("user1") to CourtReservationPlatform.Password("password1")
    )

    override suspend fun find(playerUserName: Player.UserName): CourtReservationPlatform.Password? {
        return playerUserNameToPasswordMap[playerUserName]
    }
}
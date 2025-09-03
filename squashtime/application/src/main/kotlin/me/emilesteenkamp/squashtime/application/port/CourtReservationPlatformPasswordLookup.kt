package me.emilesteenkamp.squashtime.application.port

import me.emilesteenkamp.squashtime.application.domain.Player

interface CourtReservationPlatformPasswordLookup {
    suspend fun find(playerUserName: Player.UserName): CourtReservationPlatform.Password?
}
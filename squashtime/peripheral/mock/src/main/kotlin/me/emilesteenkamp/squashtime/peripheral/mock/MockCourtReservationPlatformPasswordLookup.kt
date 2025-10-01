package me.emilesteenkamp.squashtime.peripheral.mock

import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.infrastructure.scope.testscoped.TestScoped
import me.tatarka.inject.annotations.Inject

class MockCourtReservationPlatformPasswordLookup
    @Inject
    constructor(
        private val dataSource: DataSource,
    ) : CourtReservationPlatformPasswordLookup {
        override suspend fun find(playerUserName: Player.UserName): CourtReservationPlatform.Password? =
            dataSource.playerUserNameToPasswordMap[playerUserName]

        @TestScoped
        class DataSource
            @Inject
            constructor() {
                val playerUserNameToPasswordMap =
                    mapOf(
                        Player.UserName("user1") to CourtReservationPlatform.Password("password1"),
                    )
            }
    }

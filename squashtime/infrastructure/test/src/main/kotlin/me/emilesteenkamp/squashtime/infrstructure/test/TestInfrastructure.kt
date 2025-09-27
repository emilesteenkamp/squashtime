package me.emilesteenkamp.squashtime.infrstructure.test

import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.infrstructure.test.mock.MockCourtReservationPlatform
import me.emilesteenkamp.squashtime.infrstructure.test.mock.MockCourtReservationPlatformPasswordLookup
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class TestInfrastructure {
    abstract val reserveCourtUseCase: ReserveCourtUseCase

    @Provides
    protected fun courtReservationPlatform(): CourtReservationPlatform = MockCourtReservationPlatform

    @Provides
    protected fun courtReservationPlatformPasswordLookup(): CourtReservationPlatformPasswordLookup =
        MockCourtReservationPlatformPasswordLookup
}